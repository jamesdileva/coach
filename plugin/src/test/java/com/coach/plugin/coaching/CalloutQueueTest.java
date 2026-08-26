package com.coach.plugin.coaching;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalloutQueueTest
{
	private final CalloutQueue queue = new CalloutQueue();

	private static CalloutRequest request(String id, int dueTick, int priority)
	{
		com.coach.plugin.encounter.model.CalloutDefinition callout =
			new com.coach.plugin.encounter.model.CalloutDefinition();
		callout.calloutId = id;
		callout.text = id;
		callout.category = "critical";
		return new CalloutRequest("b", "m", callout, priority, dueTick, dueTick);
	}

	@Test
	void drainsInTickOrderThenPriority()
	{
		queue.enqueue(request("late-low", 10, 10));
		queue.enqueue(request("early", 5, 10));
		queue.enqueue(request("same-high", 10, 90));

		List<CalloutRequest> dueAt5 = queue.drainDue(5);
		assertEquals(1, dueAt5.size());
		assertEquals("early", dueAt5.get(0).getCallout().calloutId);

		List<CalloutRequest> dueAt10 = queue.drainDue(10);
		assertEquals(2, dueAt10.size());
		assertEquals("same-high", dueAt10.get(0).getCallout().calloutId, "higher priority first at same tick");
		assertEquals("late-low", dueAt10.get(1).getCallout().calloutId);

		assertEquals(0, queue.drainDue(11).size());
	}

	@Test
	void nothingDrainsBeforeDue()
	{
		queue.enqueue(request("x", 20, 50));
		assertEquals(0, queue.drainDue(19).size());
		assertEquals(1, queue.size());
		assertEquals(1, queue.drainDue(20).size());
	}
}
