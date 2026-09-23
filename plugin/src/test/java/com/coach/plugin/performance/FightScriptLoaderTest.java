package com.coach.plugin.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FightScriptLoaderTest
{
	private static final String SCRIPT = "{"
		+ "\"endTick\": 20,"
		+ "\"events\":["
		+ "{\"tick\":5,\"type\":\"shout\",\"text\":\"hi\"},"
		+ "{\"tick\":1,\"type\":\"npc_spawn\",\"npcId\":11278},"
		+ "{\"tick\":2,\"type\":\"animation\",\"npcId\":11278,\"animationId\":8960},"
		+ "{\"tick\":3,\"type\":\"projectile\",\"projectId\":2955},"
		+ "{\"tick\":4,\"type\":\"graphic\",\"graphicId\":100},"
		+ "{\"tick\":6,\"type\":\"hp\",\"npcId\":11278,\"hp\":50,\"direction\":\"below\"}"
		+ "]}";

	@Test
	void parsesScriptSortsEventsAndExposesFields()
	{
		FightScript script = FightScript.fromJson(SCRIPT);
		assertEquals(20, script.getEndTick());
		List<FightScript.ScriptEvent> events = script.getEvents();
		assertEquals(6, events.size());
		assertEquals(1, events.get(0).getTick());
		assertEquals("npc_spawn", events.get(0).getType());
		assertEquals(11278, events.get(0).getNpcId());
		assertEquals("animation", events.get(1).getType());
		assertEquals(8960, events.get(1).getAnimationId());
		assertEquals(2955, events.get(2).getProjectileId());
		assertEquals(100, events.get(3).getGraphicId());
		assertEquals("hi", events.get(4).getText());
		assertEquals(0, events.get(4).getNpcId());
		assertNull(events.get(4).getAnimationId());
		assertEquals(50, events.get(5).getHp());
		assertEquals("below", events.get(5).getHpDirection());
	}

	@Test
	void endTickFallsBackToLastEvent()
	{
		FightScript script = FightScript.fromJson("{\"events\":[{\"tick\":9,\"type\":\"shout\",\"text\":\"x\"}]}");
		assertEquals(9, script.getEndTick());
		assertEquals(1, script.getEvents().size());
	}

	@Test
	void rejectsInvalidScripts()
	{
		assertThrows(IllegalArgumentException.class, () -> FightScript.fromJson("null"));
		assertThrows(IllegalArgumentException.class, () -> FightScript.fromJson("{\"endTick\":5}"));
	}

	@Test
	void loaderWrapsRuntimeExceptionAsIoException()
	{
		IOException error = assertThrows(IOException.class, () -> FightScriptLoader.fromJson("{\"endTick\":1}"));
		assertTrue(error.getMessage().contains("invalid fight script"));
	}

	@Test
	void loaderReadsFileAndDirectory(@TempDir Path dir) throws IOException
	{
		Path a = dir.resolve("a.json");
		Path b = dir.resolve("b.json");
		Path ignored = dir.resolve("notes.txt");
		Files.writeString(a, SCRIPT);
		Files.writeString(b, "{\"endTick\":1,\"events\":[{\"tick\":1,\"type\":\"hpDirection\",\"direction\":\"above\"}]}");
		Files.writeString(ignored, "not json");

		FightScript fromFile = FightScriptLoader.fromFile(a);
		assertEquals(20, fromFile.getEndTick());

		List<FightScript> all = FightScriptLoader.fromDirectory(dir);
		assertEquals(2, all.size());
	}

	@Test
	void scriptEventHpDirectionAlternateKey()
	{
		FightScript script = FightScript.fromJson(
			"{\"events\":[{\"tick\":1,\"type\":\"hp\",\"hp\":10,\"hpDirection\":\"above\"}]}");
		assertEquals("above", script.getEvents().get(0).getHpDirection());
	}
}
