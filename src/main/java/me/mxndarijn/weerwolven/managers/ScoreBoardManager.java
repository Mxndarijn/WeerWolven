package me.mxndarijn.weerwolven.managers;

import me.mxndarijn.weerwolven.WeerWolven;
import nl.mxndarijn.mxlib.mxscoreboard.MxScoreBoard;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public class ScoreBoardManager implements Listener {

    private static ScoreBoardManager instance;
    private final HashMap<UUID, MxScoreBoard> playerList;


    private ScoreBoardManager() {
        playerList = new HashMap<>();

        Bukkit.getServer().getPluginManager().registerEvents(this, JavaPlugin.getPlugin(WeerWolven.class));
    }

    public static ScoreBoardManager getInstance() {
        if (instance == null)
            instance = new ScoreBoardManager();
        return instance;
    }

    public void setPlayerScoreboard(UUID uuid, MxScoreBoard scoreboard) {
        if (playerList.containsKey(uuid))
            playerList.get(uuid).removePlayer(uuid);
        scoreboard.addPlayer(uuid);
        playerList.put(uuid, scoreboard);
    }

    @EventHandler
    public void quit(PlayerQuitEvent e) {
        if (playerList.containsKey(e.getPlayer().getUniqueId()))
            playerList.get(e.getPlayer().getUniqueId()).removePlayer(e.getPlayer().getUniqueId());
        playerList.remove(e.getPlayer().getUniqueId());
    }


    public void removePlayerScoreboard(UUID uniqueId, MxScoreBoard scoreboard) {
        if (playerList.containsKey(uniqueId) && playerList.get(uniqueId).equals(scoreboard))
            playerList.get(uniqueId).removePlayer(uniqueId);
        playerList.remove(uniqueId);
    }

    public Optional<MxScoreBoard> getPlayerScoreboard(Player p) {
        MxScoreBoard sc = playerList.get(p.getUniqueId());
        if(sc != null)
            return Optional.of(sc);
        return Optional.empty();
    }
}
