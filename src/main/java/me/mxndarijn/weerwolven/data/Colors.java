package me.mxndarijn.weerwolven.data;


import lombok.Getter;
import org.bukkit.Material;

import java.util.Optional;

public enum Colors {
    LICHTGROEN("light-green", "<green>", "Lime", Material.LIME_SHULKER_BOX, Material.LIME_BED, "light-green-block", "\uE035"),
    GROEN("green", "<dark_green>", "Groen", Material.GREEN_SHULKER_BOX, Material.GREEN_BED, "green-block", "\uE032"),
    LICHTBLAUW("light-blue", "<blue>", "Licht-Blauw", Material.LIGHT_BLUE_SHULKER_BOX, Material.LIGHT_BLUE_BED, "light-blue-block", "\uE033"),
    BLAUW("blue", "<dark_blue>", "Blauw", Material.BLUE_SHULKER_BOX, Material.BLUE_BED, "blue-block", "\uE027"),
    ZWART("black", "<black>", "Zwart", Material.BLACK_SHULKER_BOX, Material.BLACK_BED, "black-block", "\uE042"),
    WIT("white", "<white>", "Wit", Material.WHITE_SHULKER_BOX, Material.WHITE_BED, "white-block", "\uE041"),
    ORANJE("orange", "<gold>", "Oranje", Material.ORANGE_SHULKER_BOX, Material.ORANGE_BED, "orange-block", "\uE037"),
    ROOD("red", "<red>", "Rood", Material.RED_SHULKER_BOX, Material.RED_BED, "red-block", "\uE039"),
    MAGENTA("magenta", "<light_purple>", "Magenta", Material.MAGENTA_SHULKER_BOX, Material.MAGENTA_BED, "magenta-block", "\uE036"),
    ROZE("pink", "<light_purple>", "Roze", Material.PINK_SHULKER_BOX, Material.PINK_BED, "pink-block", "\uE040"),
    PAARS("purple", "<dark_purple>", "Paars", Material.PURPLE_SHULKER_BOX, Material.PURPLE_BED, "purple-block", "\uE038"),
    CYAN("cyan", "<dark_aqua>", "Cyan", Material.CYAN_SHULKER_BOX, Material.CYAN_BED, "cyan-block", "\uE0229"),
    BRUIN("brown", "<gold>", "Bruin", Material.BROWN_SHULKER_BOX, Material.BROWN_BED, "brown-block", "\uE028"),
    LICHTGRIJS("light-gray", "<gray>", "Licht-Grijs", Material.LIGHT_GRAY_SHULKER_BOX, Material.LIGHT_GRAY_BED, "light-gray-block", "\uE034"),
    GRIJS("gray", "<dark_gray>", "Grijs", Material.GRAY_SHULKER_BOX, Material.GRAY_BED, "gray-block", "\uE031"),
    GEEL("yellow", "<yellow>", "Geel", Material.YELLOW_SHULKER_BOX, Material.YELLOW_BED, "yellow-block", "\uE030");

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
    private final Material bedBlock;
    @Getter
    private final String headKey;
    @Getter
    private final String unicodeIcon;

    private final String PREFIX = "<white>\uE001\uE001\uE001\uE001\uE001";

    Colors(String type, String color, String displayName, Material shulkerBlock, Material bedBlock, String headKey, String unicodeIcon) {
        this.type = type;
        this.color = color;
        this.displayName = this.color + displayName;
        this.displayNameWithoutColor = displayName;
        this.shulkerBlock = shulkerBlock;
        this.bedBlock = bedBlock;
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
            if (color.shulkerBlock.equals(type) || color.bedBlock.equals(type)) {
                return Optional.of(color);
            }
        }

        return Optional.empty();
    }

}
