package com.coach.plugin.coaching;

import com.coach.plugin.encounter.ActiveEncounter;
import com.coach.plugin.encounter.Bosses;
import com.coach.plugin.encounter.model.MechanicDefinition;
import com.coach.plugin.encounter.model.TriggerDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PredictionEngineTest
{
	private final PredictionEngine engine = new PredictionEngine();

	private static ActiveEncounter sessionWithMechanic(MechanicDefinition mechanic)
	{
		com.coach.plugin.encounter.model.BossDefinition boss = Bosses.threePhase();
		boss.phases.get(0).mechanics = List.of(mechanic);
		return new ActiveEncounter(boss, 11278, "p1", 100);
	}

	@Test
	void predictsTickTimerMechanics()
	{
		MechanicDefinition periodic = new MechanicDefinition();
		periodic.mechanicId = "periodic";
		TriggerDefinition timer = new TriggerDefinition();
		timer.type = "tick_timer";
		timer.tickMod = 4;
		periodic.triggers = List.of(timer);

		ActiveEncounter session = sessionWithMechanic(periodic);
		session.setGlobalTick(101); // as the TICK batch would
		// phaseTick = 101 - 100 = 1 -> next fire in 3 ticks
		List<PredictedMechanic> predictions = engine.predict(List.of(session), 101);

		assertEquals(1, predictions.size());
		assertEquals(3, predictions.get(0).getTicksUntilFire());
		assertEquals("periodic", predictions.get(0).getMechanicId());
	}

	@Test
	void eventDrivenMechanicsAreNotPredicted()
	{
		MechanicDefinition anim = new MechanicDefinition();
		anim.mechanicId = "anim_only";
		TriggerDefinition animation = new TriggerDefinition();
		animation.type = "animation";
		anim.triggers = List.of(animation);

		assertNull(PredictionEngine.predictMechanic(anim, 5));
		assertTrue(engine.predict(List.of(sessionWithMechanic(anim)), 100).isEmpty());
	}

	@Test
	void respectsOffsetAndHorizon()
	{
		MechanicDefinition offsetFar = new MechanicDefinition();
		offsetFar.mechanicId = "far";
		TriggerDefinition timer = new TriggerDefinition();
		timer.type = "tick_timer";
		timer.tickMod = 50;
		offsetFar.triggers = List.of(timer);

		// phaseTick=1 -> eta 49 ticks: beyond horizon of 10
		assertTrue(engine.predict(List.of(sessionWithMechanic(offsetFar)), 101).isEmpty());

		Integer eta = PredictionEngine.predictMechanic(offsetFar, 1);
		assertEquals(49, eta);
	}

	@Test
	void emptySessionsYieldNoPredictions()
	{
		assertTrue(engine.predict(List.of(), 100).isEmpty());
		assertTrue(engine.predict(null, 100).isEmpty());
	}
}
