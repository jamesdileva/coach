package com.coach.plugin.encounter.model;

import java.util.List;

/**
 * Root of a parsed encounter pack JSON (schema v1). Populated by Gson.
 */
public class EncounterPack
{
	public static final String SUPPORTED_SCHEMA_VERSION = "1.0";

	public String schemaVersion;
	public PackMetadata metadata;
	public List<BossDefinition> bosses;

	// runtime
	public transient String sourceName; // zip file name or test-provided id
}
