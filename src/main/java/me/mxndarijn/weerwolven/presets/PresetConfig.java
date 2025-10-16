package me.mxndarijn.weerwolven.presets;

import lombok.Getter;
import lombok.Setter;
import me.mxndarijn.weerwolven.data.Colors;
import me.mxndarijn.weerwolven.data.WeerWolvenPrefix;
import nl.mxndarijn.mxlib.logger.LogLevel;
import nl.mxndarijn.mxlib.logger.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class PresetConfig {
    @Getter
    private final List<ColorData> colors;
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

        colors = new ArrayList<>();
        ConfigurationSection colorSection = fc.getConfigurationSection(PresetConfigValue.COLORS.getConfigValue());
        if (colorSection == null) {
            return;
        }
        colorSection.getKeys(false).forEach(key -> {
            Optional<ColorData> optionalColorData = ColorData.load(colorSection.getConfigurationSection(key), file);
            if (optionalColorData.isPresent()) {
                colors.add(optionalColorData.get());
            } else {
                Logger.logMessage(LogLevel.ERROR, WeerWolvenPrefix.PRESETS_MANAGER, "Could not load color data for preset: " + file.getName());
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
        for (ColorData c : colors) {
            c.save(section, file);
        }

        try {
            fc.save(file);
        } catch (IOException e) {
            Logger.logMessage(LogLevel.ERROR, WeerWolvenPrefix.PRESETS_MANAGER, "Could not save preset config: " + file.getAbsolutePath());
            e.printStackTrace();
        }

    }

    public boolean containsColor(Colors c) {
        return colors.stream().anyMatch(colorData -> colorData.getColor().equals(c));
    }

    public Optional<ColorData> getColor(Colors c) {
        return colors.stream().filter(colorData -> colorData.getColor().equals(c)).findFirst();
    }
}
