package me.mxndarijn.weerwolven.game.phase;

import me.mxndarijn.weerwolven.data.ActionKind;
import me.mxndarijn.weerwolven.data.Timing;
import me.mxndarijn.weerwolven.game.action.ActionHandler;
import me.mxndarijn.weerwolven.game.action.ActionIntent;
import me.mxndarijn.weerwolven.game.bus.GameEventBus;
import me.mxndarijn.weerwolven.game.core.Game;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The PhaseExecutor is responsible for orchestrating and executing game phases in a werewolf game.
 * <p>
 * This class manages the execution flow of different game phases (NIGHT, DAWN, DAY, DUSK) by processing
 * {@link ActionIntent}s in a specific order based on the current phase. Each action type is handled by
 * its corresponding {@link ActionHandler}, and the execution order ensures that game mechanics are
 * applied in a logically consistent manner.
 * </p>
 * <p>
 * The executor follows a strict ordering for actions within each phase:
 * <ul>
 *   <li><b>NIGHT:</b> Processes preventive actions, protective actions, information gathering, setups,
 *       and kill proposals. Includes reaction windows for special abilities like witch saves and poisons.</li>
 *   <li><b>DAWN:</b> Reserved for post-night reports and effects (currently empty).</li>
 *   <li><b>DAY:</b> Processes day-specific actions and buffs (e.g., wolf vote modifiers).</li>
 *   <li><b>DUSK:</b> Handles end-of-day effects like bomb detonations and ignitions.</li>
 * </ul>
 * </p>
 * <p>
 * The night phase includes a sophisticated pipeline with reaction windows that allow certain roles
 * (like Witch) to respond to events before final resolution. After all actions are processed,
 * the executor triggers aftermath handling for deaths and status cleanup.
 * </p>
 * <p>
 * This class is immutable and thread-safe, as all its fields are final and the handler map is
 * defensively copied during construction.
 * </p>
 *
 * @see ActionHandler
 * @see ActionIntent
 * @see PhaseHooks
 * @see Phase
 * @see ActionKind
 * @author Merijn
 * @version 1.0
 * @since 2025-10-13
 */
public final class PhaseExecutor {

    /**
     * A map associating each {@link ActionKind} with its corresponding {@link ActionHandler}.
     * <p>
     * This map is used to delegate the execution of specific action types to their respective handlers.
     * The map is immutable to ensure thread safety and prevent external modifications.
     * </p>
     */
    private final Map<ActionKind, ActionHandler> handlers;

    /**
     * The hooks that provide phase-specific behavior and lifecycle management.
     * <p>
     * PhaseHooks handle cross-cutting concerns such as:
     * <ul>
     *   <li>Applying saves and prevents to queued kills</li>
     *   <li>Processing all queued kills and generating death events</li>
     *   <li>Running aftermath logic (e.g., lovers chain reactions)</li>
     *   <li>Cleaning up temporary statuses between phases</li>
     *   <li>Determining player seat ordering for tie-breaking</li>
     * </ul>
     * </p>
     */
    private final PhaseHooks hooks;

    // ---------- Ordered steps per window ----------
    private static final List<ActionKind> NIGHT_ORDER = List.of(
            // PREVENT / DISABLE
            ActionKind.JAIL, ActionKind.SLEEP, ActionKind.SABOTAGE,
            // PROTECT
            ActionKind.PROTECT,
            // INFO
            ActionKind.INSPECT, ActionKind.AURA_SCAN, ActionKind.COMPARE_TEAM,
            ActionKind.SPY_WATCH, ActionKind.TRACK_KILL_ACTIVITY,
            // SETUPS
            ActionKind.COUPLE, ActionKind.BREAD, ActionKind.TRAP_ARM, ActionKind.MASK_AS_SHAMAN,
            // KILLS (initial proposals)
            ActionKind.SOLO_KILL, ActionKind.TEAM_KILL, ActionKind.BLESS
    );

    private static final List<ActionKind> DAWN_ORDER = List.of(
            // keep empty for now; you may add DAWN reports/effects later
    );

    private static final List<ActionKind> DAY_ORDER = List.of(
            // Day buffs etc. (wolves' hidden x2 if you implement it as an action)
            ActionKind.WOLF_VOTES_X2
    );

    private static final List<ActionKind> DUSK_ORDER = List.of(
            // End-of-day detonations/ignitions if you choose to fire them here
            ActionKind.BOMB_DETONATE, ActionKind.IGNITE
    );

    /**
     * Constructs a new PhaseExecutor with the specified action handlers and phase hooks.
     * <p>
     * The handlers map is defensively copied to ensure immutability and prevent external modifications.
     * This guarantees thread safety and consistent behavior throughout the executor's lifetime.
     * </p>
     *
     * @param handlers a map associating each {@link ActionKind} with its corresponding {@link ActionHandler};
     *                 must not be null. The map will be copied, so subsequent modifications to the
     *                 original map will not affect this executor.
     * @param hooks    the {@link PhaseHooks} implementation that provides phase-specific behavior and
     *                 lifecycle management; must not be null.
     * @throws NullPointerException if hooks is null
     */
    public PhaseExecutor(Map<ActionKind, ActionHandler> handlers, PhaseHooks hooks) {
        this.handlers = Map.copyOf(handlers);
        this.hooks = Objects.requireNonNull(hooks);
    }

    /**
     * Executes the appropriate phase logic for the current game phase with the provided action intents.
     * <p>
     * This method is the main entry point for phase execution. It first groups the provided intents
     * by their {@link Timing} and then delegates to the appropriate execution method based on the
     * current game phase:
     * <ul>
     *   <li><b>NIGHT:</b> Invokes {@link #executeNight(Game, GameEventBus, List)} with a complex
     *       pipeline including reaction windows and aftermath processing.</li>
     *   <li><b>DAWN:</b> Invokes {@link #executeSimple(Game, GameEventBus, List, List)} with
     *       the DAWN_ORDER sequence (currently empty).</li>
     *   <li><b>DAY:</b> Invokes {@link #executeSimple(Game, GameEventBus, List, List)} with
     *       the DAY_ORDER sequence.</li>
     *   <li><b>DUSK:</b> Invokes {@link #executeSimple(Game, GameEventBus, List, List)} with
     *       the DUSK_ORDER sequence.</li>
     *   <li><b>LOBBY/END:</b> No action is taken.</li>
     * </ul>
     * </p>
     * <p>
     * Only intents with a timing that matches the current phase will be processed. Intents with
     * different timings are ignored for the current execution.
     * </p>
     *
     * @param game       the current {@link Game} instance; must not be null
     * @param bus        the {@link GameEventBus} for posting and handling game events; must not be null
     * @param allIntents the list of all {@link ActionIntent}s collected for this phase; may be empty
     *                   but must not be null
     */
    public void execute(Game game, GameEventBus bus, List<ActionIntent> allIntents) {
        var phase = game.getPhase();
        var byTiming = allIntents.stream().collect(Collectors.groupingBy(ActionIntent::timing));

        switch (phase) {
            case NIGHT -> executeNight(game, bus, byTiming.getOrDefault(Timing.NIGHT, List.of()));
            case DAWN  -> executeSimple(game, bus, byTiming.getOrDefault(Timing.DAWN,  List.of()), DAWN_ORDER);
            case DAY   -> executeSimple(game, bus, byTiming.getOrDefault(Timing.DAY,   List.of()), DAY_ORDER);
            case DUSK  -> executeSimple(game, bus, byTiming.getOrDefault(Timing.DUSK,  List.of()), DUSK_ORDER);
            default    -> { /* LOBBY/END: no-op */ }
        }
    }

    /**
     * Executes the night phase with its complex pipeline of actions, reaction windows, and aftermath.
     * <p>
     * The night phase follows a sophisticated execution order:
     * <ol>
     *   <li>Process all actions in NIGHT_ORDER (preventive, protective, informational, setups, kills)</li>
     *   <li>Post a reaction window event for Witch SAVE ability</li>
     *   <li>Apply prevents and saves to the kill queue via {@link PhaseHooks#applyPreventsAndSaves(Game, GameEventBus)}</li>
     *   <li>Post reaction window events for Witch POISON and potential RESURRECT abilities</li>
     *   <li>Process all queued kills and get the list of players who just died</li>
     *   <li>Run aftermath logic (e.g., lovers chain reactions, death-triggered effects)</li>
     *   <li>Clean up temporary statuses from this phase</li>
     * </ol>
     * </p>
     * <p>
     * The reaction windows allow certain roles to respond to events (like impending kills) before
     * they are finalized, enabling save/poison mechanics and other reactive abilities.
     * </p>
     *
     * @param game         the current {@link Game} instance; must not be null
     * @param bus          the {@link GameEventBus} for posting and handling game events; must not be null
     * @param nightIntents the list of {@link ActionIntent}s with {@link Timing#NIGHT}; may be empty
     *                     but must not be null
     */
    private void executeNight(Game game, GameEventBus bus, List<ActionIntent> nightIntents) {
        var byKind = groupByKind(nightIntents);

        for (var kind : NIGHT_ORDER) runKind(kind, byKind, game, bus);

        // Reaction window A: Witch SAVE
        postReaction(bus, ReactionWindowEvent.WITCH_SAVE);
        hooks.applyPreventsAndSaves(game, bus);

        // Reaction window B: Witch POISON & Medium RESURRECT (if you add Medium later)
        postReaction(bus, ReactionWindowEvent.WITCH_POISON);
        postReaction(bus, ReactionWindowEvent.RESURRECT);

        var justDied = hooks.applyAllQueuedKills(game, bus);
        hooks.runAftermath(game, bus, justDied);

        hooks.cleanupStatuses(game);
    }

    /**
     * Executes a simple phase (DAWN, DAY, or DUSK) by processing actions in the specified order.
     * <p>
     * This method provides a simplified execution flow compared to the night phase. It processes
     * all actions in the provided order and then performs status cleanup. There are no reaction
     * windows or aftermath processing in simple phases.
     * </p>
     *
     * @param game    the current {@link Game} instance; must not be null
     * @param bus     the {@link GameEventBus} for posting and handling game events; must not be null
     * @param intents the list of {@link ActionIntent}s for this phase; may be empty but must not be null
     * @param order   the ordered list of {@link ActionKind}s to process in sequence; must not be null
     */
    private void executeSimple(Game game, GameEventBus bus, List<ActionIntent> intents, List<ActionKind> order) {
        var byKind = groupByKind(intents);
        for (var kind : order) runKind(kind, byKind, game, bus);
        hooks.cleanupStatuses(game);
    }

    /**
     * Groups a list of action intents by their {@link ActionKind}.
     * <p>
     * This method organizes intents into an {@link EnumMap} for efficient lookup by action kind.
     * Using an EnumMap provides better performance and memory efficiency compared to a standard HashMap
     * when the keys are enum constants.
     * </p>
     *
     * @param intents the list of {@link ActionIntent}s to group; must not be null
     * @return a map where each key is an {@link ActionKind} and the value is a list of intents
     *         of that kind; never null
     */
    private Map<ActionKind, List<ActionIntent>> groupByKind(List<ActionIntent> intents) {
        return intents.stream().collect(Collectors.groupingBy(
                ActionIntent::kind, () -> new EnumMap<>(ActionKind.class), Collectors.toList()
        ));
    }

    /**
     * Processes all intents of a specific action kind using the appropriate handler.
     * <p>
     * This method retrieves the intents for the given kind and delegates processing to the
     * registered {@link ActionHandler}. The handler's {@link ResolveMode} determines how
     * the intents are processed:
     * <ul>
     *   <li><b>SERIAL:</b> Intents are sorted by initiative (priority) and seat index (for tie-breaking),
     *       then processed one at a time in order via {@link ActionHandler#resolveSerial(List, Game, GameEventBus)}.</li>
     *   <li><b>AGGREGATED:</b> All intents are processed together as a group via
     *       {@link ActionHandler#resolveAggregated(List, Game, GameEventBus)}.</li>
     * </ul>
     * </p>
     * <p>
     * If no intents exist for the given kind or no handler is registered for that kind,
     * this method returns early without performing any action.
     * </p>
     *
     * @param kind   the {@link ActionKind} to process; must not be null
     * @param byKind the map of intents grouped by kind; must not be null
     * @param game   the current {@link Game} instance; must not be null
     * @param bus    the {@link GameEventBus} for posting and handling game events; must not be null
     */
    private void runKind(ActionKind kind, Map<ActionKind, List<ActionIntent>> byKind, Game game, GameEventBus bus) {
        var list = byKind.get(kind);
        if (list == null || list.isEmpty()) return;

        var handler = handlers.get(kind);
        if (handler == null) return;

        if (handler.mode() == ResolveMode.SERIAL) {
            var ordered = list.stream()
                    .sorted(Comparator
                            .comparingInt(ActionIntent::initiative)
                            .thenComparing(i -> hooks.seatIndex(game, i.actor())))
                    .toList();
            handler.resolveSerial(ordered, game, bus);
        } else {
            handler.resolveAggregated(list, game, bus);
        }
    }

    /**
     * Posts a reaction window event to the game event bus.
     * <p>
     * Reaction windows are special points in the phase execution where certain roles can respond
     * to game state changes. For example, the Witch can choose to save a player during the
     * WITCH_SAVE window or poison a player during the WITCH_POISON window.
     * </p>
     * <p>
     * Listeners subscribed to {@link ReactionWindowEvent} can react to these windows by
     * queuing additional actions or modifying the game state before execution continues.
     * </p>
     *
     * @param bus  the {@link GameEventBus} to post the event to; must not be null
     * @param name the name/identifier of the reaction window (e.g., "WITCH_SAVE", "WITCH_POISON");
     *             must not be null
     * @see ReactionWindowEvent
     */
    private void postReaction(GameEventBus bus, String name) {
        bus.post(new ReactionWindowEvent(name));
    }
}
