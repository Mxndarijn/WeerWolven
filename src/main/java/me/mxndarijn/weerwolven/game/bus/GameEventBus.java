package me.mxndarijn.weerwolven.game.bus;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple, thread-safe EventBus:
 * - Sync dispatch
 * - Priorities
 * - Stable order (based on registration sequence)
 * - AutoCloseable unsubscribe
 */
public final class GameEventBus implements EventBus {

    private static final class Subscribed<E extends GameBusEvent> {
        final long order;
        final Priority priority;
        final EventListener<E> listener;

        Subscribed(long order, Priority priority, EventListener<E> listener) {
            this.order = order;
            this.priority = priority;
            this.listener = listener;
        }
    }

    /**
     * Ordered list of listeners per event type.
     */
    private final ConcurrentMap<Class<?>, CopyOnWriteArrayList<Subscribed<?>>> listeners = new ConcurrentHashMap<>();
    /**
     * Monotonic counter for stable ordering within same priority.
     */
    private final AtomicLong seq = new AtomicLong();

    @Override
    @SuppressWarnings("unchecked")
    public <E extends GameBusEvent> AutoCloseable subscribe(
            Class<E> type,
            Priority priority,
            EventListener<E> listener
    ) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");

        var list = listeners.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>());
        var sub = new Subscribed<E>(seq.getAndIncrement(), priority, listener);

        // Insert sorted by priority, then by order
        int idx = 0;
        while (idx < list.size()) {
            var cur = (Subscribed<E>) list.get(idx);
            int byPrio = Integer.compare(priority.ordinal(), cur.priority.ordinal());
            if (byPrio < 0 || (byPrio == 0 && sub.order < cur.order)) break;
            idx++;
        }
        list.add(idx, sub);

        // Unsubscribe handle
        return () -> list.remove(sub);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void post(GameBusEvent event) {
        if (event == null) return;
        var list = listeners.get(event.getClass());
        if (list == null || list.isEmpty()) return;

        for (var any : list) {
            var sub = (Subscribed<GameBusEvent>) any;
            try {
                sub.listener.on(event);
            } catch (Throwable t) {
                // Log here with your plugin logger if you want:
                // Bukkit.getLogger().log(Level.WARNING, "Event listener error", t);
            }
            if (event instanceof Cancellable c && c.isCancelled()) {
                break; // skip remaining listeners
            }
        }
    }
}