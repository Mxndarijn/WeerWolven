package me.mxndarijn.weerwolven.game.bus;

import org.bukkit.GameEvent;

public interface EventBus {
    <E extends GameBusEvent> AutoCloseable subscribe(
            Class<E> type,
            Priority priority,
            EventListener<E> listener
    );

    void post(GameBusEvent event);
}
