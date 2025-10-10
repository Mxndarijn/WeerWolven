package me.mxndarijn.weerwolven.commands;

import nl.mxndarijn.mxlib.mxcommand.MxCommand;
import nl.mxndarijn.mxlib.permission.PermissionType;
import nl.mxndarijn.mxlib.util.MxWorldFilter;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

public abstract class WeerWolvenMxCommand extends MxCommand {

    private final boolean canBeExecutedInGame;

    public WeerWolvenMxCommand(PermissionType permission, boolean onlyPlayersCanExecute, boolean canBeExecutedInGame, MxWorldFilter worldFilter) {
        super(permission, onlyPlayersCanExecute, worldFilter);
        this.canBeExecutedInGame = canBeExecutedInGame;
    }

    public WeerWolvenMxCommand(PermissionType permission, boolean onlyPlayersCanExecute, boolean canBeExecutedInGame) {
        super(permission, onlyPlayersCanExecute);
        this.canBeExecutedInGame = canBeExecutedInGame;
    }

    @Override
    protected boolean canExecutePlayer(Player player, Command command, String label, String[] args) {
//        if(!canBeExecutedInGame) {
//            if(GameWorldManager.getInstance().isPlayerInAGame(player.getUniqueId())) {
//                return false;
//            }
//        }
        return super.canExecutePlayer(player, command, label, args);
    }
}
