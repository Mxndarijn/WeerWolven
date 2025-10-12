package me.mxndarijn.weerwolven.data;

import lombok.Getter;
import me.mxndarijn.weerwolven.items.WeerWolvenMxItem;
import me.mxndarijn.weerwolven.items.game.host.GameHostItem;
import me.mxndarijn.weerwolven.items.game.host.PlayerManagementItem;
import me.mxndarijn.weerwolven.items.game.host.VanishItem;
import me.mxndarijn.weerwolven.items.game.player.GamePlayerTool;
import me.mxndarijn.weerwolven.items.game.spectate.LeaveGameItem;
import me.mxndarijn.weerwolven.items.game.spectate.TeleportItem;
import me.mxndarijn.weerwolven.items.preset.PresetConfigureTool;
import me.mxndarijn.weerwolven.items.spawn.GamesItem;
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
    GAMES_ITEM(
            MxDefaultItemStackBuilder.create(Material.COMPASS, 1)
                    .setName("<gray>Game Menu")
                    .addLore(" ")
                    .addLore("<yellow>Met dit item kan je games joinen,")
                    .addLore("<yellow>en games aanmaken als je een host bent.")
                    .build(),
            p -> {
                return p.getWorld().getUID().equals(Functions.getSpawnLocation().getWorld().getUID());
            },
            false,
            GamesItem.class,
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK
    ),
    GAME_SPECTATOR_LEAVE_ITEM(MxDefaultItemStackBuilder.create(Material.RED_BED)
            .setName("<gray>Verlaat Game")
            .addBlankLore()
            .addLore("<yellow>Gebruik dit item om te stoppen met spectaten.")
            .addCustomTagString(ItemTag.DROPPABLE.getPersistentDataTag(), false)
            .build(),
            p -> {
                return true;
            },
            false,
            LeaveGameItem.class,
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK),
    GAME_SPECTATOR_TELEPORT_ITEM(MxDefaultItemStackBuilder.create(Material.COMPASS)
            .setName("<gray>Teleporteer naar speler")
            .addBlankLore()
            .addLore("<yellow>Gebruik dit item om te teleporteren naar spelers.")
            .addCustomTagString(ItemTag.DROPPABLE.getPersistentDataTag(), false)
            .build(),
            p -> {
                return true;
            },
            false,
            TeleportItem.class,
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK),
    VANISH_ITEM(MxDefaultItemStackBuilder.create(Material.ENDER_EYE)
            .setName("<gray>Vanish")
            .addBlankLore()
            .addLore("<yellow>Met dit item kan je ontzichtbaar worden.")
            .build(),
            p -> {
                return true;
            },
            false,
            VanishItem.class,
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK),
    PLAYER_MANAGEMENT_ITEM(MxDefaultItemStackBuilder.create(Material.CYAN_DYE)
            .setName("<gray>Kleuren tool")
            .addBlankLore()
            .addLore("<yellow>Met dit item kan je kleuren beheren.")
            .build(),
            p -> {
                return true;
            },
            false,
            PlayerManagementItem.class,
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK),
    HOST_TOOL(
            MxDefaultItemStackBuilder.create(Material.NETHER_STAR, 1)
                    .setName("<gray>Host Tool")
                    .addLore(" ")
                    .addLore("<yellow>Met dit item kan je een game beheren.")
                    .build(),
            p -> {
                return true;
            },
            false,
            GameHostItem.class,
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK
    ),
    GAME_PLAYER_TOOL(MxDefaultItemStackBuilder.create(Material.NETHER_STAR)
            .setName("<gray>Speler Tool")
            .addBlankLore()
            .addLore("<yellow>Met dit item kan je stemmen,")
            .addLore("<yellow>hosts een vraag stellen,")
            .addLore("<yellow>of alle kleuren zien.")
            .addCustomTagString(ItemTag.DROPPABLE.getPersistentDataTag(), false)
            .addCustomTagString(ItemTag.VANISHABLE.getPersistentDataTag(), false)
            .build(),
            p -> {
                return true;
            },
            true,
            GamePlayerTool.class,
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK),
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