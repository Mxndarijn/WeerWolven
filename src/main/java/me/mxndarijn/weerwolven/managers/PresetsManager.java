package me.mxndarijn.weerwolven.managers;

import me.mxndarijn.weerwolven.data.SpecialDirectories;
import me.mxndarijn.weerwolven.data.WeerWolvenPrefix;
import me.mxndarijn.weerwolven.presets.Preset;
import nl.mxndarijn.mxlib.logger.LogLevel;
import nl.mxndarijn.mxlib.logger.Logger;
import nl.mxndarijn.mxlib.mxworld.MxWorld;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

public class PresetsManager {
    private static PresetsManager instance;
    private final ArrayList<Preset> presets;

    private PresetsManager() {
        presets = new ArrayList<>();
        Arrays.stream(SpecialDirectories.PRESET_WORLDS.getDirectory().listFiles()).forEach(file -> {
            if (file.isDirectory()) {
                Optional<Preset> optionalPreset = Preset.create(file);
                if (optionalPreset.isPresent()) {
                    Logger.logMessage(LogLevel.INFORMATION, WeerWolvenPrefix.PRESETS_MANAGER, "Preset Added: " + file.getName());
                    presets.add(optionalPreset.get());
                } else {
                    Logger.logMessage(LogLevel.ERROR, WeerWolvenPrefix.PRESETS_MANAGER, "Could not load preset: " + file.getName());
                }
            }
        });
    }

    public static PresetsManager getInstance() {
        if (instance == null) {
            instance = new PresetsManager();
        }
        return instance;
    }

    public ArrayList<Preset> getConfiguredPresets() {
        ArrayList<Preset> list = new ArrayList<>();
        presets.forEach(preset -> {
            if (preset.getConfig().isConfigured()) {
                list.add(preset);
            }
        });

        return list;
    }

    public ArrayList<Preset> getNonConfiguredPresets() {
        ArrayList<Preset> list = new ArrayList<>();
        presets.forEach(preset -> {
            if (!preset.getConfig().isConfigured()) {
                list.add(preset);
            }
        });

        return list;
    }

    public Optional<Preset> getPresetById(String s) {
        for (Preset p : presets) {
            if (p.getDirectory().getName().equals(s)) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    public Optional<Preset> getPresetByWorldUID(UUID uid) {
        for (Preset p : presets) {
            if (p.getMxWorld().isPresent()) {
                MxWorld mxWorld = p.getMxWorld().get();
                if (mxWorld.isLoaded()) {
                    if (mxWorld.getWorldUID() == uid) {
                        return Optional.of(p);
                    }
                }
            }
        }
        return Optional.empty();
    }

    public ArrayList<Preset> getAllPresets() {
        return presets;
    }
}
