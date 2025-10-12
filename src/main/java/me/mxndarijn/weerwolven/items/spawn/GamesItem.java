package me.mxndarijn.weerwolven.items.spawn;

import me.mxndarijn.weerwolven.data.UpcomingGameStatus;
import me.mxndarijn.weerwolven.data.WeerWolvenChatPrefix;
import me.mxndarijn.weerwolven.data.WeerWolvenLanguageText;
import me.mxndarijn.weerwolven.data.WeerWolvenPermissions;
import me.mxndarijn.weerwolven.game.Game;
import me.mxndarijn.weerwolven.game.GameInfo;
import me.mxndarijn.weerwolven.game.RoleSet;
import me.mxndarijn.weerwolven.items.WeerWolvenMxItem;
import me.mxndarijn.weerwolven.managers.GameManager;
import me.mxndarijn.weerwolven.managers.GameWorldManager;
import me.mxndarijn.weerwolven.managers.PresetsManager;
import me.mxndarijn.weerwolven.managers.RoleSetManager;
import me.mxndarijn.weerwolven.presets.Preset;
import nl.mxndarijn.mxlib.chatinput.MxChatInputManager;
import nl.mxndarijn.mxlib.configfiles.ConfigService;
import nl.mxndarijn.mxlib.configfiles.StandardConfigFile;
import nl.mxndarijn.mxlib.inventory.MxInventoryIndex;
import nl.mxndarijn.mxlib.inventory.MxInventoryManager;
import nl.mxndarijn.mxlib.inventory.MxInventorySlots;
import nl.mxndarijn.mxlib.inventory.MxItemClicked;
import nl.mxndarijn.mxlib.inventory.menu.MxDefaultMenuBuilder;
import nl.mxndarijn.mxlib.inventory.menu.MxListInventoryBuilder;
import nl.mxndarijn.mxlib.item.MxDefaultItemStackBuilder;
import nl.mxndarijn.mxlib.item.MxSkullItemStackBuilder;
import nl.mxndarijn.mxlib.item.Pair;
import nl.mxndarijn.mxlib.language.LanguageManager;
import nl.mxndarijn.mxlib.permission.MxPermissionService;
import nl.mxndarijn.mxlib.util.MessageUtil;
import nl.mxndarijn.mxlib.util.MxWorldFilter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class GamesItem extends WeerWolvenMxItem {

    public GamesItem(ItemStack is, MxWorldFilter worldFilter, boolean gameItem, Action... actions) {
        super(is, worldFilter, gameItem, actions);
    }

    @Override
    public void execute(Player p, PlayerInteractEvent e) {
        ArrayList<Pair<ItemStack, MxItemClicked>> list = new ArrayList<>();
        GameManager.getInstance().getUpcomingGameList().forEach(upcomingGame -> {
            list.add(new Pair<>(
                    upcomingGame.getItemStack(p),
                    (mxInv, click) -> handleUpcomingGameClick(p, upcomingGame, click)
            ));
        });

        MxListInventoryBuilder builder = MxListInventoryBuilder.create("<gray>Games", MxInventorySlots.THREE_ROWS)
                .setAvailableSlots(MxInventoryIndex.ROW_ONE_TO_TWO)
                .setItem(MxDefaultItemStackBuilder.create(Material.PAPER)
                                .setName("<gray>Queue Uitleg")
                                .addBlankLore()
                                .addLore("<gray>1. Nieuwe spelers komen vooraan.")
                                .addLore("<gray>2. Spelers die >2 dagen niet hebben gespeeld.")
                                .addLore("<gray>3. Recente spelers")
                                .build(),
                        18, ((mxInv, e1) -> {}))
                .setListItems(list);

        // Plan new game button
        if (p.hasPermission(WeerWolvenPermissions.COMMAND_GAMES_CREATE_SPECIFIC_GAME.getPermission())) {
            builder.setItem(MxSkullItemStackBuilder.create(1)
                            .setName("<gray>Plan Game")
                            .setSkinFromHeadsData("wooden-plus")
                            .addBlankLore()
                            .addLore("<gray>Plan een nieuwe game.")
                            .addBlankLore()
                            .addLore("<yellow>Klik hier om een nieuwe game te plannen.")
                            .build(),
                    22,
                    (mxInv, e1) -> selectRoleSetFirst(p));
        }

        MxInventoryManager.getInstance().addAndOpenInventory(p, builder.build());
    }

    private void handleUpcomingGameClick(Player p, GameInfo upcomingGame, InventoryClickEvent click) {
        if (!GameManager.getInstance().getUpcomingGameList().contains(upcomingGame)) {
            MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAMES_COULD_NOT_FIND_GAME));
            p.closeInventory();
            return;
        }

        // If queue is not yet open, block normal click
        int hours = ConfigService.getInstance().get(StandardConfigFile.MAIN_CONFIG).getCfg().getInt("time-before-queue-is-open-in-hours");
        long minutesDiff = Math.abs(java.time.Duration.between(LocalDateTime.now(), upcomingGame.getTime()).toMinutes());
        if (!click.isShiftClick() && minutesDiff > hours * 60L && upcomingGame.getStatus().isCanJoinQueue()) {
            MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAMES_ITEM_TO_EARLY_TO_JOIN, Collections.singletonList(String.valueOf(hours))));
            return;
        }

        switch (upcomingGame.getStatus()) {
            case PLAYING, FREEZE -> {
                // Spectate
                Optional<Game> game = GameWorldManager.getInstance().getGameByGameInfo(upcomingGame);
                game.ifPresent(value -> {
                    if(click.isShiftClick() && (p.hasPermission(WeerWolvenPermissions.ITEM_GAMES_MANAGE_OTHER_GAMES.getPermission()) || upcomingGame.getHost().equals(p.getUniqueId()))) {
                        game.get().addHost(p.getUniqueId());
                    }
                    value.addSpectator(p.getUniqueId());
                });
                return;
            }
            default -> {
                if (upcomingGame.getStatus().isCanJoinQueue() && !click.isShiftClick()) {
                    if (upcomingGame.getOrderedQueue().contains(p.getUniqueId())) {
                        upcomingGame.removePlayerFromQueue(p.getUniqueId());
                        MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAMES_LEFT_QUEUE));
                        p.closeInventory();
                    } else {
                        boolean alreadyQueuedElsewhere = GameManager.getInstance().getUpcomingGameList().stream()
                                .anyMatch(info -> info.getOrderedQueue().contains(p.getUniqueId()));
                        if (alreadyQueuedElsewhere) {
                            MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + "<red>Verlaat eerst de huidige wachtrij voordat je een andere joint.");
                        } else {
                            upcomingGame.addPlayerToQueue(p.getUniqueId());
                            MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(
                                    WeerWolvenLanguageText.GAMES_ENTERED_QUEUE,
                                    List.of(String.valueOf(upcomingGame.getQueuePositionOfPlayer(p.getUniqueId()) + 1))
                            ));
                            p.closeInventory();
                        }
                    }
                } else if (click.isShiftClick() && (p.hasPermission(WeerWolvenPermissions.ITEM_GAMES_MANAGE_OTHER_GAMES.getPermission()) || upcomingGame.getHost().equals(p.getUniqueId()))) {
                    openManageGameMenu(p, upcomingGame);
                }
            }
        }
    }

    private void openManageGameMenu(Player p, GameInfo upcomingGame) {
        if (upcomingGame.getStatus() != me.mxndarijn.weerwolven.data.UpcomingGameStatus.WAITING) return;
        MxInventoryManager.getInstance().addAndOpenInventory(p, MxDefaultMenuBuilder.create("<gray>Beheer Game", MxInventorySlots.THREE_ROWS)
                .setItem(MxDefaultItemStackBuilder.create(Material.FIREWORK_ROCKET)
                                .setName("<green>Start")
                                .addBlankLore()
                                .addLore("<yellow>Klik hier om de game te starten!")
                                .build(),
                        13,
                        (mxInv1, e12) -> {
                            Optional<Game> gameOptional = Game.createGameFromGameInfo(p.getUniqueId(), upcomingGame);
                            if (gameOptional.isPresent()) {
                                gameOptional.get().addHost(p.getUniqueId());
                                upcomingGame.setStatus(UpcomingGameStatus.CHOOSING_PLAYERS);

                            } else {
                                MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAMES_COULD_NOT_CREATE_GAME));
                            }
                            p.closeInventory();

                        }
                )
                .setItem(MxDefaultItemStackBuilder.create(Material.RED_CONCRETE)
                                .setName("<red>Verwijder Game")
                                .addBlankLore()
                                .addLore("<yellow>Klik hier om de game te verwijderen.")
                                .build(), 26,
                        (mxInv12, e13) -> {
                            GameManager.getInstance().removeUpcomingGame(upcomingGame);
                            MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAMES_GAME_DELETED));
                            p.closeInventory();
                        })
                .build());
    }

    private void selectRoleSetFirst(Player p) {
        ArrayList<Pair<ItemStack, MxItemClicked>> list = new ArrayList<>();

        // Existing RoleSets
        RoleSetManager.getInstance().getAll().forEach(rs -> {
            ItemStack icon = rs.getSkullItemStackBuilder()
                    .setName("<gray>" + rs.getName())
                    .addBlankLore()
                    .addLore("<yellow>Klik om deze RoleSet te gebruiken.")
                    .build();
            list.add(new Pair<>(icon, (inv, e) -> selectPresetNext(p, rs)));
        });

        // Temporary new RoleSet option
        if(MxPermissionService.getInstance().hasSenderPermission(p,WeerWolvenPermissions.ITEM_GAMES_CREATE_TEMP_ROLESET)) {
            ItemStack temp = MxDefaultItemStackBuilder.create(Material.WRITABLE_BOOK)
                    .setName("<yellow>Tijdelijke RoleSet maken")
                    .addBlankLore()
                    .addLore("<gray>Maak en gebruik een RoleSet zonder op te slaan.")
                    .addLore("<yellow>Klik om te beginnen.")
                    .build();
            list.add(new Pair<>(temp, (inv, e) -> openTemporaryRoleSetEditor(p, RoleSet.createEmpty("Tijdelijke set"))));
        }

        MxInventoryManager.getInstance().addAndOpenInventory(p,
                MxListInventoryBuilder.create("<gray>Kies RoleSet", MxInventorySlots.SIX_ROWS)
                        .setAvailableSlots(MxInventoryIndex.ROW_ONE_TO_FIVE)
                        .setListItems(list)
                        .setItem(MxDefaultItemStackBuilder.create(Material.PAPER)
                                .setName("<gray>Info")
                                .addBlankLore()
                                .addLore("<yellow>Kies een RoleSet of maak een tijdelijke.")
                                .build(), 49, null)
                        .build());
    }

    private void selectPresetNext(Player p, RoleSet roleSet) {
        ArrayList<Pair<ItemStack, MxItemClicked>> list = new ArrayList<>();
        for (Preset preset : PresetsManager.getInstance().getConfiguredPresets()) {
            ItemStack icon = MxSkullItemStackBuilder.create(1)
                    .setSkinFromHeadsData(preset.getConfig().getSkullId())
                    .setName("<gray>" + preset.getConfig().getName())
                    .addBlankLore()
                    .addLore("<yellow>Klik om deze preset te kiezen.")
                    .build();
            list.add(new Pair<>(icon, (inv, e) -> selectDateNext(p, preset, roleSet)));
        }
        MxInventoryManager.getInstance().addAndOpenInventory(p,
                MxListInventoryBuilder.create("<gray>Kies Preset", MxInventorySlots.SIX_ROWS)
                        .setAvailableSlots(MxInventoryIndex.ROW_ONE_TO_FIVE)
                        .setListItems(list)
                        .build());
    }

    private void selectDateNext(Player p, Preset preset, RoleSet roleSet) {
        ArrayList<Pair<ItemStack, MxItemClicked>> list = new ArrayList<>();
        java.time.format.DateTimeFormatter dayFmt = java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM", java.util.Locale.forLanguageTag("nl-NL"));
        LocalDate now = LocalDate.now();
        for (int i = 0; i < 16; i++) {
            LocalDate day = now.plusDays(i);
            String label = day.format(dayFmt);
            String hint = (i == 0 ? "Vandaag" : (i == 1 ? "Morgen" : "Over " + i + " dagen."));
            ItemStack icon = MxSkullItemStackBuilder.create(1)
                    .setSkinFromHeadsData("clock")
                    .setName("<gray>" + label)
                    .addBlankLore()
                    .addLore("<gray>Datum: " + hint)
                    .addBlankLore()
                    .addLore("<yellow>Klik om de game te hosten op " + label + ".")
                    .build();
            list.add(new Pair<>(icon, (inv, e) -> askTimeAndCreate(p, preset, roleSet, day, label)));
        }
        MxInventoryManager.getInstance().addAndOpenInventory(p,
                MxListInventoryBuilder.create("<gray>Datum selecteren", MxInventorySlots.THREE_ROWS)
                        .setAvailableSlots(MxInventoryIndex.ROW_ONE_TO_TWO)
                        .setListItems(list)
                        .build());
    }

    private void askTimeAndCreate(Player p, Preset preset, RoleSet roleSet, LocalDate date, String friendlyDate) {
        p.closeInventory();
        MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAMES_ITEM_DATE_SELECTED, Collections.singletonList(friendlyDate)));
        MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAMES_ITEM_ENTER_TIME));
        MxChatInputManager.getInstance().addChatInputCallback(p.getUniqueId(), message -> {
            String[] patterns = {"H:mm", "HH:mm"};
            LocalTime parsed = null;
            for (String pattern : patterns) {
                try {
                    parsed = LocalTime.parse(message, DateTimeFormatter.ofPattern(pattern));
                    break;
                } catch (DateTimeParseException ignored) {
                }
            }
            if (parsed == null) {
                MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAMES_ITEM_COULD_NOT_PARSE_TIME));
                return;
            }
            LocalDateTime dateTime = LocalDateTime.of(date, parsed);
            if (dateTime.isAfter(LocalDateTime.now())) {
                GameManager.getInstance().addUpcomingGame(p.getUniqueId(), preset, dateTime, roleSet);
                GameManager.getInstance().save();
                MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAMES_ITEM_UPCOMING_GAME_ADDED));
            } else {
                MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAMES_ITEM_UPCOMING_GAME_TIME_IS_PAST));
            }
        });
    }

    private void openTemporaryRoleSetEditor(Player p, RoleSet roleSet) {
        // Minimal editor: only role counts and finish button without saving to file.
        renderTempEditor(p, roleSet, "<gray>Tijdelijke RoleSet: " + roleSet.getName());
    }

    private void renderTempEditor(Player p, RoleSet roleSet, String title) {
        MxDefaultMenuBuilder builder = MxDefaultMenuBuilder.create(title, MxInventorySlots.FOUR_ROWS);

        // Represent roles via skull icons available in RoleSet API
        me.mxndarijn.weerwolven.data.Roles[] roles = Arrays.stream(me.mxndarijn.weerwolven.data.Roles.values()).toArray(me.mxndarijn.weerwolven.data.Roles[]::new);
        int[] slots = new int[]{10,11,12,13,14,15,16,
                19,20,21,22,23,24,25};
        for (int i = 0; i < roles.length && i < slots.length; i++) {
            me.mxndarijn.weerwolven.data.Roles r = roles[i];
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
                if (shift) cur = Math.max(0, cur - 1); else cur = cur + 1;
                roleSet.getRoleSet().put(r, cur);
                renderTempEditor((Player) e.getWhoClicked(), roleSet, title);
            });
        }

        // Use button (does not save to RoleSetManager)
        ItemStack use = MxDefaultItemStackBuilder.create(Material.LIME_CONCRETE)
                .setName("<green>Gebruik (niet opslaan)")
                .addLore("<gray>Gebruik deze tijdelijke RoleSet voor de game.")
                .build();
        builder.setItem(use, 31, (inv, e) -> selectPresetNext((Player) e.getWhoClicked(), roleSet));

        // Cancel
        ItemStack cancel = MxDefaultItemStackBuilder.create(Material.BARRIER)
                .setName("<red>Annuleren")
                .build();
        builder.setItem(cancel, 27, (inv, e) -> e.getWhoClicked().closeInventory());

        MxInventoryManager.getInstance().addAndOpenInventory(p, builder.build());
    }

    @Override
    public void executeOnBreak(Player p, BlockBreakEvent e) {
        // no-op
    }
}
