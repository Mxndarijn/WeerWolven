package me.mxndarijn.weerwolven.data;

import nl.mxndarijn.mxlib.configfiles.ConfigService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum ScoreBoard {
    PRESET(WeerWolvenConfigFiles.SCOREBOARD_PRESET),
    GAME_HOST(WeerWolvenConfigFiles.SCOREBOARD_HOST),
    GAME_PLAYER(WeerWolvenConfigFiles.SCOREBOARD_PLAYER),
    GAME_SPECTATOR(WeerWolvenConfigFiles.SCOREBOARD_SPECTATOR),
    SPAWN(WeerWolvenConfigFiles.SCOREBOARD_SPAWN);

    private final List<String> uneditedLines;
    private final String title;

    ScoreBoard(WeerWolvenConfigFiles configFile) {
        this.uneditedLines = new ArrayList<>();
        ConfigService.getInstance().get(configFile).getCfg().getStringList("lines").forEach(string -> {
            this.uneditedLines.add(string);
        });
        this.title = ConfigService.getInstance().get(configFile).getCfg().getString("title", "Unknown");
    }

    public String getTitle(HashMap<String, String> placeholders) {
        String newTitle = title;

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            newTitle = newTitle.replaceAll(k, v);
        }
        return newTitle;
    }

    public List<String> getLines(HashMap<String, String> placeholders) {
        List<String> newLines = new ArrayList<>();

        for (String string : uneditedLines) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                String k = entry.getKey();
                String v = entry.getValue();
                string = string.replaceAll(k, v);
            }
            newLines.add(string);
        }
        return newLines;
    }
}
