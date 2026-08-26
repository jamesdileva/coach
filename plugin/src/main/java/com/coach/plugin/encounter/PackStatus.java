package com.coach.plugin.encounter;

/**
 * Outcome of loading one pack file.
 */
public final class PackStatus
{
	public enum State
	{
		LOADED,
		REJECTED,   // invalid pack (parse/validation failure)
		CONFLICT    // valid but clashes with another loaded pack
	}

	private final String fileName;
	private final String packId;
	private final String version;
	private final State state;
	private final String message;

	public PackStatus(String fileName, String packId, String version, State state, String message)
	{
		this.fileName = fileName;
		this.packId = packId;
		this.version = version;
		this.state = state;
		this.message = message;
	}

	public String getFileName()
	{
		return fileName;
	}

	public String getPackId()
	{
		return packId != null ? packId : "-";
	}

	public String getVersion()
	{
		return version != null ? version : "-";
	}

	public State getState()
	{
		return state;
	}

	public String getMessage()
	{
		return message != null ? message : "";
	}

	public String describe()
	{
		StringBuilder sb = new StringBuilder()
			.append(getFileName()).append(" -> ").append(getPackId())
			.append('@').append(getVersion()).append(" [").append(state).append(']');
		if (!getMessage().isEmpty())
		{
			sb.append(": ").append(getMessage());
		}
		return sb.toString();
	}
}
