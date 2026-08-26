package com.coach.plugin.trigger;

import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import net.runelite.api.ChatMessageType;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShoutTriggerEvaluatorTest
{
	private static GameEvent chat(String message)
	{
		ChatMessage chatMessage = new ChatMessage();
		chatMessage.setName("Nex");
		chatMessage.setMessage(message);
		chatMessage.setType(ChatMessageType.GAMEMESSAGE);
		return new GameEvent(EventType.CHAT_MESSAGE, 1, chatMessage);
	}

	@Test
	void matchesCaseInsensitiveSubstring()
	{
		ShoutTriggerEvaluator evaluator = new ShoutTriggerEvaluator("FEAR THE SHADOW", null);

		assertTrue(evaluator.matches(chat("Nex: Fear the shadow!")));
		assertTrue(evaluator.matches(chat("fear the shadow")));
		assertFalse(evaluator.matches(chat("Fear the dark beast!")));
	}

	@Test
	void senderFilterApplies()
	{
		ShoutTriggerEvaluator filtered = new ShoutTriggerEvaluator("contain this", "Nex");

		assertTrue(filtered.matches(chat("Contain this!")));
		assertFalse(filtered.matches(chatFromOther("Contain this!")));
	}

	@Test
	void nonChatEventsNeverMatch()
	{
		assertFalse(new ShoutTriggerEvaluator("anything", null)
			.matches(new GameEvent(EventType.TICK, 1, null)));
	}

	private static GameEvent chatFromOther(String message)
	{
		ChatMessage chatMessage = new ChatMessage();
		chatMessage.setName("SomePlayer");
		chatMessage.setMessage(message);
		return new GameEvent(EventType.CHAT_MESSAGE, 1, chatMessage);
	}

	@Test
	void interestIsChatOnly()
	{
		assertEquals(java.util.Set.of(EventType.CHAT_MESSAGE),
			new ShoutTriggerEvaluator("x", null).interestedIn());
	}
}
