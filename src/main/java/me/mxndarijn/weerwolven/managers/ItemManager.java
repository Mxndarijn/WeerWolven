package me.mxndarijn.weerwolven.managers;

import me.mxndarijn.weerwolven.data.Items;
import me.mxndarijn.weerwolven.data.WeerWolvenPrefix;
import nl.mxndarijn.mxlib.logger.LogLevel;
import nl.mxndarijn.mxlib.logger.Logger;
import nl.mxndarijn.mxlib.mxitem.MxItem;
import nl.mxndarijn.mxlib.util.MxWorldFilter;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class ItemManager {

    private static ItemManager instance;

    private final ArrayList<MxItem> items;

    private ItemManager() {
        Logger.logMessage(LogLevel.INFORMATION, WeerWolvenPrefix.ITEM_MANAGER, "Loading Item-Manager...");

        items = new ArrayList<>();
        for (Items item : Items.values()) {
            try {
                Logger.logMessage(LogLevel.DEBUG, WeerWolvenPrefix.ITEM_MANAGER, "Loading usable item " + item.getClassObject().getName() + "...");
                MxItem mxItem = item.getClassObject().getDeclaredConstructor(
                        ItemStack.class,
                        MxWorldFilter.class,
                        boolean.class,
                        Action[].class
                ).newInstance(item.getItemStack(), item.getWorldFilter(), item.isGameItem(), item.getActions());
                items.add(mxItem);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                Logger.logMessage(LogLevel.ERROR, WeerWolvenPrefix.ITEM_MANAGER, "Could not load MxItem: " + item.getClassObject().getName());
                e.printStackTrace();
            }
        }

    }

    public static ItemManager getInstance() {
        if (instance == null) {
            instance = new ItemManager();
        }
        return instance;
    }
}
