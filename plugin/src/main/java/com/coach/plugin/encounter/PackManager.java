package com.coach.plugin.encounter;

import com.coach.plugin.encounter.model.BossDefinition;
import com.coach.plugin.encounter.model.EncounterPack;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Owns the pack lifecycle: directory scan, per-file load status, boss-id
 * conflict detection and dependency reporting.
 *
 * Policies:
 * - files are processed in alphabetical order; on a boss conflict the FIRST
 *   pack wins and later packs are marked CONFLICT and skipped
 * - duplicate packIds are treated like boss conflicts
 * - unsatisfied dependencies are reported as warnings; the pack still loads
 */
public class PackManager
{
	private final EncounterLoader loader;

	public PackManager(EncounterLoader loader)
	{
		this.loader = loader;
	}

	public static final class LoadResult
	{
		public final List<EncounterPack> packs;
		public final List<PackStatus> statuses;
		public final List<String> warnings;

		LoadResult(List<EncounterPack> packs, List<PackStatus> statuses, List<String> warnings)
		{
			this.packs = packs;
			this.statuses = statuses;
			this.warnings = warnings;
		}
	}

	public LoadResult loadDirectory(Path directory) throws IOException
	{
		List<EncounterPack> packs = new ArrayList<>();
		List<PackStatus> statuses = new ArrayList<>();
		List<String> warnings = new ArrayList<>();

		if (directory == null || !Files.isDirectory(directory))
		{
			return new LoadResult(packs, statuses, warnings);
		}

		Map<String, String> claimedBosses = new HashMap<>(); // bossId -> owning packId
		Set<String> loadedPackIds = new HashSet<>();
		Set<String> seenPackIds = new HashSet<>();

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.zip"))
		{
			for (Path zip : stream)
			{
				String fileName = zip.getFileName() != null ? zip.getFileName().toString() : zip.toString();
				EncounterPack pack;
				try
				{
					pack = loader.loadZip(zip);
				}
				catch (PackLoadException e)
				{
					statuses.add(new PackStatus(fileName, null, null,
						PackStatus.State.REJECTED, e.getMessage()));
					continue;
				}

				String packId = pack.metadata.packId;
				if (!seenPackIds.add(packId))
				{
					statuses.add(new PackStatus(fileName, packId, pack.metadata.version,
						PackStatus.State.CONFLICT, "duplicate packId — already provided by another pack"));
					continue;
				}

				String conflictingBoss = findConflict(pack, claimedBosses);
				if (conflictingBoss != null)
				{
					String owner = claimedBosses.get(conflictingBoss);
					statuses.add(new PackStatus(fileName, packId, pack.metadata.version,
						PackStatus.State.CONFLICT,
						"boss '" + conflictingBoss + "' already covered by pack " + owner));
					continue;
				}

				for (BossDefinition boss : pack.bosses)
				{
					claimedBosses.put(boss.bossId, packId);
				}
				loadedPackIds.add(packId);
				packs.add(pack);
				statuses.add(new PackStatus(fileName, packId, pack.metadata.version,
					PackStatus.State.LOADED, ""));
			}
		}

		// dependency reporting (after all packs are in)
		for (EncounterPack pack : packs)
		{
			if (pack.metadata.dependencies == null)
			{
				continue;
			}
			for (String dependency : pack.metadata.dependencies)
			{
				if (!loadedPackIds.contains(dependency))
				{
					warnings.add("pack " + pack.metadata.packId + " requires missing dependency: " + dependency);
				}
			}
		}

		return new LoadResult(packs, statuses, warnings);
	}

	private static String findConflict(EncounterPack candidate, Map<String, String> claimedBosses)
	{
		for (BossDefinition boss : candidate.bosses)
		{
			if (claimedBosses.containsKey(boss.bossId))
			{
				return boss.bossId;
			}
		}
		return null;
	}
}
