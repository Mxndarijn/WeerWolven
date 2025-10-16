package me.mxndarijn.weerwolven.presets;

import lombok.Getter;
import lombok.Setter;
import me.mxndarijn.weerwolven.data.Colors;
import me.mxndarijn.weerwolven.data.WeerWolvenPrefix;
import nl.mxndarijn.mxlib.logger.LogLevel;
import nl.mxndarijn.mxlib.logger.Logger;
import nl.mxndarijn.mxlib.mxworld.MxLocation;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Getter
@Setter
public class ColorData {

    private MxLocation spawnLocation;
    private Colors color;
    private List<MxLocation> doorLocations;
    private List<MxLocation> windowLocations;


    private ColorData() {
    }


    public static Optional<ColorData> load(ConfigurationSection configurationSection, File file) {
        if (configurationSection == null) {
            return Optional.empty();
        }
        ColorData data = new ColorData();
        Optional<MxLocation> optionalMxLocation = MxLocation.loadFromConfigurationSection(configurationSection.getConfigurationSection("spawn"));
        Optional<Colors> optionalColor = Colors.getColorByType(configurationSection.getName());
        if (optionalColor.isPresent()) {
            data.setColor(optionalColor.get());
        } else {
            Logger.logMessage(LogLevel.ERROR, WeerWolvenPrefix.PRESETS_MANAGER, "Could not load color for color: " + configurationSection.getCurrentPath() + " (" + file.getAbsolutePath() + ")");
            return Optional.empty();
        }
        if (optionalMxLocation.isPresent()) {
            data.setSpawnLocation(optionalMxLocation.get());
        } else {
            Logger.logMessage(LogLevel.ERROR, WeerWolvenPrefix.PRESETS_MANAGER, "Could not load spawnpoint for color: " + configurationSection.getCurrentPath() + " (" + file.getAbsolutePath() + ")");
            return Optional.empty();
        }
        List<MxLocation> doors = new ArrayList<>();
        configurationSection.getMapList("doors").forEach(map -> {
            Map<String, Object> convertedMap = (Map<String, Object>) map;
            Optional<MxLocation> location = MxLocation.loadFromMap(convertedMap);
            if (location.isPresent()) {
                doors.add(location.get());
            } else {
                Logger.logMessage(LogLevel.ERROR, WeerWolvenPrefix.PRESETS_MANAGER, "Could not load door for color: " + configurationSection.getCurrentPath() + " (" + file.getAbsolutePath() + ")");
            }
        });

        List<MxLocation> windows = new ArrayList<>();
        configurationSection.getMapList("windows").forEach(map -> {
            Map<String, Object> convertedMap = (Map<String, Object>) map;
            Optional<MxLocation> location = MxLocation.loadFromMap(convertedMap);
            if (location.isPresent()) {
                windows.add(location.get());
            } else {
                Logger.logMessage(LogLevel.ERROR, WeerWolvenPrefix.PRESETS_MANAGER, "Could not load window for color: " + configurationSection.getCurrentPath() + " (" + file.getAbsolutePath() + ")");
            }
        });
        data.setDoorLocations(doors);
        data.setWindowLocations(windows);
        return Optional.of(data);
    }

    public static ColorData createNew(Colors color, MxLocation location) {
        ColorData data = new ColorData();
        data.setColor(color);
        data.setSpawnLocation(location);
        data.setDoorLocations(new ArrayList<>());
        data.setWindowLocations(new ArrayList<>());
        return data;
    }

    public void save(ConfigurationSection parentSection, File file) {
        if (parentSection == null) {
            return;
        }

        if (this.color == null) {
            Logger.logMessage(LogLevel.ERROR, WeerWolvenPrefix.PRESETS_MANAGER, "Missing color while saving under: " + parentSection.getCurrentPath() + " (" + file.getAbsolutePath() + ")");
            return;
        }

        // Create the subsection for this color using its type (similar to ContainerInformation.save)
        ConfigurationSection section = parentSection.createSection(this.color.getType());

        // Save spawn location using library method to ensure world is included
        if (this.spawnLocation != null) {
            ConfigurationSection spawnSection = section.createSection("spawn");
            try {
                this.spawnLocation.write(spawnSection);
            } catch (Exception ex) {
                Logger.logMessage(LogLevel.ERROR, WeerWolvenPrefix.PRESETS_MANAGER, "Could not save spawn for color: " + section.getCurrentPath() + " (" + file.getAbsolutePath() + ")");
            }
        } else {
            Logger.logMessage(LogLevel.ERROR, WeerWolvenPrefix.PRESETS_MANAGER, "Missing spawn while saving: " + section.getCurrentPath() + " (" + file.getAbsolutePath() + ")");
        }

        // Save doors using MxLocation.write into map list
        List<Map<String, Object>> doors = new ArrayList<>();
        if (this.doorLocations != null) {
            for (MxLocation loc : this.doorLocations) {
                if (loc == null) continue;
                org.bukkit.configuration.file.YamlConfiguration tmp = new org.bukkit.configuration.file.YamlConfiguration();
                ConfigurationSection s = tmp.createSection("loc");
                try {
                    loc.write(s);
                    doors.add(s.getValues(true));
                } catch (Exception ex) {
                    Logger.logMessage(LogLevel.ERROR, WeerWolvenPrefix.PRESETS_MANAGER, "Could not save a door for color: " + section.getCurrentPath() + " (" + file.getAbsolutePath() + ")");
                }
            }
        }
        section.set("doors", doors);

        // Save windows using MxLocation.write into map list
        List<Map<String, Object>> windows = new ArrayList<>();
        if (this.windowLocations != null) {
            for (MxLocation loc : this.windowLocations) {
                if (loc == null) continue;
                org.bukkit.configuration.file.YamlConfiguration tmp = new org.bukkit.configuration.file.YamlConfiguration();
                ConfigurationSection s = tmp.createSection("loc");
                try {
                    loc.write(s);
                    windows.add(s.getValues(true));
                } catch (Exception ex) {
                    Logger.logMessage(LogLevel.ERROR, WeerWolvenPrefix.PRESETS_MANAGER, "Could not save a window for color: " + section.getCurrentPath() + " (" + file.getAbsolutePath() + ")");
                }
            }
        }
        section.set("windows", windows);
    }
}
