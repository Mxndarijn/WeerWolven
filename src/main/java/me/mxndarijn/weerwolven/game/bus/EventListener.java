package me.mxndarijn.weerwolven.game.bus;

import org.bukkit.GameEvent;

@FunctionalInterface
public interface EventListener<E extends GameBusEvent> {
    void on(E event);
}