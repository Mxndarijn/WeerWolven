package me.mxndarijn.weerwolven.game;

import lombok.Getter;
import me.mxndarijn.weerwolven.WeerWolven;
import me.mxndarijn.weerwolven.commands.PresetsCommand;
import me.mxndarijn.weerwolven.data.SpecialDirectories;
import me.mxndarijn.weerwolven.game.bus.GameEventBus;
import me.mxndarijn.weerwolven.managers.PresetsManager;
import me.mxndarijn.weerwolven.presets.Preset;
import me.mxndarijn.weerwolven.presets.PresetConfig;
import nl.mxndarijn.mxlib.mxworld.MxAtlas;
import nl.mxndarijn.mxlib.mxworld.MxWorld;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Getter
public class Game {


    private Phase phase = Phase.LOBBY;
    private int dayNumber = 0;

    private final GameInfo gameInfo;
    private final UUID mainHost;

    private final File directory;

    @Nullable
    private MxWorld mxWorld;

    private final ArrayList<UUID> hosts;
    private final PresetConfig config;
    private final List<GamePlayer> gamePlayers;
    private final GameEventBus gameEventBus = new GameEventBus();
    private final JavaPlugin plugin;

    public Game(UUID mainHost, GameInfo gameInfo, PresetConfig config, MxWorld mxWorld) {
        this.gameInfo = gameInfo;
        this.config = config;
        this.directory = mxWorld.getDir();
        this.mxWorld = mxWorld;
        this.mainHost = mainHost;
        this.plugin = JavaPlugin.getProvidingPlugin(WeerWolven.class);
        this.hosts = new ArrayList<>(List.of(mainHost));
        this.gamePlayers = new ArrayList<>();
        this.config.getColors().forEach(color -> {
           this.gamePlayers.add(new GamePlayer(color, plugin));
        });
    }

    public static Optional<Game> createGameFromGameInfo(UUID mainHost, GameInfo gameInfo) {
        Optional<Preset> map = PresetsManager.getInstance().getPresetById(gameInfo.getPresetId());
        if (map.isEmpty() || map.get().getMxWorld().isEmpty()) {
            return Optional.empty();
        }

        File newDir = new File(SpecialDirectories.GAMES_WORLDS.getDirectory() + "");
        Optional<MxWorld> optionalWorld = MxAtlas.getInstance().duplicateMxWorld(map.get().getMxWorld().get(), newDir);

        if (optionalWorld.isEmpty()) {
            return Optional.empty();
        }

        Game g = new Game(mainHost, gameInfo, map.get().getConfig(), optionalWorld.get());

        return Optional.of(g);
    }

}
