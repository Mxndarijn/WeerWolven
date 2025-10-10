package me.mxndarijn.weerwolven.commands;

import me.mxndarijn.weerwolven.WeerWolven;
import me.mxndarijn.weerwolven.data.WeerWolvenLanguageText;
import me.mxndarijn.weerwolven.data.WeerWolvenPermissions;
import me.mxndarijn.weerwolven.managers.ScoreBoardManager;
import nl.mxndarijn.mxlib.language.LanguageManager;
import nl.mxndarijn.mxlib.mxscoreboard.MxScoreBoard;
import nl.mxndarijn.mxlib.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public class ToggleScoreboardCommand extends WeerWolvenMxCommand implements Listener {
    private final HashMap<UUID, MxScoreBoard> scoreBoardHashMap = new HashMap<>();
    public ToggleScoreboardCommand(WeerWolvenPermissions permission, boolean onlyPlayersCanExecute, boolean canBeExecutedInGame) {
        super(permission, onlyPlayersCanExecute, canBeExecutedInGame);

        Bukkit.getPluginManager().registerEvents(this, JavaPlugin.getPlugin(WeerWolven.class));

    }

    @Override
    public void execute(CommandSender sender, Command command, String label, String[] args) {
        Player p = (Player) sender;
        Optional<MxScoreBoard> scoreboard = ScoreBoardManager.getInstance().getPlayerScoreboard(p);
        if(scoreboard.isPresent()) {
            scoreBoardHashMap.put(p.getUniqueId(), scoreboard.get());
            ScoreBoardManager.getInstance().removePlayerScoreboard(p.getUniqueId(), scoreboard.get());
           MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.SCOREBOARD_HIDDEN));
        } else if(scoreBoardHashMap.containsKey(p.getUniqueId())) {
               ScoreBoardManager.getInstance().setPlayerScoreboard(p.getUniqueId(), scoreBoardHashMap.get(p.getUniqueId()));
               scoreBoardHashMap.remove(p.getUniqueId());
               MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.SCOREBOARD_SHOWED));
        } else {
               MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.SCOREBOARD_NOT_FOUND));

        }
    }

    @EventHandler
    public void quit(PlayerQuitEvent e) {
        scoreBoardHashMap.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void changeWorld(PlayerChangedWorldEvent e) {
        scoreBoardHashMap.remove(e.getPlayer().getUniqueId());
    }
}
