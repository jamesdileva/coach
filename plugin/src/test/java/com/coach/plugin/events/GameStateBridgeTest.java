package com.coach.plugin.events;

import com.coach.plugin.model.BossState;
import com.coach.plugin.model.PlayerState;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GameStateBridgeTest
{
	private GameStateBridge bridge;

	@BeforeEach
	void setUp()
	{
		bridge = new GameStateBridge();
	}

	@Test
	void extractsPlayerHpPositionAnimation()
	{
		Client client = mock(Client.class);
		Player player = mock(Player.class);

		when(client.getBoostedSkillLevel(Skill.HITPOINTS)).thenReturn(73);
		when(client.getRealSkillLevel(Skill.HITPOINTS)).thenReturn(99);
		when(client.getLocalPlayer()).thenReturn(player);
		when(player.getWorldLocation()).thenReturn(new WorldPoint(3222, 3218, 0));
		when(player.getAnimation()).thenReturn(827);

		PlayerState state = bridge.getPlayerState(client);

		assertEquals(73, state.getHp());
		assertEquals(99, state.getMaxHp());
		assertEquals(3222, state.getPosX());
		assertEquals(3218, state.getPosY());
		assertEquals(0, state.getPlane());
		assertEquals(827, state.getAnimation());
	}

	@Test
	void extractsBossIdNameHealthAndPosition()
	{
		NPC npc = mock(NPC.class);

		when(npc.getId()).thenReturn(11278);
		when(npc.getName()).thenReturn("Nex");
		when(npc.getHealthRatio()).thenReturn(42);
		when(npc.getHealthScale()).thenReturn(100);
		when(npc.getWorldLocation()).thenReturn(new WorldPoint(2965, 5204, 0));
		when(npc.getAnimation()).thenReturn(-1);

		BossState state = bridge.getBossState(npc);

		assertEquals(11278, state.getNpcId());
		assertEquals("Nex", state.getName());
		assertEquals(42, state.getHealthRatio());
		assertEquals(100, state.getHealthScale());
		assertEquals(2965, state.getPosX());
		assertEquals(5204, state.getPosY());
		assertEquals(-1, state.getAnimation());
	}

	@Test
	void isBossMatchesOnlySameNpcId()
	{
		NPC npc = mock(NPC.class);
		when(npc.getId()).thenReturn(11278);
		when(npc.getName()).thenReturn("Nex");
		when(npc.getHealthRatio()).thenReturn(1);
		when(npc.getHealthScale()).thenReturn(1);
		when(npc.getWorldLocation()).thenReturn(new WorldPoint(1, 1, 0));
		when(npc.getAnimation()).thenReturn(-1);

		assertFalse(bridge.isBoss(npc, 2054));
		assertTrue(bridge.isBoss(npc, 11278));
	}
}
