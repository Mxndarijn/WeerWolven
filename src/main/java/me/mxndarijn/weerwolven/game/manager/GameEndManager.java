package me.mxndarijn.weerwolven.game.manager;

import me.mxndarijn.weerwolven.WeerWolven;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;
import me.mxndarijn.weerwolven.data.UpcomingGameStatus;
import me.mxndarijn.weerwolven.game.core.win.WinConditionText;
import me.mxndarijn.weerwolven.game.core.win.WinResult;
import me.mxndarijn.weerwolven.data.WeerWolvenLanguageText;
import me.mxndarijn.weerwolven.util.FireworkHelper;
import net.kyori.adventure.title.Title;
import nl.mxndarijn.mxlib.changeworld.MxChangeWorldManager;
import nl.mxndarijn.mxlib.language.LanguageManager;
import nl.mxndarijn.mxlib.util.Functions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Centralizes end-of-game orchestration for a Game.
 * Reduces duplication in Game.stopGame and provides a single place to manage:
 * - Titles and announcement
 * - Optional 10s celebration with fireworks for winners
 * - Cleanup of players, scoreboards, visibility, and listeners
 * - World unload and deregistration from managers
 */
public class GameEndManager {

    private final Game game;
    private final JavaPlugin plugin;

    public GameEndManager(Game game) {
        this.game = game;
        this.plugin = JavaPlugin.getProvidingPlugin(WeerWolven.class);
    }

    /**
     * Ends the game with the provided result. If result.reason() is not NO_ONE, a 10s
     * celebration is shown and fireworks are launched around winning players; cleanup waits
     * for that period. Otherwise cleanup happens immediately.
     */
    public void endGame(WinResult result) {
        // Stop phase/timers and unhide everyone first so all see the title
        safeStopTimersAndLoop();
        broadcastStoppedMessage();
        showWinTitleToAll(result);

        if (result.reason() != WinConditionText.NO_ONE) {
            // Fireworks for winners for 10 seconds, then cleanup
            startWinFireworksFor(result.winners(), 10);
            runAfterTicks(10 * 20L, () -> cleanupAndUnload(result));
        } else {
            cleanupAndUnload(result);
        }
    }

    // ----- Initial stop helpers -----

    private void safeStopTimersAndLoop() {
        try {
            game.getActionTimerService().stop();
        } catch (Exception ignored) {
        }
        if (game.getGameVisibilityManager() != null) {
            game.getGameVisibilityManager().setCurrentState(
                    me.mxndarijn.weerwolven.game.manager.GameVisibilityManager.VisibilityState.everyone());
        }
        game.stopPhaseLoop();
    }

    private void broadcastStoppedMessage() {
        String msg = LanguageManager.getInstance().getLanguageString(
                WeerWolvenLanguageText.GAME_GAME_STOPPED);
        game.sendMessageToAll(msg);
    }

    private void showWinTitleToAll(WinResult result) {
        List<UUID> everyone = new ArrayList<>();
        everyone.addAll(game.getHosts());
        everyone.addAll(game.getGamePlayers().stream()
                .map(GamePlayer::getOptionalPlayerUUID)
                .flatMap(Optional::stream)
                .toList());
        everyone.addAll(game.getSpectators());

        // 10s title (10 in, 200 stay, 10 out) as used elsewhere in the project
        everyone.forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.showTitle(Title.title(
                        Functions.buildComponentFromString(result.reason().getPrefix()),
                        Functions.buildComponentFromString(result.reason().getSuffix()),
                        // Prefer Adventure Duration API if supported; fallback matches existing usage
                        Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(10), Duration.ofMillis(500))
                ));
            }
        });
    }

    // ----- Final cleanup and world handling -----

    private void cleanupAndUnload(WinResult result) {
        // Shutdown phase systems and runtime listeners
        if (game.getCycleManager() != null) game.getCycleManager().shutdown();
        // Close bus subscriptions via game helper
        try {
            var m = Game.class.getDeclaredMethod("unregisterRuntimeBusListeners");
            m.setAccessible(true);
            m.invoke(game);
        } catch (Exception ignored) { }

        // Remove world from change-world manager (if present)
        game.getOptionalMxWorld().ifPresent(world ->
                MxChangeWorldManager.getInstance().removeWorld(world.getWorldUID())
        );

        // Reset players (hosts, spectators, players)
        cleanupHosts();
        cleanupSpectators();
        cleanupGamePlayers();

        // Unload world and remove game from registries
        game.unloadWorld().thenAccept(unloaded -> {
            if (unloaded) {
                // Clear mxWorld via reflection since field is private; keep logic inside Game otherwise
                try {
                    var f = Game.class.getDeclaredField("mxWorld");
                    f.setAccessible(true);
                    f.set(game, null);
                } catch (Exception ignored) { }
            }
            me.mxndarijn.weerwolven.managers.GameManager.getInstance().removeUpcomingGame(game.getGameInfo());
            me.mxndarijn.weerwolven.managers.GameWorldManager.getInstance().removeGame(game);
            game.getGameInfo().setStatus(UpcomingGameStatus.FINISHED);
        });
    }

    private void cleanupHosts() {
        game.getHosts().forEach(u -> {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                me.mxndarijn.weerwolven.managers.ScoreBoardManager.getInstance()
                        .removePlayerScoreboard(p.getUniqueId(), game.getHostScoreboard());
                me.mxndarijn.weerwolven.managers.VanishManager.getInstance().showPlayerForAll(p);
                p.setHealth(20);
                p.getActivePotionEffects().clear();
                p.setFoodLevel(20);
            }
        });
    }

    private void cleanupSpectators() {
        game.getSpectators().forEach(u -> {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                me.mxndarijn.weerwolven.managers.VanishManager.getInstance().showPlayerForAll(p);
                me.mxndarijn.weerwolven.managers.ScoreBoardManager.getInstance()
                        .removePlayerScoreboard(u, game.getSpectatorScoreboard());
                p.setHealth(20);
                p.getActivePotionEffects().clear();
                p.setFoodLevel(20);
            }
        });
    }

    private void cleanupGamePlayers() {
        game.getGamePlayers().forEach(gp -> gp.getOptionalPlayerUUID().ifPresent(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                me.mxndarijn.weerwolven.managers.ScoreBoardManager.getInstance()
                        .removePlayerScoreboard(p.getUniqueId(), gp.getScoreboard());
                me.mxndarijn.weerwolven.managers.VanishManager.getInstance().showPlayerForAll(p);
                p.setHealth(20);
                p.getActivePotionEffects().clear();
                p.setFoodLevel(20);
            }
        }));
    }

    private void runAfterTicks(long ticks, Runnable action) {
        Bukkit.getScheduler().runTaskLater(plugin, action, ticks);
    }

    // ----- Fireworks orchestration -----

    private void startWinFireworksFor(List<UUID> winnerUuids, int seconds) {
        if (winnerUuids == null || winnerUuids.isEmpty()) return;
        List<Player> targets = winnerUuids.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .toList();
        if (targets.isEmpty()) return;

        final int period = 10; // ticks (0.5s)
        final int totalTicks = seconds * 20;
        AtomicInteger elapsed = new AtomicInteger(0);

        final org.bukkit.scheduler.BukkitTask[] holder = new org.bukkit.scheduler.BukkitTask[1];
        holder[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int e = elapsed.addAndGet(period);
            for (Player p : targets) {
                FireworkHelper.launchRandomHigh(p.getLocation());
            }
            if (e >= totalTicks && holder[0] != null) {
                holder[0].cancel();
            }
        }, 0L, period);
    }
}
