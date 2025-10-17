package me.mxndarijn.weerwolven.game.phase;

import me.mxndarijn.weerwolven.game.core.Game;
import nl.mxndarijn.mxlib.logger.Logger;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.CompletableFuture;

/**
 * Manages smooth day/night cycle transitions for a werewolf game.
 * <p>
 * This class handles:
 * <ul>
 *   <li>Smooth transitions between Minecraft day and night times (not instant)</li>
 *   <li>Integration with the game's phase system</li>
 *   <li>Gradual time progression for natural-looking transitions</li>
 * </ul>
 * </p>
 */
public class DayNightCycleManager {
    
    private final Game game;
    private final JavaPlugin plugin;
    private BukkitTask transitionTask;
    
    // Transition settings
    private static final long TRANSITION_TICKS = 200; // How long transition takes (5 seconds)
    private static final long UPDATE_INTERVAL = 2; // Ticks between updates (0.1 seconds)
    
    private boolean isTransitioning = false;
    private long targetTime;
    private long startTime;
    private long transitionStartedAt;
    private CompletableFuture<Void> currentTransitionFuture;
    
    public DayNightCycleManager(Game game, JavaPlugin plugin) {
        this.game = game;
        this.plugin = plugin;
    }
    
    /**
     * Smoothly transitions the world time to match the current phase.
     * 
     * @param phase The target phase to transition to
     * @return A CompletableFuture that completes when the transition is finished
     */
    public CompletableFuture<Void> transitionToPhase(Phase phase) {
        Logger.logMessage("Starting smooth day/night transition to " + phase.getColoredPhase(game.getDayNumber()));
        if (game.getMxWorld() == null) return CompletableFuture.completedFuture(null);

        World world = Bukkit.getWorld(game.getMxWorld().getWorldUID());
        if (world == null) return CompletableFuture.completedFuture(null);

        cancelTransition();

        currentTransitionFuture = new CompletableFuture<>();

        targetTime = phase.getMinecraftTime();
        startTime = world.getTime();
        transitionStartedAt = 0;
        isTransitioning = true;

        // Als het al gelijk is: klaar
        if (startTime == targetTime) {
            isTransitioning = false;
            Logger.logMessage("Finished smooth day/night transition got there");
            currentTransitionFuture.complete(null);
            return currentTransitionFuture;
        }

        transitionTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (transitionStartedAt == 0) {
                transitionStartedAt = System.currentTimeMillis();
            }

            long elapsed = (System.currentTimeMillis() - transitionStartedAt) / 50L; // ~ticks

            if (elapsed >= TRANSITION_TICKS) {
                world.setTime(targetTime);

                CompletableFuture<Void> futureToComplete = currentTransitionFuture;
                cancelTransition(); // zet flags opgeruimd
                if (futureToComplete != null) {
                    Logger.logMessage("Finished smooth day/night transition took too long");
                    futureToComplete.complete(null);
                }
                return;
            }

            // 2) Altijd vooruit interpoleren over 0..23999
            // Voorwaartse afstand (0..23999)
            long forwardDist = (targetTime - startTime) % 24000;
            if (forwardDist < 0) forwardDist += 24000; // maak positief

            double progress = (double) elapsed / TRANSITION_TICKS;
            long delta = (long) Math.round(forwardDist * progress);

            long currentTime = (startTime + delta) % 24000;
            if (currentTime < 0) currentTime += 24000;

            world.setTime(currentTime);
        }, 0L, UPDATE_INTERVAL);

        return currentTransitionFuture;
    }
    
    /**
     * Immediately sets the time to the target phase's time without smooth transition.
     * 
     * @param phase The target phase
     * @return A CompletableFuture that completes immediately (since this is instant)
     */
    public CompletableFuture<Void> setPhaseTimeInstant(Phase phase) {
        cancelTransition();
        
        if (game.getMxWorld() == null) {
            return CompletableFuture.completedFuture(null);
        }
        
        World world = Bukkit.getWorld(game.getMxWorld().getWorldUID());
        if (world == null) {
            return CompletableFuture.completedFuture(null);
        }
        
        world.setTime(phase.getMinecraftTime());
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Cancels any ongoing transition.
     */
    public void cancelTransition() {
        if (transitionTask != null) {
            transitionTask.cancel();
            transitionTask = null;
        }
        isTransitioning = false;
        
        // Complete any pending future to avoid hanging
        if (currentTransitionFuture != null && !currentTransitionFuture.isDone()) {
            currentTransitionFuture.complete(null);
        }
        currentTransitionFuture = null;
    }
    
    /**
     * Checks if a transition is currently in progress.
     * 
     * @return true if transitioning, false otherwise
     */
    public boolean isTransitioning() {
        return isTransitioning;
    }
    
    /**
     * Cleanup method to be called when the game ends.
     */
    public void shutdown() {
        cancelTransition();
    }
}
