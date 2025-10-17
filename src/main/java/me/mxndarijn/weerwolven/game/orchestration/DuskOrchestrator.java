package me.mxndarijn.weerwolven.game.orchestration;

import me.mxndarijn.weerwolven.data.ActionKind;
import me.mxndarijn.weerwolven.data.Roles;
import me.mxndarijn.weerwolven.data.Timing;
import me.mxndarijn.weerwolven.data.WeerWolvenLanguageText;
import me.mxndarijn.weerwolven.game.action.RoleAbilityRegistry;
import me.mxndarijn.weerwolven.game.action.RoleAbilityRegistry.AbilityDef;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;
import me.mxndarijn.weerwolven.game.manager.GameHouseManager;
import me.mxndarijn.weerwolven.game.manager.GameVisibilityManager;
import me.mxndarijn.weerwolven.game.timer.TimerFormats;
import me.mxndarijn.weerwolven.game.timer.TimerScope;
import me.mxndarijn.weerwolven.game.timer.TimerSpec;
import nl.mxndarijn.mxlib.language.LanguageManager;
import nl.mxndarijn.mxlib.logger.Logger;
import nl.mxndarijn.mxlib.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public final class DuskOrchestrator extends PhaseOrchestrator {

    private static final List<ActionKind> DUSK_ORDER = List.of(
            ActionKind.BOMB_DETONATE, ActionKind.IGNITE
    );

    public DuskOrchestrator(Game game,
                            IntentCollector collector,
                            OrchestrationConfig config,
                            DefaultDecisionPolicy defaults,
                            AbilityExecutorRegistry execs,
                            ResolvePolicy resolvePolicy,
                            Executor mainExec) {
        super(game, collector, config, defaults, execs, resolvePolicy, mainExec);
    }

    @Override protected List<ActionKind> orderedKinds() { return DUSK_ORDER; }

    @Override
    public void runCollection(Runnable onDone) {
        // Dusk instruction: everyone go home and click their bed
        List<GamePlayer> alive = game.getGamePlayers().stream()
                .filter(GamePlayer::isAlive)
                .filter(gp -> gp.getOptionalPlayerUUID().isPresent())
                .collect(Collectors.toList());
        int total = alive.size();

        // Inform players using language text
        String instruction = LanguageManager.getInstance()
                .getLanguageString(WeerWolvenLanguageText.DUSK_GO_HOME_INSTRUCTION);
        game.sendMessageToPlayers(instruction);
        game.sendMessageToHosts(LanguageManager.getInstance()
                .getLanguageString(WeerWolvenLanguageText.DUSK_GO_HOME_INSTRUCTION_HOST));
        game.sendMessageToSpectators(LanguageManager.getInstance()
                .getLanguageString(WeerWolvenLanguageText.DUSK_GO_HOME_INSTRUCTION_HOST));

        // Allow opening doors and set callbacks for returning home
        var houseMgr = game.getGameHouseManager();
        final Set<GamePlayer> completed = new HashSet<>();

        for (GamePlayer gp : alive) {
            houseMgr.setCanOpenDoor(gp, true);
            houseMgr.setOnPlayerReturnHome(gp, () -> {
                Logger.logMessage("callback");
                completed.add(gp);
                gp.getOptionalPlayerUUID().ifPresent(uuid -> {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance()
                                .getLanguageString(WeerWolvenLanguageText.GAME_ARRIVED_HOME));
                    }
                });
                // When all are home, finish early
                if (completed.size() >= total) {
                    // Everyone is home: hide players from each other and cleanup houses
                    game.getGameVisibilityManager().setCurrentState(
                            GameVisibilityManager.VisibilityState.predicate((a, b) -> false, id -> false, false)
                    );
                    cleanupDuskHome(houseMgr, alive);
                    // cancel timer
                    game.getActionTimerService().cancel("dusk:home:" + game.getDayNumber());
                    onDone.run();
                }
            });
        }

        // Start a group timer visible to all alive players
        String title = LanguageManager.getInstance()
                .getLanguageString(WeerWolvenLanguageText.DUSK_TIMER_TITLE);
        String timerId = "dusk:home:" + game.getDayNumber();
        long durationMs = 45_000L; // 60 seconds to get home

        var audience = new HashSet<>(alive);
        var spec = new TimerSpec(
                timerId,
                title,
                TimerScope.GROUP,
                audience,
                durationMs,
                ctx -> game.formatAction(ctx, completed.size(), total, " thuis"),
                ctx -> {
                    // Timeout: teleport all players who are not home to their house spawn, then proceed
                    var optWorld = game.getOptionalMxWorld();
                    if (optWorld.isPresent()) {
                        org.bukkit.World world = org.bukkit.Bukkit.getWorld(optWorld.get().getWorldUID());
                        if (world != null) {
                            org.bukkit.Bukkit.getScheduler().runTask(game.getPlugin(), () -> {
                                for (GamePlayer gp : alive) {
                                    if (!completed.contains(gp)) {
                                        gp.getOptionalPlayerUUID().ifPresent(uuid -> {
                                            org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(uuid);
                                            if (p != null) {
                                                var cd = gp.getColorData();
                                                if (cd != null && cd.getSpawnLocation() != null) {
                                                    org.bukkit.Location loc = cd.getSpawnLocation().getLocation(world);
                                                    if (loc != null && !p.isDead()) {
                                                        p.teleport(loc);
                                                    }
                                                }
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    }
                    // proceed to next phase even if not all are home
                    houseMgr.setAllPlayersCanOpenDoors(false);
                    cleanupDuskHome(houseMgr, alive);
                    onDone.run();
                },
                null,
                null
        );
        game.getActionTimerService().addTimer(spec);
    }

    private static void cleanupDuskHome(GameHouseManager houseMgr, List<GamePlayer> players) {
        // Revoke door permissions and callbacks
        for (GamePlayer gp : players) {
            houseMgr.setCanOpenDoor(gp, false);
            houseMgr.clearOnPlayerReturnHome(gp);
        }
        // Close all house doors for provided players
        houseMgr.closeAllDoors(players);
        // Close windows per player
        for (GamePlayer gp : players) {
            houseMgr.closeHouseWindows(gp);
        }
    }

    @Override
    protected List<GamePlayer> actorsFor(ActionKind kind) {
        return game.getGamePlayers().stream()
                .filter(GamePlayer::isAlive)
                .filter(gp -> hasAbilityAtTiming(gp.getRole(), kind, Timing.DUSK))
                .sorted(Comparator.comparing(gp -> gp.getColorData().getColor().getDisplayName()))
                .collect(Collectors.toList());
    }

    private boolean hasAbilityAtTiming(Roles role, ActionKind kind, Timing timing) {
        for (AbilityDef def : RoleAbilityRegistry.of(role)) {
            if (def.kind() == kind && def.timing() == timing) return true;
        }
        return false;
    }
}
