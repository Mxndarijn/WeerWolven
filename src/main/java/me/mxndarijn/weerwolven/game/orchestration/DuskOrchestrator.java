package me.mxndarijn.weerwolven.game.orchestration;

import me.mxndarijn.weerwolven.data.ActionKind;
import me.mxndarijn.weerwolven.data.Roles;
import me.mxndarijn.weerwolven.data.Timing;
import me.mxndarijn.weerwolven.game.action.RoleAbilityRegistry;
import me.mxndarijn.weerwolven.game.action.RoleAbilityRegistry.AbilityDef;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;

import java.util.Comparator;
import java.util.List;
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
