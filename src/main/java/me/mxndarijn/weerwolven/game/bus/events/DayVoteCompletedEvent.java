package me.mxndarijn.weerwolven.game.bus.events;

import me.mxndarijn.weerwolven.game.bus.GameBusEvent;
import me.mxndarijn.weerwolven.game.core.GamePlayer;
import me.mxndarijn.weerwolven.game.manager.GameVoteManager;

import java.util.Map;
import java.util.Optional;

/**
 * Fired when the DAY lynch vote has been tallied (after applying weights).
 * Contains the weighted tallies per candidate, the optional winner (empty on tie or none),
 * and the raw vote result snapshot from the GameVoteManager for reference.
 */
public final class DayVoteCompletedEvent implements GameBusEvent {
    private final Map<GamePlayer, Integer> weightedTallies; // candidate -> weighted votes
    private final Optional<GamePlayer> winner;
    private final GameVoteManager.Finish rawResult;

    public DayVoteCompletedEvent(Map<GamePlayer, Integer> weightedTallies,
                                 Optional<GamePlayer> winner,
                                 GameVoteManager.Finish rawResult) {
        this.weightedTallies = Map.copyOf(weightedTallies);
        this.winner = winner;
        this.rawResult = rawResult;
    }

    public Map<GamePlayer, Integer> getWeightedTallies() { return weightedTallies; }

    public Optional<GamePlayer> getWinner() { return winner; }

    public GameVoteManager.Finish getRawResult() { return rawResult; }
}
