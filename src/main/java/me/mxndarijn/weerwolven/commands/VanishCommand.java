package me.mxndarijn.weerwolven.commands;

import me.mxndarijn.weerwolven.data.WeerWolvenChatPrefix;
import me.mxndarijn.weerwolven.data.WeerWolvenLanguageText;
import me.mxndarijn.weerwolven.data.WeerWolvenPermissions;
import me.mxndarijn.weerwolven.managers.VanishManager;
import nl.mxndarijn.mxlib.language.LanguageManager;
import nl.mxndarijn.mxlib.util.MessageUtil;
import nl.mxndarijn.mxlib.util.MxWorldFilter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VanishCommand extends WeerWolvenMxCommand {

    public VanishCommand(WeerWolvenPermissions permission, boolean onlyPlayersCanExecute, boolean canBeExecutedInGame, MxWorldFilter worldFilter) {
        super(permission, onlyPlayersCanExecute, canBeExecutedInGame, worldFilter);
    }

    public VanishCommand(WeerWolvenPermissions permission, boolean onlyPlayersCanExecute, boolean canBeExecutedInGame) {
        super(permission, onlyPlayersCanExecute, canBeExecutedInGame);
    }

    @Override
    public void execute(CommandSender sender, Command command, String label, String[] args) throws Exception {
        Player p = (Player) sender;
        VanishManager.getInstance().toggleVanish(p);
        if (VanishManager.getInstance().isPlayerHidden(p)) {
            MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.VANISH_ON, WeerWolvenChatPrefix.DEFAULT));
        } else {
            MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.VANISH_OFF, WeerWolvenChatPrefix.DEFAULT));
        }
    }
}
