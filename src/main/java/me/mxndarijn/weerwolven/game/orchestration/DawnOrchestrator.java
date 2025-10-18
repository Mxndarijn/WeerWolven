package me.mxndarijn.weerwolven.game.orchestration;

import me.mxndarijn.weerwolven.data.ActionKind;
import me.mxndarijn.weerwolven.data.Roles;
import me.mxndarijn.weerwolven.data.Timing;
import me.mxndarijn.weerwolven.game.action.RoleAbilityRegistry;
import me.mxndarijn.weerwolven.game.action.RoleAbilityRegistry.AbilityDef;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;
import me.mxndarijn.weerwolven.game.orchestration.executor.AbilityExecutorRegistry;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/** Simple DAWN orchestrator: currently no ordered kinds; placeholder for future reports/effects. */
public final class DawnOrchestrator extends PhaseOrchestrator {

    private static final List<ActionKind> DAWN_ORDER = List.of(
            // Reserved for dawn-specific effects/reports
    );

    public DawnOrchestrator(Game game,
                            IntentCollector collector,
                            OrchestrationConfig config,
                            DefaultDecisionPolicy defaults,
                            AbilityExecutorRegistry execs,
                            ResolvePolicy resolvePolicy,
                            Executor mainExec) {
        super(game, collector, config, defaults, execs, resolvePolicy, mainExec);
    }

    @Override protected List<ActionKind> orderedKinds() { return DAWN_ORDER; }

    @Override
    protected List<GamePlayer> actorsFor(ActionKind kind) {
        return game.getGamePlayers().stream()
                .filter(GamePlayer::isAlive)
                .filter(gp -> hasAbilityAtTiming(gp.getRole(), kind, Timing.DAWN))
                .filter(gp -> canExecuteKind(gp, kind))
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
