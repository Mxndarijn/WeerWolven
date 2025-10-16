package me.mxndarijn.weerwolven.items.game.spectate;

import me.mxndarijn.weerwolven.data.WeerWolvenLanguageText;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.items.WeerWolvenMxItem;
import me.mxndarijn.weerwolven.managers.GameWorldManager;
import nl.mxndarijn.mxlib.language.LanguageManager;
import nl.mxndarijn.mxlib.util.MessageUtil;
import nl.mxndarijn.mxlib.util.MxWorldFilter;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.Optional;

public class LeaveGameItem extends WeerWolvenMxItem {
    public LeaveGameItem(ItemStack is, MxWorldFilter worldFilter, boolean gameItem, Action... actions) {
        super(is, worldFilter, gameItem, actions);
    }

    @Override
    public void execute(Player p, PlayerInteractEvent e) {
        Optional<Game> optionalGame = GameWorldManager.getInstance().getGameByWorldUID(p.getWorld().getUID());
        if (optionalGame.isPresent()) {
            if (optionalGame.get().getSpectators().contains(p.getUniqueId())) {
                optionalGame.get().removeSpectator(p.getUniqueId());
                MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_SPECTATOR_LEAVE));
                optionalGame.get().sendMessageToHosts(LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_SPECTATOR_LEFT, Collections.singletonList(p.getName())));
            }
        }
    }

    @Override
    public void executeOnBreak(Player p, BlockBreakEvent e) {
        e.setCancelled(true);
    }
}
