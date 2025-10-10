package me.mxndarijn.weerwolven.presets;

import lombok.Getter;

@Getter
public enum PresetConfigValue {
    NAME("name"),
    COLORS("colors"),
    SKULL_ID("skull-id"),
    LOCKED("locked"),
    LOCKED_BY("locked-by"),
    LOCK_REASON("lock-reason"),
    CONFIGURED("configured");

    private final String configValue;

    PresetConfigValue(String value) {
        this.configValue = value;
    }

    @Override
    public String toString() {
        return configValue;
    }
}
