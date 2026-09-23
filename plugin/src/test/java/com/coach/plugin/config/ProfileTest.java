package com.coach.plugin.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.runelite.client.config.ConfigManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProfileTest
{
	private ProfileStorage storage;

	@BeforeEach
	void setUp()
	{
		storage = new ProfileStorage();
	}

	private static ConfigProfile sample(String name)
	{
		ConfigProfile profile = new ConfigProfile();
		profile.name = name;
		profile.enabled = true;
		profile.debugMode = false;
		profile.muted = false;
		profile.masterVolume = 70;
		profile.criticalCallouts = true;
		profile.warningCallouts = true;
		profile.infoCallouts = true;
		profile.transitionCallouts = true;
		profile.disabledBosses = "nex";
		return profile;
	}

	private static final class ProfileHarness
	{
		final Map<String, String> stored = new HashMap<>();
		final ConfigManager configManager = mock(ConfigManager.class);
		final CoachConfig config = mock(CoachConfig.class);
		final ProfileManager manager;

		ProfileHarness()
		{
			when(configManager.getConfiguration(anyString(), anyString()))
				.thenAnswer(inv -> stored.get(inv.getArgument(1, String.class)));
			doAnswer(inv ->
			{
				stored.put(inv.getArgument(1, String.class), inv.getArgument(2, String.class));
				return null;
			}).when(configManager).setConfiguration(anyString(), anyString(), anyString());
			when(config.profilesJson())
				.thenAnswer(inv -> stored.getOrDefault("profilesJson", "{}"));
			manager = new ProfileManager(configManager, config);
		}
	}

	@Test
	void writeAndReadRoundTrip(@TempDir Path dir) throws IOException
	{
		Path file = dir.resolve("nested").resolve("Learning.json");
		storage.write(file, sample("Learning"));
		assertTrue(Files.exists(file));
		ConfigProfile read = storage.read(file);
		assertEquals("Learning", read.name);
		assertTrue(read.enabled);
		assertEquals(70, read.masterVolume);
		assertEquals("nex", read.disabledBosses);
	}

	@Test
	void writeRejectsMissingName(@TempDir Path dir)
	{
		assertThrows(IOException.class,
			() -> storage.write(dir.resolve("x.json"), null));
		ConfigProfile blank = new ConfigProfile();
		blank.name = "  ";
		assertThrows(IOException.class,
			() -> storage.write(dir.resolve("x.json"), blank));
	}

	@Test
	void readActionableErrors(@TempDir Path dir) throws IOException
	{
		IOException missing = assertThrows(IOException.class,
			() -> storage.read(dir.resolve("nope.json")));
		assertTrue(missing.getMessage().contains("could not read"));

		Path bad = dir.resolve("bad.json");
		Files.writeString(bad, "{not json");
		IOException invalid = assertThrows(IOException.class, () -> storage.read(bad));
		assertTrue(invalid.getMessage().contains("invalid profile JSON"));

		Path empty = dir.resolve("empty.json");
		Files.writeString(empty, "null");
		assertTrue(assertThrows(IOException.class, () -> storage.read(empty))
			.getMessage().contains("empty"));

		Path noName = dir.resolve("noname.json");
		Files.writeString(noName, "{\"masterVolume\":50}");
		assertTrue(assertThrows(IOException.class, () -> storage.read(noName))
			.getMessage().contains("name"));

		Path loud = dir.resolve("loud.json");
		Files.writeString(loud, "{\"name\":\"x\",\"masterVolume\":250}");
		assertEquals(100, storage.read(loud).masterVolume);

		Path quiet = dir.resolve("quiet.json");
		Files.writeString(quiet, "{\"name\":\"x\",\"masterVolume\":-5}");
		assertEquals(0, storage.read(quiet).masterVolume);
	}

	@Test
	void exporterWritesSanitizedNameAndHandlesMissing(@TempDir Path dir)
	{
		ProfileManager manager = mock(ProfileManager.class);
		when(manager.listProfiles()).thenReturn(Map.of("My Profile", sample("My Profile")));
		ProfileExporter exporter = new ProfileExporter(manager, storage, dir);

		Path written = exporter.export("My Profile");
		assertNotNull(written);
		assertEquals("My_Profile.json", written.getFileName().toString());

		assertNull(exporter.export("nope"));
		verify(manager, times(2)).listProfiles();
	}

	@Test
	void exporterReturnsNullOnWriteFailure(@TempDir Path dir) throws Exception
	{
		Path blocker = dir.resolve("blocker");
		Files.writeString(blocker, "file");
		ProfileManager manager = mock(ProfileManager.class);
		when(manager.listProfiles()).thenReturn(Map.of("x", sample("x")));
		ProfileExporter exporter = new ProfileExporter(manager, storage, blocker);
		assertNull(exporter.export("x"));
	}

	@Test
	void importerImportsViaStorage(@TempDir Path dir) throws Exception
	{
		Path file = dir.resolve("p.json");
		storage.write(file, sample("Imported"));
		ProfileManager manager = mock(ProfileManager.class);
		ProfileImporter importer = new ProfileImporter(manager, storage);
		ConfigProfile imported = importer.importFile(file);
		assertEquals("Imported", imported.name);
		verify(manager).importProfile(imported);
	}

	@Test
	void profileManagerListImportSaveApplyDelete()
	{
		ProfileHarness harness = new ProfileHarness();
		CoachConfig config = harness.config;
		when(config.enabled()).thenReturn(true);
		when(config.debugMode()).thenReturn(false);
		when(config.muted()).thenReturn(false);
		when(config.masterVolume()).thenReturn(70);
		when(config.criticalCallouts()).thenReturn(true);
		when(config.warningCallouts()).thenReturn(false);
		when(config.infoCallouts()).thenReturn(true);
		when(config.transitionCallouts()).thenReturn(false);
		when(config.disabledBosses()).thenReturn("zulrah");

		ProfileManager manager = harness.manager;
		assertTrue(manager.listProfiles().isEmpty());

		manager.importProfile(sample("Alpha"));
		assertEquals(1, manager.listProfiles().size());
		assertEquals("Alpha", manager.listProfiles().get("Alpha").name);

		manager.saveProfile("Beta");
		assertTrue(manager.listProfiles().containsKey("Beta"));
		assertEquals(70, manager.listProfiles().get("Beta").masterVolume);

		assertTrue(manager.applyProfile("Beta"));
		verify(harness.configManager).setConfiguration(eq("coach"), eq("enabled"), eq("true"));
		verify(harness.configManager).setConfiguration(eq("coach"), eq("disabledBosses"), eq("zulrah"));
		assertFalse(manager.applyProfile("missing"));

		manager.deleteProfile("Beta");
		assertFalse(manager.listProfiles().containsKey("Beta"));
		manager.deleteProfile("Beta");

		harness.stored.put("profilesJson", "{broken");
		assertTrue(manager.listProfiles().isEmpty());
	}

	@Test
	void ensureDefaultProfilesSeedsOnce()
	{
		ProfileHarness harness = new ProfileHarness();
		ProfileManager manager = harness.manager;

		manager.ensureDefaultProfiles();
		assertEquals("true", harness.stored.get("defaultsSeeded"));
		assertTrue(harness.stored.get("profilesJson").contains("Learning"));
		assertTrue(harness.stored.get("profilesJson").contains("Practice"));
		assertTrue(harness.stored.get("profilesJson").contains("Performance"));

		int before = manager.listProfiles().size();
		assertEquals(3, before);

		manager.ensureDefaultProfiles();
		assertEquals(3, manager.listProfiles().size());
		assertEquals("true", harness.stored.get("defaultsSeeded"));

		harness.stored.put("defaultsSeeded", "false");
		harness.stored.put("profilesJson", "{\"Keep\":{\"name\":\"Keep\"}}");
		manager.ensureDefaultProfiles();
		assertEquals("true", harness.stored.get("defaultsSeeded"));
		assertTrue(harness.stored.get("profilesJson").contains("Keep"));
		assertFalse(harness.stored.get("profilesJson").contains("Learning"));
	}
}
