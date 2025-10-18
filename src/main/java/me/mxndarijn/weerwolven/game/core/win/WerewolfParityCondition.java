package me.mxndarijn.weerwolven.game.core.win;

import me.mxndarijn.weerwolven.data.Team;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class WerewolfParityCondition implements WinCondition {
    @Override public int priority() { return 10; }

    @Override
    public Optional<WinResult> evaluate(Game game) {
        List<GamePlayer> alive = game.getGamePlayers().stream()
                .filter(GamePlayer::isAlive)
                .toList();
        if (alive.isEmpty()) return Optional.empty();

        long wolves = alive.stream().filter(gp -> gp.getRole().getTeam() == Team.WEREWOLF).count();
        long nonWolves = alive.size() - wolves;
        if (wolves > 0 && wolves >= nonWolves) {
            List<UUID> winners = alive.stream()
                    .filter(gp -> gp.getRole().getTeam() == Team.WEREWOLF)
                    .map(gp -> gp.getOptionalPlayerUUID().orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            return Optional.of(new WinResult(Team.WEREWOLF, winners, WinConditionText.WEREWOLF));
        }
        return Optional.empty();
    }
}