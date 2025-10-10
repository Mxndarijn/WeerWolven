package me.mxndarijn.weerwolven.data;

import nl.mxndarijn.mxlib.language.LanguageKey;

public enum WeerWolvenLanguageText implements LanguageKey {
    NO_PERMISSION("no-permission"),
    NO_PLAYER("no-player"),
    PRESET_INVENTORY_SAVED(),
    COMMAND_PRESETS_WORLD_COULD_NOT_BE_LOADED(),
    COMMAND_PRESETS_LOADING_WORLD(),
    COMMAND_PRESETS_NOW_IN_PRESET(),
    COMMAND_PRESETS_WORLD_NOT_FOUND_BUT_LOADED();

    private final String configValue;

    WeerWolvenLanguageText(String value) {
        this.configValue = value;
    }

    WeerWolvenLanguageText() {
        this.configValue = slug(name());
    }

    @Override
    public String key() {
        return configValue;
    }

    private static String slug(String enumName) {
        return enumName.toLowerCase().replaceAll("_", "-");
    }
}
