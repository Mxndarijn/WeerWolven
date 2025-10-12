package me.mxndarijn.weerwolven.items;

import me.mxndarijn.weerwolven.managers.GameWorldManager;
import nl.mxndarijn.mxlib.mxitem.MxItem;
import nl.mxndarijn.mxlib.util.MxWorldFilter;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public abstract class WeerWolvenMxItem extends MxItem {
    private final boolean isGameItem;
    public WeerWolvenMxItem(ItemStack is, MxWorldFilter worldFilter, boolean gameItem, Action... actions) {
        super(is, worldFilter, actions);

        this.isGameItem = gameItem;
    }

    @Override
    protected boolean canExecuteInteract(Player player, ItemStack item, PlayerInteractEvent event) {
        if(isGameItem) {
            return GameWorldManager.getInstance().isPlayerInAGame(player.getUniqueId());
        }
        return true;
    }

    @Override
    protected boolean canExecuteBreak(Player player, ItemStack item, BlockBreakEvent event) {
        if(isGameItem) {
            return GameWorldManager.getInstance().isPlayerInAGame(player.getUniqueId());
        }
        return true;
    }
}
