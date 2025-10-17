package me.mxndarijn.weerwolven.game.timer;

public final class TimerFormats {
    private TimerFormats() {}

    public static String mmss(long ms) {
        long sec = Math.max(0, ms / 1000);
        long m = sec / 60, s = sec % 60;
        return String.format("%02d:%02d", m, s);
    }

    public static String bar(double ratio, int width) {
        ratio = Math.max(0, Math.min(1, ratio));
        int fill = (int)Math.round(ratio * width);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < width; i++) sb.append(i < fill ? '▰' : '▱');
        return sb.toString();
    }
}
