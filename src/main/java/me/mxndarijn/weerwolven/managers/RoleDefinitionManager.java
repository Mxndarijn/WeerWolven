package me.mxndarijn.weerwolven.managers;

import me.mxndarijn.weerwolven.data.Roles;
import me.mxndarijn.weerwolven.data.WeerWolvenConfigFiles;
import nl.mxndarijn.mxlib.configfiles.ConfigService;
import nl.mxndarijn.mxlib.logger.LogLevel;
import nl.mxndarijn.mxlib.logger.Logger;
import nl.mxndarijn.mxlib.logger.StandardPrefix;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.stream.Collectors;

public class RoleDefinitionManager {
    private static RoleDefinitionManager instance;

    private final Map<Roles, ArrayList<String>> definitions = new EnumMap<>(Roles.class);

    private static final String DEFAULT_LINE = "<red>TBD :)";

    private RoleDefinitionManager() {
        loadAndValidate();
    }

    public static RoleDefinitionManager getInstance() {
        if (instance == null) {
            instance = new RoleDefinitionManager();
        }
        return instance;
    }

    private void loadAndValidate() {
        FileConfiguration cfg = ConfigService.getInstance().get(WeerWolvenConfigFiles.ROLE_DEFINITIONS).getCfg();
        boolean changed = false;

        for (Roles role : Roles.values()) {
            String key = role.name().toLowerCase();
            List<String> list = cfg.getStringList(key);
            if (list == null || list.isEmpty()) {
                list = Collections.singletonList(DEFAULT_LINE);
                cfg.set(key, list);
                changed = true;
            }
            // Normalize to ArrayList<String>
            definitions.put(role, new ArrayList<>(list));
        }

        if (changed) {
            try {
                ConfigService.getInstance().get(WeerWolvenConfigFiles.ROLE_DEFINITIONS).save();
            } catch (Exception e) {
                Logger.logMessage(LogLevel.ERROR, StandardPrefix.CONFIG_FILES, "Could not save role-definitions.yml: " + e.getMessage());
            }
        }

        Logger.logMessage(LogLevel.INFORMATION, StandardPrefix.CONFIG_FILES,
                "Loaded role definitions for roles: " + definitions.keySet().stream().map(Enum::name).collect(Collectors.joining(", ")));
    }

    public ArrayList<String> getRoleDefinition(Roles role) {
        return definitions.getOrDefault(role, new ArrayList<>(Collections.singletonList(DEFAULT_LINE)));
    }
}
