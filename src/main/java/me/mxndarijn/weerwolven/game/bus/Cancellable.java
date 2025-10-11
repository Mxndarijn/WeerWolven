package me.mxndarijn.weerwolven.game.bus;

public interface Cancellable {
    boolean isCancelled();
    void setCancelled(boolean cancelled);
}