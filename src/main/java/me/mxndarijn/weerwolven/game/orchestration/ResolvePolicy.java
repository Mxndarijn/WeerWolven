// me.mxndarijn.weerwolven.game.orchestration.ResolvePolicy
package me.mxndarijn.weerwolven.game.orchestration;

import me.mxndarijn.weerwolven.data.ActionKind;
import me.mxndarijn.weerwolven.game.phase.ResolveMode;

import java.util.EnumMap;
import java.util.Map;

public final class ResolvePolicy {
    private final Map<ActionKind, ResolveMode> byKind = new EnumMap<>(ActionKind.class);

    public ResolvePolicy defaultSerial(ActionKind... kinds) { for (var k : kinds) byKind.put(k, ResolveMode.SERIAL); return this; }
    public ResolvePolicy aggregated(ActionKind... kinds) { for (var k : kinds) byKind.put(k, ResolveMode.TEAM_AGGREGATED); return this; }

    public ResolveMode mode(ActionKind kind) {
        return byKind.getOrDefault(kind, ResolveMode.SERIAL);
    }
}
