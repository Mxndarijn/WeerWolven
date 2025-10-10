package me.mxndarijn.weerwolven.managers;

import me.clip.placeholderapi.PlaceholderAPI;
import me.mxndarijn.weerwolven.WeerWolven;
import me.mxndarijn.weerwolven.data.ScoreBoard;
import me.mxndarijn.weerwolven.data.WeerWolvenPermissions;
import nl.mxndarijn.mxlib.changeworld.ChangeWorldManager;
import nl.mxndarijn.mxlib.changeworld.MxChangeWorld;
import nl.mxndarijn.mxlib.mxscoreboard.MxSupplierScoreBoard;
import nl.mxndarijn.mxlib.util.Functions;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

public class SpawnManager implements Listener {

    private static SpawnManager instance;
    private final World spawn;
    private final JavaPlugin plugin;
    private final HashMap<UUID, MxSupplierScoreBoard> scoreboards = new HashMap<>();


    private SpawnManager() {
        plugin = JavaPlugin.getPlugin(WeerWolven.class);
        PluginManager manager = plugin.getServer().getPluginManager();
        this.spawn = Bukkit.getWorld("world");


        manager.registerEvents(this, plugin);

        ChangeWorldManager.getInstance().addWorld(spawn.getUID(), new MxChangeWorld() {
            @Override
            public void enter(Player p, World w, PlayerChangedWorldEvent e) {
                p.closeInventory();
                p.getInventory().clear();
                p.setGameMode(GameMode.ADVENTURE);

                p.getActivePotionEffects().forEach(effect -> {
                    p.removePotionEffect(effect.getType());
                });

                p.teleport(Functions.getSpawnLocation());
                //TODO
//                p.getInventory().addItem(Items.GAMES_ITEM.getItemStack());
                MxSupplierScoreBoard sb = scoreboards.get(p.getUniqueId());
                ScoreBoardManager.getInstance().setPlayerScoreboard(p.getUniqueId(), sb);

                // Add Items
            }

            @Override
            public void leave(Player p, World w, PlayerChangedWorldEvent e) {
                p.closeInventory();
                p.getInventory().clear();

                p.getActivePotionEffects().forEach(effect -> {
                    p.removePotionEffect(effect.getType());
                });

                MxSupplierScoreBoard sb = scoreboards.get(p.getUniqueId());
                ScoreBoardManager.getInstance().removePlayerScoreboard(p.getUniqueId(), sb);
            }

            @Override
            public void quit(Player p, World w, PlayerQuitEvent e) {
                // do nothing
            }
        });

    }

    public static SpawnManager getInstance() {
        if (instance == null)
            instance = new SpawnManager();
        return instance;
    }

    @EventHandler
    public void leave(PlayerQuitEvent e) {
        MxSupplierScoreBoard sb = scoreboards.remove(e.getPlayer().getUniqueId());
        sb.delete();
    }

    @EventHandler
    public void join(PlayerJoinEvent e) {
        MxSupplierScoreBoard sb =  new MxSupplierScoreBoard(plugin, () -> {
            return PlaceholderAPI.setPlaceholders(e.getPlayer(), ScoreBoard.SPAWN.getTitle(new HashMap<>()));
        }, () -> {
            return PlaceholderAPI.setPlaceholders(e.getPlayer(), ScoreBoard.SPAWN.getLines(new HashMap<>()));
        });
        sb.setUpdateTimer(100);
        scoreboards.put(e.getPlayer().getUniqueId(), sb);
        if (GameWorldManager.getInstance().isPlayerInAGame(e.getPlayer().getUniqueId())) {
            return;
        }
        if (e.getPlayer().getWorld() == spawn) {
            plugin.getServer().getPluginManager().callEvent(new PlayerChangedWorldEvent(e.getPlayer(), e.getPlayer().getWorld()));
        }
        e.getPlayer().teleport(Functions.getSpawnLocation());
    }

    @EventHandler
    public void breakEvent(BlockBreakEvent e) {
        if (!e.getBlock().getWorld().equals(spawn))
            return;

        if (!e.getPlayer().hasPermission(WeerWolvenPermissions.SPAWN_BLOCK_BREAK.getPermission()))
            e.setCancelled(true);
    }

    @EventHandler
    public void blockPlace(BlockPlaceEvent e) {
        if (!e.getBlock().getWorld().equals(spawn))
            return;

        if (!e.getPlayer().hasPermission(WeerWolvenPermissions.SPAWN_BLOCK_PLACE.getPermission()))
            e.setCancelled(true);
    }

    @EventHandler
    public void damage(EntityDamageEvent e) {
        if (!e.getEntity().getWorld().equals(spawn))
            return;
        e.setCancelled(true);
    }

    @EventHandler
    public void dropItem(PlayerDropItemEvent e) {
        if (!e.getPlayer().getWorld().equals(spawn))
            return;

        if (!e.getPlayer().hasPermission(WeerWolvenPermissions.SPAWN_DROP_ITEM.getPermission()))
            e.setCancelled(true);
    }


    @EventHandler
    public void pickupItem(EntityPickupItemEvent e) {
        if (!e.getEntity().getWorld().equals(spawn))
            return;

        if (!e.getEntity().hasPermission(WeerWolvenPermissions.SPAWN_PICKUP_ITEM.getPermission()))
            e.setCancelled(true);
    }

    @EventHandler
    public void inventoryChange(InventoryClickEvent e) {
        if (!e.getWhoClicked().getWorld().equals(spawn))
            return;

        if (e.getClickedInventory() == e.getWhoClicked().getInventory()) {
            if (!e.getWhoClicked().hasPermission(WeerWolvenPermissions.SPAWN_CHANGE_INVENTORY.getPermission()))
                e.setCancelled(true);
        }
    }

    @EventHandler
    public void hunger(FoodLevelChangeEvent e) {
        if (!e.getEntity().getWorld().equals(spawn))
            return;
        e.setFoodLevel(20);
        e.setCancelled(true);
    }

}
