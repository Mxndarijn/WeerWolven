package me.mxndarijn.weerwolven;

import me.mxndarijn.weerwolven.commands.PresetsCommand;
import me.mxndarijn.weerwolven.commands.SpawnCommand;
import me.mxndarijn.weerwolven.commands.RoleSetCommand;
import me.mxndarijn.weerwolven.data.*;
import me.mxndarijn.weerwolven.managers.*;
import net.kyori.adventure.text.minimessage.MiniMessage;
import nl.mxndarijn.mxlib.MxLib;
import nl.mxndarijn.mxlib.configfiles.ConfigService;
import nl.mxndarijn.mxlib.configfiles.StandardConfigFile;
import nl.mxndarijn.mxlib.language.LanguageManager;
import nl.mxndarijn.mxlib.logger.LogLevel;
import nl.mxndarijn.mxlib.logger.Logger;
import nl.mxndarijn.mxlib.logger.PrefixRegistry;
import nl.mxndarijn.mxlib.logger.StandardPrefix;
import nl.mxndarijn.mxlib.util.events.PlayerJoinEventHeadManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public final class WeerWolven extends JavaPlugin {

    @Override
    public void onEnable() {
        Logger.setLogLevel(LogLevel.DEBUG);
        Bukkit.getConsoleSender().sendMessage(MiniMessage.miniMessage().deserialize("<!i>" + WeerWolvenChatPrefix.DEFAULT.getPrefix() + "Starting Weerwolven..."));
        MxLib.init(this, "weerwolven", "<dark_gray>[<gold>WeerWolven");
        setLogLevel();
        PrefixRegistry.registerAll(WeerWolvenPrefix.class);
        ConfigService.getInstance().registerAll(WeerWolvenConfigFiles.class);
        PresetsManager.getInstance();
        ScoreBoardManager.getInstance();
        GameWorldManager.getInstance();
        ItemManager.getInstance();
        SpawnManager.getInstance();
        RoleSetManager.getInstance();
        RoleDefinitionManager.getInstance();

        registerCommands();
        configFilesSaver();

        getServer().getPluginManager().registerEvents(new PlayerJoinEventHeadManager(), this);

        Logger.logMessage(LogLevel.INFORMATION, "Started WeerWolven...");


    }

    private void registerCommands() {
        getCommand("presets").setExecutor(new PresetsCommand(WeerWolvenPermissions.COMMAND_PRESETS, true, false));
        getCommand("spawn").setExecutor(new SpawnCommand(WeerWolvenPermissions.COMMAND_SPAWN, true, false));
        getCommand("rolesets").setExecutor(new RoleSetCommand(WeerWolvenPermissions.COMMAND_ROLESETS, true, false));
    }

    @Override
    public void onDisable() {
        Logger.logMessage(LogLevel.INFORMATION, "Stopping WeerWolven...");
        ConfigService.getInstance().saveAll();
        GameManager.getInstance().save();
        Logger.logMessage(LogLevel.INFORMATION, "Stopped WeerWolven...");
    }

    private void setLogLevel() {
        Logger.setLogLevel(LogLevel.DEBUG);
        Optional<LogLevel> level = LogLevel.getLevelByInt(ConfigService.getInstance().get(StandardConfigFile.MAIN_CONFIG).getCfg().getInt("log-level"));
        if (level.isPresent()) {
            Logger.setLogLevel(level.get());
            Logger.logMessage(LogLevel.INFORMATION, StandardPrefix.LOGGER, "Log-level has been set to " + level.get().getName() + " (Found in config)");
        } else {
            Logger.setLogLevel(LogLevel.DEBUG);
            Logger.logMessage(LogLevel.INFORMATION, StandardPrefix.LOGGER, "Log-level has been set to " + LogLevel.DEBUG.getName() + " (default, not found in config)");
        }
    }

    private void configFilesSaver() {
        int interval = ConfigService.getInstance().get(StandardConfigFile.MAIN_CONFIG).getCfg().getInt("auto-save-configs-interval");
        if (interval == 0) {
            interval = 5;
            Logger.logMessage(LogLevel.ERROR, StandardPrefix.CONFIG_FILES, "Interval for auto-save is 0, autosetting it to 5.. (Needs to be higher than 0)");
            ConfigService.getInstance().get(StandardConfigFile.MAIN_CONFIG).getCfg().set("auto-save-configs-interval", 5);
        }
        Logger.logMessage(LogLevel.INFORMATION, StandardPrefix.CONFIG_FILES, "Saving interval for config files is " + interval + " minutes.");
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            ConfigService.getInstance().saveAll();
        }, 20L * 60L * interval, 20L * 60L * interval);
    }
}
