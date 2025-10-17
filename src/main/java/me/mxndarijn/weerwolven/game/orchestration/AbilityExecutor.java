package me.mxndarijn.weerwolven.game.orchestration;

import me.mxndarijn.weerwolven.game.action.ActionIntent;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;
import nl.mxndarijn.mxlib.inventory.MxItemClicked;
import nl.mxndarijn.mxlib.item.MxSkullItemStackBuilder;
import nl.mxndarijn.mxlib.item.Pair;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

/** executes an action, for example open menu or go outside */
public abstract class AbilityExecutor {
    abstract CompletableFuture<List<ActionIntent>> execute(Game game, GamePlayer actor, long timeoutMs);
    abstract List<ActionIntent> defaultExecute(Game game, GamePlayer actor, long timeoutMs);

    List<Pair<ItemStack, MxItemClicked>> mapGamePlayerToItem(Game game, GamePlayer actor, boolean includeItself, Predicate<GamePlayer> filter, MxItemClicked clicked, Function<GamePlayer, List<String>> getExtraLores) {
        List<Pair<ItemStack, MxItemClicked>> list = new ArrayList<>();
        game.getGamePlayers().forEach(gp -> {
            if(gp == actor && !includeItself) return;
            if(!filter.test(gp)) return;
            if(gp.getOptionalPlayerUUID().isEmpty()) return;
            Player p = Bukkit.getPlayer(gp.getOptionalPlayerUUID().get());
            if(p == null) return;
            list.add(new Pair<>(MxSkullItemStackBuilder
                    .create(1)
                    .setSkinFromHeadsData(gp.getOptionalPlayerUUID().get().toString())
                    .setName(gp.getColoredName())
                    .addCustomTagString("game_player_uuid", gp.getOptionalPlayerUUID().get().toString())
                    .addBlankLore()
                    .addLores(getExtraLores.apply(gp))
                    .build(),
                    clicked
            ));
        });
        return list;
    }
}
