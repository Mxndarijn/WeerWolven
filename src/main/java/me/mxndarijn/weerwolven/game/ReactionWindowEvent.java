// me.mxndarijn.weerwolven.game.events.ReactionWindowEvent
package me.mxndarijn.weerwolven.game;

import me.mxndarijn.weerwolven.game.Phase;
import me.mxndarijn.weerwolven.game.bus.GameBusEvent;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Posted by the executor to open a reaction window.
 * Typical names: WITCH_SAVE, WITCH_POISON, RESURRECT.
 * Not cancellable: listeners just no-op if not applicable.
 */
public final class ReactionWindowEvent implements GameBusEvent {

    // Common names (use these to avoid typos)
    public static final String WITCH_SAVE   = "WITCH_SAVE";
    public static final String WITCH_POISON = "WITCH_POISON";
    public static final String RESURRECT    = "RESURRECT";

    private final String name;
    private final Phase phase;            // phase during which this window was opened
    private final int dayNumber;          // current day counter
    private final UUID correlationId;     // unique id for tracing
    private final Map<String, Object> context; // optional extra data

    public ReactionWindowEvent(String name) {
        this(name, null, -1, UUID.randomUUID(), Map.of());
    }

    public ReactionWindowEvent(String name, Phase phase, int dayNumber) {
        this(name, phase, dayNumber, UUID.randomUUID(), Map.of());
    }

    public ReactionWindowEvent(String name, Phase phase, int dayNumber, Map<String, Object> context) {
        this(name, phase, dayNumber, UUID.randomUUID(), context);
    }

    public ReactionWindowEvent(String name, Phase phase, int dayNumber, UUID correlationId, Map<String, Object> context) {
        this.name = Objects.requireNonNull(name, "name");
        this.phase = phase;
        this.dayNumber = dayNumber;
        this.correlationId = Objects.requireNonNull(correlationId, "correlationId");
        this.context = (context == null || context.isEmpty()) ? Map.of() : Map.copyOf(context);
    }

    public String name() { return name; }
    public Phase phase() { return phase; }
    public int dayNumber() { return dayNumber; }
    public UUID correlationId() { return correlationId; }
    public Map<String, Object> context() { return context; }

    @Override public String toString() {
        return "ReactionWindowEvent{" +
                "name='" + name + '\'' +
                ", phase=" + phase +
                ", dayNumber=" + dayNumber +
                ", correlationId=" + correlationId +
                ", contextKeys=" + context.keySet() +
                '}';
    }
}
