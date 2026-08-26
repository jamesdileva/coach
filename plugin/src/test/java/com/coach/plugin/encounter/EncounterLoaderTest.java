package com.coach.plugin.encounter;

import com.coach.plugin.encounter.model.EncounterPack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EncounterLoaderTest
{
	private final EncounterLoader loader = new EncounterLoader();

	private static final String BOSS_JSON = "{"
		+ "\"bossId\": \"testboss\","
		+ "\"name\": \"Test Boss\","
		+ "\"npcId\": 1234,"
		+ "\"phases\": [{"
		+ "  \"phaseId\": \"p1\","
		+ "  \"name\": \"Phase 1\","
		+ "  \"entryTrigger\": {\"type\": \"npc_spawn\", \"npcId\": 1234},"
		+ "  \"exitTriggers\": [{\"type\": \"hp\", \"npcId\": 1234, \"hpThreshold\": 50, \"hpDirection\": \"below\"}],"
		+ "  \"mechanics\": [{"
		+ "    \"mechanicId\": \"m1\","
		+ "    \"name\": \"Special\","
		+ "    \"triggers\": [{\"triggerId\": \"t1\", \"type\": \"animation\", \"npcId\": 1234, \"animationId\": 8960}],"
		+ "    \"callouts\": [{"
		+ "      \"calloutId\": \"c1\","
		+ "      \"text\": \"Pray Ranged!\","
		+ "      \"category\": \"critical\","
		+ "      \"priority\": 90,"
		+ "      \"audioOffset\": -2,"
		+ "      \"visualOffset\": 0"
		+ "    }]"
		+ "  }]"
		+ "}]}";

	private static final String VALID_PACK = "{"
		+ "\"schemaVersion\": \"1.0\","
		+ "\"metadata\": {\"packId\": \"test\", \"name\": \"Test Pack\", \"version\": \"1.0.0\", \"gameVersion\": \"2026-08\"},"
		+ "\"bosses\": [" + BOSS_JSON + "]}";

	private String packWithBoss(String bossJson)
	{
		return "{\"schemaVersion\": \"1.0\","
			+ "\"metadata\": {\"packId\": \"t\", \"name\": \"T\", \"version\": \"1.0.0\", \"gameVersion\": \"x\"},"
			+ "\"bosses\": [" + bossJson + "]}";
	}

	@Test
	void validPackParsesAndValidates() throws PackLoadException
	{
		EncounterPack pack = loader.parseJson(VALID_PACK, "valid.zip");

		assertEquals("test", pack.metadata.packId);
		assertEquals("1.0", pack.schemaVersion);
		assertEquals("valid.zip", pack.sourceName);
		assertEquals("testboss", pack.bosses.get(0).bossId);
		assertEquals("m1", pack.bosses.get(0).phases.get(0).mechanics.get(0).mechanicId);
	}

	@Test
	void missingSchemaVersionRejected()
	{
		PackLoadException e = assertThrows(PackLoadException.class,
			() -> loader.parseJson("{\"metadata\": {}, \"bosses\": []}", "bad.json"));
		assertTrue(e.getMessage().contains("schemaVersion"));
	}

	@Test
	void unsupportedSchemaVersionRejected()
	{
		String json = VALID_PACK.replace("\"schemaVersion\": \"1.0\"", "\"schemaVersion\": \"9.9\"");
		PackLoadException e = assertThrows(PackLoadException.class,
			() -> loader.parseJson(json, "future.zip"));
		assertTrue(e.getMessage().contains("unsupported schemaVersion"));
	}

	@Test
	void malformedJsonRejected()
	{
		PackLoadException e = assertThrows(PackLoadException.class,
			() -> loader.parseJson("{not json", "broken.zip"));
		assertTrue(e.getMessage().contains("invalid JSON"));
	}

	@Test
	void emptyContentRejected()
	{
		assertThrows(PackLoadException.class,
			() -> loader.parseJson("   ", "empty.zip"));
	}

	@Test
	void duplicateBossIdsRejected()
	{
		String json = "{\"schemaVersion\": \"1.0\","
			+ "\"metadata\": {\"packId\": \"t\", \"name\": \"T\", \"version\": \"1.0.0\", \"gameVersion\": \"x\"},"
			+ "\"bosses\": [" + BOSS_JSON + "," + BOSS_JSON + "]}";
		PackLoadException e = assertThrows(PackLoadException.class,
			() -> loader.parseJson(json, "dup.zip"));
		assertTrue(e.getMessage().contains("duplicate bossId"));
	}

	@Test
	void unknownTriggerTypeRejected()
	{
		String json = packWithBoss("{\"bossId\":\"b\",\"name\":\"B\",\"npcId\":1,"
			+ "\"phases\":[{\"phaseId\":\"p\",\"name\":\"P\","
			+ "\"entryTrigger\":{\"type\":\"teleport\"}}]}");
		PackLoadException e = assertThrows(PackLoadException.class,
			() -> loader.parseJson(json, "trig.zip"));
		assertTrue(e.getMessage().contains("unknown trigger type 'teleport'"));
	}

	@Test
	void badCalloutCategoryRejected()
	{
		String json = packWithBoss("{\"bossId\":\"b\",\"name\":\"B\",\"npcId\":1,"
			+ "\"phases\":[{\"phaseId\":\"p\",\"name\":\"P\",\"entryTrigger\":{\"type\":\"npc_spawn\",\"npcId\":1},"
			+ "\"mechanics\":[{\"mechanicId\":\"m\",\"name\":\"M\","
			+ "\"triggers\":[{\"type\":\"animation\",\"npcId\":1,\"animationId\":2}],"
			+ "\"callouts\":[{\"calloutId\":\"c\",\"text\":\"hi\",\"category\":\"urgent\"}]}]}]}");
		PackLoadException e = assertThrows(PackLoadException.class,
			() -> loader.parseJson(json, "cat.zip"));
		assertTrue(e.getMessage().contains("category must be one of"));
	}

	@Test
	void outOfRangeTickOffsetRejected()
	{
		String json = packWithBoss("{\"bossId\":\"b\",\"name\":\"B\",\"npcId\":1,"
			+ "\"phases\":[{\"phaseId\":\"p\",\"name\":\"P\",\"entryTrigger\":{\"type\":\"npc_spawn\",\"npcId\":1},"
			+ "\"mechanics\":[{\"mechanicId\":\"m\",\"name\":\"M\","
			+ "\"triggers\":[{\"type\":\"animation\",\"npcId\":1,\"animationId\":2}],"
			+ "\"callouts\":[{\"calloutId\":\"c\",\"text\":\"hi\",\"category\":\"info\",\"audioOffset\":-99}]}]}]}");
		PackLoadException e = assertThrows(PackLoadException.class,
			() -> loader.parseJson(json, "off.zip"));
		assertTrue(e.getMessage().contains("audioOffset must be between -5 and 10"));
	}

	@Test
	void duplicatePhaseIdsRejected()
	{
		String json = packWithBoss("{\"bossId\":\"b\",\"name\":\"B\",\"npcId\":1,"
			+ "\"phases\":["
			+ "{\"phaseId\":\"p\",\"name\":\"P\",\"entryTrigger\":{\"type\":\"npc_spawn\",\"npcId\":1}},"
			+ "{\"phaseId\":\"p\",\"name\":\"P2\",\"entryTrigger\":{\"type\":\"npc_spawn\",\"npcId\":1}}"
			+ "]}");
		PackLoadException e = assertThrows(PackLoadException.class,
			() -> loader.parseJson(json, "phase.zip"));
		assertTrue(e.getMessage().contains("duplicate phaseId"));
	}

	@Test
	void bossWithoutPhasesRejected()
	{
		String json = packWithBoss("{\"bossId\":\"b\",\"name\":\"B\",\"npcId\":1}");
		PackLoadException e = assertThrows(PackLoadException.class,
			() -> loader.parseJson(json, "nophase.zip"));
		assertTrue(e.getMessage().contains("at least one phase is required"));
	}
}
