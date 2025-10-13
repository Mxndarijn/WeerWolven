package me.mxndarijn.weerwolven.game;

import me.mxndarijn.weerwolven.game.Game;
import me.mxndarijn.weerwolven.game.GamePlayer;
import me.mxndarijn.weerwolven.game.bus.GameEventBus;
import java.util.List;

public interface PhaseHooks {
    int seatIndex(Game game, GamePlayer seat);              // usually seat.getSeatIndex()
    void applyPreventsAndSaves(Game game, GameEventBus bus);
    List<java.util.UUID> applyAllQueuedKills(Game game, GameEventBus bus);
    void runAftermath(Game game, GameEventBus bus, List<java.util.UUID> justDied);
    void cleanupStatuses(Game game);
}
