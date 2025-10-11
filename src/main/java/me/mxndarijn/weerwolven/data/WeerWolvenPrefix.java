package me.mxndarijn.weerwolven.data;

import lombok.Getter;
import nl.mxndarijn.mxlib.logger.PrefixType;

@Getter
public enum WeerWolvenPrefix implements PrefixType {
    ITEM_MANAGER("<green>Item-Manager"),
    WORLD_MANAGER("<aqua>World-Manager"),
    PRESETS_MANAGER("<aqua>Presets-Manager"),
    MAPS_MANAGER("<aqua>Maps-Manager"),
    GAMES_MANAGER("<aqua>Games-Manager"),
    LOGGER("<dark_purple>Logger"),
    DATABASEMANAGER("<yellow>Database-Manager"),
    STORAGE_MANAGER("<green>Storage-Manager"),
    NAMETAG_MANAGER("<green>Nametag-Manager"),
    ROLE_SETS_MANAGER("<green>Role-Sets-Manager");

    private final String prefix;
    private final String name;

    WeerWolvenPrefix(String prefix) {
        this.prefix = "<dark_gray>" + "[" + prefix + "<dark_gray>" + "] ";
        this.name = prefix;
    }

    @Override
    public String toString() {
        return prefix;
    }
}
