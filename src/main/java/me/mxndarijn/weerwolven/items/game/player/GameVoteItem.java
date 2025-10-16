package me.mxndarijn.weerwolven.items.game.player;

import me.mxndarijn.weerwolven.data.WeerWolvenChatPrefix;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;
import me.mxndarijn.weerwolven.game.manager.GameVoteManager;
import me.mxndarijn.weerwolven.items.WeerWolvenMxItem;
import me.mxndarijn.weerwolven.managers.GameWorldManager;
import me.mxndarijn.weerwolven.data.WeerWolvenLanguageText;
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
import nl.mxndarijn.mxlib.logger.Logger;
import nl.mxndarijn.mxlib.util.MessageUtil;
import nl.mxndarijn.mxlib.util.MxWorldFilter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameVoteItem extends WeerWolvenMxItem {
    public GameVoteItem(ItemStack is, MxWorldFilter worldFilter, boolean gameItem, Action... actions) {
        super(is, worldFilter, gameItem, actions);
    }

    // Simple per-player vote session cache
    private static final Map<UUID, VoteSession> SESSIONS = new ConcurrentHashMap<>();

    public static VoteSession getSession(UUID uuid) { return SESSIONS.get(uuid); }
    public static void clearSession(UUID uuid) { SESSIONS.remove(uuid); }

    @Override
    public void execute(Player p, PlayerInteractEvent e) {
        Optional<Game> gameOpt = GameWorldManager.getInstance().getGameOfPlayer(p.getUniqueId());
        if (gameOpt.isEmpty()) {
            MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.VOTE_ERR_NOT_IN_GAME));
            return;
        }
        Game game = gameOpt.get();
        Optional<GamePlayer> gpOpt = game.getGamePlayerOfPlayer(p.getUniqueId());
        if (gpOpt.isEmpty()) {
            MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.VOTE_ERR_ONLY_PLAYERS));
            return;
        }
        GamePlayer gp = gpOpt.get();
        if (!gp.isAlive()) {
            MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.VOTE_ERR_DEAD_CANNOT_VOTE));
            return;
        }

        GameVoteManager vm = game.getGameVoteManager();
        var statusOpt = vm.getCurrentStatus();
        if (statusOpt.isEmpty()) {
            MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.VOTE_ERR_NO_ROUND));
            return;
        }
        var status = statusOpt.get();
        if (status.mode == GameVoteManager.VoteMode.DISABLED) {
            MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.VOTE_ERR_DISABLED));
            return;
        }
        if (!vm.canVote(gp)) {
            MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.VOTE_ERR_ALREADY_VOTED));
            return;
        }
        VoteSession session = new VoteSession(status);
        SESSIONS.put(p.getUniqueId(), session);
        openVoteMenu(p, game, gp, session);
    }

    private static void openVoteMenu(Player p, Game game, GamePlayer voter, VoteSession session) {
        String title = session.getTitleKey();
        if (session.mode == GameVoteManager.VoteMode.YES_NO) {
            openYesNoMenu(p, game, voter, session, title);
        } else {
            openNominationMenu(p, game, voter, session, title);
        }
    }

    private static void openNominationMenu(Player p, Game game, GamePlayer voter, VoteSession session, String title) {
        List<Pair<ItemStack, MxItemClicked>> list = new ArrayList<>();
        for (int i = 0; i < session.targets.size(); i++) {
            GamePlayer target = session.targets.get(i);
            if(target.getOptionalPlayerUUID().isEmpty())
                continue;
            if (!session.allowSelfVote && target == voter) continue;
            String playerName = target.getOptionalPlayerUUID().map(uuid -> {
                OfflinePlayer off = Bukkit.getOfflinePlayer(uuid);
                return off.getName() != null ? off.getName() : "Unknown";
            }).orElse("Unknown");
            boolean selected = (session.selectedIndex != null && session.selectedIndex == i);
            String name = (selected ? "[X] " : "[ ] ") + target.getColorData().getColor().getDisplayName() + " <gray>" + playerName;
            ItemStack colorItem = MxSkullItemStackBuilder.create(1)
                    .setSkinFromHeadsData(target.getColorData().getColor().getHeadKey())
                    .setName("<gray>" + name)
                    .addBlankLore()
                    .addLore(LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.VOTE_UI_CLICK_SELECT))
                    .build();
            int idx = i;
            list.add(new Pair<>(colorItem, (mxInv, click) -> {
                session.select(idx);
                Logger.logMessage("<gray>Selected nominee: " + target.getColorData().getColor().getDisplayName() + " idx: " + idx);
                openNominationMenu(p, game, voter, session, title);
            }));
        }
        if (session.allowSkip) {
            boolean selected = (session.selectedIndex != null && session.selectedIndex == -1);
            ItemStack skip = MxDefaultItemStackBuilder.create(Material.PAPER)
                    .setName("<gray>" + (selected ? "[X] " : "[ ] ") + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.VOTE_UI_SKIP_NAME))
                    .addBlankLore()
                    .addLore(LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.VOTE_UI_CLICK_SKIP))
                    .build();
            list.add(new Pair<>(skip, (mxInv, click) -> {
                session.select(-1);
                Logger.logMessage("<gray>Skipped nomination");
                openNominationMenu(p, game, voter, session, title);
            }));
        }

        MxListInventoryBuilder builder = MxListInventoryBuilder.create("<gray>" + title, MxInventorySlots.THREE_ROWS)
                .setAvailableSlots(MxInventoryIndex.ROW_ONE_TO_TWO)
                .setListItems(list)
                .setItem(MxDefaultItemStackBuilder.create(Material.LIME_DYE)
                                .setName(LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.VOTE_UI_CONFIRM_NAME))
                                .addBlankLore()
                                .addLore(LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.VOTE_UI_CLICK_CONFIRM))
                                .build(),
                        26, (mxInv, click) -> confirmVote(p, game, voter, session));

        MxInventoryManager.getInstance().addAndOpenInventory(p, builder.build());
    }

    private static void openYesNoMenu(Player p, Game game, GamePlayer voter, VoteSession session, String title) {
        MxDefaultMenuBuilder builder = MxDefaultMenuBuilder.create("<gray>" + title, MxInventorySlots.THREE_ROWS);
        Optional<GameVoteManager.CurrentStatus> optionalCurrentStatus = game.getGameVoteManager().getCurrentStatus();
        if (optionalCurrentStatus.isEmpty()) {
            throw new IllegalStateException("Current status is empty");
        }
        GameVoteManager.CurrentStatus currentStatus = optionalCurrentStatus.get();
        if(currentStatus.currentNominee == null) {
            throw new IllegalStateException("Current nominee is null");
        }
        // Yes
        boolean yesSel = session.selectedYesNo != null && session.selectedYesNo;
        builder.setItem(MxDefaultItemStackBuilder.create(yesSel ? Material.LIME_WOOL : Material.GREEN_WOOL)
                        .setName((yesSel ? "[X] " : "[ ] ") + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.VOTE_UI_YES_NAME))
                        .addBlankLore()
                        .addLore(LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.VOTE_UI_CLICK_YES))
                        .build(),
                11,
                (mxInv, click) -> {
                    session.select(0);
                    openYesNoMenu(p, game, voter, session, title);
                });

        // No
        boolean noSel = session.selectedYesNo != null && !session.selectedYesNo;
        builder.setItem(MxDefaultItemStackBuilder.create(noSel ? Material.RED_WOOL : Material.PINK_WOOL)
                        .setName((noSel ? "[X] " : "[ ] ") + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.VOTE_UI_NO_NAME))
                        .addBlankLore()
                        .addLore(LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.VOTE_UI_CLICK_NO))
                        .build(),
                15,
                (mxInv, click) -> {
                    session.select(1);
                    openYesNoMenu(p, game, voter, session, title);
                });
        builder.setItem(MxSkullItemStackBuilder.create(1)
                        .setName(currentStatus.currentNominee.getColorData().getColor().getDisplayName() + " <gray>" + currentStatus.currentNominee.getOptionalPlayerUUID().map(uuid -> {
                            OfflinePlayer off = Bukkit.getOfflinePlayer(uuid);
                            return off.getName() != null ? off.getName() : "Unknown";
                        }).orElse("Unknown"))
                        .setSkinFromHeadsData(currentStatus.currentNominee.getColorData().getColor().getHeadKey())
                        .addBlankLore()
                        .build(),
                13,
                (mxInv, click) -> {
                });

        // Confirm button
        builder.setItem(MxDefaultItemStackBuilder.create(Material.LIME_DYE)
                        .setName(LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.VOTE_UI_CONFIRM_NAME))
                        .addBlankLore()
                        .addLore(LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.VOTE_UI_CLICK_CONFIRM))
                        .build(),
                26, (mxInv, click) -> confirmVote(p, game, voter, session));

        MxInventoryManager.getInstance().addAndOpenInventory(p, builder.build());
    }

    private static void confirmVote(Player p, Game game, GamePlayer voter, VoteSession session) {
        Logger.logMessage("<gray>Confirming vote");
        GameVoteManager vm = game.getGameVoteManager();
        var statusOpt = vm.getCurrentStatus();
        if (statusOpt.isEmpty()) {
            MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.VOTE_ERR_EXPIRED, WeerWolvenChatPrefix.VOTE));
            clearSession(p.getUniqueId());
            p.closeInventory();
            return;
        }
        var status = statusOpt.get();
        if (!vm.canVote(voter)) {
            MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.VOTE_ERR_ALREADY_VOTED, WeerWolvenChatPrefix.VOTE));
            clearSession(p.getUniqueId());
            p.closeInventory();
            return;
        }

        boolean ok = false;
        switch (session.mode) {
            case NOMINATION, INSTANT -> {
                Logger.logMessage("<gray>Selected nominee: " + session.selectedIndex);
                GamePlayer nominee = session.getSelectedNominee(status);
                Logger.logMessage("<gray>Selected nominee: " + nominee);
                if (nominee == null && !status.allowSkip) {
                    MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.VOTE_ERR_CHOOSE_FIRST, WeerWolvenChatPrefix.VOTE));
                    return;
                }
                ok = vm.submitNomination(voter, nominee);
            }
            case YES_NO -> {
                Boolean choice = session.getSelectedYesNo();
                if (choice == null && !status.allowSkip) {
                    MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.VOTE_ERR_CHOOSE_FIRST, WeerWolvenChatPrefix.VOTE));
                    return;
                }
                ok = vm.submitYesNo(voter, choice);
            }
            default -> {}
        }

        if (!ok) {
            MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.VOTE_ERR_SUBMIT_FAILED, WeerWolvenChatPrefix.VOTE));
        }
        clearSession(p.getUniqueId());
        p.closeInventory();
    }

    @Override
    public void executeOnBreak(Player p, BlockBreakEvent e) {
        // nothing
    }

    public static class VoteSession {
        private final GameVoteManager.VoteMode mode;
        private final List<GamePlayer> targets;
        private final boolean allowSkip;
        private final String titleKey;
        private final boolean allowSelfVote;
        private Integer selectedIndex; // for nomination/instant; -1 means skip
        private Boolean selectedYesNo; // for yes/no

        public VoteSession(GameVoteManager.CurrentStatus status) {
            this.mode = status.mode;
            this.targets = new ArrayList<>(status.voteTargets);
            this.allowSkip = status.allowSkip;
            this.titleKey = status.titleKey;
            this.allowSelfVote = status.allowSelfVote;
            this.selectedIndex = null;
            this.selectedYesNo = null;
        }

        public void select(int index) {
            if (mode == GameVoteManager.VoteMode.YES_NO) {
                if (index == 0) selectedYesNo = true;
                else if (index == 1) selectedYesNo = false;
                else if (allowSkip && index == -1) selectedYesNo = null;
            } else {
                if (index == -1 && allowSkip) {
                    selectedIndex = -1;
                } else if (index >= 0 && index < targets.size()) {
                    selectedIndex = index;
                }
            }
        }

        public GamePlayer getSelectedNominee(GameVoteManager.CurrentStatus status) {
            if (mode == GameVoteManager.VoteMode.YES_NO) return null;
            if (selectedIndex == null) return null;
            if (selectedIndex == -1) return null; // skip
            if (selectedIndex >= 0 && selectedIndex < targets.size()) return targets.get(selectedIndex);
            return null;
        }

        public Boolean getSelectedYesNo() { return selectedYesNo; }

        public String getTitleKey() { return titleKey; }
    }
}
