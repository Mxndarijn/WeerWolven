package me.mxndarijn.weerwolven.game.bus;

public interface EventBus {
    <E extends GameBusEvent> AutoCloseable subscribe(
            Class<E> type,
            Priority priority,
            EventListener<E> listener
    );

    void post(GameBusEvent event);
}
