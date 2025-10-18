// me.mxndarijn.weerwolven.game.orchestration.NightOrchestrator
package me.mxndarijn.weerwolven.game.orchestration;

import me.mxndarijn.weerwolven.data.ActionKind;
import me.mxndarijn.weerwolven.data.Roles;
import me.mxndarijn.weerwolven.data.Timing;
import me.mxndarijn.weerwolven.game.action.RoleAbilityRegistry;
import me.mxndarijn.weerwolven.game.action.RoleAbilityRegistry.AbilityDef;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;
import me.mxndarijn.weerwolven.game.manager.GameChatManager;
import me.mxndarijn.weerwolven.game.manager.GameVisibilityManager;
import me.mxndarijn.weerwolven.game.orchestration.executor.AbilityExecutorRegistry;
import nl.mxndarijn.mxlib.logger.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public final class NightOrchestrator extends PhaseOrchestrator {

    private static final List<ActionKind> NIGHT_ORDER = List.of(
            // PREVENT / DISABLE
            ActionKind.JAIL, ActionKind.SLEEP, ActionKind.SABOTAGE,
            // PROTECT
            ActionKind.PROTECT,
            // INFO
            ActionKind.INSPECT, ActionKind.AURA_SCAN, ActionKind.COMPARE_TEAM,
            ActionKind.SPY_WATCH, ActionKind.TRACK_ELIMINATE_ACTIVITY,
            // SETUPS
            ActionKind.COUPLE, ActionKind.BREAD, ActionKind.TRAP_ARM, ActionKind.MASK_AS_SHAMAN,
            // KILLS
            ActionKind.TEAM_ELIMINATE,ActionKind.SOLO_ELIMINATE, ActionKind.BLESS
    );

    public NightOrchestrator(Game game,
                             IntentCollector collector,
                             OrchestrationConfig config,
                             DefaultDecisionPolicy defaults,
                             AbilityExecutorRegistry execs,
                             ResolvePolicy resolvePolicy,
                             Executor mainExec) {
        super(game, collector, config, defaults, execs, resolvePolicy, mainExec);
    }

    @Override protected List<ActionKind> orderedKinds() { return NIGHT_ORDER; }

    @Override
    public void runCollection(Runnable onDone) {
        final var phaseAtStart = game.getPhase();

        game.getGameChatManager().setCurrentState(GameChatManager.ChatState.noOne());
        game.getGameVisibilityManager().setCurrentState(GameVisibilityManager.VisibilityState.noOne());

        Logger.logMessage("Running night: " + phaseAtStart);
        runKindsSequentially(0, () -> {
            Logger.logMessage("Finished night: " + phaseAtStart);
            onDone.run();
        });
    }

    @Override
    protected List<GamePlayer> actorsFor(ActionKind kind) {
        return game.getGamePlayers().stream()
                .filter(GamePlayer::isAlive)
                .filter(gp -> hasNightAbility(gp.getRole(), kind))
                .filter(gp -> canExecuteKind(gp, kind))
                .sorted(Comparator.comparing(gp -> gp.getColorData().getColor().getDisplayName()))
                .collect(Collectors.toList());
    }

    private boolean hasNightAbility(Roles role, ActionKind kind) {
        for (AbilityDef def : RoleAbilityRegistry.of(role)) {
            if (def.kind() == kind && def.timing() == Timing.NIGHT) return true;
        }
        return false;
    }

    @Override
    protected void beforePrompt(ActionKind kind, GamePlayer actor) {
        // Optional: pre-UI/world prep (usually handled inside the concrete executor).
    }
}
