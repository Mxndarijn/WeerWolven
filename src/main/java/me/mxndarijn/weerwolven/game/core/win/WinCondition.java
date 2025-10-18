package me.mxndarijn.weerwolven.game.core.win;

import me.mxndarijn.weerwolven.game.core.Game;
import java.util.Optional;

public interface WinCondition {
    /**
     * Return a result if this condition is met, otherwise empty.
     */
    Optional<WinResult> evaluate(Game game);

    /**
     * Smaller first. Reserve negative values for high priority (e.g., hard locks).
     */
    default int priority() {
        return 0;
    }
}
