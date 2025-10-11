package me.mxndarijn.weerwolven.managers;


import lombok.Getter;
import me.mxndarijn.weerwolven.data.WeerWolvenConfigFiles;
import me.mxndarijn.weerwolven.game.GameInfo;
import me.mxndarijn.weerwolven.game.RoleSet;
import me.mxndarijn.weerwolven.presets.Preset;
import nl.mxndarijn.mxlib.configfiles.ConfigService;
import org.bukkit.configuration.ConfigurationSection;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GameManager {

    private static GameManager instance;
    private final WeerWolvenConfigFiles config;
    @Getter
    private List<GameInfo> upcomingGameList;

    private GameManager() {
        config = WeerWolvenConfigFiles.UPCOMING_GAMES;
        loadGames();
    }

    public static GameManager getInstance() {
        if (instance == null)
            instance = new GameManager();
        return instance;
    }

//    public void removeAllGamesWithMap(Map map) {
//        List<GameInfo> gamesToRemove = new ArrayList<>();
//
//        for (GameInfo game : upcomingGameList) {
//            if (game.getMapId().equals(map.getDirectory().getName())) {
//                gamesToRemove.add(game);
//            }
//        }
//        upcomingGameList.removeAll(gamesToRemove);
//    }

    private void loadGames() {
        upcomingGameList = new ArrayList<>();
        ConfigurationSection section = ConfigService.getInstance().get(config).getCfg();
        List<java.util.Map<?, ?>> list = section.getMapList("upcoming-games");
        list.forEach(map -> {
            java.util.Map<String, Object> convertedMap = (java.util.Map<String, Object>) map;
            Optional<GameInfo> game = GameInfo.loadFromFile(convertedMap);
            game.ifPresent(upcomingGameList::add);
        });
    }

    public void addUpcomingGame(UUID host, Preset preset, LocalDateTime date, RoleSet roleSet) {
        GameInfo upcomingGame = GameInfo.create(preset, host, date, roleSet);
        upcomingGameList.add(upcomingGame);
    }

    public void save() {

        List<java.util.Map<String, Object>> list = new ArrayList<>();
        upcomingGameList.forEach(upcomingGame -> {
            list.add(upcomingGame.getDataForSaving());
        });

        ConfigService.getInstance().get(config).getCfg().set("upcoming-games", list);
    }

    public void removeUpcomingGame(GameInfo upcomingGame) {
        upcomingGameList.remove(upcomingGame);
        upcomingGame.clearQueue();
    }
}