package me.mxndarijn.weerwolven.data;

import lombok.Getter;
import nl.mxndarijn.mxlib.permission.PermissionType;

@Getter
public enum WeerWolvenPermissions implements PermissionType {
    NO_PERMISSION(""),
    COMMAND_GAMES_CREATE_SPECIFIC_GAME("command.games.create.specific-game"),
    COMMAND_PRESETS("command.presets"),
    SPAWN_BLOCK_PLACE("spawn.block.place"),
    SPAWN_BLOCK_BREAK("spawn.block.break"),
    SPAWN_DROP_ITEM("spawn.drop-item"),
    SPAWN_PICKUP_ITEM("spawn.pickup-item"),
    SPAWN_CHANGE_INVENTORY("spawn.change-inventory"),
    COMMAND_SPAWN("command.spawn"),
    COMMAND_ROLESETS("command.rolesets"),
    ITEM_GAMES_MANAGE_OTHER_GAMES("items.games.manageother"),
    ITEM_GAMES_CREATE_TEMP_ROLESET("items.games.createtemproleset");


    private final String permission;

    WeerWolvenPermissions(String permission) {
        this.permission = permission;
    }

    @Override
    public String toString() {
        return this.permission;
    }

    @Override
    public String node() {
        return this.permission;
    }
}
