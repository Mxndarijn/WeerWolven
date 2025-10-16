package me.mxndarijn.weerwolven.game.events.minecraft;

import me.mxndarijn.weerwolven.data.UpcomingGameStatus;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public class GameFreezeEvents extends GameEvent {
    public GameFreezeEvents(Game g, JavaPlugin plugin) {
        super(g, plugin);
    }

    @EventHandler
    public void move(PlayerMoveEvent e) {
        if (game.getGameInfo().getStatus() != UpcomingGameStatus.FREEZE)
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
        e.getPlayer().teleport(new Location(e.getFrom().getWorld(), e.getFrom().getBlockX(), e.getFrom().getBlockY(), e.getFrom().getBlockZ()));

    }


    public boolean checkValues(int i, int b) {
        return i == b;
    }

    @EventHandler
    public void damage(EntityDamageEvent e) {
        if (game.getGameInfo().getStatus() != UpcomingGameStatus.FREEZE)
            return;
        if (!validateWorld(e.getEntity().getWorld()))
            return;
        e.setCancelled(true);
    }

    @EventHandler
    public void interact(PlayerInteractEvent e) {
        if (game.getGameInfo().getStatus() != UpcomingGameStatus.FREEZE)
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
        if (game.getGameInfo().getStatus() != UpcomingGameStatus.FREEZE)
            return;
        if (!validateWorld(e.getEntity().getWorld()))
            return;
        e.setCancelled(true);
    }

}
