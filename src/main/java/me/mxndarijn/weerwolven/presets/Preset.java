package me.mxndarijn.weerwolven.presets;

import lombok.Getter;
import me.mxndarijn.weerwolven.ChangeScoreboardOnChangeWorld;
import me.mxndarijn.weerwolven.SaveInventoryChangeWorld;
import me.mxndarijn.weerwolven.WeerWolven;
import me.mxndarijn.weerwolven.data.Items;
import me.mxndarijn.weerwolven.data.ScoreBoard;
import me.mxndarijn.weerwolven.data.WeerWolvenPermissions;
import me.mxndarijn.weerwolven.data.WeerWolvenPrefix;
import nl.mxndarijn.mxlib.changeworld.ChangeWorldManager;
import nl.mxndarijn.mxlib.changeworld.MxChangeWorld;
import nl.mxndarijn.mxlib.inventory.heads.MxHeadManager;
import nl.mxndarijn.mxlib.item.MxSkullItemStackBuilder;
import nl.mxndarijn.mxlib.item.Pair;
import nl.mxndarijn.mxlib.language.LanguageManager;
import nl.mxndarijn.mxlib.logger.LogLevel;
import nl.mxndarijn.mxlib.logger.Logger;
import nl.mxndarijn.mxlib.mxscoreboard.MxScoreBoard;
import nl.mxndarijn.mxlib.mxscoreboard.MxSupplierScoreBoard;
import nl.mxndarijn.mxlib.mxworld.MxAtlas;
import nl.mxndarijn.mxlib.mxworld.MxWorld;
import nl.mxndarijn.mxlib.util.Functions;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class Preset {
    public static final String PRESET_ITEMMETA_TAG = "preset_id";

    @Getter
    private final File directory;
    @Getter
    private final File inventoriesFile;
    @Getter
    private PresetConfig config;
    @Getter
    private Optional<MxWorld> mxWorld;

    private MxScoreBoard scoreboard;

    private Preset(File directory) {
        this.directory = directory;
        this.mxWorld = Optional.empty();
        File presetConfigFile = new File(directory + File.separator + "preset.yml");
        inventoriesFile = new File(directory + File.separator + "inventories.yml");
        if (containsWorld()) {
            if (!presetConfigFile.exists()) {
                Functions.copyFileFromResources("preset.yml", presetConfigFile);
            }
            if (!inventoriesFile.exists()) {
                try {
                    inventoriesFile.createNewFile();
                } catch (IOException e) {
                    Logger.logMessage(LogLevel.ERROR, "Could not create file: " + inventoriesFile.getAbsolutePath());
                    e.printStackTrace();
                }
            }
            this.config = new PresetConfig(presetConfigFile);
            this.mxWorld = MxAtlas.getInstance().loadWorld(directory);
        }
    }

    public static Optional<Preset> create(File file) {
        Preset preset = new Preset(file);

        if (preset.containsWorld() && preset.mxWorld.isPresent()) {
            return Optional.of(preset);
        } else {
            if (!preset.containsWorld()) {
                Logger.logMessage(LogLevel.ERROR, WeerWolvenPrefix.PRESETS_MANAGER, "Could not find world. (" + preset.getDirectory() + ")");
            }
            if (preset.getMxWorld().isEmpty()) {
                Logger.logMessage(LogLevel.ERROR, WeerWolvenPrefix.PRESETS_MANAGER, "Could not find MxWorld. (" + preset.getDirectory() + ")");
            }
            return Optional.empty();
        }
    }

    public ItemStack getItemStack() {
        MxSkullItemStackBuilder builder = MxSkullItemStackBuilder.create(1);
        if (MxHeadManager.getInstance().getAllHeadKeys().contains(config.getSkullId())) {
            builder.setSkinFromHeadsData(config.getSkullId());
        } else {
            builder.setSkinFromHeadsData("question-mark");
        }

        builder.setName("<gray>" + config.getName())
                .addLore(" ");

        if (!config.isConfigured()) {
            builder.addLore("<gray>Wereld-Naam: " + directory.getName());
            builder.addLore(" ");
        }
        if (config.isLocked()) {
            builder.addLore("<gray>Locked: " + (config.isLocked() ? "<green>Ja" : "<red>Nee"))
                    .addLore("<gray>Door: " + config.getLockedBy())
                    .addLore("<gray>Reden: ")
                    .addLore("<red>" + config.getLockReason());
        }
        builder.addLore(" ")
                .addLore("<gray>Geconfigureerd: " + (config.isConfigured() ? "<green>Ja" : "<red>Nee"));

        builder.addCustomTagString(PRESET_ITEMMETA_TAG, directory.getName());


        return builder.build();
    }

    public ItemStack getItemStackForNewMap(Player p) {
        MxSkullItemStackBuilder builder = MxSkullItemStackBuilder.create(1);
        if (MxHeadManager.getInstance().getAllHeadKeys().contains(config.getSkullId())) {
            builder.setSkinFromHeadsData(config.getSkullId());
        } else {
            builder.setSkinFromHeadsData("question-mark");
        }

        builder.addLore(" ")
                .addLore("<gray>Aantal Spelers: " + config.getColors().size());
        builder.setName("<gray>" + config.getName())
                .addLore(" ");

        if (config.isLocked()) {
            builder.addLore("<gray>Locked: " + "<green>Ja")
                    .addLore("<gray>Door: " + config.getLockedBy())
                    .addLore("<gray>Reden: ")
                    .addLore("<red>" + config.getLockReason());
        }
        builder.addCustomTagString(PRESET_ITEMMETA_TAG, directory.getName());

        if(!p.hasPermission(WeerWolvenPermissions.COMMAND_GAMES_CREATE_SPECIFIC_GAME + config.getName().toLowerCase().replaceAll(" ", "_"))) {
            builder.addLore("<red>Jij kan deze preset niet zelf aanmaken.");
        }

        return builder.build();
    }

    private boolean containsWorld() {
        return containsFolder("region");
    }

    private boolean containsFolder(String folderName) {
        File f = new File(directory.getAbsolutePath() + File.separator + folderName);
        return f.exists();
    }

    public String getStars(int stars) {
        StringBuilder hostStars = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            if (i <= stars) {
                hostStars.append("<yellow>\u272B");
            } else {
                hostStars.append("<gray>\u272B");
            }
        }
        return hostStars.toString();
    }

    public CompletableFuture<Boolean> loadWorld() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        if (this.mxWorld.isEmpty()) {
            future.complete(false);
            return future;
        }
        if (this.mxWorld.get().isLoaded()) {
            future.complete(false);
            return future;
        }


        MxAtlas.getInstance().loadMxWorld(this.mxWorld.get()).thenAccept(loaded -> {
            if (loaded) {
                this.scoreboard = new MxSupplierScoreBoard(JavaPlugin.getPlugin(WeerWolven.class), () -> ScoreBoard.PRESET.getTitle(new HashMap<>() {{
                    put("%%preset_name%%", config.getName());
                }}), () -> ScoreBoard.PRESET.getLines(new HashMap<>() {{
                    put("%%colors_amount%%", config.getColors().size() + "");
                    put("%%configured%%", (config.isConfigured() ? "<green>Ja" : "<red>Nee"));
                }}));
                scoreboard.setUpdateTimer(20L);
                ChangeWorldManager.getInstance().addWorld(this.mxWorld.get().getWorldUID(), new SaveInventoryChangeWorld(getInventoriesFile(), new ArrayList<>(
                        Arrays.asList(
//                                new Pair<>(Items.PRESET_CONFIGURE_TOOL.getItemStack(), WidmChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WidmLanguageText.PRESET_INFO_CONFIGURE_TOOL)),
//                                new Pair<>(Items.CHEST_CONFIGURE_TOOL.getItemStack(), WidmChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WidmLanguageText.CHEST_CONFIGURE_TOOL_INFO)),
//                                new Pair<>(Items.SHULKER_CONFIGURE_TOOL.getItemStack(), WidmChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WidmLanguageText.SHULKER_CONFIGURE_TOOL_INFO)),
//                                new Pair<>(Items.DOOR_CONFIGURE_TOOL.getItemStack(), WidmChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WidmLanguageText.DOOR_CONFIGURE_TOOL_INFO))
                        )),
                        (p, w, e) -> {
                            unloadWorld();
                        }));
                ChangeWorldManager.getInstance().addWorld(this.mxWorld.get().getWorldUID(), new ChangeScoreboardOnChangeWorld(scoreboard));
                ChangeWorldManager.getInstance().addWorld(this.mxWorld.get().getWorldUID(), new MxChangeWorld() {
                    @Override
                    public void enter(Player p, World w, PlayerChangedWorldEvent e) {
                        p.setGameMode(GameMode.CREATIVE);
                    }

                    @Override
                    public void leave(Player p, World w, PlayerChangedWorldEvent e) {
                        p.setGameMode(GameMode.ADVENTURE);
                    }

                    @Override
                    public void quit(Player p, World w, PlayerQuitEvent e) {
                        // do nothing
                    }
                });
            }
            future.complete(loaded);
        });
        return future;
    }

    public void unloadWorld() {
        Bukkit.getScheduler().scheduleSyncDelayedTask(JavaPlugin.getPlugin(WeerWolven.class), () -> {
            if (this.mxWorld.isEmpty()) {
                return;
            }
            if (!this.mxWorld.get().isLoaded()) {
                return;
            }
            config.save();
            MxAtlas.getInstance().unloadMxWorld(this.mxWorld.get(), true);
        });
    }
}
