package me.mxndarijn.weerwolven.game.core.win;

import me.mxndarijn.weerwolven.data.Team;
import java.util.List;
import java.util.UUID;

public record WinResult(Team team, List<UUID> winners, WinConditionText reason) { }