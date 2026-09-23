package com.coach.plugin.trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coach.plugin.encounter.model.TriggerDefinition;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TriggerRegistryAndEdgeTest
{
	@Test
	void edgeDetectorFiresOnlyOnRisingEdge()
	{
		EdgeDetector edge = new EdgeDetector();
		assertFalse(edge.onNext(false));
		assertTrue(edge.onNext(true));
		assertFalse(edge.onNext(true));
		assertFalse(edge.onNext(false));
		assertTrue(edge.onNext(true));
		edge.reset();
		assertTrue(edge.onNext(true));
	}

	@Test
	void registryRejectsNullUnknownAndMissingFields()
	{
		TriggerRegistry registry = new TriggerRegistry(null);
		assertFalse(registry.create(null).isPresent());
		assertFalse(registry.create(new TriggerDefinition()).isPresent());

		TriggerDefinition unknown = new TriggerDefinition();
		unknown.type = "not_a_type";
		assertFalse(registry.create(unknown).isPresent());

		TriggerDefinition custom = new TriggerDefinition();
		custom.type = "custom";
		assertFalse(registry.create(custom).isPresent());

		TriggerDefinition animation = new TriggerDefinition();
		animation.type = "animation";
		assertFalse(registry.create(animation).isPresent());

		TriggerDefinition projectile = new TriggerDefinition();
		projectile.type = "projectile";
		assertFalse(registry.create(projectile).isPresent());

		TriggerDefinition graphic = new TriggerDefinition();
		graphic.type = "graphic";
		assertFalse(registry.create(graphic).isPresent());

		TriggerDefinition wave = new TriggerDefinition();
		wave.type = "wave_cleared";
		assertFalse(registry.create(wave).isPresent());

		TriggerDefinition shout = new TriggerDefinition();
		shout.type = "shout";
		assertFalse(registry.create(shout).isPresent());

		TriggerDefinition hp = new TriggerDefinition();
		hp.type = "hp";
		assertFalse(registry.create(hp).isPresent());

		TriggerDefinition tick = new TriggerDefinition();
		tick.type = "tick_timer";
		assertFalse(registry.create(tick).isPresent());

		TriggerDefinition player = new TriggerDefinition();
		player.type = "player_state";
		assertFalse(registry.create(player).isPresent());

		TriggerDefinition composite = new TriggerDefinition();
		composite.type = "composite";
		assertFalse(registry.create(composite).isPresent());

		TriggerDefinition location = new TriggerDefinition();
		location.type = "location";
		assertFalse(registry.create(location).isPresent());
	}

	@Test
	void registryBuildsSupportedTypes()
	{
		TriggerRegistry registry = new TriggerRegistry(null);

		TriggerDefinition animation = new TriggerDefinition();
		animation.type = "animation";
		animation.npcId = 11278;
		animation.animationId = 8960;
		Optional<TriggerEvaluator> anim = registry.create(animation);
		assertTrue(anim.isPresent());
		assertEquals("animation 8960 from npc 11278", anim.get().describe());

		TriggerDefinition anyAnim = new TriggerDefinition();
		anyAnim.type = "animation";
		anyAnim.animationId = 100;
		assertEquals("animation 100", registry.create(anyAnim).orElseThrow().describe());

		TriggerDefinition projectile = new TriggerDefinition();
		projectile.type = "projectile";
		projectile.projectId = 2955;
		assertEquals("projectile 2955",
			registry.create(projectile).orElseThrow().describe());

		TriggerDefinition fromNpc = new TriggerDefinition();
		fromNpc.type = "projectile";
		fromNpc.projectId = 1;
		fromNpc.srcNpcId = 42;
		assertEquals("projectile 1 from npc 42",
			registry.create(fromNpc).orElseThrow().describe());

		TriggerDefinition graphic = new TriggerDefinition();
		graphic.type = "graphic";
		graphic.graphicId = 77;
		assertEquals("graphic 77", registry.create(graphic).orElseThrow().describe());

		TriggerDefinition onNpc = new TriggerDefinition();
		onNpc.type = "graphic";
		onNpc.npcId = 9;
		onNpc.graphicId = 77;
		assertEquals("graphic 77 on npc 9", registry.create(onNpc).orElseThrow().describe());

		TriggerDefinition spawn = new TriggerDefinition();
		spawn.type = "npc_spawn";
		spawn.npcId = 11278;
		assertTrue(registry.create(spawn).isPresent());

		TriggerDefinition despawn = new TriggerDefinition();
		despawn.type = "npc_despawn";
		despawn.npcIds = List.of(1, 2);
		assertTrue(registry.create(despawn).isPresent());

		TriggerDefinition shout = new TriggerDefinition();
		shout.type = "shout";
		shout.containsText = "Fear";
		shout.senderName = "Nex";
		assertTrue(registry.create(shout).isPresent());

		TriggerDefinition wave = new TriggerDefinition();
		wave.type = "wave_cleared";
		wave.npcIds = List.of(7690, 7691);
		assertTrue(registry.create(wave).isPresent());

		TriggerDefinition hp = new TriggerDefinition();
		hp.type = "hp";
		hp.npcId = 11278;
		hp.hpThreshold = 50;
		hp.hpDirection = "below";
		assertTrue(registry.create(hp).isPresent());

		TriggerDefinition hpAbove = new TriggerDefinition();
		hpAbove.type = "hp";
		hpAbove.npcIds = List.of(1);
		hpAbove.hpThreshold = 25;
		hpAbove.hpDirection = "above";
		assertTrue(registry.create(hpAbove).isPresent());

		TriggerDefinition tick = new TriggerDefinition();
		tick.type = "tick_timer";
		tick.tickMod = 4;
		tick.tickOffset = 2;
		assertEquals("every 4 ticks offset 2",
			registry.create(tick).orElseThrow().describe());

		TriggerDefinition tickDefault = new TriggerDefinition();
		tickDefault.type = "tick_timer";
		tickDefault.tickMod = 3;
		assertEquals("every 3 ticks",
			registry.create(tickDefault).orElseThrow().describe());

		TriggerDefinition playerAnim = new TriggerDefinition();
		playerAnim.type = "player_state";
		playerAnim.animationId = 8960;
		assertEquals("player animation 8960",
			registry.create(playerAnim).orElseThrow().describe());

		TriggerDefinition playerHp = new TriggerDefinition();
		playerHp.type = "player_state";
		playerHp.hpThreshold = 30;
		playerHp.hpDirection = "above";
		assertEquals("player hp >=30",
			registry.create(playerHp).orElseThrow().describe());

		TriggerDefinition composite = new TriggerDefinition();
		composite.type = "composite";
		composite.logic = "AND";
		TriggerDefinition child = new TriggerDefinition();
		child.type = "tick_timer";
		child.tickMod = 2;
		composite.children = List.of(child);
		assertInstanceOf(CompositeTriggerEvaluator.class,
			registry.create(composite).orElseThrow());
	}

	@Test
	void compositeRejectsBadLogicAndInvalidChildren()
	{
		TriggerRegistry registry = new TriggerRegistry(null);
		TriggerDefinition composite = new TriggerDefinition();
		composite.type = "composite";
		composite.logic = "MAYBE";
		TriggerDefinition child = new TriggerDefinition();
		child.type = "tick_timer";
		child.tickMod = 2;
		composite.children = List.of(child);
		assertFalse(registry.create(composite).isPresent());

		TriggerDefinition badChild = new TriggerDefinition();
		badChild.type = "animation";
		composite.children = List.of(badChild);
		assertFalse(registry.create(composite).isPresent());
	}
}
