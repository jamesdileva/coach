package com.coach.plugin.events;

import com.coach.plugin.model.BossState;
import com.coach.plugin.model.PlayerState;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;

/**
 * Translates RuneLite client objects into internal state models.
 * Stateless — pass in the objects you already have; safe to call from any handler.
 */
public class GameStateBridge
{
	public PlayerState getPlayerState(Client client)
	{
		Player player = client.getLocalPlayer();
		WorldPoint pos = player.getWorldLocation();
		return new PlayerState(
			client.getBoostedSkillLevel(Skill.HITPOINTS),
			client.getRealSkillLevel(Skill.HITPOINTS),
			pos.getX(),
			pos.getY(),
			pos.getPlane(),
			player.getAnimation());
	}

	public BossState getBossState(NPC npc)
	{
		WorldPoint pos = npc.getWorldLocation();
		return new BossState(
			npc.getId(),
			npc.getName(),
			npc.getHealthRatio(),
			npc.getHealthScale(),
			pos.getX(),
			pos.getY(),
			pos.getPlane(),
			npc.getAnimation());
	}

	public boolean isBoss(Actor actor, int npcId)
	{
		return actor instanceof NPC && ((NPC) actor).getId() == npcId;
	}

	/**
	 * Find a live NPC by id in the top-level world view, or null.
	 */
	public static NPC findNpc(Client client, int npcId)
	{
		if (client == null)
		{
			return null;
		}
		for (NPC candidate : client.getTopLevelWorldView().npcs())
		{
			if (candidate.getId() == npcId)
			{
				return candidate;
			}
		}
		return null;
	}
}
