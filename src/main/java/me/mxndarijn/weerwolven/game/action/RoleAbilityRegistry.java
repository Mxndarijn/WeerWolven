package me.mxndarijn.weerwolven.game.action;


import me.mxndarijn.weerwolven.data.ActionKind;
import me.mxndarijn.weerwolven.data.Roles;
import me.mxndarijn.weerwolven.data.Timing;
import me.mxndarijn.weerwolven.game.phase.ResolveMode;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Maps a role identity to its abilities (what it can do).
 * Keep this data-driven: adding a new role is just adding entries here.
 */
public final class RoleAbilityRegistry {

    /** Immutable ability definition the UI/executor can use. */
    public record AbilityDef(
            ActionKind kind,
            Timing timing,
            ResolveMode mode,
            int initiative,     // lower runs earlier within the same kind (SERIAL)
            boolean oncePerGame // engine/UI can enforce single use
    ) {}

    // Suggested initiatives for current roles (keep consistent for determinism)
    private static final int INIT_JAIL          = 10;  // prevent step (very early)
    private static final int INIT_HUNTER_SHOOT  = 20;  // day ability before vote
    private static final int INIT_CUPIDO        = 30;  // reaction window B (late)
    private static final int INIT_SEER_INSPECT  = 50;  // info after protects
    private static final int INIT_SPY_WATCH     = 55;  // info, after seer
    private static final int INIT_SK_ELIMINATE  = 65;  // solo eliminate before team apply? (up to your plan)
    private static final int INIT_WW_ELIMINATE  = 60;  // solo eliminate before team apply? (up to your plan)
    private static final int INIT_WITCH_SAVE    = 10;  // reaction window A (early)
    private static final int INIT_WITCH_POISON  = 90;  // reaction window B (late)

    private static final Map<Roles, List<AbilityDef>> MAP = new EnumMap<>(Roles.class);

    static {
        // ---- Citizens ----
        MAP.put(Roles.SEER, List.of(
                new AbilityDef(ActionKind.INSPECT, Timing.NIGHT, ResolveMode.SERIAL, INIT_SEER_INSPECT, false)
        ));

        MAP.put(Roles.WITCH, List.of(
                // Save happens in a reaction window after kills are chosen
                new AbilityDef(ActionKind.SAVE,   Timing.REACTION, ResolveMode.SERIAL, INIT_WITCH_SAVE,   true),
                new AbilityDef(ActionKind.POISON, Timing.REACTION, ResolveMode.SERIAL, INIT_WITCH_POISON, true)
        ));

        MAP.put(Roles.HUNTER, List.of());

        MAP.put(Roles.JAILER, List.of(
                // Chosen during day, resolves at NIGHT in the PREVENT step
                new AbilityDef(ActionKind.JAIL, Timing.NIGHT, ResolveMode.SERIAL, INIT_JAIL, false)
        ));

        MAP.put(Roles.SPY, List.of(
                new AbilityDef(ActionKind.SPY_WATCH, Timing.NIGHT, ResolveMode.SERIAL, INIT_SPY_WATCH, false)
        ));
        MAP.put(Roles.CUPID, List.of(
                new AbilityDef(ActionKind.COUPLE, Timing.NIGHT, ResolveMode.SERIAL, INIT_CUPIDO, /*oncePerGame*/ true)
        ));

        // Vanilla villager/jester/unknown have no engine-level abilities
        MAP.put(Roles.VILLAGER, List.of());
        MAP.put(Roles.JESTER, List.of());
        MAP.put(Roles.UNKNOWN, List.of());

        // ---- Solo killer ----
        MAP.put(Roles.SERIAL_KILLER, List.of(
                new AbilityDef(ActionKind.SOLO_ELIMINATE, Timing.NIGHT, ResolveMode.SERIAL, INIT_SK_ELIMINATE, false)
        ));

        MAP.put(Roles.WEREWOLF, List.of(
                new AbilityDef(ActionKind.TEAM_ELIMINATE, Timing.NIGHT, ResolveMode.TEAM_AGGREGATED, INIT_WW_ELIMINATE, false)
        ));
    }

    private RoleAbilityRegistry() {}

    /** Get all abilities this role exposes. */
    public static List<AbilityDef> of(Roles role) {
        return MAP.getOrDefault(role, List.of());
    }

    /** Optional: allow plugins/presets to extend or override abilities at runtime. */
    public static void register(Roles role, List<AbilityDef> defs) {
        MAP.put(role, List.copyOf(defs));
    }

    /** Optional: append abilities without replacing existing ones. */
    public static void add(Roles role, AbilityDef def) {
        var current = new java.util.ArrayList<>(MAP.getOrDefault(role, List.of()));
        current.add(def);
        MAP.put(role, List.copyOf(current));
    }
}
