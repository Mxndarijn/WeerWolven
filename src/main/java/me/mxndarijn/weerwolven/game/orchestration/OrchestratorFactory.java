package me.mxndarijn.weerwolven.game.orchestration;

import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.orchestration.executor.AbilityExecutorRegistry;
import me.mxndarijn.weerwolven.game.phase.Phase;

import java.util.concurrent.Executor;

/** Factory to build the correct PhaseOrchestrator for the current phase. */
public final class OrchestratorFactory {
    private final IntentCollector collector;
    private final OrchestrationConfig config;
    private final DefaultDecisionPolicy defaults;
    private final AbilityExecutorRegistry execs;
    private final ResolvePolicy resolvePolicy;
    private final Executor mainExec;

    public OrchestratorFactory(IntentCollector collector,
                               OrchestrationConfig config,
                               DefaultDecisionPolicy defaults,
                               AbilityExecutorRegistry execs,
                               ResolvePolicy resolvePolicy,
                               Executor mainExec) {
        this.collector = collector;
        this.config = config;
        this.defaults = defaults;
        this.execs = execs;
        this.resolvePolicy = resolvePolicy;
        this.mainExec = mainExec;
    }

    public PhaseOrchestrator create(Game game, Phase phase) {
        return switch (phase) {
            case NIGHT -> new NightOrchestrator(game, collector, config, defaults, execs, resolvePolicy, mainExec);
            case DAWN  -> new DawnOrchestrator(game, collector, config, defaults, execs, resolvePolicy, mainExec);
            case DAY   -> new DayOrchestrator(game, collector, config, defaults, execs, resolvePolicy, mainExec);
            case DUSK  -> new DuskOrchestrator(game, collector, config, defaults, execs, resolvePolicy, mainExec);
            default    -> null; // LOBBY/END have no orchestrators
        };
    }
}
