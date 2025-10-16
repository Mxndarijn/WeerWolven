package me.mxndarijn.weerwolven.data;

import lombok.Getter;
import me.mxndarijn.weerwolven.WeerWolven;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import nl.mxndarijn.mxlib.inventory.MxInventoryIndex;
import nl.mxndarijn.mxlib.inventory.MxInventoryManager;
import nl.mxndarijn.mxlib.inventory.MxInventorySlots;
import nl.mxndarijn.mxlib.inventory.MxItemClicked;
import nl.mxndarijn.mxlib.inventory.menu.MxListInventoryBuilder;
import nl.mxndarijn.mxlib.item.MxSkullItemStackBuilder;
import nl.mxndarijn.mxlib.item.Pair;
import nl.mxndarijn.mxlib.language.LanguageManager;
import nl.mxndarijn.mxlib.logger.LogLevel;
import nl.mxndarijn.mxlib.logger.Logger;
import nl.mxndarijn.mxlib.util.MessageUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

@Getter
public enum ItemTag {
    DROPPABLE("droppable", data -> {
        boolean dataBoolean = data == null || !data.equalsIgnoreCase("false");
        return MxSkullItemStackBuilder.create(1)
                .setSkinFromHeadsData("dropper")
                .setName("<gray>Droppable")
                .addBlankLore()
                .addLore("<gray>Status: " + (dataBoolean ? "<green>Droppable" : "<red>Undroppable"))
                .addBlankLore()
                .addLore("<yellow>Klik hier om de status te togglen.")
                .build();
    }, (mxInv, e) -> {
        onClick(e, "droppable", "Undroppable");
    }),
    VANISHABLE("vanishable", data -> {
        boolean dataBoolean = data == null || !data.equalsIgnoreCase("false");
        return MxSkullItemStackBuilder.create(1)
                .setSkinFromHeadsData("ghost")
                .setName("<gray>Vanish")
                .addBlankLore()
                .addLore("<gray>Status: " + (dataBoolean ? "<green>Blijft" : "<red>Verdwijnt"))
                .addBlankLore()
                .addLore("<yellow>Klik hier om de status te togglen.")
                .build();
    }, (mxInv, e) -> {
        onClick(e, "vanishable", "Vanish");
    }),
    PLACEABLE("placeable", data -> {
        boolean dataBoolean = data == null || !data.equalsIgnoreCase("false");
        return MxSkullItemStackBuilder.create(1)
                .setSkinFromHeadsData("piston")
                .setName("<gray>Placeable")
                .addBlankLore()
                .addLore("<gray>Status: " + (dataBoolean ? "<green>Placeable" : "<red>Unplaceable"))
                .addBlankLore()
                .addLore("<yellow>Klik hier om de status te togglen.")
                .build();
    }, (mxInv, e) -> {
        onClick(e, "placeable", "Unplaceable");
    }),
    CLEARABLE("clearable", data -> {
        boolean dataBoolean = data == null || !data.equalsIgnoreCase("false");
        return MxSkullItemStackBuilder.create(1)
                .setSkinFromHeadsData("barrier")
                .setName("<gray>Clearable")
                .addBlankLore()
                .addLore("<gray>Status: " + (dataBoolean ? "<green>Clearable" : "<red>Unclearable"))
                .addBlankLore()
                .addLore("<yellow>Klik hier om de status te togglen.")
                .build();
    }, (mxInv, e) -> {
        onClick(e, "clearable", "Unclearable");
    });

    private final String persistentDataTag;
    private final ItemTagContainer container;
    private final MxItemClicked clicked;
    ItemTag(String persistentDataTag, ItemTagContainer item, MxItemClicked clicked) {
        this.persistentDataTag = persistentDataTag;
        this.container = item;
        this.clicked = clicked;

    }

    private static void onClick(InventoryClickEvent e, String key, String loreName) {
        ItemStack is = e.getWhoClicked().getInventory().getItemInMainHand();
        ItemMeta im = is.getItemMeta();

        PersistentDataContainer container = im.getPersistentDataContainer();
        NamespacedKey namespacedKey = new NamespacedKey(JavaPlugin.getPlugin(WeerWolven.class), key);

        String data = container.get(namespacedKey, PersistentDataType.STRING);

        boolean dataBoolean = data != null && data.equalsIgnoreCase("true");
        dataBoolean = !dataBoolean;

        container.set(namespacedKey, PersistentDataType.STRING, String.valueOf(dataBoolean));

        Component loreComponent = MiniMessage.miniMessage().deserialize("<!i><red>" + loreName);

        List<Component> list = im.hasLore() ? new ArrayList<>(im.lore()) : new ArrayList<>();

        if (!dataBoolean) {
            list.add(loreComponent);
        } else {
            list.removeIf(c -> PlainTextComponentSerializer.plainText().serialize(c)
                    .equalsIgnoreCase(PlainTextComponentSerializer.plainText().serialize(loreComponent)));
        }

        im.lore(list);
        is.setItemMeta(im);

        openItemTagInventory((Player) e.getWhoClicked(), is);
        MessageUtil.sendMessageToPlayer(e.getWhoClicked(), LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.ITEMTAG_CHANGED));
    }

    private static void openItemTagInventory(Player p, ItemStack is) {
        PersistentDataContainer container = is.getItemMeta().getPersistentDataContainer();
        List<Pair<ItemStack, MxItemClicked>> list = new ArrayList<>();
        for (ItemTag value : ItemTag.values()) {
            try {
                list.add(new Pair<>(
                        value.getContainer().getItem(container.get(new NamespacedKey(JavaPlugin.getPlugin(WeerWolven.class), value.getPersistentDataTag()), PersistentDataType.STRING)),
                        value.getClicked()
                ));
            } catch(Exception ex) {
                Logger.logMessage(LogLevel.ERROR, "Could not load itemtag: ");
                ex.printStackTrace();
            }
        }
        MxInventoryManager.getInstance().addAndOpenInventory(p, new MxListInventoryBuilder("<gray>ItemTags", MxInventorySlots.THREE_ROWS)
                .setShowPageNumbers(false)
                .setListItems(list)
                .setAvailableSlots(MxInventoryIndex.ROW_ONE_TO_TWO)
                .build());
    }

}

