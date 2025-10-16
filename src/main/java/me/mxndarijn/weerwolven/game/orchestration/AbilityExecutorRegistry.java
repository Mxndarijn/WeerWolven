// me.mxndarijn.weerwolven.game.orchestration.AbilityExecutorRegistry
package me.mxndarijn.weerwolven.game.orchestration;

import me.mxndarijn.weerwolven.data.ActionKind;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class AbilityExecutorRegistry {
    private final Map<ActionKind, AbilityExecutor> solo = new EnumMap<>(ActionKind.class);
    private final Map<ActionKind, TeamAbilityExecutor> team = new EnumMap<>(ActionKind.class);

    public void registerSolo(ActionKind kind, AbilityExecutor exec) { solo.put(kind, exec); }
    public void registerTeam(ActionKind kind, TeamAbilityExecutor exec) { team.put(kind, exec); }

    public Optional<AbilityExecutor> solo(ActionKind kind) { return Optional.ofNullable(solo.get(kind)); }
    public Optional<TeamAbilityExecutor> team(ActionKind kind) { return Optional.ofNullable(team.get(kind)); }
}
