package com.coach.plugin.debug;

import com.coach.plugin.encounter.ActiveEncounter;
import com.coach.plugin.model.PlayerState;
import com.coach.plugin.model.PlayerState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Live player/boss state view for the debug overlay and exports.
 * update() stores a snapshot; format() renders it as text lines.
 */
public class StateInspector
{
	private volatile PlayerState player;
	private volatile List<SessionSummary> sessions = new ArrayList<>();

	public static final class SessionSummary
	{
		public final String bossId;
		public final String phaseId;
		public final int phaseTick;

		public SessionSummary(String bossId, String phaseId, int phaseTick)
		{
			this.bossId = bossId;
			this.phaseId = phaseId;
			this.phaseTick = phaseTick;
		}
	}

	public synchronized void update(PlayerState playerState, List<ActiveEncounter> encounters)
	{
		this.player = playerState;
		List<SessionSummary> summaries = new ArrayList<>();
		for (ActiveEncounter encounter : encounters)
		{
			summaries.add(new SessionSummary(
				encounter.getBoss().bossId,
				encounter.getCurrentPhaseId(),
				encounter.getPhaseTick()));
		}
		this.sessions = summaries;
	}

	public synchronized PlayerState getPlayer()
	{
		return player;
	}

	public synchronized List<SessionSummary> getSessions()
	{
		return sessions;
	}

	public synchronized List<String> format()
	{
		List<String> lines = new ArrayList<>();
		PlayerState p = this.player;
		if (p != null)
		{
			lines.add("player: hp " + p.getHp() + "/" + p.getMaxHp()
				+ " pos(" + p.getPosX() + "," + p.getPosY() + "," + p.getPlane() + ")"
				+ " anim=" + p.getAnimation());
		}
		else
		{
			lines.add("player: (no snapshot)");
		}
		if (sessions.isEmpty())
		{
			lines.add("encounters: none active");
		}
		else
		{
			lines.add("encounters: " + sessions.size());
			int i = 1;
			for (SessionSummary session : sessions)
			{
				lines.add("  #" + i++ + " boss=" + session.bossId
					+ " phase=" + session.phaseId
					+ " phaseTick=" + session.phaseTick);
			}
		}
		return lines;
	}

	public synchronized MapLike toExportData()
	{
		MapLike data = new MapLike();
		PlayerState p = this.player;
		if (p != null)
		{
			data.put("playerHp", String.valueOf(p.getHp()));
			data.put("playerMaxHp", String.valueOf(p.getMaxHp()));
			data.put("playerX", String.valueOf(p.getPosX()));
			data.put("playerY", String.valueOf(p.getPosY()));
			data.put("playerPlane", String.valueOf(p.getPlane()));
			data.put("playerAnimation", String.valueOf(p.getAnimation()));
		}
		int i = 0;
		for (SessionSummary session : sessions)
		{
			data.put("session" + i, session.bossId + ":" + session.phaseId
				+ "@tick" + session.phaseTick);
			i++;
		}
		return data;
	}

	/** Simple string-map wrapper for export serialisation. */
	public static final class MapLike extends LinkedHashMap<String, String>
	{
	}
}

