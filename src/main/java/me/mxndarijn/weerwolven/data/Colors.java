package me.mxndarijn.weerwolven.data;


import lombok.Getter;
import org.bukkit.Material;

import java.util.Optional;

public enum Colors {
    LICHTGROEN("light-green", "<green>", "Lime", Material.LIME_SHULKER_BOX, "light-green-block", "\uE035"),
    GROEN("green", "<dark_green>", "Groen", Material.GREEN_SHULKER_BOX, "green-block", "\uE032"),
    LICHTBLAUW("light-blue", "<blue>", "Licht-Blauw", Material.LIGHT_BLUE_SHULKER_BOX, "light-blue-block", "\uE033"),
    BLAUW("blue", "<dark_blue>", "Blauw", Material.BLUE_SHULKER_BOX, "blue-block", "\uE027"),
    ZWART("black", "<black>", "Zwart", Material.BLACK_SHULKER_BOX, "black-block", "\uE042"),
    WIT("white", "<white>", "Wit", Material.WHITE_SHULKER_BOX, "white-block", "\uE041"),
    ORANJE("orange", "<orange>", "Oranje", Material.ORANGE_SHULKER_BOX, "orange-block", "\uE037"),
    ROOD("red", "<red>", "Rood", Material.RED_SHULKER_BOX, "red-block", "\uE039"),
    MAGENTA("magenta", "<light_purple>", "Magenta", Material.MAGENTA_SHULKER_BOX, "magenta-block", "\uE036"),
    ROZE("pink", "<light_purple>", "Roze", Material.PINK_SHULKER_BOX, "pink-block", "\uE040"),
    PAARS("purple", "<dark_purple>", "Paars", Material.PURPLE_SHULKER_BOX, "purple-block", "\uE038"),
    CYAN("cyan", "<dark_aqua>", "Cyan", Material.CYAN_SHULKER_BOX, "cyan-block", "\uE0229"),
    BRUIN("brown", "<brown>", "Bruin", Material.BROWN_SHULKER_BOX, "brown-block", "\uE028"),
    LICHTGRIJS("light-gray", "<gray>", "Licht-Grijs", Material.LIGHT_GRAY_SHULKER_BOX, "light-gray-block", "\uE034"),
    GRIJS("gray", "<dark_gray>", "Grijs", Material.GRAY_SHULKER_BOX, "gray-block", "\uE031"),
    GEEL("yellow", "<yellow>", "Geel", Material.YELLOW_SHULKER_BOX, "yellow-block", "\uE030");

    @Getter
    private final String type;
    @Getter
    private final String color;
    @Getter
    private final String displayName;
    @Getter
    private final String displayNameWithoutColor;
    @Getter
    private final Material shulkerBlock;
    @Getter
    private final String headKey;
    @Getter
    private final String unicodeIcon;

    private final String PREFIX = "<white>\uE001\uE001\uE001\uE001\uE001";

    Colors(String type, String color, String displayName, Material shulkerBlock, String headKey, String unicodeIcon) {
        this.type = type;
        this.color = color;
        this.displayName = this.color + displayName;
        this.displayNameWithoutColor = displayName;
        this.shulkerBlock = shulkerBlock;
        this.headKey = headKey;
        this.unicodeIcon = "  " + PREFIX + unicodeIcon + "  ";
    }

    public static Optional<Colors> getColorByType(String type) {
        for (Colors color : values()) {
            if (color.type.equals(type)) {
                return Optional.of(color);
            }
        }

        return Optional.empty();
    }

    public static Optional<Colors> getColorByMaterial(Material type) {
        for (Colors color : values()) {
            if (color.shulkerBlock.equals(type)) {
                return Optional.of(color);
            }
        }

        return Optional.empty();
    }

}
