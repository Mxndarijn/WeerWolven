package me.mxndarijn.weerwolven.items.game.host;

import me.mxndarijn.weerwolven.data.Items;
import me.mxndarijn.weerwolven.data.WeerWolvenChatPrefix;
import me.mxndarijn.weerwolven.data.WeerWolvenLanguageText;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;
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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


public class PlayerManagementItem extends WeerWolvenMxItem {

    public PlayerManagementItem(ItemStack is, MxWorldFilter worldFilter, boolean gameItem, Action... actions) {
        super(is, worldFilter, gameItem, actions);
    }

    @Override
    public void execute(Player p, PlayerInteractEvent e) {
        Optional<Game> optionalGame = GameWorldManager.getInstance().getGameByWorldUID(p.getWorld().getUID());

        if (optionalGame.isEmpty())
            return;
        Game game = optionalGame.get();

        if (!game.getHosts().contains(p.getUniqueId()))
            return;

        ArrayList<Pair<ItemStack, MxItemClicked>> list = new ArrayList<>();
        game.getGamePlayers().forEach(((gamePlayer) -> {
            list.add(new Pair<>(
                    MxSkullItemStackBuilder.create(1)
                            .setSkinFromHeadsData(gamePlayer.getColorData().getColor().getHeadKey())
                            .setName(gamePlayer.getOptionalPlayerUUID().isPresent() && Bukkit.getPlayer(gamePlayer.getOptionalPlayerUUID().get()) != null ? "<gray>" + Bukkit.getPlayer(gamePlayer.getOptionalPlayerUUID().get()).getName() : "<gray>Geen speler toegewezen")
                            .addBlankLore()
                            .addLore("<gray>Kleur: " + gamePlayer.getColorData().getColor().getDisplayName())
                            .addLore("<gray>Rol: " + gamePlayer.getRole().getRoleWithColor())
                            .addBlankLore()
                            .addLore("<yellow>Klik om deze kleur aan te passen.")
                            .build(),
                    (mxInv, e1) -> {
                        MxInventoryManager.getInstance().addAndOpenInventory(p, MxDefaultMenuBuilder.create("<gray>Beheer kleur: " + gamePlayer.getColorData().getColor().getDisplayName(), MxInventorySlots.THREE_ROWS)
//                                .setItem(MxDefaultItemStackBuilder.create(Material.BOOK)
//                                                .setName("<gray>Wijzig rol")
//                                                .addBlankLore()
//                                                .addLore("<gray>Huidig: " + gamePlayer.getRole().getRoleWithColor())
//                                                .addBlankLore()
//                                                .addLore("<yellow>Klik hier om de rol aan te passen.")
//                                                .build(),
//                                        10,
//                                        (mxInv1, e2) -> {
//                                            MxInventoryManager.getInstance().addAndOpenInventory(p, MxDefaultMenuBuilder.create("<gray>Rol aannpassen " + gamePlayer.getColorData().getColor().getDisplayName(), MxInventorySlots.THREE_ROWS)
//                                                    .setPrevious(mxInv)
//                                                    .setItem(getItemForRole(Role.SPELER), 11, getClickForRole(p, mapPlayer, Role.SPELER, mxInv1))
//                                                    .setItem(getItemForRole(Role.MOL), 13, getClickForRole(p, mapPlayer, Role.MOL, mxInv1))
//                                                    .setItem(getItemForRole(Role.EGO), 15, getClickForRole(p, mapPlayer, Role.EGO, mxInv1))
//                                                    .setItem(MxDefaultItemStackBuilder.create(Material.DIAMOND_SWORD)
//                                                            .setName("<gray>Toggle peacekeeper")
//                                                            .addBlankLore()
//                                                            .addLore("<gray>Status: " + (mapPlayer.isPeacekeeper() ? "Is Peacekeeper" : "Is geen Peacekeeper"))
//                                                            .addBlankLore()
//                                                            .addLore("<yellow>Klik hier om peacekeeper te togglen.")
//                                                            .build(), 22, getClickForPeacekeeper(p, mapPlayer, Role.EGO, mxInv1, game.getConfig()))
//                                                    .build());
//
//                                        })
                                .setItem(MxDefaultItemStackBuilder.create(Material.SKELETON_SKULL)
                                                .setName("<gray>Wijzig speler")
                                                .addBlankLore()
                                                .addLore("<gray>Huidig: " + (gamePlayer.getOptionalPlayerUUID().isPresent() ? Bukkit.getPlayer(gamePlayer.getOptionalPlayerUUID().get()).getName() : "Geen speler"))
                                                .addBlankLore()
                                                .addLore("<yellow>Klik hier om de speler aan te passen.")
                                                .build(),
                                        13,
                                        (mxInv1, e2) -> {
                                            List<Pair<ItemStack, MxItemClicked>> players = new ArrayList<>();
                                            AtomicInteger queueAmount = new AtomicInteger(1);
                                            game.getGameInfo().getOrderedQueue().forEach(playerUUID -> {
                                                if (playerUUID == null || Bukkit.getPlayer(playerUUID) == null)
                                                    return;
                                                players.add(new Pair<>(
                                                        MxSkullItemStackBuilder.create(1)
                                                                .setSkinFromHeadsData(playerUUID.toString())
                                                                .setName("<gray>" + Bukkit.getPlayer(playerUUID).getName())
                                                                .addBlankLore()
                                                                .addLore("<gray>Nummer in wachtrij: " + queueAmount)
                                                                .addBlankLore()
                                                                .addLore("<yellow>Klik hier om deze speler te selecteren.")
                                                                .build(),
                                                        (mxInv22, e32) -> {
                                                            game.addPlayer(playerUUID, gamePlayer);
                                                            game.getGameInfo().removePlayerFromQueue(p.getUniqueId());
                                                            p.closeInventory();
                                                        }
                                                ));
                                                queueAmount.getAndIncrement();
                                            });
                                            game.getSpectators().forEach(playerUUID -> {
                                                if (playerUUID == null || Bukkit.getPlayer(playerUUID) == null)
                                                    return;
                                                players.add(new Pair<>(
                                                        MxSkullItemStackBuilder.create(1)
                                                                .setSkinFromHeadsData(playerUUID.toString())
                                                                .setName("<gray>" + Bukkit.getPlayer(playerUUID).getName())
                                                                .addBlankLore()
                                                                .addLore("<gray>Nummer in wachtrij: Spectator")
                                                                .addBlankLore()
                                                                .addLore("<yellow>Klik hier om deze speler te selecteren.")
                                                                .build(),
                                                        (mxInv22, e32) -> {
                                                            game.removeSpectator(playerUUID, false);
                                                            game.addPlayer(playerUUID, gamePlayer);
                                                            p.closeInventory();
                                                        }
                                                ));
                                            });
                                            MxInventoryManager.getInstance().addAndOpenInventory(p, new MxListInventoryBuilder("<gray>Kies speler (" + gamePlayer.getColorData().getColor().getDisplayName() + "<gray>)", MxInventorySlots.FOUR_ROWS)
                                                    .setAvailableSlots(MxInventoryIndex.ROW_ONE_TO_THREE)
                                                    .setPrevious(mxInv1)
                                                    .setListItems(players)
                                                    .setItem(MxDefaultItemStackBuilder.create(Material.NAME_TAG)
                                                                    .setName("<gray>Typ naam")
                                                                    .addBlankLore()
                                                                    .addLore("<yellow>Klik hier om de naam te typen ipv te selecteren.")
                                                                    .build(),
                                                            27,
                                                            (mxInv2, e3) -> {
                                                                p.closeInventory();
                                                                MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_ENTER_NAME, WeerWolvenChatPrefix.DEFAULT));
                                                                MxChatInputManager.getInstance().addChatInputCallback(p.getUniqueId(), message -> {
                                                                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                                                        Player enteredPlayer = Bukkit.getPlayer(message);
                                                                        if (enteredPlayer == null) {
                                                                            MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_ENTER_NAME_NOT_FOUND, WeerWolvenChatPrefix.DEFAULT));
                                                                            return;
                                                                        }
                                                                        if (!game.getGameInfo().getOrderedQueue().contains(enteredPlayer.getUniqueId())) {
                                                                            MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_ENTER_NAME_NOT_IN_QUEUE, WeerWolvenChatPrefix.DEFAULT));
                                                                            return;
                                                                        }
                                                                        game.addPlayer(enteredPlayer.getUniqueId(), gamePlayer);
                                                                    }, 1);
                                                                });

                                                            })
                                                    .setItem(MxDefaultItemStackBuilder.create(Material.SKELETON_SKULL)
                                                            .setName("<gray>Verwijder Speler")
                                                            .addBlankLore()
                                                            .addLore("<yellow>Verander de kleur naar niemand.")
                                                            .build(), 28, (mxInv23, e33) -> {
                                                        if (gamePlayer.getOptionalPlayerUUID().isEmpty())
                                                            return;
                                                        game.removePlayer(gamePlayer.getOptionalPlayerUUID().get());
                                                        p.closeInventory();
                                                        MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_COLOR_CLEARED, Collections.singletonList(gamePlayer.getColorData().getColor().getDisplayName())));
                                                    })
                                                    .build());
                                        })
                                .setItem(MxDefaultItemStackBuilder.create(Material.GOLDEN_SWORD)
                                                .setName("<gray>Kill / Reborn")
                                                .addBlankLore()
                                                .addLore("<gray>Kill of reborn de speler.")
                                                .addBlankLore()
                                                .addLore("<yellow>Klik hier om de speler te killen / rebornen.")
                                                .build(),
                                        8,
                                        (mxInv1, e2) -> {
                                            gamePlayer.setAlive(!gamePlayer.isAlive());
                                            p.closeInventory();
                                            MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_PLAYERSTATE_CHANGED, Arrays.asList(gamePlayer.getColorData().getColor().getDisplayName(), gamePlayer.isAlive() ? "<green>Levend" : "<red>Dood")));
                                            if (gamePlayer.getOptionalPlayerUUID().isEmpty())
                                                return;
                                            Player player = Bukkit.getPlayer(gamePlayer.getOptionalPlayerUUID().get());
                                            if (gamePlayer.isAlive()) {
                                                game.removeSpectator(player.getUniqueId(), false);
                                                player.getInventory().clear();
                                                player.teleport(p);
                                                player.getInventory().addItem(Items.GAME_PLAYER_TOOL.getItemStack());
                                                player.setAllowFlight(false);
                                            } else {
                                                player.setHealth(0);
                                            }
                                            //TODO
                                        })
                                .setItem(MxDefaultItemStackBuilder.create(Material.CHEST)
                                                .setName("<gray>Bekijk inventory")
                                                .addBlankLore()
                                                .addLore("<yellow>Klik hier om de inventory te bekijken.")
                                                .build(),
                                        24,
                                        (mxInv1, e2) -> {
                                            if (gamePlayer.getOptionalPlayerUUID().isEmpty())
                                                return;
                                            Player player = Bukkit.getPlayer(gamePlayer.getOptionalPlayerUUID().get());
                                            if (gamePlayer.isAlive()) {
                                                p.openInventory(player.getInventory());
                                            }
                                        })
                                .setItem(MxDefaultItemStackBuilder.create(Material.ENDER_PEARL)
                                                .setName("<gray>Teleporteer naar je toe")
                                                .addBlankLore()
                                                .addLore("<yellow>Klik hier om de speler naar je te tpen.")
                                                .build(),
                                        25,
                                        (mxInv1, e2) -> {
                                            if (gamePlayer.getOptionalPlayerUUID().isEmpty())
                                                return;
                                            Player player = Bukkit.getPlayer(gamePlayer.getOptionalPlayerUUID().get());
                                            if (gamePlayer.isAlive()) {
                                                player.teleport(p);
                                            }
                                        })
                                .setItem(MxDefaultItemStackBuilder.create(Material.ENDER_PEARL)
                                                .setName("<gray>Teleporteer naar speler")
                                                .addBlankLore()
                                                .addLore("<yellow>Klik hier om naar de speler te teleporten.")
                                                .build(),
                                        26,
                                        (mxInv1, e2) -> {
                                            if (gamePlayer.getOptionalPlayerUUID().isEmpty())
                                                return;
                                            Player player = Bukkit.getPlayer(gamePlayer.getOptionalPlayerUUID().get());
                                            if (gamePlayer.isAlive()) {
                                                p.teleport(player);
                                            }
                                        })
                                .setPrevious(mxInv)
                                .build()
                        );
                    }
            ));
        }));

        MxInventoryManager.getInstance().addAndOpenInventory(p, MxListInventoryBuilder.create("<gray>Spelers beheren", MxInventorySlots.THREE_ROWS)
                .setAvailableSlots(MxInventoryIndex.ROW_ONE_TO_TWO)
                .setListItems(list)
                .setItem(MxSkullItemStackBuilder.create(1)
                                .setSkinFromHeadsData("command-block")
                                .setName("<gray>Automatisch Vullen")
                                .addBlankLore()
                                .addLore("<yellow>Klik hier om de game automatisch")
                                .addLore("<yellow>te vullen met mensen uit de queue.")
                                .build(),
                        26,
                        (mxInv, e12) -> {
                            p.closeInventory();
                            List<GamePlayer> colors = new ArrayList<>(game.getGamePlayers());
                            Collections.shuffle(colors);

                            colors.forEach(gp -> {
                                if (gp.getOptionalPlayerUUID().isPresent())
                                    return;
                                List<UUID> ordered = game.getGameInfo().getOrderedQueue();
                                if (!ordered.isEmpty()) {
                                    game.addPlayer(ordered.getFirst(), gp);
                                }
                            });
                            game.sendMessageToAll(LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_AUTOMATICALLY_FILLED));

                        })
                .setShowPageNumbers(false)
                .build()
        );
    }

    @Override
    public void executeOnBreak(Player p, BlockBreakEvent e) {

    }
}
