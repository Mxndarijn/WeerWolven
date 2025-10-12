package me.mxndarijn.weerwolven.managers;

import de.myzelyam.api.vanish.VanishAPI;
import me.mxndarijn.weerwolven.WeerWolven;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class VanishManager implements Listener {

    private static VanishManager instance;


    private final Plugin plugin;

    public VanishManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public static VanishManager getInstance() {
        if (instance == null)
            instance = new VanishManager(JavaPlugin.getPlugin(WeerWolven.class));
        return instance;
    }

    private void showPlayer(Player player) {
        VanishAPI.showPlayer(player);
    }



    public void toggleVanish(Player p) {
        if (VanishAPI.isInvisible(p)) {
            showPlayer(p);
        } else {
            hidePlayerForAll(p);
        }
    }

    public boolean isPlayerHidden(Player p) {
        return VanishAPI.isInvisible(p);
    }

    public void hidePlayerForAll(Player p) {
        VanishAPI.hidePlayer(p);
    }

    public void showPlayerForAll(Player p) {
        showPlayer(p);
    }
}
