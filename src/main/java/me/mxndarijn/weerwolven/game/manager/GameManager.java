package me.mxndarijn.weerwolven.game.manager;


import me.mxndarijn.weerwolven.game.core.Game;
import org.bukkit.event.Listener;

public abstract class GameManager implements Listener {

    public final Game game;

    public GameManager(Game game) {
        this.game = game;
        game.getPlugin().getServer().getPluginManager().registerEvents(this, game.getPlugin());
    }
}
