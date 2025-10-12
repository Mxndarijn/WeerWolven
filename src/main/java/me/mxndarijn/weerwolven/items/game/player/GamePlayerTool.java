package me.mxndarijn.weerwolven.items.game.player;

import me.mxndarijn.weerwolven.data.UpcomingGameStatus;
import me.mxndarijn.weerwolven.data.WeerWolvenLanguageText;
import me.mxndarijn.weerwolven.game.Game;
import me.mxndarijn.weerwolven.game.GamePlayer;
import me.mxndarijn.weerwolven.items.WeerWolvenMxItem;
import me.mxndarijn.weerwolven.managers.GameWorldManager;
import nl.mxndarijn.mxlib.chatinput.MxChatInputManager;
import nl.mxndarijn.mxlib.inventory.MxInventoryIndex;
import nl.mxndarijn.mxlib.inventory.MxInventoryManager;
import nl.mxndarijn.mxlib.inventory.MxInventorySlots;
import nl.mxndarijn.mxlib.inventory.MxItemClicked;
import nl.mxndarijn.mxlib.inventory.menu.MxDefaultMenuBuilder;
import nl.mxndarijn.mxlib.inventory.menu.MxListInventoryBuilder;
import nl.mxndarijn.mxlib.item.MxSkullItemStackBuilder;
import nl.mxndarijn.mxlib.item.Pair;
import nl.mxndarijn.mxlib.language.LanguageManager;
import nl.mxndarijn.mxlib.util.MessageUtil;
import nl.mxndarijn.mxlib.util.MxWorldFilter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class GamePlayerTool extends WeerWolvenMxItem {

    public GamePlayerTool(ItemStack is, MxWorldFilter worldFilter, boolean gameItem, Action... actions) {
        super(is, worldFilter, gameItem, actions);
    }

    @Override
    public void execute(Player p, PlayerInteractEvent e) {

        Optional<Game> mapOptional = GameWorldManager.getInstance().getGameByWorldUID(p.getWorld().getUID());

        if (mapOptional.isEmpty()) {
            return;
        }

        Game game = mapOptional.get();
        if(game.getGameInfo().getStatus() != UpcomingGameStatus.PLAYING)
            return;

        Optional<GamePlayer> gp = game.getGamePlayerOfPlayer(p.getUniqueId());
        if (gp.isEmpty())
            return;

        MxInventoryManager.getInstance().addAndOpenInventory(p, MxDefaultMenuBuilder.create("<gray>Speler Tool", MxInventorySlots.THREE_ROWS)
                .setItem(MxSkullItemStackBuilder.create(1)
                                .setName("<gray>Stel een vraag")
                                .addBlankLore()
                                .addLore("<yellow>Klik hier om een vraag te stellen aan een host.")
                                .setSkinFromHeadsData("message-icon")
                                .build(),
                        11,
                        (mxInv, e1) -> {
                            p.closeInventory();
                            MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_ENTER_QUESTION));
                            MxChatInputManager.getInstance().addChatInputCallback(p.getUniqueId(), message -> {
                                game.sendMessageToHosts(LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_MESSAGE_HOST, Arrays.asList(p.getName(), message)));
                                MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_QUESTION_SEND));
                            });
                        })
//                .setItem(MxSkullItemStackBuilder.create(1)
//                                .setName("<gray>Stemmen")
//                                .addBlankLore()
//                                .addLore("<yellow>Klik hier om te stemmen.")
//                                .setSkinFromHeadsData("book")
//                                .build(),
//                        13,
//                        (mxInv, e1) -> {
//                            List<Pair<ItemStack, MxItemClicked>> list = new ArrayList<>();
//                            game.getColors().forEach(gamePlayer -> {
//                                if (gamePlayer.getPlayer().isPresent()) {
//                                    if (gamePlayer.getPlayer().get().equals(p.getUniqueId()))
//                                        return;
//                                    if(!gamePlayer.isAlive())
//                                        return;
//                                    OfflinePlayer pl = Bukkit.getOfflinePlayer(gamePlayer.getPlayer().get());
//                                    list.add(new Pair<>(
//                                            MxSkullItemStackBuilder.create(1)
//                                                    .setSkinFromHeadsData(pl.getUniqueId() + "")
//                                                    .setName("<gray>" + pl.getName())
//                                                    .addLore("<gray>Kleur: " + gamePlayer.getMapPlayer().getColor().getDisplayName())
//                                                    .addBlankLore()
//                                                    .addLore("<yellow>Klik hier om op deze kleur te stemmen.")
//                                                    .build(),
//                                            (mxInv12, e22) -> {
//                                                p.closeInventory();
//                                                if(gp.get().getVotedOn().isPresent() && gp.get().getVotedOn().get() == gamePlayer) {
//                                                    MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_VOTED, Arrays.asList(pl.getName(), gamePlayer.getMapPlayer().getColor().getDisplayName())));
//                                                    return;
//                                                }
//                                                gp.get().setVotedOn(Optional.of(gamePlayer));
//                                                MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_VOTED, Arrays.asList(pl.getName(), gamePlayer.getMapPlayer().getColor().getDisplayName())));
//                                                game.sendMessageToAll(LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_PLAYER_VOTED, Arrays.asList(p.getName(), gp.get().getMapPlayer().getColor().getDisplayName(), game.getTotalVotes() + "", ""+game.getAlivePlayerCount())));
//                                            }
//                                    ));
//                                }
//                            });
//                            MxInventoryManager.getInstance().addAndOpenInventory(p, MxListInventoryBuilder.create("<gray>Stemmen", MxInventorySlots.THREE_ROWS)
//                                    .setListItems(list)
//                                    .setPrevious(mxInv)
//                                    .setAvailableSlots(MxInventoryIndex.ROW_ONE_TO_TWO)
//                                    .setShowPageNumbers(false)
//                                    .setItem(MxSkullItemStackBuilder.create(1)
//                                                    .setSkinFromHeadsData("message-icon")
//                                                    .setName("<gray>Laat resultaten zien")
//                                                    .addBlankLore()
//                                                    .addLore("<yellow>Klik hier om de resultaten aan")
//                                                    .addLore("<yellow>iedereen te laten zien.")
//                                                    .build(),
//                                            18, (mxInv1, e2) -> {
//                                                p.closeInventory();
//                                                if (game.isPlayersCanEndVote()) {
//                                                    game.showVotingResults(p.getName());
//                                                } else {
//                                                    MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_HOST_DISABLED_VOTE));
//                                                }
//                                            }
//                                    )
//                                    .build()
//                            );
//                        })
                .setItem(MxSkullItemStackBuilder.create(1)
                                .setName("<gray>Kleuren")
                                .addBlankLore()
                                .addLore("<yellow>Klik hier om alle kleuren te zien.")
                                .setSkinFromHeadsData("color-block")
                                .build(),
                        15,
                        (mxInv, e1) -> {
                            p.closeInventory();
                            MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_COLOR_INFORMATION));
                            game.getGamePlayers().forEach(gamePlayer -> {
                                String name = gamePlayer.getOptionalPlayerUUID().isPresent() ? Bukkit.getOfflinePlayer(gamePlayer.getOptionalPlayerUUID().get()).getName() : "Niemand";
                                MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_COLOR_INFORMATION_PIECE, Arrays.asList(name, gamePlayer.getColorData().getColor().getDisplayName(), gamePlayer.isAlive() ? "<green>Levend" : "<red>Dood")));
                            });
                        })
                .build());

    }

    @Override
    public void executeOnBreak(Player p, BlockBreakEvent e) {

    }
}
