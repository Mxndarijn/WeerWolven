package me.mxndarijn.weerwolven.data;

import lombok.Getter;
import me.mxndarijn.weerwolven.game.role.EmptyRoleData;
import me.mxndarijn.weerwolven.game.role.RoleData;
import me.mxndarijn.weerwolven.game.VillagerRole;
import nl.mxndarijn.mxlib.item.MxSkullItemStackBuilder;

@Getter
public enum Roles {
    SEER("Ziener", Team.CITIZEN, "seer", VillagerRole.class),
    WITCH("Heks", Team.CITIZEN, "witch", VillagerRole.class),
    HUNTER("Jager", Team.CITIZEN, "hunter", VillagerRole.class),
    JAILER("Cipier", Team.CITIZEN, "jailer", VillagerRole.class),
    SPY("Spion", Team.CITIZEN, "spy", VillagerRole.class),
    VILLAGER("Burger", Team.CITIZEN, "villager", VillagerRole.class),
    JESTER("Dorpsgek", Team.SOLO, "jester", VillagerRole.class),
    SERIAL_KILLER("Moordenaar", Team.SOLO, "serial-killer", VillagerRole.class),
    UNKNOWN("Onbekend", Team.SOLO , "unknown-role" , EmptyRoleData.class);

    private final String rolName;
    private final Team team;
    private final String headKey;
    private final Class<? extends RoleData> roleClass;

    Roles(String rolName, Team team, String headKey, Class<? extends RoleData> roleClass) {
        this.rolName = rolName;
        this.team = team;
        this.headKey = headKey;
        this.roleClass = roleClass;
    }

    public String getRoleWithColor() {
        return team.getTeamColor() + rolName;
    }

    public MxSkullItemStackBuilder getHead() {
        return MxSkullItemStackBuilder.create(1).setSkinFromHeadsData(headKey);
    }
}
