package com.coach.plugin.encounter.model;

/**
 * Pack metadata section of an encounter pack (schema v1).
 * Public fields are populated by Gson.
 */
public class PackMetadata
{
	public String packId;
	public String name;
	public String description;
	public String author;
	public String version;
	public String gameVersion;
}
