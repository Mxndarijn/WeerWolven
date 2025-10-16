package me.mxndarijn.weerwolven.data;

import lombok.Getter;
import me.mxndarijn.weerwolven.WeerWolven;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

@Getter
public enum SpecialDirectories {
    PRESET_WORLDS("presets"),
    GAMES_WORLDS("games");

    private static final List<SpecialDirectories> values = List.of(values());
    private final File directory;
    private final String folderName;

    SpecialDirectories(String folderName) {
        this.folderName = folderName;
        JavaPlugin plugin = JavaPlugin.getPlugin(WeerWolven.class);
        directory = new File(plugin.getDataFolder() + getPath());
        if (!directory.exists()) {
            boolean success = directory.mkdirs();

        }

    }

    private String getPath() {
        return folderName.startsWith("/") ? folderName : "/" + folderName;
    }

}
