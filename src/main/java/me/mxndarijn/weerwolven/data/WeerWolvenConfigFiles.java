package me.mxndarijn.weerwolven.data;

import lombok.Getter;
import nl.mxndarijn.mxlib.configfiles.ConfigFileType;

public enum WeerWolvenConfigFiles implements ConfigFileType {
    SCOREBOARD_PRESET("scoreboard_preset.yml", "scoreboards/scoreboard_preset.yml", false),
    SCOREBOARD_HOST("scoreboard_host.yml", "scoreboards/scoreboard_host.yml", false),
    SCOREBOARD_PLAYER("scoreboard_player.yml", "scoreboards/scoreboard_player.yml", false),
    SCOREBOARD_SPECTATOR("scoreboard_spectator.yml", "scoreboards/scoreboard_spectator.yml", false),
    UPCOMING_GAMES("upcoming-games.yml", "upcoming-games.yml", false),
    SCOREBOARD_SPAWN("scoreboard_spawn.yml", "scoreboards/scoreboard_spawn.yml", false),
    ROLESETS("rolesets.yml", "rolesets.yml", false);
    @Getter
    private final String fileName;

    private final String path;

    private final boolean autoSave;

    WeerWolvenConfigFiles(String fileName, String path, boolean autoSave) {
        this.fileName = fileName;
        this.path = path;
        this.autoSave = autoSave;
    }

    @Override
    public String fileName() {
        return fileName;
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public boolean autoSave() {
        return autoSave;
    }

}
