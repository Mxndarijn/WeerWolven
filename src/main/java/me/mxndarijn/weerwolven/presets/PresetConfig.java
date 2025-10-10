package me.mxndarijn.weerwolven.presets;

import lombok.Getter;
import lombok.Setter;
import me.mxndarijn.weerwolven.data.Colors;
import me.mxndarijn.weerwolven.data.WeerWolvenPrefix;
import nl.mxndarijn.mxlib.logger.LogLevel;
import nl.mxndarijn.mxlib.logger.Logger;
import nl.mxndarijn.mxlib.mxworld.MxLocation;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

public class PresetConfig {
    @Getter
    private final HashMap<Colors, MxLocation> colors;
    @Setter
    private File file;
    @Setter
    @Getter
    private String name;
    @Setter
    @Getter
    private String skullId;
    @Setter
    @Getter
    private boolean locked;
    @Setter
    @Getter
    private String lockedBy;
    @Setter
    @Getter
    private String lockReason;
    @Setter
    @Getter
    private boolean configured;

    public PresetConfig(File file) {
        this.file = file;
        FileConfiguration fc = YamlConfiguration.loadConfiguration(file);
        Arrays.stream(PresetConfigValue.values()).forEach(value -> {
            if (!fc.contains(value.getConfigValue())) {
                Logger.logMessage(LogLevel.ERROR, WeerWolvenPrefix.PRESETS_MANAGER, "Could not find config value: " + value + " (" + file.getAbsolutePath() + ")");
            }
        });

        name = fc.getString(PresetConfigValue.NAME.getConfigValue());
        skullId = fc.getString(PresetConfigValue.SKULL_ID.getConfigValue());

        locked = fc.getBoolean(PresetConfigValue.LOCKED.getConfigValue());
        lockedBy = fc.getString(PresetConfigValue.LOCKED_BY.getConfigValue());
        lockReason = fc.getString(PresetConfigValue.LOCK_REASON.getConfigValue());

        configured = fc.getBoolean(PresetConfigValue.CONFIGURED.getConfigValue());

        colors = new HashMap<>();
        ConfigurationSection colorSection = fc.getConfigurationSection(PresetConfigValue.COLORS.getConfigValue());
        if (colorSection == null) {
            return;
        }
        colorSection.getKeys(false).forEach(key -> {
            Optional<Colors> color = Colors.getColorByType(key);
            if (color.isPresent()) {
                Optional<MxLocation> optionalMxLocation = MxLocation.loadFromConfigurationSection(colorSection.getConfigurationSection(key));
                if (optionalMxLocation.isPresent()) {
                    MxLocation mxLocation = optionalMxLocation.get();
                    colors.put(color.get(), mxLocation);
                } else {
                    Logger.logMessage(LogLevel.ERROR, WeerWolvenPrefix.PRESETS_MANAGER, "Could not load spawnpoint for color: " + key + " (" + file.getAbsolutePath() + ")");
                }

            } else {
                Logger.logMessage(LogLevel.ERROR, WeerWolvenPrefix.PRESETS_MANAGER, "Could not load color: " + key + " (" + file.getAbsolutePath() + ")");
            }

        });
    }

    public void save() {
        FileConfiguration fc = YamlConfiguration.loadConfiguration(file);
        fc.set(PresetConfigValue.NAME.getConfigValue(), name);
        fc.set(PresetConfigValue.SKULL_ID.getConfigValue(), skullId);
        fc.set(PresetConfigValue.LOCKED.getConfigValue(), locked);
        fc.set(PresetConfigValue.LOCKED_BY.getConfigValue(), lockedBy);
        fc.set(PresetConfigValue.LOCK_REASON.getConfigValue(), lockReason);
        fc.set(PresetConfigValue.CONFIGURED.getConfigValue(), configured);

        fc.set(PresetConfigValue.COLORS.getConfigValue(), null);

        ConfigurationSection section = fc.createSection(PresetConfigValue.COLORS.getConfigValue());
        for (Colors c : colors.keySet()) {
            colors.get(c).write(section.createSection(c.getType()));
        }

        try {
            fc.save(file);
        } catch (IOException e) {
            Logger.logMessage(LogLevel.ERROR, WeerWolvenPrefix.PRESETS_MANAGER, "Could not save preset config: " + file.getAbsolutePath());
            e.printStackTrace();
        }

    }

}
