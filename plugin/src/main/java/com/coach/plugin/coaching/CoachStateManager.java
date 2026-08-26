package com.coach.plugin.coaching;

import com.coach.plugin.events.GameStateBridge;
import com.coach.plugin.model.BossState;
import com.coach.plugin.model.PlayerState;
import net.runelite.api.Client;

/**
 * Holds the latest player/boss state snapshots for decision-making.
 * All values may be null before the first tick; consumers must tolerate that.
 */
public class CoachStateManager
{
	private volatile PlayerState player;
	private volatile int currentTick = -1;

	public void update(GameStateBridge bridge, Client client, int tick)
	{
		this.currentTick = tick;
		if (bridge != null && client != null)
		{
			try
			{
				this.player = bridge.getPlayerState(client);
			}
			catch (Exception ignored)
			{
				// state snapshots are best-effort
			}
		}
	}

	public PlayerState getPlayer()
	{
		return player;
	}

	public int getCurrentTick()
	{
		return currentTick;
	}
}
