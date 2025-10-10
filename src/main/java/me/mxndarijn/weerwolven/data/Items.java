package me.mxndarijn.weerwolven.data;

import lombok.Getter;
import me.mxndarijn.weerwolven.items.WeerWolvenMxItem;
import me.mxndarijn.weerwolven.items.preset.PresetConfigureTool;
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
    PRESET_CONFIGURE_TOOL(
            MxDefaultItemStackBuilder.create(Material.NETHER_STAR, 1)
                    .setName("<gray>Preset Configure-Tool")
                    .addLore(" ")
                    .addLore("<yellow>Met dit item kan je instellingen in een preset aanpassen.")
                    .build(),
            p -> {
                return true;
            },
            false,
            PresetConfigureTool.class,
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK
    ),

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