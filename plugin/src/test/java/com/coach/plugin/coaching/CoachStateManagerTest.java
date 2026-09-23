package com.coach.plugin.coaching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.coach.plugin.events.GameStateBridge;
import com.coach.plugin.model.PlayerState;
import net.runelite.api.Client;
import org.junit.jupiter.api.Test;

class CoachStateManagerTest
{
	@Test
	void startsEmpty()
	{
		CoachStateManager state = new CoachStateManager();
		assertNull(state.getPlayer());
		assertEquals(-1, state.getCurrentTick());
	}

	@Test
	void updateCapturesTickAndPlayer()
	{
		CoachStateManager state = new CoachStateManager();
		GameStateBridge bridge = mock(GameStateBridge.class);
		Client client = mock(Client.class);
		PlayerState snapshot = new PlayerState(50, 99, 10, 20, 0, 8960);
		when(bridge.getPlayerState(client)).thenReturn(snapshot);

		state.update(bridge, client, 42);
		assertEquals(42, state.getCurrentTick());
		assertSame(snapshot, state.getPlayer());
	}

	@Test
	void updateToleratesNullArgs()
	{
		CoachStateManager state = new CoachStateManager();
		state.update(null, null, 7);
		assertEquals(7, state.getCurrentTick());
		assertNull(state.getPlayer());
	}

	@Test
	void updateSwallowsBridgeExceptions()
	{
		CoachStateManager state = new CoachStateManager();
		GameStateBridge bridge = mock(GameStateBridge.class);
		when(bridge.getPlayerState(null)).thenThrow(new IllegalStateException("no player"));
		state.update(bridge, null, 9);
		assertEquals(9, state.getCurrentTick());
		assertNull(state.getPlayer());
	}

	@Test
	void predictionMechanicAccessors()
	{
		PredictedMechanic mechanic = new PredictedMechanic("nex", "smoke", 3);
		assertEquals("nex", mechanic.getBossId());
		assertEquals("smoke", mechanic.getMechanicId());
		assertEquals(3, mechanic.getTicksUntilFire());
		assertEquals("smoke in 3t", mechanic.toString());
	}
}
