package me.mxndarijn.weerwolven.util;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Random;

public final class FireworkHelper {

    private static final Random RND = new Random();
    /* Optioneel: laatste task id bijhouden voor debug/cancel elders */
    @Setter
    @Getter
    private static volatile int lastTaskTouch = -1;

    private static final List<Color> COLORS = List.of(
            Color.AQUA, Color.BLUE, Color.FUCHSIA, Color.LIME, Color.ORANGE,
            Color.PURPLE, Color.RED, Color.WHITE, Color.YELLOW
    );

    private FireworkHelper() {}

    /** Schiet een enkel willekeurig vuurwerk ±8–14 blokken boven de speler, met lichte spreiding. */
    public static void launchRandomHigh(Location base) {
        if (base == null) return;
        World world = base.getWorld();
        if (world == null) return;

        // Spawn iets boven en met kleine horizontale offset
        double yBoost = 8 + RND.nextInt(7); // 8..14 blokken
        double xOff = (RND.nextDouble() - 0.5) * 6.0; // -3..3
        double zOff = (RND.nextDouble() - 0.5) * 6.0; // -3..3
        Location spawnLoc = base.clone().add(xOff, Math.max(2.0, yBoost), zOff);

        Firework fw = (Firework) world.spawnEntity(spawnLoc, EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = fw.getFireworkMeta();

        // Random effect
        FireworkEffect.Type type = switch (RND.nextInt(5)) {
            case 0 -> FireworkEffect.Type.BALL;
            case 1 -> FireworkEffect.Type.BALL_LARGE;
            case 2 -> FireworkEffect.Type.BURST;
            case 3 -> FireworkEffect.Type.CREEPER;
            default -> FireworkEffect.Type.STAR;
        };

        Color c1 = COLORS.get(RND.nextInt(COLORS.size()));
        Color c2 = COLORS.get(RND.nextInt(COLORS.size()));

        FireworkEffect effect = FireworkEffect.builder()
                .with(type)
                .withColor(c1, c2)
                .withFade(COLORS.get(RND.nextInt(COLORS.size())))
                .flicker(RND.nextBoolean())
                .trail(true)
                .build();

        meta.addEffect(effect);
        // Power 2..3 voor hogere hoogte (power ~ vluchtduur)
        meta.setPower(2 + RND.nextInt(2));
        fw.setFireworkMeta(meta);

        // Geef een kleine extra verticale snelheid voor “shot up” gevoel
        fw.setVelocity(new Vector(
                (RND.nextDouble() - 0.5) * 0.2,
                0.6 + RND.nextDouble() * 0.2,
                (RND.nextDouble() - 0.5) * 0.2
        ));
    }

}
