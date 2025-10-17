package me.mxndarijn.weerwolven.game.bus.events;

import lombok.Getter;
import me.mxndarijn.weerwolven.game.bus.GameBusEvent;
import me.mxndarijn.weerwolven.game.core.GamePlayer;

/**
 * Fired for each individual voter during the DAY lynch tally so listeners can
 * modify the weight of that single vote (e.g., wolves count x2).
 * Default weight is 1. Listeners may multiply or set the weight.
 */
@Getter
public final class DayVoteWeightEvent implements GameBusEvent {
    private final GamePlayer voter;
    private int weight;

    public DayVoteWeightEvent(GamePlayer voter) {
        this.voter = voter;
        this.weight = 1;
    }

    /** Multiply current weight by a positive factor. */
    public void multiply(int factor) {
        if (factor <= 0) return;
        long v = (long) this.weight * (long) factor;
        this.weight = (int) Math.max(0, Math.min(Integer.MAX_VALUE, v));
    }

    /** Directly set the weight to a non-negative value. */
    public void setWeight(int weight) {
        if (weight < 0) weight = 0;
        this.weight = weight;
    }

    /** Increase weight by delta (may be negative). */
    public void add(int delta) {
        long v = (long) this.weight + (long) delta;
        if (v < 0) v = 0;
        if (v > Integer.MAX_VALUE) v = Integer.MAX_VALUE;
        this.weight = (int) v;
    }
}
