package me.mxndarijn.weerwolven.game.core;

import me.mxndarijn.weerwolven.data.WeerWolvenLanguageText;
import nl.mxndarijn.mxlib.language.LanguageManager;

import java.util.List;

public class SeerRole extends InspectRole{
    public SeerRole(GamePlayer gamePlayer) {
        super(gamePlayer);
    }

    @Override
    public String getTitle() {
        return "<gray>Ziener Actie";
    }

    @Override
    public String getMessage() {
        return LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_SEER_SELECT_PLAYER);
    }

    @Override
    public String getCompletedMessage(GamePlayer gp) {
        return LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_SEER_PLAYER_RESULT, List.of(gp.getColoredName(), gp.getRole().getRoleWithColor()));
    }
}
