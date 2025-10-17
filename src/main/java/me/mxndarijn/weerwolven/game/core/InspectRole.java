package me.mxndarijn.weerwolven.game.core;

import me.mxndarijn.weerwolven.game.role.RoleData;

import java.util.ArrayList;
import java.util.List;

public abstract class InspectRole extends RoleData {
    public InspectRole(GamePlayer gamePlayer) {
        super(gamePlayer);
    }

    public abstract String getTitle();
    public abstract String getMessage();
    public abstract String getCompletedMessage(GamePlayer gp);

    public List<GamePlayer> playersSeen = new ArrayList<>();
}
