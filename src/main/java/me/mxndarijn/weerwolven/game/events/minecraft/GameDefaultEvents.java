package me.mxndarijn.weerwolven.game.events.minecraft;

import me.mxndarijn.weerwolven.WeerWolven;
import me.mxndarijn.weerwolven.data.WeerWolvenLanguageText;
import me.mxndarijn.weerwolven.game.Game;
import me.mxndarijn.weerwolven.game.GamePlayer;
import me.mxndarijn.weerwolven.managers.GameManager;
import me.mxndarijn.weerwolven.managers.ScoreBoardManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import nl.mxndarijn.mxlib.language.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Painting;
import org.bukkit.event.EventHandler;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.Optional;

public class GameDefaultEvents extends GameEvent {

    public GameDefaultEvents(Game g, JavaPlugin plugin) {
        super(g, plugin);
    }

    @EventHandler
    public void paintingBreak(HangingBreakByEntityEvent e) {
        if (e.getRemover() == null)
            return;
        if (!validateWorld(e.getRemover().getWorld()))
            return;
        Optional<GamePlayer> gamePlayer = game.getGamePlayerOfPlayer(e.getRemover().getUniqueId());
        if (gamePlayer.isEmpty())
            return;
        if (e.getEntity() instanceof Painting) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void join(PlayerJoinEvent e) {
        if (!validateWorld(e.getPlayer().getWorld())) return;

        Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(WeerWolven.class), () -> {
            if (!validateWorld(e.getPlayer().getWorld())) return;

            Optional<GamePlayer> gamePlayer = game.getGamePlayerOfPlayer(e.getPlayer().getUniqueId());
            if (gamePlayer.isEmpty() && !game.getHosts().contains(e.getPlayer().getUniqueId())) return;

            e.joinMessage(MiniMessage.miniMessage().deserialize("<!i>"));
            game.sendMessageToAll(LanguageManager.getInstance().getLanguageString(
                    WeerWolvenLanguageText.GAME_PLAYER_JOINED_AGAIN, Collections.singletonList(e.getPlayer().getName())));

            // Apply correct scoreboard after other systems finished their join work
            gamePlayer.ifPresent(gp -> ScoreBoardManager.getInstance()
                    .setPlayerScoreboard(e.getPlayer().getUniqueId(), gp.getScoreboard()));

            if (game.getHosts().contains(e.getPlayer().getUniqueId())) {
                ScoreBoardManager.getInstance().setPlayerScoreboard(
                        e.getPlayer().getUniqueId(), game.getHostScoreboard());
            }
        });
    }

    @EventHandler
    public void quit(PlayerQuitEvent e) {
        GameManager.getInstance().getUpcomingGameList().forEach(gameInfo -> {
            gameInfo.removePlayerFromQueue(e.getPlayer().getUniqueId());
        });
        if (!validateWorld(e.getPlayer().getWorld()))
            return;
        Optional<GamePlayer> gamePlayer = game.getGamePlayerOfPlayer(e.getPlayer().getUniqueId());
        if (gamePlayer.isEmpty() && !game.getHosts().contains(e.getPlayer().getUniqueId()))
            return;
        e.quitMessage(MiniMessage.miniMessage().deserialize("<!i>"));
        game.sendMessageToAll(LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_PLAYER_LEAVED_AGAIN, Collections.singletonList(e.getPlayer().getName())));
    }

}
