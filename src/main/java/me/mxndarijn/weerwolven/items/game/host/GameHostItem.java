package me.mxndarijn.weerwolven.items.game.host;

import me.mxndarijn.weerwolven.data.UpcomingGameStatus;
import me.mxndarijn.weerwolven.data.WeerWolvenLanguageText;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.items.WeerWolvenMxItem;
import me.mxndarijn.weerwolven.managers.GameWorldManager;
import nl.mxndarijn.mxlib.chatinput.MxChatInputManager;
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
import nl.mxndarijn.mxlib.util.MessageUtil;
import nl.mxndarijn.mxlib.util.MxWorldFilter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
                                .setSkinFromHeadsData("skip-glass")
                                .setName("<gray>Acties Overslaan")
                                .addBlankLore()
                                .addLore("<yellow>Klik hier om acties te beheren")
                                .build(),
                        16,
                        (mxInv, e13) -> {
                            List<Pair<ItemStack, MxItemClicked>> list = new ArrayList<>();
                            game.getActionTimerService().getTimers().forEach(timerSpec -> {
                                list.add(new Pair<>(
                                        MxSkullItemStackBuilder.create(1)
                                                .setSkinFromHeadsData("skip-glass")
                                                .setName(timerSpec.title)
                                                .addBlankLore()
                                                .addLore("<dark_gray>ID: <gray>" + timerSpec.id)
                                                .build(),
                                        (mxInv1, ee) -> {
                                            game.getActionTimerService().forceEnd(timerSpec);
                                            MessageUtil.sendMessageToPlayer(ee.getWhoClicked(), LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_HOST_TOOL_CANCEL_TIMESPEC, List.of(timerSpec.title)));
                                            MxInventoryManager.getInstance().addAndOpenInventory((Player) ee.getWhoClicked(), mxInv);
                                        }
                                ));
                            });
                            MxInventoryManager.getInstance().addAndOpenInventory((Player) e13.getWhoClicked(),MxListInventoryBuilder.create("<gray>Acties", MxInventorySlots.THREE_ROWS)
                                    .defaultCancelEvent(true)
                                    .setPrevious(mxInv)
                                    .setShowPageNumbers(false)
                                    .setAvailableSlots(MxInventoryIndex.ROW_ONE_TO_TWO)
                                    .setListItems(list)
                                    .build());

                        })

                .build());

    }

    @Override
    public void executeOnBreak(Player p, BlockBreakEvent e) {

    }
}
