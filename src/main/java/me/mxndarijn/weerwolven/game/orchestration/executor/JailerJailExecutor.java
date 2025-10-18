package me.mxndarijn.weerwolven.game.orchestration.executor;

import me.mxndarijn.weerwolven.data.ActionKind;
import me.mxndarijn.weerwolven.data.Timing;
import me.mxndarijn.weerwolven.game.action.ActionIntent;
import me.mxndarijn.weerwolven.game.action.RoleAbilityRegistry;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;
import me.mxndarijn.weerwolven.game.phase.Phase;
import me.mxndarijn.weerwolven.game.status.FlagStatus;
import me.mxndarijn.weerwolven.game.status.StatusKey;
import me.mxndarijn.weerwolven.data.WeerWolvenLanguageText;
import me.mxndarijn.weerwolven.game.timer.TimerFormats;
import me.mxndarijn.weerwolven.game.timer.TimerScope;
import me.mxndarijn.weerwolven.game.timer.TimerSpec;
import nl.mxndarijn.mxlib.inventory.*;
import nl.mxndarijn.mxlib.inventory.menu.MxListInventoryBuilder;
import nl.mxndarijn.mxlib.item.MxSkullItemStackBuilder;
import nl.mxndarijn.mxlib.item.Pair;
import nl.mxndarijn.mxlib.language.LanguageManager;
import nl.mxndarijn.mxlib.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Jailer night action executor.
 * - Shows a list of alive players (excluding self)
 * - On selection, applies JAILED_TONIGHT (expires at DAWN)
 * - Plays a jail-like sound near the target
 * - Includes a "Do nothing" option
 */
public final class JailerJailExecutor extends AbilityExecutor {

    @Override
    public CompletableFuture<List<ActionIntent>> execute(Game game, GamePlayer actor, long timeoutMs) {
        // Resolve initiative for ordering consistency (not used to produce intents here, but kept for parity)
        int initiative = RoleAbilityRegistry.of(actor.getRole()).stream()
                .filter(def -> def.kind() == ActionKind.JAIL && def.timing() == Timing.NIGHT)
                .map(RoleAbilityRegistry.AbilityDef::initiative)
                .findFirst()
                .orElse(10);

        if (actor.getOptionalPlayerUUID().isEmpty()) {
            return CompletableFuture.completedFuture(defaultExecute(game, actor, timeoutMs));
        }
        Player p = Bukkit.getPlayer(actor.getOptionalPlayerUUID().get());
        if (p == null) return CompletableFuture.completedFuture(defaultExecute(game, actor, timeoutMs));

        CompletableFuture<List<ActionIntent>> future = new CompletableFuture<>();

        // Inform the actors
        MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(
                WeerWolvenLanguageText.GAME_JAILER_SELECT_PLAYER));

        String timerId = "jailer:jail:" + actor.getOptionalPlayerUUID().get() + ":" + game.getDayNumber();

        // Build selectable list of alive targets excluding self
        List<Pair<ItemStack, MxItemClicked>> items = new ArrayList<>();
        items.addAll(AbilityExecutorHelper.mapGamePlayerToItem(game, actor, /*includeItself*/ false, GamePlayer::isAlive, (mxInv, e) -> {
            ItemMeta im = e.getCurrentItem().getItemMeta();
            if (im == null) return;
            PersistentDataContainer container = im.getPersistentDataContainer();
            String key = container.get(new NamespacedKey(game.getPlugin(), "game_player_uuid"), PersistentDataType.STRING);
            if (key == null) return;
            Optional<GamePlayer> optionalGamePlayer = game.getGamePlayerOfPlayer(UUID.fromString(key));
            if (optionalGamePlayer.isPresent()) {
                mxInv.setCanBeClosed(true);
                p.closeInventory();

                game.getActionTimerService().cancel(timerId);

                GamePlayer target = optionalGamePlayer.get();
                List<ActionIntent> intents = handleSelection(game, actor, target, initiative);
                future.complete(intents);
            }
        }, gp -> List.of("", LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_JAILER_LIST_LORE))));

        items.add(new Pair<>(MxSkullItemStackBuilder.create(1)
                .setName(LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_UI_DO_NOTHING_NAME))
                .setSkinFromHeadsData("skip")
                .addBlankLore()
                .addLore(LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_UI_DO_NOTHING_LORE))
                .build(), (mxInv, e) -> {
            mxInv.setCanBeClosed(true);
            p.closeInventory();
            game.getActionTimerService().cancel(timerId);
            future.complete(List.of()); // Do nothing
        }));

        MxInventory mxInventory = MxListInventoryBuilder.create(
                LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_JAILER_INVENTORY_TITLE),
                MxInventorySlots.THREE_ROWS)
                .setAvailableSlots(MxInventoryIndex.ROW_ONE_TO_TWO)
                .canBeClosed(false)
                .defaultCancelEvent(true)
                .setListItems(items)
                .build();

        MxInventoryManager.getInstance().addAndOpenInventory(p, mxInventory);

        // Personal timer; cancel when a choice is made
        var spec = new TimerSpec(
                timerId,
                LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_JAILER_TIMER_TITLE),
                TimerScope.PER_PLAYER,
                java.util.Set.of(actor),
                Math.max(0, timeoutMs - 1000),
                ctx -> ctx.spec().title + " <gray>[" + TimerFormats.mmss(ctx.remainingMs()) + "]",
                ctx -> {
                    // Timeout -> default action (do nothing)
                    if (!future.isDone()) {
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
        // Apply JAILED_TONIGHT to the target; expires at DAWN
        UUID src = actor.getOptionalPlayerUUID().orElse(null);
        target.getStatusStore().add(new FlagStatus(
                StatusKey.JAILED_TONIGHT,
                src,
                game.getDayNumber(),
                Phase.DAWN
        ));

        // Audio feedback near the target and actors
        AbilityExecutorHelper.playSoundInRadiusSmooth(target, Sound.BLOCK_IRON_DOOR_CLOSE, 1.0f, 0.8f, 18);

        // Notify actors and (optionally) the target
        actor.getBukkitPlayer().ifPresent(ap -> MessageUtil.sendMessageToPlayer(ap,
                LanguageManager.getInstance().getLanguageString(
                        WeerWolvenLanguageText.GAME_JAILER_ACTOR_CONFIRMED,
                        List.of(target.getColoredName())
                )));
        target.getBukkitPlayer().ifPresent(tp -> MessageUtil.sendMessageToPlayer(tp,
                LanguageManager.getInstance().getLanguageString(
                        WeerWolvenLanguageText.GAME_JAILER_TARGET_NOTIFICATION
                )));

        ActionIntent intent = new ActionIntent(
                List.of(actor),
                ActionKind.JAIL,
                Timing.NIGHT,
                Map.of("target", target),
                initiative
        );
        return List.of(intent);
    }

    @Override
    public List<ActionIntent> defaultExecute(Game game, GamePlayer actor, long timeoutMs) {
        return List.of();
    }
}
