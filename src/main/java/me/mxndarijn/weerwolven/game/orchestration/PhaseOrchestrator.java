// me.mxndarijn.weerwolven.game.orchestration.PhaseOrchestrator
package me.mxndarijn.weerwolven.game.orchestration;

import me.mxndarijn.weerwolven.data.ActionKind;
import me.mxndarijn.weerwolven.game.action.ActionIntent;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;
import me.mxndarijn.weerwolven.game.orchestration.executor.AbilityExecutor;
import me.mxndarijn.weerwolven.game.orchestration.executor.AbilityExecutorRegistry;
import me.mxndarijn.weerwolven.game.orchestration.executor.TeamAbilityExecutor;
import me.mxndarijn.weerwolven.game.phase.ResolveMode;
import me.mxndarijn.weerwolven.game.status.StatusKey;
import nl.mxndarijn.mxlib.logger.LogLevel;
import nl.mxndarijn.mxlib.logger.Logger;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Generic async orchestrator:
 * Walks an ordered list of ActionKind -> prompts SOLO or TEAM executors -> collects intents -> onDone().
 */
public abstract class PhaseOrchestrator {
    protected final Game game;
    protected final IntentCollector collector;
    protected final OrchestrationConfig config;
    protected final DefaultDecisionPolicy defaults;
    protected final AbilityExecutorRegistry execs;
    protected final ResolvePolicy resolvePolicy;
    protected final Executor mainExec;

    protected PhaseOrchestrator(Game game,
                                IntentCollector collector,
                                OrchestrationConfig config,
                                DefaultDecisionPolicy defaults,
                                AbilityExecutorRegistry execs,
                                ResolvePolicy resolvePolicy,
                                Executor mainExec) {
        this.game = game;
        this.collector = collector;
        this.config = config;
        this.defaults = defaults;
        this.execs = execs;
        this.resolvePolicy = resolvePolicy;
        this.mainExec = mainExec;
    }

    /** Ordered kinds for this phase (e.g., NIGHT_ORDER). */
    protected abstract List<ActionKind> orderedKinds();

    /** Who is eligible to act for this kind right now. */
    protected abstract List<GamePlayer> actorsFor(ActionKind kind);

    /** Optional: side-effect before prompting a SOLO actors of a given kind (e.g., open doors). */
    protected void beforePrompt(ActionKind kind, GamePlayer actor) {}

    /** Optional: hook after some intents were produced (or defaulted). */
    protected void afterIntents(ActionKind kind, List<GamePlayer> actors, List<ActionIntent> intents) {}

    /** Entry point: walk ORDER, prompt executors, then onDone. */
    public void runCollection(Runnable onDone) {
        final var phaseAtStart = game.getPhase();
        Logger.logMessage("Running phase orchestrator for phase: " + phaseAtStart);
        runKindsSequentially(0, () -> {
            Logger.logMessage("Finished phase orchestrator for phase: " + phaseAtStart);
            onDone.run();
        });
    }

    public boolean canExecuteKind(GamePlayer gp, ActionKind kind) {
        // Let PREVENT actions run so Jailers/Sleepers/Saboteurs can still act
        if (kind == ActionKind.JAIL || kind == ActionKind.SLEEP || kind == ActionKind.SABOTAGE) return true;

        var st = gp.getStatusStore();
        return !st.has(StatusKey.JAILED_TONIGHT) && !st.has(StatusKey.SLEEPS_TONIGHT);
    }

    public void runKindsSequentially(int idx, Runnable onDone) {
        var kinds = orderedKinds();
        if (kinds == null || kinds.isEmpty() || idx >= kinds.size()) {
            // Dispatch completion on the configured main executor to avoid deep re-entrancy
            mainExec.execute(onDone);
            return;
        }

        var kind = kinds.get(idx);
        Logger.logMessage("Running kind " + kind + " for phase " + game.getPhase());
        var actors = actorsFor(kind);
        if (actors == null || actors.isEmpty()) { runKindsSequentially(idx + 1, onDone); return; }

        var mode = resolvePolicy.mode(kind);
        Logger.logMessage("Kind " + kind + " resolves in " + mode);
        if (mode == ResolveMode.TEAM_AGGREGATED) {
            // TEAM path
            Optional<TeamAbilityExecutor> teamExec = execs.team(kind);
            Logger.logMessage("Kind " + kind + " resolves in TEAM_AGGREGATED, executorPresent=" + teamExec.isPresent());
            if (teamExec.isEmpty()) { runKindsSequentially(idx + 1, onDone); return; }

            long timeout = config.timeoutMs(kind);
            var fut = teamExec.get().executeTeam(game, actors, timeout);

            withTimeoutList(
                    fut,
                    timeout,
                    () -> teamExec.get().defaultExecute(game, actors, timeout),
                    intents -> {
                        if (intents != null) intents.forEach(collector::add);
                        afterIntents(kind, actors, intents == null ? List.of() : intents);
                        runKindsSequentially(idx + 1, onDone);
                    }
            );
        } else {
            promptActorsSequentially(kind, actors, () -> runKindsSequentially(idx + 1, onDone));
        }
    }

    private void promptActorsSequentially(ActionKind kind, List<GamePlayer> actors, Runnable onActorsDone) {
        promptOne(kind, actors, 0, onActorsDone);
    }

    private void promptOne(ActionKind kind, List<GamePlayer> actors, int i, Runnable onActorsDone) {
        if (i >= actors.size()) { onActorsDone.run(); return; }

        var actor = actors.get(i);
        if (!actor.isAlive()) { promptOne(kind, actors, i + 1, onActorsDone); return; }

        Optional<AbilityExecutor> soloExec = execs.solo(kind);
        if (soloExec.isEmpty()) { promptOne(kind, actors, i + 1, onActorsDone); return; }

        beforePrompt(kind, actor);

        long timeout = config.timeoutMs(kind);
        var fut = soloExec.get().execute(game, actor, timeout);

        withTimeoutList(
                fut,
                timeout,
                () -> {
                    try {
                        return soloExec.get().defaultExecute(game, actor, timeout);
                    } catch (Exception ex) {
                        Logger.logMessage(LogLevel.ERROR, "Error during defaultExecute for kind " + kind + ", actors=" + actor + ": " + ex.getMessage());
                        return List.of();
                    }
                },
                intents -> {
                    if (intents != null) intents.forEach(collector::add);
                    afterIntents(kind, List.of(actor), intents == null ? List.of() : intents);
                    promptOne(kind, actors, i + 1, onActorsDone);
                }
        );
    }

    /** Wrap a future<List<ActionIntent>> with timeout/default; always calls 'done' on completion. */
    private void withTimeoutList(CompletableFuture<List<ActionIntent>> fut,
                                 long timeoutMs,
                                 Supplier<List<ActionIntent>> onTimeout,
                                 Consumer<List<ActionIntent>> done) {
        final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        final AtomicBoolean finished = new AtomicBoolean(false);

        final ScheduledFuture<?> task = scheduler.schedule(() -> {
            try {
                if (finished.compareAndSet(false, true)) {
                    List<ActionIntent> fallback = onTimeout.get();
                    done.accept(fallback);
                }
            } finally {
                scheduler.shutdown();
            }
        }, Math.max(0, timeoutMs), TimeUnit.MILLISECONDS);

        fut.whenComplete((res, err) -> {
            try {
                task.cancel(false);
                if (finished.compareAndSet(false, true)) {
                    if (err != null) {
                        done.accept(onTimeout.get());
                    } else {
                        done.accept(res);
                    }
                }
            } finally {
                scheduler.shutdown();
            }
        });
    }
}
