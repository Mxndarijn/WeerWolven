package me.mxndarijn.weerwolven.game.role;

import me.mxndarijn.weerwolven.game.GamePlayer;

public abstract class RoleData {


    private final GamePlayer gamePlayer;
    public RoleData(GamePlayer gamePlayer) {
        this.gamePlayer = gamePlayer;
    }
}
