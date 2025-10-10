package me.mxndarijn.weerwolven.commands;

import me.mxndarijn.weerwolven.data.WeerWolvenPermissions;
import nl.mxndarijn.mxlib.util.MxWorldFilter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand extends WeerWolvenMxCommand {

    public SpawnCommand(WeerWolvenPermissions permission, boolean onlyPlayersCanExecute, boolean canBeExecutedInGame, MxWorldFilter worldFilter) {
        super(permission, onlyPlayersCanExecute, canBeExecutedInGame, worldFilter);
    }

    public SpawnCommand(WeerWolvenPermissions permission, boolean onlyPlayersCanExecute, boolean canBeExecutedInGame) {
        super(permission, onlyPlayersCanExecute, canBeExecutedInGame);
    }

    @Override
    public void execute(CommandSender sender, Command command, String label, String[] args) {
        World w = Bukkit.getWorld("world");
        Player p = (Player) sender;
        p.teleport(w.getSpawnLocation());
        //TODO Improve this...
    }
}
