package com.skilldataexporter;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.CommandExecuted;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
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

	@Inject
	private ConfigManager configManager;

	private static final String defaultFileName = "skill_levels";

	@Subscribe
	public void onCommandExecuted(CommandExecuted commandExecuted)
	{
		if (commandExecuted.getCommand().equals("export"))
		{
			exportPlayerStats();
		}
	}

	private void resetOutputDirectory()
	{
		Path defaultPath = Paths.get(RuneLite.RUNELITE_DIR.getAbsolutePath(), "skill_data_exporter");
		String message = String.format("Output directory reset to default: %s", defaultPath);
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);

	}

	private void saveFile(String fileName, String filePath, String playerName) {

		File outputFile = Paths.get(filePath, fileName).toFile();
		try (FileWriter writer = new FileWriter(outputFile, false))
		{
			// Write player name once at the top
			writer.write(String.format("Name,%s%n", playerName));

			// Write skill data
			if(!config.includeXP()) {
				for (Skill skill : Skill.values()) {
					int level = client.getRealSkillLevel(skill);
					int xp = client.getSkillExperience(skill);
					writer.write(String.format("%s,%d%n", skill.getName(), level));
				}
			}
			else
			{
				for (Skill skill : Skill.values()) {
					int level = client.getRealSkillLevel(skill);
					int xp = client.getSkillExperience(skill);
					writer.write(String.format("%s,%d,%d%n", skill.getName(), level, xp));
				}
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

			String fileName = defaultFileName;
			String playerName = client.getLocalPlayer().getName();

			if (config.includeTimestamp())
			{
				LocalDateTime today = LocalDateTime.now();
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
				fileName += '_' + today.format(formatter);
			}

			fileName += ".csv";


			// Ensure output directory exists. If it does not, the config value for output directory
			// is reset to  the default, RUNELITE_DIR/skill_data_exporter/
			String outputPath = config.outputDirectory().trim();
			if (outputPath.endsWith(File.separator)) {
				outputPath = outputPath.substring(0, outputPath.length() - 1);
			}
			File outputFile = new File(outputPath);
			String defaultDirectory = Paths.get(RuneLite.RUNELITE_DIR.getAbsolutePath(), "skill_data_exporter").toString();

			if(!outputFile.exists() || !outputFile.isDirectory()) {
				String message = "Output directory invalid. Resetting config value to default.";
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
				configManager.setConfiguration("skilldataexporter", "outputDirectory", defaultDirectory);

				// Create default directory
				File defaultDir = new File(defaultDirectory);
				if(!defaultDir.exists()) {
					if(!defaultDir.mkdirs()) {
						// This shouldn't happen but could if for some reason it can't create a subdirectory in
						// the RUNELITE_DIR for some reason. It will abort the export in this case.
						client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Failed to create default output directory. Aborting export.", null);
						return;
					}
				}
			}

			// Now use the config value for output directory
			String outputDirectory = config.outputDirectory();

			// Ensure the output directory ends with a file separator if it doesn't already.
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