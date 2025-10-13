package me.mxndarijn.weerwolven.game;

import me.mxndarijn.weerwolven.data.ActionKind;
import me.mxndarijn.weerwolven.data.Timing;
import me.mxndarijn.weerwolven.game.GamePlayer;

import java.util.Map;

/** Intent created by UI/menus and consumed by the PhaseExecutor. */
public record ActionIntent(
        GamePlayer actor,            // the seat performing the action
        ActionKind kind,
        Timing timing,
        Map<String,Object> params,   // e.g. {"target": GamePlayer}, {"targets": List<GamePlayer>}
        int initiative               // lower runs earlier for SERIAL kinds
) {}
