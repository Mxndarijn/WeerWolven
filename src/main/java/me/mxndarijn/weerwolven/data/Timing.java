package me.mxndarijn.weerwolven.data;

/** When an action is intended to execute in the game flow. */
public enum Timing {
    /** Runs during the night pipeline (prevent → protect → info → kills → reactions). */
    NIGHT,

    /** Runs right after night, during the reveal/aftermath window. */
    DAWN,

    /** Runs during the day (pre-/post-vote abilities like SHOOT, buffs). */
    DAY,

    /** Runs at the end of the day (lynch aftermath, scheduled deaths). */
    DUSK,

    /**
     * Runs inside explicit reaction windows posted by the executor,
     * e.g., Witch SAVE/POISON or Medium RESURRECT.
     */
    REACTION
}
