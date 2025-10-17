package me.mxndarijn.weerwolven.game.timer;

public final class HostTimerFormats {
    private HostTimerFormats() {}
    public static String simple(TimerSpec s, long remainingMs) {
        return "<gray>" + s.title + " [" + TimerFormats.mmss(remainingMs) + "] ";
    }
}
