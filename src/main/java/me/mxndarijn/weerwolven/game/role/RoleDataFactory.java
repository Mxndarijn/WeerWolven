package me.mxndarijn.weerwolven.game.role;

import me.mxndarijn.weerwolven.data.Roles;
import me.mxndarijn.weerwolven.game.GamePlayer;

import java.lang.reflect.Constructor;

public class RoleDataFactory {

    public static RoleData createRoleData(Roles role, GamePlayer player) {
        if (role == null) return new EmptyRoleData();
        Class<? extends RoleData> cls = role.getRoleClass();
        if (cls == null) return new EmptyRoleData();
        try {
            // Try preferred constructors in order
            // 1) (GamePlayer)
            try {
                Constructor<? extends RoleData> c = cls.getDeclaredConstructor(GamePlayer.class);
                c.setAccessible(true);
                return c.newInstance(player);
            } catch (NoSuchMethodException ignored) {}
            // 2) (GamePlayer, Roles)
            try {
                Constructor<? extends RoleData> c = cls.getDeclaredConstructor(GamePlayer.class, Roles.class);
                c.setAccessible(true);
                return c.newInstance(player, role);
            } catch (NoSuchMethodException ignored) {}
            // 3) no-arg
            try {
                Constructor<? extends RoleData> c = cls.getDeclaredConstructor();
                c.setAccessible(true);
                return c.newInstance();
            } catch (NoSuchMethodException ignored) {}
        } catch (Throwable t) {
            // Fall through to return empty
        }
        return new EmptyRoleData();
    }
}
