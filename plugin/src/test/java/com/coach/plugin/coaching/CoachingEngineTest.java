package com.coach.plugin.coaching;

import com.coach.plugin.encounter.MechanicActivation;
import com.coach.plugin.encounter.model.CalloutDefinition;
import com.coach.plugin.encounter.model.MechanicDefinition;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoachingEngineTest
{
	private CoachingEngine engine;
	private List<CoachingEngine.DeliveredCallout> delivered;

	@BeforeEach
	void setUp()
	{
		engine = new CoachingEngine();
		delivered = new ArrayList<>();
		engine.addListener(d -> delivered.add(d));
	}

	private static CalloutDefinition callout(String id, String category, int priority,
		int visualOffset, int audioOffset)
	{
		CalloutDefinition callout = new CalloutDefinition();
		callout.calloutId = id;
		callout.text = id;
		callout.category = category;
		callout.priority = priority;
		callout.visualOffset = visualOffset;
		callout.audioOffset = audioOffset;
		return callout;
	}

	private static MechanicActivation activation(int tick, CalloutDefinition... callouts)
	{
		MechanicDefinition mechanic = new MechanicDefinition();
		mechanic.mechanicId = "m1";
		mechanic.name = "M1";
		mechanic.callouts = List.of(callouts);
		return new MechanicActivation(tick, "boss", "p1", mechanic, mechanic.callouts);
	}

	@Test
	void schedulesAtTickOffsetsAndDeliversWhenDue()
	{
		// audio due t=98 (offset -2), visual due t=100 (offset 0); delivery = min
		engine.onActivation(activation(100, callout("pray", "critical", 90, 0, -2)));

		assertEquals(0, engine.onTick(97), "not yet due");
		assertEquals(1, engine.onTick(98), "fires 2 ticks early via negative audio offset");
		assertEquals(0, engine.onTick(99), "already delivered");
	}

	@Test
	void duplicateWithinCooldownSuppressed()
	{
		CalloutDefinition c = callout("dup", "info", 50, 0, 0);
		engine.onActivation(activation(10, c));
		engine.onTick(10);

		engine.onActivation(activation(12, c)); // within default 4-tick window
		engine.onTick(12);
		assertEquals(1, delivered.size(), "duplicate suppressed");

		engine.onActivation(activation(15, c)); // cooldown expired
		engine.onTick(15);
		assertEquals(2, delivered.size());
	}

	@Test
	void disabledFilterBlocksCallouts()
	{
		engine.setEnabledFilter(c -> !c.calloutId.equals("muted"));
		engine.onActivation(activation(1,
			callout("kept", "critical", 90, 0, 0),
			callout("muted", "warning", 70, 0, 0)));
		engine.onTick(1);

		assertEquals(1, delivered.size());
		assertEquals("kept", delivered.get(0).getCallout().calloutId);
	}

	@Test
	void concurrentCalloutsQueueWithoutOverlap()
	{
		// three activations same tick -> all scheduled, delivered in priority order
		engine.onActivation(activation(50, callout("low", "transition", 30, 2, 2)));
		engine.onActivation(activation(50, callout("high", "critical", 95, 2, 2)));
		engine.onActivation(activation(50, callout("mid", "warning", 60, 2, 2)));

		assertEquals(3, engine.onTick(52));
		assertEquals("high", delivered.get(0).getCallout().calloutId);
		assertEquals("mid", delivered.get(1).getCallout().calloutId);
		assertEquals("low", delivered.get(2).getCallout().calloutId);
	}

	private List<CoachingEngine.DeliveredCallout> drain()
	{
		List<CoachingEngine.DeliveredCallout> copy = new ArrayList<>(delivered);
		delivered.clear();
		return copy;
	}
}
