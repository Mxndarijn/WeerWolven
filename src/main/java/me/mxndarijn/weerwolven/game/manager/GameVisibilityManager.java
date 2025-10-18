package me.mxndarijn.weerwolven.game.manager;

import me.mxndarijn.weerwolven.data.Roles;
import me.mxndarijn.weerwolven.data.Team;
import me.mxndarijn.weerwolven.data.WeerWolvenPrefix;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;
import me.mxndarijn.weerwolven.game.status.StatusKey;
import nl.mxndarijn.mxlib.logger.LogLevel;
import nl.mxndarijn.mxlib.logger.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Controls in-game player visibility on a per-viewer basis.
 * - Hosts can always see everyone.
 * - Spectators see nobody by default, unless the state declares everyone sees everyone.
 * - Designed similar to GameChatManager with a flexible state API (role/status/team/custom function).
 *
 * Using Bukkit's per-viewer hide/show ensures the client doesn't track hidden entities,
 * which also suppresses related sounds/particles (like footsteps) from those entities for the viewer.
 */
public class GameVisibilityManager extends GameManager {

    private volatile VisibilityState currentState;

    public GameVisibilityManager(Game game) {
        super(game);
        this.currentState = VisibilityState.everyone();
    }

    public void setCurrentState(VisibilityState state) {
        if (state == null) {
            Logger.logMessage(LogLevel.DEBUG, WeerWolvenPrefix.DEBUG_HIGHLIGHT, 
                "Attempted to set null visibility state");
            return;
        }
        Logger.logMessage(LogLevel.DEBUG, WeerWolvenPrefix.DEBUG_HIGHLIGHT, 
            "Setting visibility state: everyoneSeesEveryone=" + state.everyoneSeesEveryone);
        this.currentState = state;
        applyCurrentState();
    }

    /**
     * Recompute all visibility relations according to the current state.
     * Runs on the main thread.
     */
    public void applyCurrentState() {
        Bukkit.getScheduler().runTask(game.getPlugin(), () -> {
            VisibilityState state = this.currentState;
            Logger.logMessage(LogLevel.DEBUG, WeerWolvenPrefix.DEBUG_HIGHLIGHT, 
                "Applying visibility state for game");

            // Prepare viewer pools
            List<UUID> hosts = new ArrayList<>(game.getHosts());
            List<GamePlayer> players = new ArrayList<>(game.getGamePlayers());
            List<UUID> spectators = new ArrayList<>(game.getSpectators());
            
            Logger.logMessage(LogLevel.DEBUG, WeerWolvenPrefix.DEBUG_HIGHLIGHT, 
                String.format("Visibility pools - Hosts: %d, Players: %d, Spectators: %d", 
                    hosts.size(), players.size(), spectators.size()));

            // Prepare Player handles
            Map<UUID, Player> onlineById = new HashMap<>();
            for (UUID id : hosts) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) onlineById.put(id, p);
            }
            for (GamePlayer gp : players) {
                gp.getOptionalPlayerUUID().ifPresent(id -> {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) onlineById.put(id, p);
                });
            }
            for (UUID id : spectators) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) onlineById.put(id, p);
            }

            // Build a lookup GamePlayer by UUID for player participants
            Map<UUID, GamePlayer> gpById = new HashMap<>();
            for (GamePlayer gp : players) {
                gp.getOptionalPlayerUUID().ifPresent(id -> gpById.put(id, gp));
            }

            // For each viewer, decide visibility for each target that is a host or game player.
            int visibilityChanges = 0;
            for (Map.Entry<UUID, Player> viewerEntry : onlineById.entrySet()) {
                UUID viewerId = viewerEntry.getKey();
                Player viewer = viewerEntry.getValue();
                boolean viewerIsHost = hosts.contains(viewerId);
                boolean viewerIsSpectator = spectators.contains(viewerId);
                @Nullable GamePlayer viewerGp = gpById.get(viewerId);

                Logger.logMessage(LogLevel.DEBUG, WeerWolvenPrefix.DEBUG_HIGHLIGHT, 
                    String.format("Processing viewer: %s (host=%b, spectator=%b, player=%b)", 
                        viewer.getName(), viewerIsHost, viewerIsSpectator, viewerGp != null));

                for (Map.Entry<UUID, Player> targetEntry : onlineById.entrySet()) {
                    UUID targetId = targetEntry.getKey();
                    if (Objects.equals(viewerId, targetId)) continue; // self always ignored

                    Player target = targetEntry.getValue();
                    boolean targetIsHost = hosts.contains(targetId);
                    @Nullable GamePlayer targetGp = gpById.get(targetId); // null for hosts/spectators

                    boolean visible;
                    if (viewerIsHost) {
                        // Hosts can always see everyone
                        visible = true;
                    } else if (viewerIsSpectator) {
                        // Spectators: only if explicitly allowed OR when everyone sees everyone
                        visible = state.everyoneSeesEveryone || state.spectatorCanSee.test(viewerId);
                    } else {
                        // Regular game player viewing
                        if (targetIsHost) {
                            // By default, players can see hosts (game facilitators)
                            visible = true;
                        } else {
                            // Only evaluate visibility against other game players
                            visible = (viewerGp != null && targetGp != null) && state.canSee.test(viewerGp, targetGp);
                        }
                    }

                    // Apply per-viewer visibility
                    if (visible) viewer.showPlayer(game.getPlugin(), target);
                    else viewer.hidePlayer(game.getPlugin(), target);
                    
                    visibilityChanges++;
                    Logger.logMessage(LogLevel.DEBUG, WeerWolvenPrefix.DEBUG_HIGHLIGHT, 
                        String.format("  %s -> %s: %s", viewer.getName(), target.getName(), 
                            visible ? "VISIBLE" : "HIDDEN"));
                }
            }
            
            Logger.logMessage(LogLevel.DEBUG, WeerWolvenPrefix.DEBUG_HIGHLIGHT, 
                String.format("Visibility state applied - %d visibility relations processed", visibilityChanges));
        });
    }

    // VisibilityState API and factories
    public static class VisibilityState {
        private final BiPredicate<GamePlayer, GamePlayer> canSee;
        private final Predicate<UUID> spectatorCanSee;
        private final boolean everyoneSeesEveryone;

        public VisibilityState(BiPredicate<GamePlayer, GamePlayer> canSee,
                               Predicate<UUID> spectatorCanSee,
                               boolean everyoneSeesEveryone) {
            this.canSee = (canSee == null) ? (a, b) -> true : canSee;
            this.spectatorCanSee = (spectatorCanSee == null) ? id -> false : spectatorCanSee;
            this.everyoneSeesEveryone = everyoneSeesEveryone;
        }

        public static VisibilityState everyone() {
            // Everyone (players/hosts) can see each other. Spectators see nobody by default.
            return new VisibilityState((a, b) -> true, id -> false, true);
        }

        public static VisibilityState onlyRoleSeesEachOther(Roles role) {
            return new VisibilityState((viewer, target) -> viewer.getRole() == role && target.getRole() == role,
                    id -> false,
                    false);
        }

        public static VisibilityState onlyWithStatus(StatusKey key) {
            return new VisibilityState((viewer, target) -> viewer.getStatusStore().has(key) && target.getStatusStore().has(key),
                    id -> false,
                    false);
        }

        public static VisibilityState sameTeamOnly() {
            return new VisibilityState((viewer, target) -> viewer.getRole().getTeam() == target.getRole().getTeam(),
                    id -> false,
                    false);
        }

        public static VisibilityState teamVisible(Team team) {
            return new VisibilityState((viewer, target) -> viewer.getRole().getTeam() == team && target.getRole().getTeam() == team,
                    id -> false,
                    false);
        }

        public static VisibilityState predicate(BiPredicate<GamePlayer, GamePlayer> canSee,
                                                Predicate<UUID> spectatorCanSee,
                                                boolean everyoneSeesEveryone) {
            return new VisibilityState(canSee, spectatorCanSee, everyoneSeesEveryone);
        }

        public static VisibilityState noOne() {
            return new VisibilityState((a, b) -> false, id -> false, false);
        }
    }
}
