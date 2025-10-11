package me.mxndarijn.weerwolven.commands;

import me.mxndarijn.weerwolven.WeerWolven;
import me.mxndarijn.weerwolven.data.WeerWolvenPermissions;
import me.mxndarijn.weerwolven.data.WeerWolvenPrefix;
import me.mxndarijn.weerwolven.data.Roles;
import me.mxndarijn.weerwolven.data.WeerWolvenChatPrefix;
import me.mxndarijn.weerwolven.data.WeerWolvenLanguageText;
import me.mxndarijn.weerwolven.game.RoleSet;
import me.mxndarijn.weerwolven.managers.RoleSetManager;
import nl.mxndarijn.mxlib.chatinput.MxChatInputManager;
import nl.mxndarijn.mxlib.configfiles.ConfigService;
import nl.mxndarijn.mxlib.inventory.MxInventoryIndex;
import nl.mxndarijn.mxlib.inventory.MxInventoryManager;
import nl.mxndarijn.mxlib.inventory.MxInventorySlots;
import nl.mxndarijn.mxlib.inventory.MxItemClicked;
import nl.mxndarijn.mxlib.inventory.menu.MxDefaultMenuBuilder;
import nl.mxndarijn.mxlib.inventory.menu.MxListInventoryBuilder;
import nl.mxndarijn.mxlib.item.MxDefaultItemStackBuilder;
import nl.mxndarijn.mxlib.item.MxSkullItemStackBuilder;
import nl.mxndarijn.mxlib.inventory.heads.MxHeadManager;
import nl.mxndarijn.mxlib.inventory.heads.MxHeadSection;
import nl.mxndarijn.mxlib.language.LanguageManager;
import nl.mxndarijn.mxlib.logger.LogLevel;
import nl.mxndarijn.mxlib.logger.Logger;
import nl.mxndarijn.mxlib.util.MessageUtil;
import nl.mxndarijn.mxlib.util.MxWorldFilter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class RoleSetCommand extends WeerWolvenMxCommand {

    private static final String ROLESET_KEY = "roleset-id";

    public RoleSetCommand(WeerWolvenPermissions permission, boolean onlyPlayersCanExecute, boolean canBeExecutedInGame, MxWorldFilter worldFilter) {
        super(permission, onlyPlayersCanExecute, canBeExecutedInGame, worldFilter);
    }

    public RoleSetCommand(WeerWolvenPermissions permission, boolean onlyPlayersCanExecute, boolean canBeExecutedInGame) {
        super(permission, onlyPlayersCanExecute, canBeExecutedInGame);
    }

    @Override
    public void execute(CommandSender sender, Command command, String label, String[] args) {
        Player p = (Player) sender;
        openRoleSetList(p);
    }

    private void openRoleSetList(Player p) {
        ArrayList<RoleSet> sets = new ArrayList<>(RoleSetManager.getInstance().getAll());
        // Sort by name
        sets.sort(Comparator.comparing(rs -> rs.getName().toLowerCase()));

        MxItemClicked onClick = (mxInv, e1) -> Bukkit.getScheduler().scheduleSyncDelayedTask(JavaPlugin.getPlugin(WeerWolven.class), () -> {
            ItemStack is = e1.getCurrentItem();
            if (is == null) return;
            ItemMeta im = is.getItemMeta();
            if (im == null) return;
            PersistentDataContainer c = im.getPersistentDataContainer();
            String id = c.get(new NamespacedKey(JavaPlugin.getPlugin(WeerWolven.class), ROLESET_KEY), PersistentDataType.STRING);
            if (id == null) return;
            Optional<RoleSet> opt = RoleSetManager.getInstance().getById(id);
            opt.ifPresent(roleSet -> openRoleSetEditor((Player) e1.getWhoClicked(), roleSet));
        });

        ArrayList<nl.mxndarijn.mxlib.item.Pair<ItemStack, MxItemClicked>> list = sets.stream().map(rs -> new nl.mxndarijn.mxlib.item.Pair<>(buildRoleSetIcon(rs), onClick)).collect(Collectors.toCollection(ArrayList::new));

        MxListInventoryBuilder builder = MxListInventoryBuilder.create("<gray>RoleSets", MxInventorySlots.SIX_ROWS)
                .setAvailableSlots(MxInventoryIndex.ROW_ONE_TO_FIVE)
                .setListItems(list)
                .setItem(MxDefaultItemStackBuilder.create(Material.PAPER)
                        .setName("<gray>Info")
                        .addBlankLore()
                        .addLore("<yellow>Klik op een set om te bewerken.")
                        .addLore("<yellow>Klik op de groene knop om een nieuwe set te maken.")
                        .build(), 49, null);

        // Create new roleset button
        ItemStack create = MxDefaultItemStackBuilder.create(Material.LIME_CONCRETE)
                .setName("<green>Nieuwe RoleSet")
                .addBlankLore()
                .addLore("<gray>Maak een lege RoleSet aan.")
                .addLore("<gray>Je kunt daarna de rollen instellen en opslaan.")
                .build();
        builder.setItem(create, 53, (inv, e) -> {
            String newName = generateUniqueName();
            RoleSet rs = RoleSet.createEmpty(newName);
            // Do not register yet; add to manager upon saving
            openRoleSetEditor((Player) e.getWhoClicked(), rs);
        });

        MxInventoryManager.getInstance().addAndOpenInventory(p, builder.build());
    }

    private ItemStack buildRoleSetIcon(RoleSet rs) {
        MxSkullItemStackBuilder skull = rs.getSkullItemStackBuilder();
        ItemStack item = skull.build();
        // attach roleset id key
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer cont = meta.getPersistentDataContainer();
            cont.set(new NamespacedKey(JavaPlugin.getPlugin(WeerWolven.class), ROLESET_KEY), PersistentDataType.STRING, rs.getId());
            item.setItemMeta(meta);
        }
        return item;
    }

    private void openRoleSetEditor(Player p, RoleSet original) {
        // working copy of roles map
        final String title = "<gray>Edit: " + original.getName();
        renderEditor(p, original, title);
    }

    private void renderEditor(Player p, RoleSet roleSet, String title) {
        MxDefaultMenuBuilder builder = MxDefaultMenuBuilder.create(title, MxInventorySlots.SIX_ROWS);

        // Place each role head in grid using a common centered layout, remove villager will be auto filled.
        Roles[] roles = Arrays.stream(Roles.values()).filter(roles1 -> {
            return !roles1.getRolName().equalsIgnoreCase("villager");
        }).toArray(Roles[]::new);
        int[] slots = new int[]{10,11,12,13,14,15,16,
                19,20,21,22,23,24,25,
                28,29,30,31,32,33,34,
                37,38,39,40,41,42,43};
        for (int i = 0; i < roles.length && i < slots.length; i++) {
            Roles r = roles[i];
            int count = roleSet.getRoleSet().getOrDefault(r, 0);
            ItemStack head = r.getHead()
                    .setName(r.getRoleWithColor())
                    .addBlankLore()
                    .addLore("<gray>In deze set: <yellow>" + count)
                    .addBlankLore()
                    .addLore("<green>Linkerklik: +1")
                    .addLore("<red>Shift+klik: -1")
                    .build();
            int slot = slots[i];
            builder.setItem(head, slot, (inv, e) -> {
                boolean shift = e.isShiftClick();
                int cur = roleSet.getRoleSet().getOrDefault(r, 0);
                if (shift) {
                    cur = Math.max(0, cur - 1);
                } else {
                    cur = cur + 1;
                }
                roleSet.getRoleSet().put(r, cur);
                renderEditor((Player) e.getWhoClicked(), roleSet, title);
            });
        }

        // Change Name button
        ItemStack changeName = MxDefaultItemStackBuilder.create(Material.NAME_TAG)
                .setName("<gray>Verander naam")
                .addBlankLore()
                .addLore("<gray>Huidige naam: <yellow>" + roleSet.getName())
                .addBlankLore()
                .addLore("<yellow>Klik en typ vervolgens de nieuwe naam in de chat.")
                .build();
        builder.setItem(changeName, 47, (inv, e) -> {
            Player pl = (Player) e.getWhoClicked();
            MessageUtil.sendMessageToPlayer(pl, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.ROLE_SETS_ENTER_NEW_NAME));
            pl.closeInventory();
            MxChatInputManager.getInstance().addChatInputCallback(pl.getUniqueId(), message -> {
                roleSet.setName(message);
                MessageUtil.sendMessageToPlayer(pl, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.PRESET_CONFIGURE_TOOL_NAME_CHANGED, Collections.singletonList(message)));

                Bukkit.getScheduler().scheduleSyncDelayedTask(JavaPlugin.getPlugin(WeerWolven.class), () -> {
                    renderEditor(pl, roleSet, "<gray>Edit: " + roleSet.getName());
                });
            });
        });

        // Change Skull button
        ItemStack changeSkull = MxDefaultItemStackBuilder.create(Material.SKELETON_SKULL)
                .setName("<gray>Verander skull")
                .addBlankLore()
                .addLore("<gray>Huidig: " + (MxHeadManager.getInstance().getHeadSection(roleSet.getSkullId()).isPresent() ?
                        MxHeadManager.getInstance().getHeadSection(roleSet.getSkullId()).get().getNameOptional().orElse("Niet-gevonden") : "Niet-gevonden"))
                .addBlankLore()
                .addLore("<yellow>Klik hier om de skull van de RoleSet te veranderen.")
                .build();
        builder.setItem(changeSkull, 46, (mainInv, clickMain) -> {
            ArrayList<nl.mxndarijn.mxlib.item.Pair<ItemStack, MxItemClicked>> list = new ArrayList<>();
            MxItemClicked clicked = (mxInv, e1) -> {
                ItemStack is = e1.getCurrentItem();
                if (is == null) return;
                ItemMeta im = is.getItemMeta();
                if (im == null) return;
                PersistentDataContainer container = im.getPersistentDataContainer();
                String key = container.get(new NamespacedKey(JavaPlugin.getPlugin(WeerWolven.class), "skull_key"), PersistentDataType.STRING);
                if (key == null) return;

                Optional<MxHeadSection> section = MxHeadManager.getInstance().getHeadSection(key);
                section.ifPresent(mxHeadSection -> {
                    MessageUtil.sendMessageToPlayer((Player) clickMain.getWhoClicked(), WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.PRESET_CONFIGURE_TOOL_SKULL_CHANGED, Collections.singletonList(mxHeadSection.getNameOptional().orElse(key))));
                });
                roleSet.setSkullId(key);

                // Re-open editor to reflect new skull on the icon
                Bukkit.getScheduler().scheduleSyncDelayedTask(JavaPlugin.getPlugin(WeerWolven.class), () -> {
                    renderEditor((Player) clickMain.getWhoClicked(), roleSet, "<gray>Edit: " + roleSet.getName());
                });
            };

            MxHeadManager mxHeadManager = MxHeadManager.getInstance();
            mxHeadManager.getAllHeadKeys().forEach(key -> {
                Optional<MxHeadSection> section = mxHeadManager.getHeadSection(key);
                section.ifPresent(mxHeadSection -> {
                    MxSkullItemStackBuilder b = MxSkullItemStackBuilder.create(1)
                            .setSkinFromHeadsData(key)
                            .setName("<gray>" + mxHeadSection.getNameOptional().orElse(key))
                            .addBlankLore()
                            .addLore("<yellow>Klik om de skull te selecteren.")
                            .addCustomTagString("skull_key", mxHeadSection.getKey());
                    list.add(new nl.mxndarijn.mxlib.item.Pair<>(b.build(), clicked));
                });
            });

            MxInventoryManager.getInstance().addAndOpenInventory((Player) clickMain.getWhoClicked(),
                    MxListInventoryBuilder.create("<gray>Kies skull", MxInventorySlots.SIX_ROWS)
                            .setAvailableSlots(MxInventoryIndex.ROW_ONE_TO_FIVE)
                            .addListItems(list)
                            .setItem(MxDefaultItemStackBuilder.create(Material.PAPER)
                                    .setName("<gray>Info")
                                    .addBlankLore()
                                    .addLore("<yellow>Klik op een skull om deze als icon te gebruiken.")
                                    .build(), 48, null)
                            .build());
        });

        // Save button
        ItemStack save = MxDefaultItemStackBuilder.create(Material.LIME_CONCRETE)
                .setName("<green>Opslaan")
                .addLore("<gray>Sla de wijzigingen.")
                .build();
        builder.setItem(save, 53, (inv, e) -> {
            // commit: update original RoleSet with working values
            RoleSetManager.getInstance().addOrUpdate(roleSet);
            RoleSetManager.getInstance().saveAll();
            ConfigService.getInstance().saveAll();
            e.getWhoClicked().closeInventory();
            MessageUtil.sendMessageToPlayer(e.getWhoClicked(), WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.ROLE_SETS_SAVED));
            Logger.logMessage(LogLevel.INFORMATION, WeerWolvenPrefix.ROLE_SETS_MANAGER, "Saved roleset: " + roleSet.getName());
        });

        // Delete button
        ItemStack delete = MxDefaultItemStackBuilder.create(Material.RED_CONCRETE)
                .setName("<red>Verwijderen")
                .addLore("<gray>Verwijder deze RoleSet.")
                .build();
        builder.setItem(delete, 51, (inv, e) -> {
            RoleSetManager.getInstance().removeById(roleSet.getId());
            RoleSetManager.getInstance().saveAll();
            ConfigService.getInstance().saveAll();
            e.getWhoClicked().closeInventory();
            Logger.logMessage(LogLevel.INFORMATION, WeerWolvenPrefix.ROLE_SETS_MANAGER, "Deleted roleset: " + roleSet.getName() + " (" + roleSet.getId() + ")");
        });

        // Cancel button
        ItemStack cancel = MxDefaultItemStackBuilder.create(Material.BARRIER)
                .setName("<red>Annuleren")
                .addLore("<gray>Annuleer en sluit zonder op te slaan")
                .build();
        builder.setItem(cancel, 45, (inv, e) -> e.getWhoClicked().closeInventory());

        // Current set icon updated with new counts
        ItemStack icon = roleSet.getSkullItemStackBuilder().build();
        builder.setItem(icon, 49, null);

        MxInventoryManager.getInstance().addAndOpenInventory(p, builder.build());
    }


    private String generateUniqueName() {
        String base = "Nieuwe-RoleSet";
        String name = base;
        int i = 1;
        while (RoleSetManager.getInstance().get(name).isPresent()) {
            name = base + "-" + i;
            i++;
        }
        return name;
    }
}
