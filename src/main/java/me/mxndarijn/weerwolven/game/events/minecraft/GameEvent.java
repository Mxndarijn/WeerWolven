package me.mxndarijn.weerwolven.game.events.minecraft;

import me.mxndarijn.weerwolven.game.Game;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;


public abstract class GameEvent implements Listener {

    public Game game;
    public JavaPlugin plugin;

    public GameEvent(Game g, JavaPlugin plugin) {
        this.game = g;
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }


    public boolean validateWorld(World w) {
        return game.getOptionalMxWorld().isPresent() && w.getUID().equals(game.getOptionalMxWorld().get().getWorldUID());
    }
}
