package me.mxndarijn.weerwolven.game.events;

import me.mxndarijn.weerwolven.data.UpcomingGameStatus;
import me.mxndarijn.weerwolven.game.Game;
import me.mxndarijn.weerwolven.game.GamePlayer;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public class GamePreStartEvents extends GameEvent {
    public GamePreStartEvents(Game g, JavaPlugin plugin) {
        super(g, plugin);
    }

    @EventHandler
    public void move(PlayerMoveEvent e) {
        if (game.getGameInfo().getStatus() != UpcomingGameStatus.CHOOSING_PLAYERS)
            return;
        if (
                checkValues(e.getFrom().getBlockX(), e.getTo().getBlockX()) &&
                        checkValues(e.getFrom().getBlockZ(), e.getTo().getBlockZ())
        ) {
            return;
        }
        if (!validateWorld(e.getPlayer().getWorld())) {
            return;
        }
        Optional<GamePlayer> gp = game.getGamePlayerOfPlayer(e.getPlayer().getUniqueId());
        if (gp.isEmpty()) {
            return;
        }
        GamePlayer gamePlayer = gp.get();
        Location l = e.getPlayer().getLocation();
        Location gL = gamePlayer.getColorData().getSpawnLocation().getLocation(e.getPlayer().getWorld());
        if (l.getBlockX() == gL.getBlockX() && l.getBlockZ() == gL.getBlockZ()) {
            return;
        }
        e.getPlayer().teleport(gL);

    }


    public boolean checkValues(int i, int b) {
        return i == b;
    }

    @EventHandler
    public void damage(EntityDamageEvent e) {
        if (game.getGameInfo().getStatus() != UpcomingGameStatus.CHOOSING_PLAYERS)
            return;
        if (!validateWorld(e.getEntity().getWorld()))
            return;
        e.setCancelled(true);
    }

    @EventHandler
    public void interact(PlayerInteractEvent e) {
        if (game.getGameInfo().getStatus() != UpcomingGameStatus.CHOOSING_PLAYERS)
            return;
        if (!validateWorld(e.getPlayer().getWorld()))
            return;
        if (game.getHosts().contains(e.getPlayer().getUniqueId())) {
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void hunger(FoodLevelChangeEvent e) {
        if (game.getGameInfo().getStatus() != UpcomingGameStatus.CHOOSING_PLAYERS)
            return;
        if (!validateWorld(e.getEntity().getWorld()))
            return;
        e.setCancelled(true);
    }

    @EventHandler
    public void breakBlock(BlockBreakEvent e) {
        if (game.getGameInfo().getStatus() != UpcomingGameStatus.CHOOSING_PLAYERS)
            return;
        if (!validateWorld(e.getPlayer().getWorld()))
            return;

        Optional<GamePlayer> gamePlayer = game.getGamePlayerOfPlayer(e.getPlayer().getUniqueId());
        if (gamePlayer.isEmpty())
            return;

        e.setCancelled(true);
    }

}
