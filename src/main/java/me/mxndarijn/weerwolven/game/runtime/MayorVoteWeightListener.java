package me.mxndarijn.weerwolven.game.runtime;

import me.mxndarijn.weerwolven.data.WeerWolvenChatPrefix;
import me.mxndarijn.weerwolven.data.WeerWolvenLanguageText;
import me.mxndarijn.weerwolven.game.bus.AutoCloseableGroup;
import me.mxndarijn.weerwolven.game.bus.GameEventBus;
import me.mxndarijn.weerwolven.game.bus.Priority;
import me.mxndarijn.weerwolven.game.bus.events.DayVoteWeightEvent;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;
import me.mxndarijn.weerwolven.game.status.StatusKey;
import nl.mxndarijn.mxlib.language.LanguageManager;
import nl.mxndarijn.mxlib.logger.Logger;
import nl.mxndarijn.mxlib.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Doubles the day vote weight for a voter who has the mayor status.
 */
public final class MayorVoteWeightListener {
    private MayorVoteWeightListener() {}

    public static AutoCloseable subscribe(Game game, GameEventBus bus) {
        var group = new AutoCloseableGroup();
        group.add(bus.subscribe(DayVoteWeightEvent.class, Priority.NORMAL, e -> {
            GamePlayer voter = e.getVoter();
            if (voter == null || !voter.isAlive()) return;
            if (!voter.getStatusStore().has(StatusKey.VOTE_DOUBLE_MAYOR)) return;

            // Double the vote weight
            e.multiply(2);

            // Notify the voter in Dutch via LanguageManager
            voter.getOptionalPlayerUUID().ifPresent(uuid -> {
                var msg = LanguageManager.getInstance().getLanguageString(
                        WeerWolvenLanguageText.MAYOR_VOTE_WEIGHT_DOUBLE,
                        WeerWolvenChatPrefix.VOTE
                );
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    MessageUtil.sendMessageToPlayer(p, msg);
                }
            });
        }));
        return group;
    }
}
