package me.mxndarijn.weerwolven.game;

import lombok.Getter;
import me.mxndarijn.weerwolven.data.Roles;
import me.mxndarijn.weerwolven.game.role.EmptyRoleData;
import me.mxndarijn.weerwolven.game.role.RoleData;
import me.mxndarijn.weerwolven.game.role.RoleDataFactory;
import me.mxndarijn.weerwolven.presets.ColorData;
import nl.mxndarijn.mxlib.mxscoreboard.MxSupplierScoreBoard;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

@Getter
public class GamePlayer {

    private RoleData roleData = new EmptyRoleData();
    private Roles role = Roles.UNKNOWN;
    private final ColorData color;

    @Nullable
    private UUID playerUUID;

    @Nullable
    private GamePlayer votedOn;
    private boolean alive;
    private JavaPlugin plugin;
    
    private MxSupplierScoreBoard scoreboard;

    private final StatusStore statusStore = new StatusStore();

    public GamePlayer(ColorData color, JavaPlugin plugin) {
        this.color = color;
        this.plugin = plugin;
        this.alive = true;
    }
    

    public void setRole(Roles role) {
        this.role = role;
        this.roleData = RoleDataFactory.createRoleData(role, this);
    }
    
    public void setPlayerUUID(UUID playerUUID) {
        this.playerUUID = playerUUID;
        //TODO more
    }
    

}
