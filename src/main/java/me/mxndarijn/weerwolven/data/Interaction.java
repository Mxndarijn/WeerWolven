package me.mxndarijn.weerwolven.data;

import lombok.Getter;
import org.bukkit.Material;

@Getter
public enum Interaction {
    CHEST(Material.CHEST),
    TRAPPED_CHEST(Material.TRAPPED_CHEST),
    FURNACE(Material.FURNACE),
    BLAST_FURNACE(Material.BLAST_FURNACE, false),
    OAK_GATE(Material.OAK_FENCE_GATE, false),
    SPRUCE_GATE(Material.SPRUCE_FENCE_GATE, false),
    BIRCH_GATE(Material.BIRCH_FENCE_GATE),
    JUNGLE_GATE(Material.JUNGLE_FENCE_GATE),
    ACACIA_GATE(Material.ACACIA_FENCE_GATE),
    DARK_OAK_GATE(Material.DARK_OAK_FENCE_GATE),
    CRIMSON_GATE(Material.CRIMSON_FENCE_GATE),
    WARPED_GATE(Material.WARPED_FENCE_GATE),
    OAK_DOOR(Material.OAK_DOOR, true),
    SPRUCE_DOOR(Material.SPRUCE_DOOR, true),
    BIRCH_DOOR(Material.BIRCH_DOOR, true),
    JUNGLE_DOOR(Material.JUNGLE_DOOR, true),
    ACACIA_DOOR(Material.ACACIA_DOOR, true),
    DARK_OAK_DOOR(Material.DARK_OAK_DOOR, true),
    CRIMSON_DOOR(Material.CRIMSON_DOOR, true),
    WARPED_DOOR(Material.WARPED_DOOR, true),
    OAK_TRAPDOOR(Material.OAK_TRAPDOOR),
    SPRUCE_TRAPDOOR(Material.SPRUCE_TRAPDOOR),
    BIRCH_TRAPDOOR(Material.BIRCH_TRAPDOOR),
    JUNGLE_TRAPDOOR(Material.JUNGLE_TRAPDOOR),
    ACACIA_TRAPDOOR(Material.ACACIA_TRAPDOOR),
    DARK_OAK_TRAPDOOR(Material.DARK_OAK_TRAPDOOR),
    CRIMSON_TRAPDOOR(Material.CRIMSON_TRAPDOOR),
    WARPED_TRAPDOOR(Material.WARPED_TRAPDOOR),
    CHERRY_TRAPDOOR(Material.CHERRY_TRAPDOOR),
    BAMBOO_TRAPDOOR(Material.BAMBOO_TRAPDOOR),
    OAK_BUTTON(Material.OAK_BUTTON),
    SPRUCE_BUTTON(Material.SPRUCE_BUTTON),
    BIRCH_BUTTON(Material.BIRCH_BUTTON),
    JUNGLE_BUTTON(Material.JUNGLE_BUTTON),
    ACACIA_BUTTON(Material.ACACIA_BUTTON),
    DARK_OAK_BUTTON(Material.DARK_OAK_BUTTON),
    CRIMSON_BUTTON(Material.CRIMSON_BUTTON),
    WARPED_BUTTON(Material.WARPED_BUTTON),
    STONE_BUTTON(Material.STONE_BUTTON),
    POlISHED_BLACKSTONE_BUTTON(Material.POLISHED_BLACKSTONE_BUTTON),
    BLACK_BED(Material.BLACK_BED, false),
    BLUE_BED(Material.BLUE_BED, false),
    BROWN_BED(Material.BROWN_BED, false),
    CYAN_BED(Material.CYAN_BED, false),
    GRAY_BED(Material.GRAY_BED, false),
    GREEN_BED(Material.GREEN_BED, false),
    LIGHT_BLUE_BED(Material.LIGHT_BLUE_BED, false),
    LIGHT_GRAY_BED(Material.LIGHT_GRAY_BED, false),
    MAGENTA_BED(Material.MAGENTA_BED, false),
    PINK_BED(Material.PINK_BED, false),
    LIME_BED(Material.LIME_BED, false),
    WHITE_BED(Material.WHITE_BED, false),
    ORANGE_BED(Material.ORANGE_BED, false),
    YELLOW_BED(Material.YELLOW_BED, false),
    PURPLE_BED(Material.PURPLE_BED, false),
    RED_BED(Material.RED_BED, false),
    ACACIA_PRESSURE_PLATE(Material.ACACIA_PRESSURE_PLATE),
    BIRCH_PRESSURE_PLATE(Material.BIRCH_PRESSURE_PLATE),
    CRIMSON_PRESSURE_PLATE(Material.CRIMSON_PRESSURE_PLATE),
    JUNGLE_PRESSURE_PLATE(Material.JUNGLE_PRESSURE_PLATE),
    MANGROVE_PRESSURE_PLATE(Material.MANGROVE_PRESSURE_PLATE),
    OAK_PRESSURE_PLATE(Material.OAK_PRESSURE_PLATE),
    DARK_OAK_PRESSURE_PLATE(Material.DARK_OAK_PRESSURE_PLATE),
    SPRUCE_PRESSURE_PLATE(Material.SPRUCE_PRESSURE_PLATE),
    STONE_PRESSURE_PLATE(Material.STONE_PRESSURE_PLATE),
    WARPED_PRESSURE_PLATE(Material.WARPED_PRESSURE_PLATE),
    HEAVY_PRESSURE_PLATE(Material.HEAVY_WEIGHTED_PRESSURE_PLATE),
    POLISHED_BLACKSTONE_PRESSURE_PLATE(Material.POLISHED_BLACKSTONE_PRESSURE_PLATE),
    LIGHT_PRESSURE_PLATE(Material.LIGHT_WEIGHTED_PRESSURE_PLATE),

    LEVER(Material.LEVER),
    FLOWER_POT(Material.FLOWER_POT, false),
    ENCHANTMENT_TABLE(Material.ENCHANTING_TABLE, false),
    ANVIL(Material.ANVIL, false),
    BREWING_STAND(Material.BREWING_STAND, false),
    JUKEBOX(Material.JUKEBOX, false),
    NOTE_BLOCK(Material.NOTE_BLOCK, false),
    DISPENSER(Material.DISPENSER),
    DROPPER(Material.DROPPER),
    REDSTONE_COMPARATOR(Material.COMPARATOR, false),
    REDSTONE_REPEATER(Material.REPEATER, false),
    DAYLIGHT_SENSOR(Material.DAYLIGHT_DETECTOR, false),
    TRIPWIRE(Material.TRIPWIRE_HOOK, false),
    TARGET(Material.TARGET, false),
    CARTOGRAPHY_TABLE(Material.CARTOGRAPHY_TABLE, false),
    LECTERN(Material.LECTERN, false),
    BEACON(Material.BEACON, false),
    BELL(Material.BELL, false),
    CAMPFIRE(Material.CAMPFIRE, false),
    SMOKER(Material.SMOKER, false),
    STONECUTTER(Material.STONECUTTER, false),
    LOOM(Material.LOOM, false),
    GRINDSTONE(Material.GRINDSTONE, false),
    BARREL(Material.BARREL),
    TNT(Material.TNT, false),
    END_CHEST(Material.ENDER_CHEST, false),
    END_PORTAL_FRAME(Material.END_PORTAL_FRAME, false),
    CHIPPED_ANVIL(Material.CHIPPED_ANVIL, false),
    DAMAGED_ANVIL(Material.DAMAGED_ANVIL, false),
    CAULDRON(Material.CAULDRON),
    COMPOSTER(Material.COMPOSTER, false),
    SOUL_CAMPFIRE(Material.SOUL_CAMPFIRE, false),
    SMITHING_TABLE(Material.SMITHING_TABLE, false),
    ITEM_FRAME(Material.ITEM_FRAME, false),
    ARMOR_STAND(Material.ARMOR_STAND, false),
    MANGROVE_BUTTON(Material.MANGROVE_BUTTON),
    MANGROVE_DOOR(Material.MANGROVE_DOOR, true),
    MANGROVE_FENCE_GATE(Material.MANGROVE_FENCE_GATE),
    MANGROVE_TRAPDOOR(Material.MANGROVE_TRAPDOOR),
    CHERRY_BUTTON(Material.CHERRY_BUTTON),
    CHERRY_DOOR(Material.CHERRY_DOOR, true),
    CHERRY_FENCE_GATE(Material.CHERRY_FENCE_GATE),
    CHERRY_PRESSURE_PLATE(Material.CHERRY_PRESSURE_PLATE),
    CHISELED_BOOKSHELF(Material.CHISELED_BOOKSHELF, false),
    GLOW_ITEM_FRAME(Material.GLOW_ITEM_FRAME, false);


    private final Material mat;
    private final boolean defaultValue;

    Interaction(Material mat) {
        this.mat = mat;
        this.defaultValue = false;
    }

    Interaction(Material mat, boolean defValue) {
        this.mat = mat;
        this.defaultValue = defValue;
    }

    public static boolean isAllowedToInteract(Material type) {
        for (Interaction interaction : values()) {
            if (interaction.getMat() == type) return true;
        }
        return false;
    }
}
