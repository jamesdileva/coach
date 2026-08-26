package com.coach.plugin.encounter;

import com.coach.plugin.encounter.model.BossDefinition;
import com.coach.plugin.encounter.model.EncounterPack;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds all loaded encounters. Packs are loaded from the user's pack
 * directory at startup and can be reloaded on config change without restart.
 */
public class EncounterEngine
{
	private static final Logger log = LoggerFactory.getLogger(EncounterEngine.class);

	private final EncounterLoader loader = new EncounterLoader();
	private volatile List<EncounterPack> packs = Collections.emptyList();

	/**
	 * Scan the directory for *.zip packs, validate and load each one.
	 * Invalid packs are logged and skipped, never fatal (rule 4).
	 *
	 * @return number of successfully loaded packs
	 */
	public synchronized int loadPacks(Path directory)
	{
		List<EncounterPack> loaded = new ArrayList<>();

		if (directory == null || !Files.isDirectory(directory))
		{
			log.info("[coach] no encounter pack directory at {} — nothing loaded",
				directory);
			packs = Collections.emptyList();
			return 0;
		}

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.zip"))
		{
			for (Path zip : stream)
			{
				try
				{
					loaded.add(loader.loadZip(zip));
				}
				catch (PackLoadException e)
				{
					log.warn("[coach] rejected encounter pack: {}", e.getMessage());
				}
			}
		}
		catch (IOException e)
		{
			log.warn("[coach] could not scan pack directory {}: {}", directory, e.getMessage());
		}

		packs = Collections.unmodifiableList(loaded);
		int bossCount = packs.stream().mapToInt(p -> p.bosses.size()).sum();
		log.info("[coach] loaded {} encounter pack(s) covering {} boss(es)", packs.size(), bossCount);
		return packs.size();
	}

	public List<EncounterPack> getPacks()
	{
		return packs;
	}

	/**
	 * Find the boss definition matching a live NPC id, if any pack defines it.
	 */
	public Optional<BossDefinition> getBossForNpcId(int npcId)
	{
		for (EncounterPack pack : packs)
		{
			for (BossDefinition boss : pack.bosses)
			{
				if (boss.npcId != null && boss.npcId == npcId)
				{
					return Optional.of(boss);
				}
			}
		}
		return Optional.empty();
	}
}
