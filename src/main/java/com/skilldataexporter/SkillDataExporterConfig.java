package com.skilldataexporter;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("skilldataexporter")
public interface SkillDataExporterConfig extends Config
{
    @ConfigItem(
            keyName = "outputDirectory",
            name = "Output Directory",
            description = "The directory where the CSV file will be written",
            position = 1
    )
    default String outputDirectory()
    {
        return System.getProperty("user.home");
    }

    @ConfigItem(
            keyName = "includeTimestamp",
            name = "Include Timestamp",
            description = "Include timestamp in the filename (skill_levels_YYYY-MM-DD.csv)",
            position = 2
    )
    default boolean includeTimestamp() {
        return false;
    }

    @ConfigItem(
            keyName = "fileName",
            name = "File Name",
            description = "The name of the CSV file (without extension)",
            position = 3
    )
    default String fileName() {
        return "skill_levels";
    }

    @ConfigItem(
            keyName = "includeXP",
            name = "Include XP Values",
            description = "Include XP values in the CSV file",
            position = 4
    )
    default boolean includeXP() {
        return false;
    }
}
