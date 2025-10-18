package me.mxndarijn.weerwolven.game.action;

import me.mxndarijn.weerwolven.data.ActionKind;
import me.mxndarijn.weerwolven.data.WeerWolvenChatPrefix;
import me.mxndarijn.weerwolven.data.WeerWolvenLanguageText;
import me.mxndarijn.weerwolven.game.bus.GameEventBus;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;
import me.mxndarijn.weerwolven.game.phase.ResolveMode;
import nl.mxndarijn.mxlib.language.LanguageManager;

import java.util.List;

/** Logs INSPECT results for hosts; no state changes here. */
public final class JailLogHandler implements ActionHandler {

    @Override
    public ActionKind kind() { return ActionKind.JAIL; }

    @Override
    public ResolveMode mode() { return ResolveMode.SERIAL; }

    @Override
    public void resolveSerial(List<ActionIntent> ordered, Game game, GameEventBus bus) {
        for (var intent : ordered) {
            GamePlayer actor = intent.actors().getFirst();
            Object raw = intent.params() == null ? null : intent.params().get("target");
            GamePlayer target = (raw instanceof GamePlayer gp) ? gp : null;

            String actorName = actor == null ? "?" : actor.getColoredName();
            String targetName = target == null ? "?" : target.getColoredName();

            String hostMsg = LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_HOST_LOG_JAIL, List.of(actorName, targetName), WeerWolvenChatPrefix.HOST_LOG);

            game.sendMessageToHosts(hostMsg);
        }
    }

    @Override
    public void resolveAggregated(List<ActionIntent> group, Game game, GameEventBus bus) {
        // Not used; INSPECT is SERIAL per RoleAbilityRegistry.
    }

    private static String safe(GamePlayer gp) {
        return gp == null ? "?" : gp.getColoredName().replaceAll("<[^>]+>", "");
    }
}
