package me.mxndarijn.weerwolven.items.game.host;

import me.mxndarijn.weerwolven.data.UpcomingGameStatus;
import me.mxndarijn.weerwolven.data.WeerWolvenLanguageText;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.items.WeerWolvenMxItem;
import me.mxndarijn.weerwolven.managers.GameWorldManager;
import nl.mxndarijn.mxlib.chatinput.MxChatInputManager;
import nl.mxndarijn.mxlib.inventory.MxInventoryManager;
import nl.mxndarijn.mxlib.inventory.MxInventorySlots;
import nl.mxndarijn.mxlib.inventory.menu.MxDefaultMenuBuilder;
import nl.mxndarijn.mxlib.item.MxDefaultItemStackBuilder;
import nl.mxndarijn.mxlib.item.MxSkullItemStackBuilder;
import nl.mxndarijn.mxlib.language.LanguageManager;
import nl.mxndarijn.mxlib.util.MessageUtil;
import nl.mxndarijn.mxlib.util.MxWorldFilter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.Optional;

public class GameHostItem extends WeerWolvenMxItem {


    public GameHostItem(ItemStack is, MxWorldFilter worldFilter, boolean gameItem, Action... actions) {
        super(is, worldFilter, gameItem, actions);
    }

    @Override
    public void execute(Player p, PlayerInteractEvent e) {
        Optional<Game> optionalGame = GameWorldManager.getInstance().getGameByWorldUID(e.getPlayer().getWorld().getUID());
        if (optionalGame.isEmpty())
            return;
        Game game = optionalGame.get();
        if (!game.getHosts().contains(e.getPlayer().getUniqueId()))
            return;
        MxInventoryManager.getInstance().addAndOpenInventory(p, new MxDefaultMenuBuilder("<gray>Host-Tool", MxInventorySlots.THREE_ROWS)

                .setItem(MxDefaultItemStackBuilder.create(Material.COMPARATOR)
                                .setName("<gray>Verander Game status")
                                .addBlankLore()
                                .addLore("<yellow>Klik hier om de game status te veranderen.")
                                .build(), 13
                        , (mxInv, e1) -> {
                            MxInventoryManager.getInstance().addAndOpenInventory(p, new MxDefaultMenuBuilder("<gray>Verander Game Status", MxInventorySlots.THREE_ROWS)
                                    .setPrevious(mxInv)
                                    .setItem(MxSkullItemStackBuilder.create(1)
                                                    .setSkinFromHeadsData("ice-block")
                                                    .setName(UpcomingGameStatus.FREEZE.getStatus())
                                                    .addBlankLore()
                                                    .addLore("<yellow>Klik hier om de status te veranderen naar: " + UpcomingGameStatus.FREEZE.getStatus())
                                                    .build(),
                                            13,
                                            (mxInv1, e2) -> {
                                                game.setGameStatus(UpcomingGameStatus.FREEZE);
                                                p.closeInventory();
                                            })
                                    .setItem(MxSkullItemStackBuilder.create(1)
                                                    .setSkinFromHeadsData("red-block")
                                                    .setName(UpcomingGameStatus.FINISHED.getStatus())
                                                    .addBlankLore()
                                                    .addLore("<gray>Hierbij worden geen statistics aangepast!")
                                                    .addLore("<yellow>Klik hier om de status te veranderen naar: " + UpcomingGameStatus.FINISHED.getStatus())
                                                    .build(),
                                            16,
                                            (mxInv1, e2) -> {
                                                game.setGameStatus(UpcomingGameStatus.FINISHED);
                                                p.closeInventory();
                                            })
                                    .setItem(MxSkullItemStackBuilder.create(1)
                                                    .setSkinFromHeadsData("light-green-block")
                                                    .setName(UpcomingGameStatus.PLAYING.getStatus())
                                                    .addBlankLore()
                                                    .addLore("<yellow>Klik hier om de status te veranderen naar: " + UpcomingGameStatus.PLAYING.getStatus())
                                                    .build(),
                                            10,
                                            (mxInv1, e2) -> {
                                                game.setGameStatus(UpcomingGameStatus.PLAYING);
                                                p.closeInventory();
                                            })
                                    .build());
                        })
                .setItem(MxSkullItemStackBuilder.create(1)
                                .setSkinFromHeadsData("wooden-plus")
                                .setName("<gray>Voeg host toe")
                                .addBlankLore()
                                .addLore("<yellow>Klik hier om een host toe te voegen.")
                                .build(),
                        10,
                        (mxInv, e12) -> {
                            MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_HOST_ADD_ENTER_NAME));
                            p.closeInventory();
                            MxChatInputManager.getInstance().addChatInputCallback(p.getUniqueId(), message -> {
                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    Player player = Bukkit.getPlayer(message);
                                    if (player == null) {
                                        MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_HOST_ADD_NOT_FOUND));
                                        return;
                                    }
                                    if (game.getHosts().contains(player.getUniqueId())) {
                                        MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_HOST_ADD_ALREADY_HOST));
                                        return;
                                    }
                                    if (game.getSpectators().contains(player.getUniqueId()) || game.getGameInfo().getOrderedQueue().contains(player.getUniqueId())) {
                                        if (game.getSpectators().contains(player.getUniqueId())) {
                                            game.removeSpectator(player.getUniqueId(), false);
                                        }
                                        MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_HOST_ADD_HOST_ADDED, Collections.singletonList(player.getName())));
                                        game.addHost(player.getUniqueId());
                                    } else {
                                        MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_HOST_ADD_NOT_IN_QUEUE, Collections.singletonList(player.getName())));
                                    }
                                });
                            });
                        })
                .setItem(MxSkullItemStackBuilder.create(1)
                                .setSkinFromHeadsData("book")
                                .setName("<gray>Votes Beheren")
                                .addBlankLore()
                                .addLore("<yellow>Klik hier om votes te beheren")
                                .build(),
                        16,
                        (mxInv, e13) -> {
                            return;
                        })
//                            MxInventoryManager.getInstance().addAndOpenInventory(p,
//                                    MxDefaultMenuBuilder.create("<gray>Beheer Votes", MxInventorySlots.THREE_ROWS)
//                                            .setPrevious(mxInv)
//                                            .setItem(MxSkullItemStackBuilder.create(1)
//                                                            .setSkinFromHeadsData("book")
//                                                            .setName("<gray>Toggle Votes")
//                                                            .addBlankLore()
//                                                            .addLore("<gray>Status: " + (game.isPlayersCanEndVote() ? "<green>Spelers kunnen de votes beeindigen" : "<red>Spelers kunnen niet de votes beeindigen"))
//                                                            .addBlankLore()
//                                                            .addLore("<yellow>Klik hier om de status te togglen")
//                                                            .build(),
//                                                    13,
//                                                    (mxInv12, e14) -> {
//                                                        game.setPlayersCanEndVote(!game.isPlayersCanEndVote());
//                                                        p.closeInventory();
//                                                        MessageUtil.sendMessageToPlayer(p, (game.isPlayersCanEndVote() ? "<green>Spelers kunnen de votes beeindigen." : "<red>Spelers kunnen niet de votes beeindigen."));
//                                                    }
//                                            )
//                                            .setItem(MxSkullItemStackBuilder.create(1)
//                                                            .setSkinFromHeadsData("anonymous")
//                                                            .setName("<gray>Stem-anoniemheid " + (game.areVotesAnonymous() ? "<red>Uitschakelen" : "<green>Inschakelen"))
//                                                            .addBlankLore()
//                                                            .addLore("<gray>Status: " + (game.areVotesAnonymous() ? "<green>Anoniem" : "<red>Publiek") + "<gray>.")
//                                                            .addBlankLore()
//                                                            .addLore("<yellow>Klik hier om de status te togglen.")
//                                                            .build(),
//                                                    10,
//                                                    (mxInv12, e14) -> {
//                                                        game.setVotesAnonymous(!game.areVotesAnonymous());
//                                                        p.closeInventory();
//                                                        game.sendMessageToAll("<gray>Stemmen zijn nu " + (game.areVotesAnonymous() ? "<green>Anoniem<gray>." : "<red>Publiek<gray>."));
//                                                    }
//                                            )
//                                            .setItem(MxSkullItemStackBuilder.create(1)
//                                                            .setSkinFromHeadsData("message-icon")
//                                                            .setName("<gray>Laat resultaten zien.")
//                                                            .addBlankLore()
//                                                            .addLore("<yellow>Klik hier om de vote resultaten te bekijken.")
//                                                            .build(),
//                                                    16,
//                                                    (mxInv12, e14) -> {
//                                                        game.showVotingResults("Host");
//                                                        p.closeInventory();
//                                                    }
//                                            )
//                                            .build()
//
//                            );
//                        }
//                )
                .build());

    }

    @Override
    public void executeOnBreak(Player p, BlockBreakEvent e) {

    }
}
