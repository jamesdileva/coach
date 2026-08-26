package com.coach.plugin.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.runelite.client.config.ConfigManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileFileLifecycleTest
{
	@TempDir
	Path tempDir;

	private ConfigManager configManager;
	private CoachConfig config;
	private final java.util.Map<String, String> stored = new java.util.HashMap<>();

	@BeforeEach
	void setUp()
	{
		configManager = mock(ConfigManager.class);
		// make the mock actually persist writes so read-after-write paths work
		org.mockito.Mockito.doAnswer(inv -> {
			stored.put(inv.getArgument(1), inv.getArgument(2));
			return null;
		}).when(configManager)
			.setConfiguration(org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyString());
		when(configManager.getConfiguration(org.mockito.ArgumentMatchers.eq("coach"),
			org.mockito.ArgumentMatchers.anyString()))
			.thenAnswer(inv -> stored.get(inv.getArgument(1)));

		config = mock(CoachConfig.class);
		when(config.profilesJson()).thenAnswer(
			inv -> stored.getOrDefault("profilesJson", "{}"));
		when(config.enabled()).thenReturn(true);
		when(config.debugMode()).thenReturn(false);
		when(config.muted()).thenReturn(false);
		when(config.masterVolume()).thenReturn(70);
		when(config.criticalCallouts()).thenReturn(true);
		when(config.warningCallouts()).thenReturn(false);
		when(config.infoCallouts()).thenReturn(false);
		when(config.transitionCallouts()).thenReturn(true);
		when(config.disabledBosses()).thenReturn("inferno");
	}

	private ProfileManager newManager()
	{
		return new ProfileManager(configManager, config);
	}

	// ---- storage ----

	@Test
	void storageRoundTripsProfile() throws Exception
	{
		ConfigProfile profile = new ConfigProfile();
		profile.name = "My_Profile-1";
		profile.enabled = true;
		profile.muted = false;
		profile.masterVolume = 45;
		profile.criticalCallouts = true;
		profile.warningCallouts = false;
		profile.disabledBosses = "nex";

		Path file = tempDir.resolve("nested").resolve("My_Profile-1.json");
		new ProfileStorage().write(file, profile);

		assertTrue(Files.exists(file));
		ConfigProfile read = new ProfileStorage().read(file);
		assertEquals("My_Profile-1", read.name);
		assertEquals(45, read.masterVolume);
		assertFalse(read.warningCallouts);
		assertEquals("nex", read.disabledBosses);
	}

	@Test
	void storageRejectsCorruptJsonWithClearError()
	{
		Path file = tempDir.resolve("bad.json");
		assertThrows(IOException.class, () -> {
			Files.writeString(file, "{not json");
			new ProfileStorage().read(file);
		});
	}

	@Test
	void storageRejectsMissingName()
	{
		Path file = tempDir.resolve("noname.json");
		assertThrows(IOException.class, () -> {
			Files.writeString(file, "{\"masterVolume\":50}");
			new ProfileStorage().read(file);
		});
	}

	// ---- exporter / importer ----

	@Test
	void exportThenImportRoundTripThroughFiles() throws Exception
	{
		ProfileManager manager = newManager();
		manager.saveProfile("ExportMe");

		var dir = tempDir.resolve("profiles");
		Path written = new ProfileExporter(manager,
			new ProfileStorage(), dir).export("ExportMe");

		assertNotNull(written);
		assertTrue(written.startsWith(dir));

		JsonObject parsed = new Gson().fromJson(Files.readString(written), JsonObject.class);
		assertEquals("ExportMe", parsed.get("name").getAsString());

		// import into a fresh manager sharing the same persisted storage
		ProfileManager fresh = newManager();
		new ProfileImporter(fresh, new ProfileStorage()).importFile(written);

		assertTrue(fresh.listProfiles().containsKey("ExportMe"));
	}

	@Test
	void exportUnknownProfileReturnsNullAndImportsNothing()
	{
		var dir = tempDir.resolve("profiles");
		var exportedPath = new ProfileExporter(newManager(),
			new ProfileStorage(), dir).export("ghost");

		assertNull(exportedPath);
		assertEquals(0, newManager().listProfiles().size());
	}

	@Test
	void importerRejectsInvalidStructure() throws Exception
	{
		Path bad = tempDir.resolve("invalid.json");
		Files.writeString(bad, "{\"someRandomField\":true}");

		ProfileImporter importer = new ProfileImporter(newManager(), new ProfileStorage());
		assertThrows(IOException.class, () -> importer.importFile(bad));
		assertEquals(0, newManager().listProfiles().size(),
			"invalid profiles never touch stored config");
	}

	@Test
	void importerClampsOutOfRangeVolume() throws Exception
	{
		Path file = tempDir.resolve("loud.json");
		Files.writeString(file, "{\"name\":\"loud\",\"masterVolume\":900}");

		ConfigProfile imported = new ProfileImporter(newManager(),
			new ProfileStorage()).importFile(file);

		assertEquals(100, imported.masterVolume);
	}

	// ---- default presets ----

	@Test
	void defaultsSeededOnceWithExpectedToggles()
	{
		ProfileManager manager = newManager();
		manager.ensureDefaultProfiles();

		var profiles = manager.listProfiles();
		assertEquals(java.util.Set.of("Learning", "Practice", "Performance"),
			profiles.keySet());

		// Learning: everything on
		assertTrue(profiles.get("Learning").criticalCallouts);
		assertTrue(profiles.get("Learning").infoCallouts);

		// Practice: criticals only
		assertTrue(profiles.get("Practice").criticalCallouts);
		assertFalse(profiles.get("Practice").warningCallouts);

		// Performance: silent
		assertTrue(profiles.get("Performance").muted);
		assertFalse(profiles.get("Performance").criticalCallouts);

		// seeded flag persisted via ConfigManager
		verify(configManager).setConfiguration("coach", "defaultsSeeded", "true");
	}

	@Test
	void defaultsNotReseededAfterDeletionWhenFlagSet()
	{
		when(configManager.getConfiguration("coach", "defaultsSeeded")).thenReturn("true");
		ProfileManager manager = newManager();
		manager.ensureDefaultProfiles();
		assertEquals(0, manager.listProfiles().size(), "respects user deletions");
	}
}
