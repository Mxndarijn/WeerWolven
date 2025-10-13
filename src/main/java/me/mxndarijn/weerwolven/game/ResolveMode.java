package me.mxndarijn.weerwolven.game;

/**
 * How an action should be resolved by the executor.
 * SERIAL actions are ordered per intent; TEAM_AGGREGATED actions are merged into one outcome.
 */
public enum ResolveMode {
    /**
     * Handle each intent one-by-one in a deterministic order
     * (sorted by initiative, then seatIndex). Example: Seer INSPECT.
     */
    SERIAL,

    /**
     * Merge all intents of the same team/kind into a single outcome.
     * Example: wolves' TEAM_KILL where multiple votes produce one target.
     */
    TEAM_AGGREGATED
}
