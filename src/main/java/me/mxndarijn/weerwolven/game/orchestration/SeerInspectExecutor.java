package me.mxndarijn.weerwolven.game.orchestration;

import me.mxndarijn.weerwolven.WeerWolven;
import me.mxndarijn.weerwolven.data.ActionKind;
import me.mxndarijn.weerwolven.data.Timing;
import me.mxndarijn.weerwolven.game.action.ActionIntent;
import me.mxndarijn.weerwolven.game.action.RoleAbilityRegistry;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;
import me.mxndarijn.weerwolven.game.core.InspectRole;
import me.mxndarijn.weerwolven.game.core.SeerRole;
import nl.mxndarijn.mxlib.inventory.MxInventoryIndex;
import nl.mxndarijn.mxlib.inventory.MxInventoryManager;
import nl.mxndarijn.mxlib.inventory.MxInventorySlots;
import nl.mxndarijn.mxlib.inventory.menu.MxListInventoryBuilder;
import nl.mxndarijn.mxlib.logger.Logger;
import nl.mxndarijn.mxlib.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Minimal scaffold for the Seer night action.
 * Currently produces a placeholder intent so the role has an action slot at night.
 * Extend this to open a target selection UI and fill params with e.g. {"target": GamePlayer}.
 */
public final class SeerInspectExecutor extends AbilityExecutor {

    @Override
    public CompletableFuture<List<ActionIntent>> execute(Game game, GamePlayer actor, long timeoutMs) {
        Logger.logMessage("Seer run");
        int initiative = RoleAbilityRegistry.of(actor.getRole()).stream()
                .filter(def -> def.kind() == ActionKind.INSPECT && def.timing() == Timing.NIGHT)
                .map(RoleAbilityRegistry.AbilityDef::initiative)
                .findFirst()
                .orElse(50);

        InspectRole inspectRole = (InspectRole) actor.getRoleData();
        if (actor.getOptionalPlayerUUID().isEmpty())
            return CompletableFuture.completedFuture(defaultExecute(game, actor, timeoutMs));
        Player p = Bukkit.getPlayer(actor.getOptionalPlayerUUID().get());
        if (p == null) return CompletableFuture.completedFuture(defaultExecute(game, actor, timeoutMs));

        CompletableFuture<List<ActionIntent>> future = new CompletableFuture<>();

        MessageUtil.sendMessageToPlayer(p, inspectRole.getMessage());
        MxInventoryManager.getInstance().addAndOpenInventory(p, MxListInventoryBuilder.create(inspectRole.getTitle(), MxInventorySlots.THREE_ROWS)
                .setAvailableSlots(MxInventoryIndex.ROW_ONE_TO_TWO)
                .canBeClosed(false)
                .defaultCancelEvent(true)
                .setListItems(mapGamePlayerToItem(game, actor, false, (GamePlayer::isAlive), (mxInv, e) -> {
                    ItemMeta im = e.getCurrentItem().getItemMeta();
                    PersistentDataContainer container = im.getPersistentDataContainer();
                    String key = container.get(new NamespacedKey(game.getPlugin(), "game_player_uuid"), PersistentDataType.STRING);
                    Optional<GamePlayer> optionalGamePlayer = game.getGamePlayerOfPlayer(UUID.fromString(key));
                    if (optionalGamePlayer.isPresent()) {
                        mxInv.setCanBeClosed(true);
                        p.closeInventory();
                        ActionIntent intent = new ActionIntent(
                                actor,
                                ActionKind.INSPECT,
                                Timing.NIGHT,
                                Map.of("target", optionalGamePlayer.get()),
                                initiative
                        );
                        MessageUtil.sendMessageToPlayer(p, (inspectRole.getCompletedMessage(optionalGamePlayer.get())));
                        future.complete(List.of(intent));
                    }

                }, (gamePlayer -> {
                    List<String> dataList = new ArrayList<>();
                    if (inspectRole.playersSeen.contains(gamePlayer)) {
                        dataList.add("<gray>Rol: " + gamePlayer.getRole().getRoleWithColor());
                    }
                    dataList.add("");
                    dataList.add("<yellow>Klik om de rol van deze speler te bekijken.");
                    return dataList;
                }))).build());
        return future;
    }

    @Override
    public List<ActionIntent> defaultExecute(Game game, GamePlayer actor, long timeoutMs) {
        return List.of();
    }
}
