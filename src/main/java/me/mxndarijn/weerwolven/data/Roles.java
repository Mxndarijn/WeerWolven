package me.mxndarijn.weerwolven.data;

import lombok.Getter;
import me.mxndarijn.weerwolven.game.core.SeerRole;
import me.mxndarijn.weerwolven.game.core.VillagerRole;
import me.mxndarijn.weerwolven.game.role.EmptyRoleData;
import me.mxndarijn.weerwolven.game.role.RoleData;
import nl.mxndarijn.mxlib.item.MxSkullItemStackBuilder;

import javax.annotation.Nullable;

@Getter
public enum Roles {
    SEER("Ziener", Team.CITIZEN, "seer", RolePriority.HIGHEST, SeerRole.class),
    WITCH("Heks", Team.CITIZEN, "witch", RolePriority.HIGH, VillagerRole.class),
    HUNTER("Jager", Team.CITIZEN, "hunter", RolePriority.LOW, VillagerRole.class),
    JAILER("Cipier", Team.CITIZEN, "jailer", RolePriority.LOW, VillagerRole.class, 2),
    SPY("Spion", Team.CITIZEN, "spy", RolePriority.NORMAL, VillagerRole.class),
    VILLAGER("Burger", Team.CITIZEN, "villager", RolePriority.LOWEST, VillagerRole.class),
    JESTER("Dorpsgek", Team.SOLO, "jester", RolePriority.NORMAL, VillagerRole.class),
    SERIAL_KILLER("Moordenaar", Team.SOLO, "serial-killer", RolePriority.HIGH, VillagerRole.class),
    CUPID("Cupido", Team.CITIZEN, "cupid", RolePriority.LOW, EmptyRoleData.class, 1),
    WEREWOLF("Weerwolf", Team.CITIZEN, "werewolf", RolePriority.HIGH, EmptyRoleData.class),
    UNKNOWN("Onbekend", Team.SOLO, "question-mark", RolePriority.LOWEST, EmptyRoleData.class);

    private final String rolName;
    private final Team team;
    private final String headKey;
    private final Class<? extends RoleData> roleClass;
    private final RolePriority priority;
    // null means unlimited
    private final @Nullable Integer maxPerRoleSet;

    Roles(String rolName, Team team, String headKey, RolePriority priority, Class<? extends RoleData> roleClass) {
        this(rolName, team, headKey, priority, roleClass, null);
    }

    Roles(String rolName, Team team, String headKey, RolePriority priority, Class<? extends RoleData> roleClass, @Nullable Integer maxPerRoleSet) {
        this.rolName = rolName;
        this.team = team;
        this.headKey = headKey;
        this.roleClass = roleClass;
        this.priority = priority;
        this.maxPerRoleSet = maxPerRoleSet;
    }

    public String getRoleWithColor() {
        return team.getTeamColor() + rolName;
    }

    public MxSkullItemStackBuilder getHead() {
        return MxSkullItemStackBuilder.create(1).setSkinFromHeadsData(headKey);
    }
}
