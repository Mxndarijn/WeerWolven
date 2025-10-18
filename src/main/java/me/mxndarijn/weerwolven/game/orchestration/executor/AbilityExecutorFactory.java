package me.mxndarijn.weerwolven.game.orchestration.executor;

import me.mxndarijn.weerwolven.data.ActionKind;

import java.util.Optional;

public interface AbilityExecutorFactory {
    Optional<AbilityExecutor> forKind(ActionKind kind);
}
