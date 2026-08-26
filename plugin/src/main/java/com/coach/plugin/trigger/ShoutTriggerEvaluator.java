package com.coach.plugin.trigger;

import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import java.util.Locale;
import java.util.Set;
import net.runelite.api.events.ChatMessage;

/**
 * Fires when a chat message contains the configured text (case-insensitive).
 *
 * This is the primary telegraph channel for shout-based boss mechanics —
 * e.g. every Nex special attack is announced by a shout ("Fear the shadow!").
 */
public class ShoutTriggerEvaluator implements TriggerEvaluator
{
	private final String containsText;
	private final String senderFilter; // optional; null = any sender

	public ShoutTriggerEvaluator(String containsText, String senderFilter)
	{
		this.containsText = containsText == null ? "" : containsText.toLowerCase(Locale.ROOT);
		this.senderFilter = senderFilter;
	}

	@Override
	public Set<EventType> interestedIn()
	{
		return Set.of(EventType.CHAT_MESSAGE);
	}

	@Override
	public boolean matches(GameEvent event)
	{
		Object payload = event.getPayload();
		if (!(payload instanceof ChatMessage))
		{
			return false;
		}
		ChatMessage message = (ChatMessage) payload;
		if (senderFilter != null && !senderFilter.equalsIgnoreCase(message.getName()))
		{
			return false;
		}
		String body = message.getMessage();
		return body != null && body.toLowerCase(Locale.ROOT).contains(containsText);
	}

	@Override
	public String describe()
	{
		return "shout \"" + containsText + "\""
			+ (senderFilter != null ? " from " + senderFilter : "");
	}
}
