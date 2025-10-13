
package me.mxndarijn.weerwolven.game.bus;

import java.util.ArrayList;
import java.util.List;

/** Groups multiple AutoCloseables so you can close them in one go. */
public final class AutoCloseableGroup implements AutoCloseable {
    private final List<AutoCloseable> list = new ArrayList<>();

    /** Add a handle (e.g., the return value of bus.subscribe(...)). */
    public AutoCloseable add(AutoCloseable c) {
        if (c != null) list.add(c);
        return c;
    }

    /** Remove a handle you previously added (optional). */
    public void remove(AutoCloseable c) {
        list.remove(c);
    }

    /** Close everything (ignore individual errors). */
    @Override public void close() {
        for (var c : new ArrayList<>(list)) {
            try { c.close(); } catch (Throwable ignored) {}
        }
        list.clear();
    }
}
