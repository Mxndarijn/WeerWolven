package me.mxndarijn.weerwolven.game.core.win;

import me.mxndarijn.weerwolven.data.Team;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class CitizensEliminateThreatsCondition implements WinCondition {
    @Override public int priority() { return 20; }

    @Override
    public Optional<WinResult> evaluate(Game game) {
        List<GamePlayer> alive = game.getGamePlayers().stream()
                .filter(GamePlayer::isAlive)
                .toList();
        boolean anyWolf = alive.stream().anyMatch(gp -> gp.getRole().getTeam() == Team.WEREWOLF);
        boolean anyMurder = alive.stream().anyMatch(gp -> gp.getRole().getTeam() == Team.MURDERER);
        if (!anyWolf && !anyMurder) {
            List<UUID> winners = alive.stream()
                    .filter(gp -> gp.getRole().getTeam() == Team.CITIZEN)
                    .map(gp -> gp.getOptionalPlayerUUID().orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            return Optional.of(new WinResult(Team.CITIZEN, winners, WinConditionText.VILLAGERS));
        }
        return Optional.empty();
    }
}