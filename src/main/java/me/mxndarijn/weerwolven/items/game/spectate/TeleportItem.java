package me.mxndarijn.weerwolven.items.game.spectate;

import me.mxndarijn.weerwolven.data.WeerWolvenLanguageText;
import me.mxndarijn.weerwolven.game.Game;
import me.mxndarijn.weerwolven.game.GamePlayer;
import me.mxndarijn.weerwolven.items.WeerWolvenMxItem;
import me.mxndarijn.weerwolven.managers.GameWorldManager;
import nl.mxndarijn.mxlib.inventory.MxInventoryIndex;
import nl.mxndarijn.mxlib.inventory.MxInventoryManager;
import nl.mxndarijn.mxlib.inventory.MxInventorySlots;
import nl.mxndarijn.mxlib.inventory.MxItemClicked;
import nl.mxndarijn.mxlib.inventory.menu.MxListInventoryBuilder;
import nl.mxndarijn.mxlib.item.MxSkullItemStackBuilder;
import nl.mxndarijn.mxlib.item.Pair;
import nl.mxndarijn.mxlib.language.LanguageManager;
import nl.mxndarijn.mxlib.util.MessageUtil;
import nl.mxndarijn.mxlib.util.MxWorldFilter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TeleportItem extends WeerWolvenMxItem {
    public TeleportItem(ItemStack is, MxWorldFilter worldFilter, boolean gameItem, Action... actions) {
        super(is, worldFilter, gameItem, actions);
    }

    @Override
    public void execute(Player p, PlayerInteractEvent e) {
        Optional<Game> optionalGame = GameWorldManager.getInstance().getGameByWorldUID(p.getWorld().getUID());
        if (optionalGame.isPresent()) {
            Game game = optionalGame.get();
            Optional<GamePlayer> optionalGamePlayer = game.getGamePlayerOfPlayer(p.getUniqueId());
            Optional<GamePlayer> oGp = game.getGamePlayerOfPlayer(p.getUniqueId());
            if(oGp.isEmpty()) {
                if (!game.getSpectators().contains(e.getPlayer().getUniqueId())) {
                    return;
                }
            } else {
                if(oGp.get().isAlive()) {
                    return;
                }
            }
            List<Pair<ItemStack, MxItemClicked>> list = new ArrayList<>();
            optionalGame.get().getGamePlayers().forEach(gp -> {
                if (gp.getOptionalPlayerUUID().isPresent() && gp.isAlive()) {
                    Player pl = Bukkit.getPlayer(gp.getOptionalPlayerUUID().get());
                    if (pl != null) {
                        list.add(new Pair<>(MxSkullItemStackBuilder.create(1)
                                .setSkinFromHeadsData(gp.getOptionalPlayerUUID().get().toString())
                                .setName("<gray>" + pl.getName())
                                .addBlankLore()
                                .addLore("<gray>Kleur: " + gp.getColorData().getColor().getDisplayName())
                                .build(),
                                (mxInv, e1) -> {
                                    p.teleport(pl.getLocation());
                                    p.closeInventory();
                                }));
                    }
                }
            });
            if (list.isEmpty()) {
                MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_SPECTATOR_TELEPORT_NO_PLAYERS));
                return;
            }
            MxInventoryManager.getInstance().addAndOpenInventory(p, new MxListInventoryBuilder("<gray>Teleporteer naar speler", MxInventorySlots.THREE_ROWS)
                    .setListItems(list)
                    .setAvailableSlots(MxInventoryIndex.ROW_ONE_TO_THREE)
                    .build());

        }
    }

    @Override
    public void executeOnBreak(Player p, BlockBreakEvent e) {
        e.setCancelled(true);
    }
}
