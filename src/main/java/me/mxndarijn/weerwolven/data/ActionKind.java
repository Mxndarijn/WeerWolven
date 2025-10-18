package me.mxndarijn.weerwolven.data;

/**
 * Represents the different categories of in-game actions at the engine level.
 * <p>
 * Multiple roles may map to the same {@code ActionKind}. The {@code PhaseExecutor}
 * resolves actions by kind (e.g. serialized or aggregated), not by individual role.
 * </p>
 */
public enum ActionKind {

    // ===== PREVENT / DISABLE (before protection and information) =====

    /**
     * Imprison a target, preventing them from performing any night action.
     * Often also restricts speech or voting the next day.
     * <p><b>Examples:</b> Jailer, Saboteur (with additional effects).</p>
     */
    JAIL,

    /**
     * Put the target to sleep, blocking all night actions for that phase.
     * <p><b>Examples:</b> Nightmare Wolf.</p>
     */
    SLEEP,

    /**
     * Sabotage the target — a combination of imprisonment, muting, no-vote,
     * and a scheduled end-of-day elimination.
     * <p><b>Examples:</b> Saboteur.</p>
     */
    SABOTAGE,


    // ===== PROTECT (before information) =====

    /**
     * Protect a target from night eliminations.
     * <p><b>Examples:</b> Doctor, Guardian Angel (Bodyguard triggers via event listener).</p>
     */
    PROTECT,


    // ===== INFORMATION (serial execution) =====

    /**
     * Reveal the target’s exact role or identity, depending on policy.
     * <p><b>Examples:</b> Seer, Wolf Seer.</p>
     */
    INSPECT,

    /**
     * Reveal the target’s aura as Good, Evil, or Unknown.
     * <p><b>Examples:</b> Aura Seer.</p>
     */
    AURA_SCAN,

    /**
     * Compare two players to learn if they belong to the same team
     * (team names are not revealed).
     * <p><b>Examples:</b> Detective.</p>
     */
    COMPARE_TEAM,

    /**
     * Learn whether the target performed any action during the previous night.
     * <p><b>Examples:</b> Spy.</p>
     */
    SPY_WATCH,

    /**
     * Learn whether one or more of the targets were responsible for an elimination
     * during the last action window.
     * <p><b>Examples:</b> Spirit Seer.</p>
     */
    TRACK_ELIMINATE_ACTIVITY,


    // ===== SETUP / BUFFS / LINKS (usually before eliminations) =====

    /**
     * Link two players as lovers.
     * <p><b>Examples:</b> Cupid.</p>
     */
    COUPLE,

    /**
     * Grant the target an extra vote during the next day phase.
     * <p><b>Examples:</b> Baker.</p>
     */
    BREAD,

    /**
     * Arm a trap that will trigger on future attacks against the target.
     * <p><b>Examples:</b> Beast Hunter.</p>
     */
    TRAP_ARM,

    /**
     * Mask the target’s identity so that seers perceive them as a Wolf Shaman
     * during the next night.
     * <p><b>Examples:</b> Wolf Shaman.</p>
     */
    MASK_AS_SHAMAN,

    /**
     * Double the wolves’ day-vote power (team-wide, hidden from citizens).
     * <p><b>Examples:</b> Shadow Wolf.</p>
     */
    WOLF_VOTES_X2,


    // ===== ELIMINATIONS (night/day) =====

    /**
     * A coordinated team elimination performed by all wolves.
     * <p><b>Examples:</b> Werewolves.</p>
     */
    TEAM_ELIMINATE,

    /**
     * A solo elimination performed independently at night.
     * <p><b>Examples:</b> Serial Eliminator, Cannibal.</p>
     */
    SOLO_ELIMINATE,

    /**
     * A holy ritual: eliminates the target if they are a wolf; otherwise,
     * the caster is eliminated.
     * <p><b>Examples:</b> Priest.</p>
     */
    BLESS,

    /**
     * A daytime shot that immediately eliminates a target (often reveals the shooter’s role).
     * <p><b>Examples:</b> Marksman, Jailer’s bullet.</p>
     */
    SHOOT,


    // ===== WITCH ACTIONS (reaction windows) =====

    /**
     * Save a pending elimination target.
     * <p><b>Examples:</b> Witch (Reaction A).</p>
     */
    SAVE,

    /**
     * Poison a target, scheduling a delayed elimination.
     * <p><b>Examples:</b> Witch (Reaction B).</p>
     */
    POISON,


    // ===== TWO-STEP ELIMINATORS =====

    /**
     * Plant a bomb on the target.
     * <p><b>Examples:</b> Bomber.</p>
     */
    BOMB_PLANT,

    /**
     * Detonate all previously planted bombs.
     * <p><b>Examples:</b> Bomber.</p>
     */
    BOMB_DETONATE,

    /**
     * Soak the target with gasoline.
     * <p><b>Examples:</b> Pyromaniac.</p>
     */
    SOAK,

    /**
     * Ignite all soaked targets.
     * <p><b>Examples:</b> Pyromaniac.</p>
     */
    IGNITE,

    /**
     * Give a red or black potion; black potions cause an end-of-day elimination.
     * <p><b>Examples:</b> Alchemist.</p>
     */
    ALCHEMIST_GIVE,


    // ===== MEDIUM / SPECIAL =====

    /**
     * Enables night-only communication with the deceased.
     * <p><b>Examples:</b> Medium.</p>
     */
    CHAT_WITH_DEAD,

    /**
     * Revive a previously eliminated player (usually once per game).
     * <p><b>Examples:</b> Medium.</p>
     */
    RESURRECT,

    /**
     * Visit another player at night; survival depends on encounter outcomes
     * with evil roles or attacks.
     * <p><b>Examples:</b> Red Lady.</p>
     */
    VISIT,

    /**
     * Choose a mentor whose role will be inherited later.
     * <p><b>Examples:</b> Apprentice.</p>
     */
    FOLLOW
}
