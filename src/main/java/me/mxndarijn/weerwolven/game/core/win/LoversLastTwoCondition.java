package me.mxndarijn.weerwolven.game.core.win;

import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;
import me.mxndarijn.weerwolven.game.status.StatusKey;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class LoversLastTwoCondition implements WinCondition {
    @Override public int priority() { return -10; } // run first as a hard override

    @Override
    public Optional<WinResult> evaluate(Game game) {
        List<GamePlayer> alive = game.getGamePlayers().stream().filter(GamePlayer::isAlive).toList();
        if (alive.size() != 2) return Optional.empty();
        // Assuming you track a lovers status in StatusStore; replace with your actual check
        boolean bothAreLovers = alive.stream().allMatch(gp -> gp.getStatusStore().has(StatusKey.LOVERS));
        if (!bothAreLovers) return Optional.empty();
        List<UUID> winners = alive.stream()
                .map(gp -> gp.getOptionalPlayerUUID().orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return Optional.of(new WinResult(null, winners, WinConditionText.LOVERS));
    }
}