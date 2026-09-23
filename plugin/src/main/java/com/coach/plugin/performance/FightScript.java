package com.coach.plugin.performance;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parses fight-script JSON:
 * {
 *   "endTick": 100,
 *   "events": [
 *     {"tick": 1, "type": "npc_spawn", "npcId": 11278},
 *     {"tick": 5, "type": "animation", "npcId": 11278, "animationId": 8960},
 *     {"tick": 7, "type": "projectile", "projectId": 2955},
 *     {"tick": 10, "type": "shout", "text": "Fear the shadow"},
 *     {"tick": 12, "type": "hp", "npcId": 11278, "hp": 79, "direction": "below"}
 *   ]
 * }
 */
public final class FightScript
{
	private static final Gson GSON = new Gson();

	public static final class ScriptEvent
	{
		private final int tick;
		private final String type;
		private final int npcId;
		private final Integer animationId;
		private final Integer projectId;
		private final Integer graphicId;
		private final Integer hp;
		private final String hpDirection;
		private final String text;

		ScriptEvent(int tick, String type, int npcId, Integer animationId,
			Integer projectId, Integer graphicId, Integer hp, String hpDirection, String text)
		{
			this.tick = tick;
			this.type = type;
			this.npcId = npcId;
			this.animationId = animationId;
			this.projectId = projectId;
			this.graphicId = graphicId;
			this.hp = hp;
			this.hpDirection = hpDirection;
			this.text = text;
		}

		public int getTick()
		{
			return tick;
		}

		public String getType()
		{
			return type;
		}

		public int getNpcId()
		{
			return npcId;
		}

		public Integer getAnimationId()
		{
			return animationId;
		}

		public Integer getProjectileId()
		{
			return projectId;
		}

		public Integer getGraphicId()
		{
			return graphicId;
		}

		public Integer getHp()
		{
			return hp;
		}

		public String getHpDirection()
		{
			return hpDirection;
		}

		public String getText()
		{
			return text;
		}
	}

	private final List<ScriptEvent> events;
	private final int endTick;

	FightScript(List<ScriptEvent> events, int endTick)
	{
		this.events = Collections.unmodifiableList(events);
		this.endTick = endTick;
	}

	public List<ScriptEvent> getEvents()
	{
		return events;
	}

	public int getEndTick()
	{
		return endTick;
	}

	public static FightScript fromJson(String json)
	{
		JsonObject root;
		try
		{
			root = GSON.fromJson(json, JsonObject.class);
		}
		catch (RuntimeException e)
		{
			throw new IllegalArgumentException("invalid fight script JSON: " + e.getMessage(), e);
		}
		if (root == null)
		{
			throw new IllegalArgumentException("empty fight script");
		}
		int endTick = root.has("endTick") ? root.get("endTick").getAsInt() : 0;
		JsonArray arr = root.getAsJsonArray("events");
		if (arr == null)
		{
			throw new IllegalArgumentException("fight script missing events array");
		}
		List<ScriptEvent> events = new ArrayList<>(arr.size());
		for (JsonElement el : arr)
		{
			JsonObject o = el.getAsJsonObject();
			events.add(new ScriptEvent(
				o.get("tick").getAsInt(),
				o.get("type").getAsString(),
				o.has("npcId") ? o.get("npcId").getAsInt() : 0,
				o.has("animationId") ? o.get("animationId").getAsInt() : null,
				o.has("projectId") ? o.get("projectId").getAsInt() : null,
				o.has("graphicId") ? o.get("graphicId").getAsInt() : null,
				o.has("hp") ? o.get("hp").getAsInt() : null,
				o.has("direction") ? o.get("direction").getAsString()
					: (o.has("hpDirection") ? o.get("hpDirection").getAsString() : null),
				o.has("text") ? o.get("text").getAsString() : null));
		}
		events.sort((a, b) -> Integer.compare(a.tick, b.tick));
		if (endTick <= 0)
		{
			endTick = events.isEmpty() ? 0 : events.get(events.size() - 1).tick;
		}
		return new FightScript(events, endTick);
	}

	public static FightScript fromFile(Path file) throws IOException
	{
		return fromJson(Files.readString(file));
	}
}
