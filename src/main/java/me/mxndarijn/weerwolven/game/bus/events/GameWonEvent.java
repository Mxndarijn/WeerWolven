package me.mxndarijn.weerwolven.game.bus.events;

import me.mxndarijn.weerwolven.game.bus.GameBusEvent;
import me.mxndarijn.weerwolven.game.core.win.WinResult;

public record GameWonEvent(WinResult result) implements GameBusEvent {

}