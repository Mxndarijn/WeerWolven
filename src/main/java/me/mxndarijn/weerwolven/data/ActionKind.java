package me.mxndarijn.weerwolven.data;

/**
 * Engine-level action categories.
 * Many roles can map to the same ActionKind.
 * The PhaseExecutor resolves by kind (serial vs aggregated), not by role.
 */
public enum ActionKind {

    // ===== PREVENT / DISABLE (before protects & info) =====
    /** Jail a target: no night action (and often muted/no-vote next day). */
    JAIL,           // Cipier, Saboteur (with extra effects)
    /** Put target to sleep: no night action. */
    SLEEP,          // Nachtermerriewolf
    /** Sabotage target: jail-like + mute/no-vote + scheduled end-of-day death. */
    SABOTAGE,       // Saboteur

    // ===== PROTECT (before info) =====
    /** Protect against night kills. */
    PROTECT,        // Dokter, Beschermengel (Bodyguard intercept via event listener)

    // ===== INFORMATION (serial) =====
    /** Reveal exact role (or policy-based identity). */
    INSPECT,        // Ziener, Wolvenziener
    /** Reveal Good / Evil / Unknown aura. */
    AURA_SCAN,      // Auraziener
    /** Compare two players: same team or not (no team names). */
    COMPARE_TEAM,   // Detective
    /** Learn if target performed any action last night. */
    SPY_WATCH,      // Spion
    /** Learn if (one of) the targets killed during the previous window. */
    TRACK_KILL_ACTIVITY, // Geestenziener

    // ===== SETUP / BUFFS / LINKS (usually before kills) =====
    /** Link two lovers. */
    COUPLE,         // Cupido
    /** Give +1 vote for next day. */
    BREAD,          // Bakker
    /** Arm a trap that triggers on future attacks. */
    TRAP_ARM,       // Beestenjager
    /** Mask target so seers see them as Wolvensjamaan next night. */
    MASK_AS_SHAMAN, // Wolvensjamaan
    /** Double wolves' day votes (team-wide, hidden to citizens). */
    WOLF_VOTES_X2,  // Schaduwwolf

    // ===== KILLS (night/day) =====
    /** Team-aggregated wolf kill. */
    TEAM_KILL,      // Alle weerwolven
    /** Solo night kill. */
    SOLO_KILL,      // Seriemoordenaar, Kanibaal
    /** Holy water: kill if target is wolf, else caster dies. */
    BLESS,          // Priester
    /** Daytime shot (usually reveals the role). */
    SHOOT,          // Schutter, (Cipier bullet)

    // ===== WITCH (reaction windows) =====
    /** Save a pending night-kill target. */
    SAVE,           // Heks (reactie A)
    /** Poison a target (queued kill). */
    POISON,         // Heks (reactie B)

    // ===== TWO-STEP MURDERERS =====
    /** Plant a bomb on a player. */
    BOMB_PLANT,     // Bommenwerper
    /** Detonate all planted bombs. */
    BOMB_DETONATE,  // Bommenwerper
    /** Soak a player with gasoline. */
    SOAK,           // Pyromaan
    /** Ignite all soaked players. */
    IGNITE,         // Pyromaan
    /** Give red/black potion (black -> end-of-day death). */
    ALCHEMIST_GIVE, // Alchemist

    // ===== MEDIUM / SPECIAL =====
    /** Night-only chat with the dead. */
    CHAT_WITH_DEAD, // Helderziende
    /** Once per game, resurrect a dead player. */
    RESURRECT,      // Helderziende
    /** Visit a player at night; special survival rules vs. evil/attacks. */
    VISIT,          // Rode dame
    /** Pick a mentor to inherit later. */
    FOLLOW          // Leerling
}
