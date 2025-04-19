package com.skilldataexporter;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.CommandExecuted;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@PluginDescriptor(
		name = "Skill Data Exporter",
		description = "Exports skill levels to a CSV file"
)
public class SkillDataExporter extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ChatCommandManager chatCommandManager;

	@Inject
	private SkillDataExporterConfig config;

	@Subscribe
	public void onCommandExecuted(CommandExecuted commandExecuted)
	{
		if (commandExecuted.getCommand().equals("export"))
		{
			exportPlayerStats();
		}
	}

	private void saveFile(String fileName, String filePath, String playerName) {
		File outputFile = Paths.get(filePath, fileName).toFile();
		try (FileWriter writer = new FileWriter(outputFile, false))
		{
			// Write player name once at the top
			writer.write(String.format("Name,%s%n", playerName));

			// Write skill data
			for (Skill skill : Skill.values())
			{
				int level = client.getRealSkillLevel(skill);
				writer.write(String.format("%s,%d%n", skill.getName(), level));
			}

			// Add XP values section at the end if enabled
			if (config.includeXP()) {
				writer.write(String.format("XP Data:%n"));

				long totalXp = 0;
				for (Skill skill : Skill.values()) {
					int xp = client.getSkillExperience(skill);
					totalXp += xp;
					writer.write(String.format("%s,%d%n", skill.getName(), xp));
				}
				writer.write(String.format("Total XP,%d%n", totalXp));
			}

			// Notify the player
			String clientMessage = String.format("Skill data exported successfully to: %s", outputFile.getAbsolutePath());
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", clientMessage, null);
		}
		catch (IOException e) {
			log.error("Failed to write to file", e);
		}
	}

	private void exportPlayerStats()
	{
		clientThread.invoke(() -> {
			if (client.getLocalPlayer() == null)
			{
				log.warn("Player is not logged in.");
				return;
			}

			String playerName = client.getLocalPlayer().getName();
			String fileName = config.fileName();

			if (config.includeTimestamp())
			{
				LocalDate today = LocalDate.now();
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
				fileName += '_' + today.format(formatter);
			}

			fileName += ".csv";
			String outputDirectory = config.outputDirectory();

			if(!outputDirectory.isEmpty() && !outputDirectory.endsWith(File.separator)) {
				outputDirectory += File.separator;
			}

			saveFile(fileName, outputDirectory, playerName);
		});
	}

	// This version is for the chatCommandManager to call
	private void exportPlayerStats(ChatMessage message, String arguments)
	{
		exportPlayerStats();
	}

	@Override
	protected void startUp()
	{
		chatCommandManager.registerCommand("export", this::exportPlayerStats);
	}

	@Override
	protected void shutDown()
	{
		chatCommandManager.unregisterCommand("export");
	}

	@Provides
	SkillDataExporterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SkillDataExporterConfig.class);
	}
}