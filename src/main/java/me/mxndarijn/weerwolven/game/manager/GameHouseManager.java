package me.mxndarijn.weerwolven.game.manager;

import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;
import org.bukkit.block.Bed;
import me.mxndarijn.weerwolven.presets.ColorData;
import nl.mxndarijn.mxlib.mxworld.MxLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameHouseManager extends GameManager {

    private static final double EXTRA_SOUND_RADIUS_SQUARED = 64.0; // 8 blocks radius

    // Permission map: whether a GamePlayer may open their door
    private final Map<GamePlayer, Boolean> canOpenDoor = new ConcurrentHashMap<>();

    // Callback map for when a player returns home (right-clicks their own bed)
    private final Map<GamePlayer, Runnable> onPlayerReturnHome = new ConcurrentHashMap<>();

    public GameHouseManager(Game game) {
        super(game);
    }

    // Permission API
    public void setCanOpenDoor(GamePlayer player, boolean canOpen) {
        if (player == null) return;
        canOpenDoor.put(player, canOpen);
    }

    public boolean canOpenDoor(GamePlayer player) {
        if (player == null) return false;
        return canOpenDoor.getOrDefault(player, false);
    }

    // onPlayerReturnHome API
    public void setOnPlayerReturnHome(GamePlayer player, Runnable callback) {
        if (player == null || callback == null) return;
        onPlayerReturnHome.put(player, callback);
    }

    public Runnable getOnPlayerReturnHome(GamePlayer player) {
        if (player == null) return null;
        return onPlayerReturnHome.get(player);
    }

    public void clearOnPlayerReturnHome(GamePlayer player) {
        if (player == null) return;
        onPlayerReturnHome.remove(player);
    }

    public void closeHouseDoor(GamePlayer gamePlayer, List<GamePlayer> optionalExtraSoundPlayers) {
        Bukkit.getScheduler().runTask(game.getPlugin(), () -> {
            if (gamePlayer == null) return;
            boolean changed = toggleDoors(gamePlayer, false);
            if (changed) {
                playExtraDoorSounds(gamePlayer, optionalExtraSoundPlayers, false);
            }
        });
    }

    public void openHouseDoor(GamePlayer gamePlayer, List<GamePlayer> optionalExtraSoundPlayers) {
        Bukkit.getScheduler().runTask(game.getPlugin(), () -> {
            if (gamePlayer == null) return;
            if (!canOpenDoor(gamePlayer)) return; // respect permission
            boolean changed = toggleDoors(gamePlayer, true);
            if (changed) {
                playExtraDoorSounds(gamePlayer, optionalExtraSoundPlayers, true);
            }
        });
    }

    public void closeHouseWindows(GamePlayer gamePlayer) {
        Bukkit.getScheduler().runTask(game.getPlugin(), () -> toggleWindows(gamePlayer, false));
    }

    public void openHouseWindows(GamePlayer gamePlayer) {
        Bukkit.getScheduler().runTask(game.getPlugin(), () -> toggleWindows(gamePlayer, true));
    }

    // Bulk operations
    public void openAllDoors(Collection<GamePlayer> players) {
        Bukkit.getScheduler().runTask(game.getPlugin(), () -> {
            Collection<GamePlayer> targets = (players == null || players.isEmpty()) ? game.getGamePlayers() : players;
            for (GamePlayer gp : targets) {
                if (gp == null) continue;
                if (!canOpenDoor(gp)) continue;
                toggleDoors(gp, true);
            }
        });
    }

    public void closeAllDoors(Collection<GamePlayer> players) {
        Bukkit.getScheduler().runTask(game.getPlugin(), () -> {
            Collection<GamePlayer> targets = (players == null || players.isEmpty()) ? game.getGamePlayers() : players;
            for (GamePlayer gp : targets) {
                if (gp == null) continue;
                toggleDoors(gp, false);
            }
        });
    }

    private boolean toggleDoors(GamePlayer gamePlayer, boolean open) {
        var optionalMxWorld = gamePlayer.getGame().getOptionalMxWorld();
        if (optionalMxWorld.isEmpty()) return false;
        World world = Bukkit.getWorld(optionalMxWorld.get().getWorldUID());
        if (world == null) return false;

        ColorData colorData = gamePlayer.getColorData();
        if (colorData == null || colorData.getDoorLocations() == null) return false;

        boolean changedAny = false;
        for (MxLocation mxLoc : colorData.getDoorLocations()) {
            if (mxLoc == null) continue;
            Location loc = mxLoc.getLocation(world);
            if (loc == null) continue;
            Block block = loc.getBlock();
            BlockData data = block.getBlockData();
            if (!(data instanceof Openable openable)) continue;

            if (openable.isOpen() != open) {
                openable.setOpen(open);
                block.setBlockData(openable);
                changedAny = true;

                // Play sound only to the specific GamePlayer
                gamePlayer.getOptionalPlayerUUID().ifPresent(uuid -> {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        Sound s = open ? Sound.BLOCK_WOODEN_DOOR_OPEN : Sound.BLOCK_WOODEN_DOOR_CLOSE;
                        player.playSound(block.getLocation(), s, 1.0f, 1.0f);
                    }
                });
            }
        }
        return changedAny;
    }

    private void playExtraDoorSounds(GamePlayer gamePlayer, List<GamePlayer> optionalExtraSoundPlayers, boolean open) {
        if (gamePlayer == null || optionalExtraSoundPlayers == null) return;
        var optionalMxWorld = gamePlayer.getGame().getOptionalMxWorld();
        if (optionalMxWorld.isEmpty()) return;
        World world = Bukkit.getWorld(optionalMxWorld.get().getWorldUID());
        if (world == null) return;

        ColorData colorData = gamePlayer.getColorData();
        if (colorData == null || colorData.getDoorLocations() == null) return;
        Sound sound = open ? Sound.BLOCK_WOODEN_DOOR_OPEN : Sound.BLOCK_WOODEN_DOOR_CLOSE;

        for (MxLocation mxLoc : colorData.getDoorLocations()) {
            if (mxLoc == null) continue;
            Location loc = mxLoc.getLocation(world);
            if (loc == null) continue;
            for (GamePlayer gp : optionalExtraSoundPlayers) {
                if (gp == null) continue;
                gp.getOptionalPlayerUUID().ifPresent(uuid -> {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && Objects.equals(p.getWorld(), world) && p.getLocation().distanceSquared(loc) <= EXTRA_SOUND_RADIUS_SQUARED) {
                        p.playSound(loc, sound, 1.0f, 1.0f);
                    }
                });
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerRightClickBed(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block clicked = event.getClickedBlock();
        if (clicked == null) return;
        var optionalMxWorld = game.getOptionalMxWorld();
        if (optionalMxWorld.isEmpty()) return;
        clicked.getWorld();
        if (!Objects.equals(clicked.getWorld().getUID(), optionalMxWorld.get().getWorldUID())) return;

        // Find GamePlayer for this player
        Optional<GamePlayer> optionalGamePlayer = game.getGamePlayerOfPlayer(event.getPlayer().getUniqueId());
        if (optionalGamePlayer.isEmpty()) return;
        GamePlayer gp = optionalGamePlayer.get();
        if (!isPlayersOwnBed(clicked, gp.getColorData())) return;

        Runnable cb = onPlayerReturnHome.remove(gp);
        if (cb != null) {
            Bukkit.getScheduler().runTask(game.getPlugin(), cb);
        }
    }

    private static boolean isPlayersOwnBed(Block b, ColorData colorData) {
        if (b == null || colorData == null || colorData.getColor() == null) return false;
        var data = b.getBlockData();
        if (!(data instanceof Bed bed)) return false;
        return bed.getColor().equals(colorData.getColor().getDyeColor());
    }

    private void toggleWindows(GamePlayer gamePlayer, boolean open) {
        if (gamePlayer == null) return;
        var optionalMxWorld = gamePlayer.getGame().getOptionalMxWorld();
        if (optionalMxWorld.isEmpty()) return;
        World world = Bukkit.getWorld(optionalMxWorld.get().getWorldUID());
        if (world == null) return;

        ColorData colorData = gamePlayer.getColorData();
        if (colorData == null || colorData.getWindowLocations() == null) return;

        for (MxLocation mxLoc : colorData.getWindowLocations()) {
            if (mxLoc == null) continue;
            Location loc = mxLoc.getLocation(world);
            if (loc == null) continue;
            Block block = loc.getBlock();

            // Desired material for the window based on open/close instruction
            final Material desired = open ? Material.AIR : Material.BLACK_CONCRETE;
            if (block.getType() != desired) {
                block.setType(desired, false);
            }
        }
    }
}
