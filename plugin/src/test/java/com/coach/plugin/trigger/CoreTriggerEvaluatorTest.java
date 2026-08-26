package com.coach.plugin.trigger;

import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import net.runelite.api.NPC;
import net.runelite.api.events.AnimationChanged;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CoreTriggerEvaluatorTest
{
	private static GameEvent event(EventType type, Object payload)
	{
		return new GameEvent(type, 100, payload);
	}

	private static AnimationChanged animation(NPC npc)
	{
		AnimationChanged changed = new AnimationChanged();
		changed.setActor(npc);
		return changed;
	}

	@Test
	void animationTriggerMatchesNpcAndAnimation()
	{
		NPC npc = mock(NPC.class);
		when(npc.getId()).thenReturn(11278);
		when(npc.getAnimation()).thenReturn(8960);

		AnimationTriggerEvaluator evaluator = new AnimationTriggerEvaluator(11278, 8960);

		assertTrue(evaluator.matches(event(EventType.ANIMATION_CHANGED, animation(npc))));
	}

	@Test
	void animationTriggerRejectsWrongNpc()
	{
		NPC npc = mock(NPC.class);
		when(npc.getId()).thenReturn(11111);
		when(npc.getAnimation()).thenReturn(8960);

		AnimationTriggerEvaluator evaluator = new AnimationTriggerEvaluator(11278, 8960);

		assertFalse(evaluator.matches(event(EventType.ANIMATION_CHANGED, animation(npc))));
	}

	@Test
	void animationTriggerRejectsWrongAnimation()
	{
		NPC npc = mock(NPC.class);
		when(npc.getId()).thenReturn(11278);
		when(npc.getAnimation()).thenReturn(-1);

		assertFalse(new AnimationTriggerEvaluator(11278, 8960)
			.matches(event(EventType.ANIMATION_CHANGED, animation(npc))));
	}

	@Test
	void animationTriggerWithNullNpcMatchesAnyNpc()
	{
		NPC npc = mock(NPC.class);
		when(npc.getId()).thenReturn(1);
		when(npc.getAnimation()).thenReturn(42);

		assertTrue(new AnimationTriggerEvaluator(null, 42)
			.matches(event(EventType.ANIMATION_CHANGED, animation(npc))));
	}

	@Test
	void projectileTriggerMatchesIdOnly()
	{
		net.runelite.api.Projectile projectile = mock(net.runelite.api.Projectile.class);
		when(projectile.getId()).thenReturn(2955);
		net.runelite.api.events.ProjectileMoved moved = new net.runelite.api.events.ProjectileMoved();
		moved.setProjectile(projectile);

		assertTrue(new ProjectileTriggerEvaluator(2955, null)
			.matches(event(EventType.PROJECTILE_MOVED, moved)));
		assertFalse(new ProjectileTriggerEvaluator(2001, null)
			.matches(event(EventType.PROJECTILE_MOVED, moved)));
	}

	@Test
	void graphicTriggerMatchesGraphicsObjectCreated()
	{
		net.runelite.api.GraphicsObject graphicsObject = mock(net.runelite.api.GraphicsObject.class);
		when(graphicsObject.getId()).thenReturn(1832);
		net.runelite.api.events.GraphicsObjectCreated created =
			new net.runelite.api.events.GraphicsObjectCreated(graphicsObject);

		GraphicTriggerEvaluator evaluator = new GraphicTriggerEvaluator(null, 1832);

		assertTrue(evaluator.matches(event(EventType.GRAPHICS_OBJECT_CREATED, created)));
		assertFalse(new GraphicTriggerEvaluator(null, 999)
			.matches(event(EventType.GRAPHICS_OBJECT_CREATED, created)));
	}

	@Test
	void graphicTriggerMatchesActorGraphicWithNpcFilter()
	{
		NPC npc = mock(NPC.class);
		when(npc.getId()).thenReturn(11278);
		when(npc.getGraphic()).thenReturn(83);

		GraphicTriggerEvaluator evaluator = new GraphicTriggerEvaluator(11278, 83);

		assertTrue(evaluator.matches(event(EventType.GRAPHIC_CHANGED,
			graphicChanged(npc))));
		assertFalse(new GraphicTriggerEvaluator(5555, 83)
			.matches(event(EventType.GRAPHIC_CHANGED, graphicChanged(npc))));
	}

	@Test
	void evaluatorsDeclareCorrectInterest()
	{
		assertTrue(new AnimationTriggerEvaluator(1, 1).interestedIn()
			.contains(EventType.ANIMATION_CHANGED));
		assertTrue(new ProjectileTriggerEvaluator(1, null).interestedIn()
			.contains(EventType.PROJECTILE_MOVED));
		assertTrue(new GraphicTriggerEvaluator(1, 1).interestedIn()
			.containsAll(java.util.Set.of(EventType.GRAPHIC_CHANGED, EventType.GRAPHICS_OBJECT_CREATED)));

		// no cross-matching
		assertFalse(new AnimationTriggerEvaluator(1, 1).interestedIn()
			.contains(EventType.PROJECTILE_MOVED));
	}

	private static net.runelite.api.events.GraphicChanged graphicChanged(NPC npc)
	{
		net.runelite.api.events.GraphicChanged changed = new net.runelite.api.events.GraphicChanged();
		changed.setActor(npc);
		return changed;
	}
}
