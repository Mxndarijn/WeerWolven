package me.mxndarijn.weerwolven.game.core;

import lombok.Getter;
import lombok.Setter;
import me.clip.placeholderapi.PlaceholderAPI;
import me.mxndarijn.weerwolven.data.Roles;
import me.mxndarijn.weerwolven.data.ScoreBoard;
import me.mxndarijn.weerwolven.game.role.EmptyRoleData;
import me.mxndarijn.weerwolven.game.role.RoleData;
import me.mxndarijn.weerwolven.game.role.RoleDataFactory;
import me.mxndarijn.weerwolven.game.status.StatusStore;
import me.mxndarijn.weerwolven.presets.ColorData;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.TitlePart;
import nl.mxndarijn.mxlib.mxscoreboard.MxSupplierScoreBoard;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

@Getter
public class GamePlayer {

    private RoleData roleData = new EmptyRoleData(this);
    private Roles role = Roles.UNKNOWN;
    private final ColorData colorData;

    //TODO more
    @Setter
    @Nullable
    private UUID playerUUID;

    @Nullable
    private GamePlayer votedOn;

    @Setter
    private boolean alive;
    private final JavaPlugin plugin;
    private Game game;
    
    private final MxSupplierScoreBoard scoreboard;

    private final StatusStore statusStore = new StatusStore();

    public GamePlayer(ColorData color, JavaPlugin plugin, Game game) {
        this.colorData = color;
        this.plugin = plugin;
        this.alive = true;
        this.game = game;

        String host = Bukkit.getOfflinePlayer(game.getMainHost()).getName();
        scoreboard = new MxSupplierScoreBoard(plugin, () -> {
            if(getOptionalPlayerUUID().isPresent()) {
                Player p = Bukkit.getPlayer(getOptionalPlayerUUID().get());
                if (p != null) {
                    return PlaceholderAPI.setPlaceholders(p, ScoreBoard.GAME_PLAYER.getTitle(new HashMap<>() {{
                        put("%%map_name%%", game.getConfig().getName());
                    }}));
                }
            }
            return ScoreBoard.GAME_PLAYER.getTitle(new HashMap<>() {{
                put("%%map_name%%", game.getConfig().getName());
            }});
        }, () -> {
            if(getOptionalPlayerUUID().isPresent()) {
                Player p = Bukkit.getPlayer(getOptionalPlayerUUID().get());
                if (p != null) {
                    return PlaceholderAPI.setPlaceholders(p, ScoreBoard.GAME_PLAYER.getLines(new HashMap<>() {{
                        put("%%game_status%%", game.getGameInfo().getStatus().getStatus());
                        put("%%game_time%%", game.getFormattedGameTime());
                        put("%%color%%", getColorData().getColor().getDisplayName());
                        put("%%host%%", host);
                        put("%%role%%", role.getRoleWithColor());
                        put("%%phase%%", game.getPhase().getColoredPhase(game.getDayNumber()));
                    }}));
                }
            }
            return ScoreBoard.GAME_PLAYER.getLines(new HashMap<>() {{
                put("%%game_status%%", game.getGameInfo().getStatus().getStatus());
                put("%%game_time%%", game.getFormattedGameTime());
                put("%%color%%", getColorData().getColor().getDisplayName());
                put("%%host%%", host);
                put("%%role%%", role.getRoleWithColor());
                put("%%phase%%", game.getPhase().getColoredPhase(game.getDayNumber()));
            }});
        });
        scoreboard.setUpdateTimer(10);
    }
    

    public void setRole(Roles role) {
        this.role = role;
        this.roleData = RoleDataFactory.createRoleData(role, this);
        if (getOptionalPlayerUUID().isPresent()) {
            Player p = Bukkit.getPlayer(getOptionalPlayerUUID().get());
            if (p == null) return;
            p.sendTitlePart(TitlePart.TITLE, MiniMessage.miniMessage().deserialize("<!i>" + role.getRoleWithColor()));
        }
    }


    public Optional<UUID> getOptionalPlayerUUID() {
        return Optional.ofNullable(playerUUID);
    }

    public String getColoredName() {
        if( playerUUID == null) {
            return colorData.getColor().getDisplayName() + " <gray>Onbekend";
        }
        return colorData.getColor().getDisplayName() + " <gray>" + Bukkit.getOfflinePlayer(playerUUID).getName();
    }

    @Override
    public String toString() {
        return "GamePlayer{" +
                "roleData=" + roleData.toString() +
                ", role=" + role.toString() +
                ", colorData=" + colorData.toString() +
                ", playerUUID=" + playerUUID +
                ", alive=" + alive +
                '}';
    }

    public Optional<Player> getBukkitPlayer() {
        return getOptionalPlayerUUID().map(Bukkit::getPlayer);
    }
}
