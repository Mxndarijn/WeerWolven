
package me.mxndarijn.weerwolven.game.orchestration;

import me.mxndarijn.weerwolven.data.ActionKind;

import java.util.EnumMap;
import java.util.Map;

public final class OrchestrationConfig {
    private final Map<ActionKind, Long> timeoutsMs = new EnumMap<>(ActionKind.class);
    private long defaultTimeoutMs = 30_000; // sensible default

    public OrchestrationConfig setDefaultTimeoutMs(long ms) {
        this.defaultTimeoutMs = ms; return this;
    }
    public OrchestrationConfig setTimeout(ActionKind kind, long ms) {
        timeoutsMs.put(kind, ms); return this;
    }
    public long timeoutMs(ActionKind kind) {
        return timeoutsMs.getOrDefault(kind, defaultTimeoutMs);
    }
}
