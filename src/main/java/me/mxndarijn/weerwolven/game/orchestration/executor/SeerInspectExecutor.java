package me.mxndarijn.weerwolven.game.orchestration.executor;

import me.mxndarijn.weerwolven.data.ActionKind;
import me.mxndarijn.weerwolven.data.Timing;
import me.mxndarijn.weerwolven.data.WeerWolvenLanguageText;
import me.mxndarijn.weerwolven.game.action.ActionIntent;
import me.mxndarijn.weerwolven.game.action.RoleAbilityRegistry;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;
import me.mxndarijn.weerwolven.game.core.InspectRole;
import me.mxndarijn.weerwolven.game.timer.TimerFormats;
import me.mxndarijn.weerwolven.game.timer.TimerScope;
import me.mxndarijn.weerwolven.game.timer.TimerSpec;
import nl.mxndarijn.mxlib.inventory.MxInventory;
import nl.mxndarijn.mxlib.inventory.MxInventoryIndex;
import nl.mxndarijn.mxlib.inventory.MxInventoryManager;
import nl.mxndarijn.mxlib.inventory.MxInventorySlots;
import nl.mxndarijn.mxlib.inventory.menu.MxListInventoryBuilder;
import nl.mxndarijn.mxlib.language.LanguageManager;
import nl.mxndarijn.mxlib.logger.Logger;
import nl.mxndarijn.mxlib.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

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

        String timerId = "seer:inspect:" + actor.getOptionalPlayerUUID().get() + ":" + game.getDayNumber();

        MxInventory mxInventory = MxListInventoryBuilder.create(inspectRole.getTitle(), MxInventorySlots.THREE_ROWS)
                .setAvailableSlots(MxInventoryIndex.ROW_ONE_TO_TWO)
                .canBeClosed(false)
                .defaultCancelEvent(true)
                .setListItems(AbilityExecutorHelper.mapGamePlayerToItem(game, actor, false, (GamePlayer::isAlive), (mxInv, e) -> {
                    ItemMeta im = e.getCurrentItem().getItemMeta();
                    PersistentDataContainer container = im.getPersistentDataContainer();
                    String key = container.get(new NamespacedKey(game.getPlugin(), "game_player_uuid"), PersistentDataType.STRING);
                    Optional<GamePlayer> optionalGamePlayer = game.getGamePlayerOfPlayer(UUID.fromString(key));
                    if (optionalGamePlayer.isPresent()) {
                        mxInv.setCanBeClosed(true);
                        p.closeInventory();

                        game.getActionTimerService().cancel(timerId);

                        GamePlayer target = optionalGamePlayer.get();
                        List<ActionIntent> intents = handleSelection(game, actor, target, initiative);
                        future.complete(intents);
                    }

                }, (gamePlayer -> {
                    List<String> dataList = new ArrayList<>();
                    if (inspectRole.playersSeen.contains(gamePlayer)) {
                        dataList.add("<gray>Rol: " + gamePlayer.getRole().getRoleWithColor());
                    }
                    dataList.add("");
                    dataList.add("<yellow>Klik om de rol van deze speler te bekijken.");
                    return dataList;
                }))).build();

        MxInventoryManager.getInstance().addAndOpenInventory(p, mxInventory);

        // Start a 45s personal action-bar timer; cancel it when player makes a choice
        var spec = new TimerSpec(
                timerId,
                "<blue>Ziener",
                TimerScope.PER_PLAYER,
                java.util.Set.of(actor),
                timeoutMs - 1000,
                ctx -> ctx.spec().title + " <gray>[" + TimerFormats.mmss(ctx.remainingMs()) + "]",
                ctx -> {
                    // Timeout -> default action: inform player and complete future if not yet completed
                    if (!future.isDone()) {
                        // Close menu on main thread
                        Bukkit.getScheduler().runTask(game.getPlugin(), () -> {
                            try {
                                mxInventory.setCanBeClosed(true);
                                p.closeInventory();
                            } catch (Exception ignored) {}
                        });
                        var res = defaultExecute(game, actor, timeoutMs);
                        future.complete(res);
                    }
                },
                null,
                null
        );
        game.getActionTimerService().addTimer(spec);
        return future;
    }

    private List<ActionIntent> handleSelection(Game game, GamePlayer actor, GamePlayer target, int initiative) {
        InspectRole inspectRole = (InspectRole) actor.getRoleData();
        ActionIntent intent = new ActionIntent(
                List.of(actor),
                ActionKind.INSPECT,
                Timing.NIGHT,
                Map.of("target", target),
                initiative
        );
        if (!inspectRole.playersSeen.contains(target)) {
            inspectRole.playersSeen.add(target);
        }
        // Audio feedback near the target
        AbilityExecutorHelper.playSoundInRadiusSmooth(target, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.2f, 1.15f, 25);
        // Notify the actors if online
        actor.getOptionalPlayerUUID().ifPresent(uuid -> {
            Player ap = Bukkit.getPlayer(uuid);
            if (ap != null) {
                MessageUtil.sendMessageToPlayer(ap, inspectRole.getCompletedMessage(target));
            }
        });
        return List.of(intent);
    }

    @Override
    public List<ActionIntent> defaultExecute(Game game, GamePlayer actor, long timeoutMs) {
        // Ensure a default action always happens: pick a random target
        int initiative = RoleAbilityRegistry.of(actor.getRole()).stream()
                .filter(def -> def.kind() == ActionKind.INSPECT && def.timing() == Timing.NIGHT)
                .map(RoleAbilityRegistry.AbilityDef::initiative)
                .findFirst()
                .orElse(50);

        // Candidates: alive players excluding the actors
        List<GamePlayer> candidates = new ArrayList<>();
        for (GamePlayer gp : game.getGamePlayers()) {
            if (gp != actor && gp.isAlive()) {
                candidates.add(gp);
            }
        }
        // Fallback to any other player if no alive candidate (edge case)
        if (candidates.isEmpty()) {
            for (GamePlayer gp : game.getGamePlayers()) {
                if (gp != actor) {
                    candidates.add(gp);
                }
            }
        }
        if (candidates.isEmpty()) {
            // Only actors in game; nothing we can do
            return List.of();
        }
        GamePlayer target = candidates.get(new Random().nextInt(candidates.size()));

        // Host log and subtle cue that default was executed
        game.sendMessageToHosts(LanguageManager.getInstance().getLanguageString(
                WeerWolvenLanguageText.GAME_HOST_LOG_EXECUTING_DEFAULT_TIMEOUT,
                List.of(actor.getColoredName(), actor.getRole().getRoleWithColor())));
        AbilityExecutorHelper.playSoundInRadiusSmooth(actor, Sound.BLOCK_NOTE_BLOCK_BASS, 0.9f, 0.7f, 16);

        return handleSelection(game, actor, target, initiative);
    }
}
