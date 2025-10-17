package me.mxndarijn.weerwolven.game.timer;

import me.mxndarijn.weerwolven.game.core.Game;

public record TimerContext(Game game, TimerSpec spec, long nowMs, long remainingMs) {
    public TimerContext(Game game, TimerSpec spec) { this(game, spec, System.currentTimeMillis(), 0); }
    public double progress() { return Math.min(1.0, Math.max(0.0, 1.0 - (double)remainingMs / (double)spec.durationMs)); }
}
