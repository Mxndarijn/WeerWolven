package me.mxndarijn.weerwolven.game.orchestration.executor;

import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;
import me.mxndarijn.weerwolven.game.timer.TimerScope;
import me.mxndarijn.weerwolven.game.timer.TimerSpec;
import nl.mxndarijn.mxlib.inventory.MxItemClicked;
import nl.mxndarijn.mxlib.item.MxSkullItemStackBuilder;
import nl.mxndarijn.mxlib.item.Pair;
import nl.mxndarijn.mxlib.logger.LogLevel;
import nl.mxndarijn.mxlib.logger.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;

public class AbilityExecutorHelper {

    public static List<Pair<ItemStack, MxItemClicked>> mapGamePlayerToItem(Game game, GamePlayer actor, boolean includeItself, Predicate<GamePlayer> filter, MxItemClicked clicked, Function<GamePlayer, List<String>> getExtraLores) {
        List<Pair<ItemStack, MxItemClicked>> list = new ArrayList<>();
        game.getGamePlayers().forEach(gp -> {
            if(gp == actor && !includeItself) return;
            if(!filter.test(gp)) return;
            if(gp.getOptionalPlayerUUID().isEmpty()) return;
            Player p = Bukkit.getPlayer(gp.getOptionalPlayerUUID().get());
            if(p == null) return;
            list.add(new Pair<>(MxSkullItemStackBuilder
                    .create(1)
                    .setSkinFromHeadsData(gp.getOptionalPlayerUUID().get().toString())
                    .setName(gp.getColoredName())
                    .addCustomTagString("game_player_uuid", gp.getOptionalPlayerUUID().get().toString())
                    .addBlankLore()
                    .addLores(getExtraLores.apply(gp))
                    .build(),
                    clicked
            ));
        });
        return list;
    }

    /**
     * Speel een geluid af bij 'source' voor alle spelers binnen 'radius' (in blokken),
     * waarbij volume per speler wordt geschaald o.b.v. afstand.
     *
     * @param world   wereld van de bron
     * @param source  locatie van het geluid
     * @param sound   te spelen geluid
     * @param baseVolume  maximum volume aan de bron (meestal 1.0f)
     * @param pitch   toonhoogte (1.0f normaal)
     * @param radius  straal in blokken waarbinnen spelers het horen
     * @param falloff volumecurve (input = afstand/radius in [0..1], output = schaal [0..1])
     *                Voorbeeld: t -> 1 - t  (lineair), of t -> Math.pow(1 - t, 2) (kwadratisch)
     * @param minAudibleVolume ondergrens zodat het niet helemaal wegvalt (bv. 0.05f), 0 voor geen ondergrens
     */
    public static void playSoundInRadius(
            World world,
            Location source,
            Sound sound,
            float baseVolume,
            float pitch,
            double radius,
            DoubleUnaryOperator falloff,
            float minAudibleVolume
    ) {
        if (world == null || source == null || sound == null || radius <= 0 || baseVolume <= 0) return;

        final double r2 = radius * radius;

        for (Player pl : world.getPlayers()) {
            // Alleen spelers in dezelfde wereld en binnen radius
            double dist2 = pl.getLocation().distanceSquared(source);
            if (dist2 > r2) continue;

            double dist = Math.sqrt(dist2);
            double t = Math.min(1.0, Math.max(0.0, dist / radius)); // genormaliseerde afstand [0..1]
            double scale = Math.max(0.0, Math.min(1.0, falloff.applyAsDouble(t)));

            float vol = (float) (baseVolume * scale);
            if (minAudibleVolume > 0 && vol < minAudibleVolume && dist2 <= r2) {
                vol = minAudibleVolume;
            }

            if (vol <= 0f) continue;

            // Speel op de bron-locatie voor ruimtelijk geluid
            pl.playSound(source, sound, vol, pitch);
        }
    }

    /** Handige overload met lineaire afname en geen ondergrens. */
    public static void playSoundInRadiusLinear(World world, Location source, Sound sound, float baseVolume, float pitch, double radius) {
        playSoundInRadius(world, source, sound, baseVolume, pitch, radius, t -> 1.0 - t, 0f);
    }

    /** Handige overload met kwadratische afname (zachter op afstand) + minieme ondergrens. */
    public static void playSoundInRadiusSmooth(World world, Location source, Sound sound, float baseVolume, float pitch, double radius) {
        playSoundInRadius(world, source, sound, baseVolume, pitch, radius, t -> {
            // Kwadratisch: dichterbij duidelijk hoorbaar, verder sneller zachter
            double s = 1.0 - t;
            return s * s;
        }, 0.03f);
    }

    public static void playSoundInRadiusSmooth(GamePlayer gp, Sound sound, float baseVolume, float pitch, double radius) {
        Optional<Player> optional = gp.getBukkitPlayer();
        if(optional.isEmpty()) {
            Logger.logMessage(LogLevel.ERROR, "GamePlayer " + gp.getColorData().getColor().getDisplayName() + "<gray> has no BukkitPlayer, cannot play sound");
            return;
        }
        playSoundInRadius(optional.get().getWorld(), optional.get().getLocation(), sound, baseVolume, pitch, radius, t -> {
            double s = 1.0 - t;
            return s * s;
        }, 0.03f);
    }

    /**
     * Plays a sound directly at a player's current location.
     * <p>
     * This is a simple convenience method for playing feedback sounds that should be heard clearly
     * by a specific player without distance-based volume falloff. The sound is played at the player's
     * location, making it spatial and audible to nearby players as well.
     * <p>
     * If the GamePlayer has no associated Bukkit Player (offline or invalid), logs an error and returns.
     *
     * @param gp         the GamePlayer at whose location the sound should play
     * @param sound      the Sound to play
     * @param volume     the volume (typically 1.0f for normal volume)
     * @param pitch      the pitch (1.0f for normal pitch, higher = higher tone)
     */
    public static void playSoundAtPlayer(GamePlayer gp, Sound sound, float volume, float pitch) {
        Optional<Player> optional = gp.getBukkitPlayer();
        if(optional.isEmpty()) {
            Logger.logMessage(LogLevel.ERROR, "GamePlayer " + gp.getColorData().getColor().getDisplayName() + "<gray> has no BukkitPlayer, cannot play sound");
            return;
        }
        Player player = optional.get();
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    /**
     * Sends players home and waits for them to arrive, with a timeout that teleports stragglers.
     * <p>
     * This method:
     * <ul>
     *   <li>Cleans up house permissions (closes doors/windows, disables door opening)</li>
     *   <li>Sets up callbacks to track when each player arrives home</li>
     *   <li>Starts a timer with the specified timeout</li>
     *   <li>If timeout occurs, teleports players who haven't arrived to their spawn location</li>
     *   <li>Completes the returned future when all players are home or timeout occurs</li>
     * </ul>
     *
     * @param actors    the list of players to send home
     * @param game      the game instance
     * @param timerId   unique identifier for the timer (e.g., "werewolf:eliminate:home:1")
     * @param timeoutMs timeout in milliseconds before teleporting stragglers (default: 45000ms / 45s)
     * @return a CompletableFuture that completes with true when all players are home (or teleported)
     */
    public static CompletableFuture<Boolean> goHome(List<GamePlayer> actors, Game game, String timerId, long timeoutMs) {
        CompletableFuture<Boolean> homeFuture = new CompletableFuture<>();
        
        // Track which players have arrived home
        final Set<GamePlayer> completed = new HashSet<>();
        final int total = actors.size();
        
        var houseMgr = game.getGameHouseManager();
        
        // Clean up house permissions first
        for (GamePlayer gp : actors) {
            houseMgr.setCanOpenDoor(gp, true);
            houseMgr.openHouseDoor(gp, null);
        }
        
        // Set up callbacks for when players reach home
        for (GamePlayer gp : actors) {
            houseMgr.setOnPlayerReturnHome(gp, () -> {
                completed.add(gp);
                // When all are home, finish early
                if (completed.size() >= total) {
                    game.getActionTimerService().cancel(timerId);
                    cleanupHomeCallbacks(houseMgr, actors);
                    if (!homeFuture.isDone()) {
                        homeFuture.complete(true);
                    }
                }
                houseMgr.closeHouseDoor(gp, null);
                houseMgr.setCanOpenDoor(gp, false);
                houseMgr.closeHouseWindows(gp);
            });
        }
        
        // Start a timer; if timeout, teleport stragglers
        var spec = new TimerSpec(
                timerId,
                "<blue>Naar huis", // Timer title
                TimerScope.GROUP,
                new HashSet<>(actors),
                timeoutMs,
                ctx -> game.formatAction(ctx, completed.size(), total, " thuis"),
                ctx -> {
                    // Timeout: teleport players who haven't arrived home yet
                    var optWorld = game.getOptionalMxWorld();
                    if (optWorld.isPresent()) {
                        World world = Bukkit.getWorld(optWorld.get().getWorldUID());
                        if (world != null) {
                            Bukkit.getScheduler().runTask(game.getPlugin(), () -> {
                                for (GamePlayer gp : actors) {
                                    if (!completed.contains(gp)) {
                                        gp.getOptionalPlayerUUID().ifPresent(uuid -> {
                                            Player p = Bukkit.getPlayer(uuid);
                                            if (p != null) {
                                                var cd = gp.getColorData();
                                                if (cd != null && cd.getSpawnLocation() != null) {
                                                    var loc = cd.getSpawnLocation().getLocation(world);
                                                    if (loc != null && !p.isDead()) {
                                                        p.teleport(loc);
                                                    }
                                                }
                                            }
                                        });
                                    }
                                }
                                cleanupHomeCallbacks(houseMgr, actors);
                                if (!homeFuture.isDone()) {
                                    homeFuture.complete(true);
                                }
                            });
                        } else {
                            cleanupHomeCallbacks(houseMgr, actors);
                            homeFuture.complete(false);
                        }
                    } else {
                        cleanupHomeCallbacks(houseMgr, actors);
                        homeFuture.complete(false);
                    }
                },
                null,
                null
        );
        game.getActionTimerService().addTimer(spec);
        
        return homeFuture;
    }

    /**
     * Overload with default timeout of 45 seconds.
     */
    public static CompletableFuture<Boolean> goHome(List<GamePlayer> actors, Game game, String timerId) {
        return goHome(actors, game, timerId, 45_000L);
    }

    private static void cleanupHomeCallbacks(me.mxndarijn.weerwolven.game.manager.GameHouseManager houseMgr, List<GamePlayer> players) {
        for (GamePlayer gp : players) {
            houseMgr.clearOnPlayerReturnHome(gp);
        }
    }

    public static void playSoundAtPlayers(List<GamePlayer> actors, Sound entityWolfGrowl, float v, float v1) {
        actors.forEach(gp -> playSoundAtPlayer(gp, entityWolfGrowl, v, v1));
    }
}
