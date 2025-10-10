package me.mxndarijn.weerwolven.commands;

import me.mxndarijn.weerwolven.WeerWolven;
import me.mxndarijn.weerwolven.data.WeerWolvenChatPrefix;
import me.mxndarijn.weerwolven.data.WeerWolvenLanguageText;
import me.mxndarijn.weerwolven.data.WeerWolvenPermissions;
import me.mxndarijn.weerwolven.managers.PresetsManager;
import me.mxndarijn.weerwolven.presets.Preset;
import nl.mxndarijn.mxlib.inventory.MxInventoryIndex;
import nl.mxndarijn.mxlib.inventory.MxInventoryManager;
import nl.mxndarijn.mxlib.inventory.MxInventorySlots;
import nl.mxndarijn.mxlib.inventory.MxItemClicked;
import nl.mxndarijn.mxlib.inventory.menu.MxListInventoryBuilder;
import nl.mxndarijn.mxlib.item.MxDefaultItemStackBuilder;
import nl.mxndarijn.mxlib.item.Pair;
import nl.mxndarijn.mxlib.language.LanguageManager;
import nl.mxndarijn.mxlib.mxworld.MxWorld;
import nl.mxndarijn.mxlib.util.MessageUtil;
import nl.mxndarijn.mxlib.util.MxWorldFilter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

public class PresetsCommand extends WeerWolvenMxCommand {


    public PresetsCommand(WeerWolvenPermissions permission, boolean onlyPlayersCanExecute, boolean canBeExecutedInGame, MxWorldFilter worldFilter) {
        super(permission, onlyPlayersCanExecute, canBeExecutedInGame, worldFilter);
    }

    public PresetsCommand(WeerWolvenPermissions permission, boolean onlyPlayersCanExecute, boolean canBeExecutedInGame) {
        super(permission, onlyPlayersCanExecute, canBeExecutedInGame);
    }

    @Override
    public void execute(CommandSender sender, Command command, String label, String[] args) {
        Player p = (Player) sender;

        ArrayList<Preset> presets = PresetsManager.getInstance().getAllPresets();
        // Default sort by name
        presets.sort(Comparator.comparing(preset -> preset.getConfig().getName().toLowerCase()));

        // Click handler for selecting a preset
        MxItemClicked clickedOnPreset = (mxInv, e1) -> {
            Bukkit.getScheduler().scheduleSyncDelayedTask(JavaPlugin.getPlugin(WeerWolven.class), () -> {
                if (e1.getCurrentItem() != null) {
                    ItemStack is = e1.getCurrentItem();
                    ItemMeta im = is.getItemMeta();
                    if (im == null) return;
                    PersistentDataContainer container = im.getPersistentDataContainer();
                    Optional<Preset> optionalPreset = PresetsManager.getInstance().getPresetById(container.get(new NamespacedKey(JavaPlugin.getPlugin(WeerWolven.class), Preset.PRESET_ITEMMETA_TAG), PersistentDataType.STRING));

                    if (optionalPreset.isPresent()) {
                        Preset preset = optionalPreset.get();
                        p.closeInventory();
                        //check if world is already loaded
                        if (preset.getMxWorld().isPresent()) {
                            if (preset.getMxWorld().get().isLoaded()) {
                                MxWorld mxWorld = preset.getMxWorld().get();
                                World w = Bukkit.getWorld(mxWorld.getWorldUID());
                                if (w != null) {
                                    p.teleport(w.getSpawnLocation());
                                    MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.COMMAND_PRESETS_NOW_IN_PRESET, Collections.emptyList()));
                                } else {
                                    MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.COMMAND_PRESETS_WORLD_NOT_FOUND_BUT_LOADED, Collections.emptyList()));
                                }
                                return;
                            }
                        }
                        MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.COMMAND_PRESETS_LOADING_WORLD, Collections.emptyList()));
                        preset.loadWorld().thenAccept(loaded -> {
                            if (loaded) {
                                MxWorld mxWorld = preset.getMxWorld().get();
                                World w = Bukkit.getWorld(mxWorld.getWorldUID());
                                if (w != null) {
                                    p.teleport(w.getSpawnLocation());
                                    MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.COMMAND_PRESETS_NOW_IN_PRESET, Collections.emptyList()));
                                } else {
                                    MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.COMMAND_PRESETS_WORLD_NOT_FOUND_BUT_LOADED, Collections.emptyList()));
                                }
                            }
                        });
                    } else {
                        MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.COMMAND_PRESETS_WORLD_COULD_NOT_BE_LOADED, Collections.emptyList()));
                    }
                }
            });
        };

        openPresetsInventory(p, presets, clickedOnPreset);
    }

    private void openPresetsInventory(Player p, ArrayList<Preset> presets, MxItemClicked clickedOnPreset) {
        ArrayList<Pair<ItemStack, MxItemClicked>> list = presets.stream()
                .map(preset -> new Pair<>(preset.getItemStack(), clickedOnPreset))
                .collect(Collectors.toCollection(ArrayList::new));

        MxListInventoryBuilder builder = MxListInventoryBuilder.create("<gray>Configureer presets", MxInventorySlots.SIX_ROWS)
                .setAvailableSlots(MxInventoryIndex.ROW_ONE_TO_FIVE)
                // Do not override default back button slot (45); free up 46 and 47 for sorting controls
                .setListItems(list)
                .setItem(MxDefaultItemStackBuilder.create(Material.PAPER)
                        .setName("<gray>Info")
                        .addLore(" ")
                        .addLore("<yellow>Klik op een preset om deze te configuren.")
                        .build(), 49, null);

        // Sort by name button at slot 46
        ItemStack sortByName = MxDefaultItemStackBuilder.create(Material.NAME_TAG)
                .setName("<gray>Sorteer op Naam")
                .addBlankLore()
                .addLore("<yellow>Klik hier om de presets te sorteren op naam.")
                .build();
        builder.setItem(sortByName, 46, (inv, e) -> {
            ArrayList<Preset> sorted = new ArrayList<>(presets);
            sorted.sort(Comparator.comparing(preset -> preset.getConfig().getName().toLowerCase()));
            openPresetsInventory((Player) e.getWhoClicked(), sorted, clickedOnPreset);
        });

        // Sort by host difficulty button at slot 47
        ItemStack sortByHostDif = MxDefaultItemStackBuilder.create(Material.TURTLE_EGG)
                .setName("<gray>Sorteer op Host-Moeilijk")
                .addBlankLore()
                .addLore("<yellow>Klik hier om de presets te sorteren op host-moeilijkheid.")
                .build();
        builder.setItem(sortByHostDif, 47, (inv, e) -> {
            ArrayList<Preset> sorted = new ArrayList<>(presets);
            sorted.sort(Comparator
                    .comparing((Preset pr) -> pr.getConfig().getName()));
            openPresetsInventory((Player) e.getWhoClicked(), sorted, clickedOnPreset);
        });

        MxInventoryManager.getInstance().addAndOpenInventory(p, builder.build());
    }
}
