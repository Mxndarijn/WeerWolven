package me.mxndarijn.weerwolven.game.core;

import lombok.Getter;
import lombok.Setter;
import me.mxndarijn.weerwolven.data.Roles;
import me.mxndarijn.weerwolven.data.WeerWolvenPrefix;
import nl.mxndarijn.mxlib.item.MxSkullItemStackBuilder;
import nl.mxndarijn.mxlib.logger.LogLevel;
import nl.mxndarijn.mxlib.logger.Logger;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
public class RoleSet {

    private String id;
    private String name;
    private String skullId;
    private HashMap<Roles, Integer> roleSet = new HashMap<>();

    private RoleSet() {
    }

    public static RoleSet createEmpty(String name) {
        RoleSet rs = new RoleSet();
        rs.setId(UUID.randomUUID().toString());
        rs.setName(name);
        rs.setSkullId("question-mark");
        rs.setRoleSet(new HashMap<>());
        return rs;
    }

    public static Optional<RoleSet> load(ConfigurationSection configurationSection, File file) {
        if (configurationSection == null) {
            return Optional.empty();
        }
        RoleSet set = new RoleSet();
        // Section key is the ID
        set.setId(configurationSection.getName());
        // Human-readable name stored inside (fallback to id or legacy key name)
        String loadedName = configurationSection.getString("name", configurationSection.getName());
        set.setName(loadedName);

        // Load optional skull id
        String skull = configurationSection.getString("skull-id", null);
        if (skull != null && !skull.isEmpty()) {
            set.setSkullId(skull);
        }

        // Iterate over keys in this roleset section; each key should be a role enum name with integer count
        for (String key : configurationSection.getKeys(false)) {
            if (key.equalsIgnoreCase("skull-id") || key.equalsIgnoreCase("name")) continue; // reserved keys
            String path = configurationSection.getCurrentPath() + "." + key;
            String roleName = key;
            int amount = configurationSection.getInt(key, -1);
            if (amount < 0) {
                Logger.logMessage(LogLevel.ERROR, WeerWolvenPrefix.PRESETS_MANAGER, "Invalid or missing amount for role '" + roleName + "' at " + path + " (" + file.getAbsolutePath() + ")");
                continue;
            }
            Optional<Roles> roleOpt = getRoleByEnumNameIgnoreCase(roleName);
            if (roleOpt.isEmpty()) {
                Logger.logMessage(LogLevel.ERROR, WeerWolvenPrefix.PRESETS_MANAGER, "Unknown role '" + roleName + "' at " + path + " (" + file.getAbsolutePath() + ")");
                continue;
            }
            set.getRoleSet().put(roleOpt.get(), amount);
        }
        return Optional.of(set);
    }

    public void save(ConfigurationSection parentSection, File file) {
        if (parentSection == null) {
            return;
        }
        if (this.id == null || this.id.isEmpty()) {
            Logger.logMessage(LogLevel.ERROR, WeerWolvenPrefix.PRESETS_MANAGER, "Missing roleset ID while saving under: " + parentSection.getCurrentPath() + " (" + file.getAbsolutePath() + ")");
            return;
        }
        ConfigurationSection section = parentSection.createSection(this.id);
        // Save display name
        if (this.name != null && !this.name.isEmpty()) {
            section.set("name", this.name);
        }
        // Save optional skull-id for display icon
        if (this.skullId != null && !this.skullId.isEmpty()) {
            section.set("skull-id", this.skullId);
        }
        for (Map.Entry<Roles, Integer> entry : roleSet.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            section.set(entry.getKey().name(), entry.getValue());
        }
    }

    public MxSkullItemStackBuilder getSkullItemStackBuilder() {
        MxSkullItemStackBuilder builder = MxSkullItemStackBuilder.create(1);
        if (this.skullId != null && !this.skullId.isEmpty()) {
            builder.setSkinFromHeadsData(this.skullId);
        } else {
            builder.setSkinFromHeadsData("question-mark");
        }
        builder.setName("<gray>" + this.name)
                .addBlankLore()
                .addLore("<gray>Rollen: ");
        for (Map.Entry<Roles, Integer> entry : roleSet.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            builder.addLore("<dark_gray>- " + entry.getKey().getRoleWithColor() + "<gray>: " + entry.getValue());
        }
        return builder.addBlankLore();
    }

    private static Optional<Roles> getRoleByEnumNameIgnoreCase(String s) {
        for (Roles r : Roles.values()) {
            if (r.name().equalsIgnoreCase(s)) return Optional.of(r);
        }
        return Optional.empty();
    }
}
