package me.mxndarijn.weerwolven.data;

import lombok.Getter;
import me.mxndarijn.weerwolven.items.WeerWolvenMxItem;
import net.md_5.bungee.api.chat.ItemTag;
import nl.mxndarijn.mxlib.inventory.saver.InventoryManager;
import nl.mxndarijn.mxlib.item.MxDefaultItemStackBuilder;
import nl.mxndarijn.mxlib.mxitem.MxItem;
import nl.mxndarijn.mxlib.util.Functions;
import nl.mxndarijn.mxlib.util.MxWorldFilter;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public enum Items {

    ;


    private final ItemStack itemStack;
    @Getter
    private final MxWorldFilter worldFilter;
    @Getter
    private final boolean gameItem;
    @Getter
    private final Class<? extends MxItem> classObject;
    @Getter
    private final Action[] actions;

    Items(ItemStack is, MxWorldFilter mxWorldFilter, boolean gameItem, Class<? extends WeerWolvenMxItem> classObject, Action... actions) {
        this.itemStack = is;
        this.worldFilter = mxWorldFilter;
        this.gameItem = gameItem;
        this.classObject = classObject;
        this.actions = actions;
    }

    public static boolean isItemAGameItem(@NotNull ItemStack activeItem) {
        for (Items item : Items.values()) {
            if (InventoryManager.validateItem(activeItem, item.getItemStack())) {
                return item.isGameItem();
            }
        }
        return false;
    }

    public ItemStack getItemStack() {
        return itemStack.clone();
    }

}