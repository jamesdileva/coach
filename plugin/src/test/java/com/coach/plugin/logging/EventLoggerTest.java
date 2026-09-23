package com.coach.plugin.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.coach.plugin.events.EventBus;
import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import java.util.List;
import net.runelite.api.Actor;
import net.runelite.api.GraphicsObject;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Projectile;
import net.runelite.api.Skill;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import org.junit.jupiter.api.Test;

class EventLoggerTest
{
	private static GameEvent event(EventType type, int tick, Object payload)
	{
		return new GameEvent(type, tick, payload);
	}

	@Test
	void onTickBatchLogsEachEvent()
	{
		LogBuffer buffer = new LogBuffer();
		EventLogger logger = new EventLogger(buffer);
		logger.onTickBatch(3, List.of(
			event(EventType.TICK, 3, null),
			event(EventType.CHAT_MESSAGE, 3, "payload")));
		assertEquals(2, buffer.size());
		assertTrue(buffer.snapshot().get(0).startsWith("t3 "));
	}

	@Test
	void summarizesNullPayloadAndDefault()
	{
		assertEquals("", EventLogger.summarize(event(EventType.TICK, 1, null)));
		assertEquals("String", EventLogger.summarize(event(EventType.CHAT_MESSAGE, 1, "x")));
		assertEquals("Integer", EventLogger.summarize(event(EventType.CHAT_MESSAGE, 1, Integer.valueOf(5))));
	}

	@Test
	void summarizesAnimationGraphicAndGraphicsObject()
	{
		Actor actor = mock(Actor.class);
		when(actor.getName()).thenReturn("Nex");
		when(actor.getAnimation()).thenReturn(8960);
		AnimationChanged anim = mock(AnimationChanged.class);
		when(anim.getActor()).thenReturn(actor);
		assertTrue(EventLogger.summarize(event(EventType.ANIMATION_CHANGED, 1, anim)).contains("anim=8960"));

		when(actor.getGraphic()).thenReturn(100);
		GraphicChanged graphic = mock(GraphicChanged.class);
		when(graphic.getActor()).thenReturn(actor);
		assertEquals("Nex graphic=100", EventLogger.summarize(event(EventType.GRAPHIC_CHANGED, 1, graphic)));

		Actor anonymous = mock(Actor.class);
		when(anonymous.getName()).thenReturn(null);
		AnimationChanged noName = mock(AnimationChanged.class);
		when(noName.getActor()).thenReturn(anonymous);
		String summary = EventLogger.summarize(event(EventType.ANIMATION_CHANGED, 1, noName));
		assertTrue(summary.startsWith("actor@"));
		assertTrue(EventLogger.summarize(event(EventType.ANIMATION_CHANGED, 1, anim)).contains("actor=null") == false);

		GraphicsObject go = mock(GraphicsObject.class);
		when(go.getId()).thenReturn(123);
		GraphicsObjectCreated created = new GraphicsObjectCreated(go);
		assertEquals("id=123", EventLogger.summarize(event(EventType.GRAPHICS_OBJECT_CREATED, 1, created)));

		AnimationChanged nullActor = mock(AnimationChanged.class);
		when(nullActor.getActor()).thenReturn(null);
		assertEquals("actor=null", EventLogger.summarize(event(EventType.ANIMATION_CHANGED, 1, nullActor)));
	}

	@Test
	void summarizesProjectileNpcStatVarbitItem()
	{
		Projectile projectile = mock(Projectile.class);
		when(projectile.getId()).thenReturn(2955);
		ProjectileMoved moved = mock(ProjectileMoved.class);
		when(moved.getProjectile()).thenReturn(projectile);
		assertEquals("projectId=2955", EventLogger.summarize(event(EventType.PROJECTILE_MOVED, 1, moved)));

		NPC npc = mock(NPC.class);
		when(npc.getId()).thenReturn(11278);
		when(npc.getName()).thenReturn("Nex");
		NpcSpawned spawn = new NpcSpawned(npc);
		assertEquals("npcId=11278 name=Nex", EventLogger.summarize(event(EventType.NPC_SPAWNED, 1, spawn)));

		NpcDespawned despawn = new NpcDespawned(npc);
		assertEquals("npcId=11278 name=Nex", EventLogger.summarize(event(EventType.NPC_DESPAWNED, 1, despawn)));

		StatChanged stat = new StatChanged(Skill.HITPOINTS, 0, 99, 50);
		assertEquals("HITPOINTS=50/99", EventLogger.summarize(event(EventType.PLAYER_STATS_CHANGED, 1, stat)));

		VarbitChanged varbit = mock(VarbitChanged.class);
		when(varbit.getVarbitId()).thenReturn(100);
		when(varbit.getValue()).thenReturn(1);
		assertEquals("varbit=100 value=1", EventLogger.summarize(event(EventType.VARBIT_CHANGED, 1, varbit)));

		ItemContainerChanged item = new ItemContainerChanged(93, null);
		assertEquals("container=93", EventLogger.summarize(event(EventType.ITEM_CONTAINER_CHANGED, 1, item)));
	}

	@Test
	void formatIncludesTickTypeAndSummary()
	{
		EventLogger logger = new EventLogger(new LogBuffer());
		assertEquals("t7 CHAT_MESSAGE String", logger.format(event(EventType.CHAT_MESSAGE, 7, "hi")));
	}

	@Test
	void formatToleratesBrokenPayload()
	{
		BrokenPayload payload = new BrokenPayload();
		assertEquals("BrokenPayload", EventLogger.summarize(event(EventType.ANIMATION_CHANGED, 1, payload)));
	}

	static class BrokenPayload
	{
		@SuppressWarnings("unused")
		public Object getActor()
		{
			throw new IllegalStateException("boom");
		}
	}
}
