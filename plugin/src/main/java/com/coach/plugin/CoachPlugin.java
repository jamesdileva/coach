package com.coach.plugin;

import com.coach.plugin.config.CoachConfig;
import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "Coach",
	description = "Real-time boss coaching with visual and audio callouts",
	tags = {"pvm", "bossing", "coaching", "overlay", "audio"}
)
public class CoachPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(CoachPlugin.class);

	@Inject
	private Client client;

	@Inject
	private CoachConfig config;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Project Coach started (debug={})", config.debugMode());
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Project Coach shut down");
	}

	@Provides
	CoachConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CoachConfig.class);
	}
}
