package me.mxndarijn.weerwolven.managers;

import me.mxndarijn.weerwolven.data.WeerWolvenConfigFiles;
import me.mxndarijn.weerwolven.data.WeerWolvenPrefix;
import me.mxndarijn.weerwolven.game.core.RoleSet;
import nl.mxndarijn.mxlib.configfiles.ConfigService;
import nl.mxndarijn.mxlib.logger.LogLevel;
import nl.mxndarijn.mxlib.logger.Logger;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.util.*;

public class RoleSetManager {

    private static RoleSetManager instance;
    // Keyed by RoleSet ID
    private final Map<String, RoleSet> roleSets = new LinkedHashMap<>();

    private RoleSetManager() {
        Logger.logMessage(LogLevel.INFORMATION, WeerWolvenPrefix.ROLE_SETS_MANAGER, "Loading RoleSet-Manager...");
        reload();
    }

    public static RoleSetManager getInstance() {
        if (instance == null) {
            instance = new RoleSetManager();
        }
        return instance;
    }

    public void reload() {
        roleSets.clear();
        org.bukkit.configuration.file.FileConfiguration cfg = ConfigService.getInstance().get(WeerWolvenConfigFiles.ROLESETS).getCfg();
        File file = ConfigService.getInstance().get(WeerWolvenConfigFiles.ROLESETS).getFile();
        Set<String> keys = cfg.getKeys(false);
        for (String key : keys) {
            ConfigurationSection section = cfg.getConfigurationSection(key);
            RoleSet.load(section, file).ifPresentOrElse(rs -> {
                roleSets.put(rs.getId(), rs);
                Logger.logMessage(LogLevel.INFORMATION, WeerWolvenPrefix.ROLE_SETS_MANAGER, "Loaded roleset: " + rs.getName() + " (" + rs.getId() + ")");
            }, () -> {
                Logger.logMessage(LogLevel.ERROR, WeerWolvenPrefix.ROLE_SETS_MANAGER, "Failed to load roleset: " + key + " (" + file.getAbsolutePath() + ")");
            });
        }
    }

    // Legacy name-based lookup (returns first match if names are not unique)
    public Optional<RoleSet> get(String name) {
        if (name == null) return Optional.empty();
        for (RoleSet rs : roleSets.values()) {
            if (name.equalsIgnoreCase(rs.getName())) return Optional.of(rs);
        }
        return Optional.empty();
    }

    public Optional<RoleSet> getById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(roleSets.get(id));
    }

    public Collection<RoleSet> getAll() {
        return Collections.unmodifiableCollection(roleSets.values());
    }

    public void saveAll() {
        org.bukkit.configuration.file.FileConfiguration cfg = ConfigService.getInstance().get(WeerWolvenConfigFiles.ROLESETS).getCfg();
        File file = ConfigService.getInstance().get(WeerWolvenConfigFiles.ROLESETS).getFile();
        // clear existing
        for (String key : new HashSet<>(cfg.getKeys(false))) {
            cfg.set(key, null);
        }
        for (RoleSet rs : roleSets.values()) {
            rs.save(cfg, file);
        }
        // Ensure the config file is actually written to disk
        ConfigService.getInstance().get(WeerWolvenConfigFiles.ROLESETS).save();
    }

    public void addOrUpdate(RoleSet roleSet) {
        if (roleSet == null || roleSet.getId() == null || roleSet.getId().isEmpty()) return;
        roleSets.put(roleSet.getId(), roleSet);
        Logger.logMessage(LogLevel.DEBUG, WeerWolvenPrefix.ROLE_SETS_MANAGER, "Added/Updated roleset: " + roleSet.getName() + " (" + roleSet.getId() + ")");
    }

    public void removeById(String id) {
        if (id == null) return;
        roleSets.remove(id);
    }
}
