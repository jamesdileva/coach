package com.coach.plugin.encounter;

/**
 * Thrown when an encounter pack fails parsing or validation.
 * The message is user-facing (shown in logs) — keep it actionable.
 */
public class PackLoadException extends Exception
{
	public PackLoadException(String message)
	{
		super(message);
	}
}
