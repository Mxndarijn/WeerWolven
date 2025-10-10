package me.mxndarijn.weerwolven;

import me.mxndarijn.weerwolven.managers.ScoreBoardManager;
import nl.mxndarijn.mxlib.changeworld.MxChangeWorld;
import nl.mxndarijn.mxlib.mxscoreboard.MxScoreBoard;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ChangeScoreboardOnChangeWorld implements MxChangeWorld {
    private final MxScoreBoard scoreboard;

    public ChangeScoreboardOnChangeWorld(MxScoreBoard scoreBoard) {
        this.scoreboard = scoreBoard;
    }

    @Override
    public void enter(Player p, World w, PlayerChangedWorldEvent e) {
        ScoreBoardManager.getInstance().setPlayerScoreboard(p.getUniqueId(), scoreboard);
    }

    @Override
    public void leave(Player p, World w, PlayerChangedWorldEvent e) {
        ScoreBoardManager.getInstance().removePlayerScoreboard(p.getUniqueId(), scoreboard);
    }

    @Override
    public void quit(Player p, World w, PlayerQuitEvent e) {
        // do nothing
    }
}
