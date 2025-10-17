package me.mxndarijn.weerwolven.game.timer;

import me.mxndarijn.weerwolven.game.core.GamePlayer;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public final class TimerSpec {
    public final String id;
    public final String title;
    public final TimerScope scope;
    public final Set<GamePlayer> audience;
    public final long durationMs;

    public volatile long startedAtMs; // set by service

    public final Consumer<TimerContext> onTimeout;
    public final Function<TimerContext, String> renderForPlayer;
    public final @Nullable Consumer<TimerContext> onTick;
    public final @Nullable Consumer<TimerContext> onCancel;

    public TimerSpec(String id,
                     String title,
                     TimerScope scope,
                     Set<GamePlayer> audience,
                     long durationMs,
                     Function<TimerContext, String> renderForPlayer,
                     Consumer<TimerContext> onTimeout,
                     @Nullable Consumer<TimerContext> onTick,
                     @Nullable Consumer<TimerContext> onCancel) {
        this.id = id;
        this.title = title;
        this.scope = scope;
        this.audience = audience;
        this.durationMs = durationMs;
        this.renderForPlayer = renderForPlayer;
        this.onTimeout = onTimeout;
        this.onTick = onTick;
        this.onCancel = onCancel;
    }
}
