package me.mxndarijn.weerwolven.game.bus;

@FunctionalInterface
public interface EventListener<E extends GameBusEvent> {
    void on(E event);
}