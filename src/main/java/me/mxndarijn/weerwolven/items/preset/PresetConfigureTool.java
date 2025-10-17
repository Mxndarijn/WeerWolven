package me.mxndarijn.weerwolven.items.preset;

import me.mxndarijn.weerwolven.WeerWolven;
import me.mxndarijn.weerwolven.data.Colors;
import me.mxndarijn.weerwolven.data.WeerWolvenChatPrefix;
import me.mxndarijn.weerwolven.data.WeerWolvenLanguageText;
import me.mxndarijn.weerwolven.items.WeerWolvenMxItem;
import me.mxndarijn.weerwolven.managers.PresetsManager;
import me.mxndarijn.weerwolven.presets.ColorData;
import me.mxndarijn.weerwolven.presets.Preset;
import me.mxndarijn.weerwolven.presets.PresetConfig;
import nl.mxndarijn.mxlib.changeworld.MxChangeWorld;
import nl.mxndarijn.mxlib.changeworld.MxChangeWorldManager;
import nl.mxndarijn.mxlib.chatinput.MxChatInputManager;
import nl.mxndarijn.mxlib.inventory.*;
import nl.mxndarijn.mxlib.inventory.heads.MxHeadManager;
import nl.mxndarijn.mxlib.inventory.heads.MxHeadSection;
import nl.mxndarijn.mxlib.inventory.menu.MxDefaultMenuBuilder;
import nl.mxndarijn.mxlib.inventory.menu.MxListInventoryBuilder;
import nl.mxndarijn.mxlib.item.MxDefaultItemStackBuilder;
import nl.mxndarijn.mxlib.item.MxSkullItemStackBuilder;
import nl.mxndarijn.mxlib.item.Pair;
import nl.mxndarijn.mxlib.language.LanguageManager;
import nl.mxndarijn.mxlib.logger.LogLevel;
import nl.mxndarijn.mxlib.logger.Logger;
import nl.mxndarijn.mxlib.logger.StandardPrefix;
import nl.mxndarijn.mxlib.mxworld.MxLocation;
import nl.mxndarijn.mxlib.util.MessageUtil;
import nl.mxndarijn.mxlib.util.MxWorldFilter;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PresetConfigureTool extends WeerWolvenMxItem {

    private enum EditMode { NONE, WINDOW, DOOR }
    private static class EditSession {
        final Preset preset;
        final PresetConfig config;
        final Colors color;
        EditMode mode;
        BukkitTask task;
        EditSession(Preset preset, PresetConfig config, Colors color, EditMode mode) {
            this.preset = preset; this.config = config; this.color = color; this.mode = mode; this.task = null;
        }
    }
    private static final Map<UUID, EditSession> EDIT_STATE = new HashMap<>();

    public PresetConfigureTool(ItemStack is, MxWorldFilter worldFilter, boolean gameItem, Action... actions) {
        super(is, worldFilter, gameItem, actions);

        MxChangeWorldManager.getInstance().addUnspecificWorld(new MxChangeWorld() {

            @Override
            public void enter(Player player, World world, PlayerChangedWorldEvent playerChangedWorldEvent) {

            }

            @Override
            public void leave(Player player, World world, PlayerChangedWorldEvent playerChangedWorldEvent) {
                if (EDIT_STATE.containsKey(player.getUniqueId())) {

                    EditSession s = EDIT_STATE.get(player.getUniqueId());
                    if (s.preset.getMxWorld().isPresent() && s.preset.getMxWorld().get().getWorldUID() != null) {
                        if (world.getUID().equals(
                                s.preset.getMxWorld().get().getWorldUID()
                        )) {
                            stopEditing(player.getUniqueId(), true);
                        }
                    }
                }
            }

            @Override
            public void quit(Player player, World world, PlayerQuitEvent playerQuitEvent) {
                if (EDIT_STATE.containsKey(player.getUniqueId())) {

                    EditSession s = EDIT_STATE.get(player.getUniqueId());
                    if (s.preset.getMxWorld().isPresent() && s.preset.getMxWorld().get().getWorldUID() != null) {
                        if (world.getUID().equals(
                                s.preset.getMxWorld().get().getWorldUID()
                        )) {
                            stopEditing(player.getUniqueId(), true);
                        }
                    }
                }
            }
        });
    }

    private String getEditStatusLore(UUID uuid, Preset preset, Colors color, EditMode mode) {
        EditSession s = EDIT_STATE.get(uuid);
        boolean on = s != null && s.preset.equals(preset) && s.color == color && s.mode == mode;
        return on ? "<gray>Status: <green>Actief" : "<gray>Status: <red>Uit";
    }

    private void stopEditing(UUID uuid, boolean save) {
        EditSession s = EDIT_STATE.remove(uuid);
        if (s != null) {
            if (s.task != null) {
                try { s.task.cancel(); } catch (Exception ignored) {}
                s.task = null;
            }
            if (save) {
                s.config.save();
            }
        }
    }

    private void toggleWindowEdit(Player p, Preset preset, PresetConfig config, Colors color) {
        UUID id = p.getUniqueId();
        EditSession current = EDIT_STATE.get(id);
        if (current != null) {
            // If switching mode/color/preset, stop previous and save
            boolean same = current.preset.equals(preset) && current.color == color && current.mode == EditMode.WINDOW;
            if (same) {
                stopEditing(id, true);
                MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.PRESET_CONFIGURE_TOOL_WINDOWS_EDIT_DISABLED));
                return;
            }
            stopEditing(id, true);
        }
        EditSession session = new EditSession(preset, config, color, EditMode.WINDOW);
        EDIT_STATE.put(id, session);
        startVisualization(p, session);
        MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.PRESET_CONFIGURE_TOOL_WINDOWS_EDIT_ENABLED, Collections.singletonList(color.getColor() + color.getDisplayNameWithoutColor())));
        p.closeInventory();
    }

    private void toggleDoorEdit(Player p, Preset preset, PresetConfig config, Colors color) {
        UUID id = p.getUniqueId();
        EditSession current = EDIT_STATE.get(id);
        if (current != null) {
            boolean same = current.preset.equals(preset) && current.color == color && current.mode == EditMode.DOOR;
            if (same) {
                stopEditing(id, true);
                MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.PRESET_CONFIGURE_TOOL_DOORS_EDIT_DISABLED));
                return;
            }
            stopEditing(id, true);
        }
        EditSession session = new EditSession(preset, config, color, EditMode.DOOR);
        EDIT_STATE.put(id, session);
        startVisualization(p, session);
        MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.PRESET_CONFIGURE_TOOL_DOORS_EDIT_ENABLED, Collections.singletonList(color.getColor() + color.getDisplayNameWithoutColor())));
        p.closeInventory();
    }

    private boolean isDoor(Material m) {
        String n = m.name();
        return n.endsWith("_DOOR");
    }

    private void startVisualization(Player player, EditSession session) {
        // Cancel existing task if present
        if (session.task != null) {
            try { session.task.cancel(); } catch (Exception ignored) {}
            session.task = null;
        }
        // Choose DUST particle colors per mode
        final Particle.DustOptions windowDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(40, 200, 255), 1.0F);
        final Particle.DustOptions doorDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 90, 40), 1.0F);

        session.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Ensure still editing this session
            EditSession current = EDIT_STATE.get(player.getUniqueId());
            if (current == null || current != session) {
                if (session.task != null) { try { session.task.cancel(); } catch (Exception ignored) {} }
                return;
            }
            Optional<ColorData> cdOpt = session.config.getColor(session.color);
            if (cdOpt.isEmpty()) return;
            ColorData cd = cdOpt.get();
            World world = player.getWorld();
            List<MxLocation> list = (session.mode == EditMode.WINDOW) ? cd.getWindowLocations() : cd.getDoorLocations();
            if (list == null || list.isEmpty()) return;
            Particle.DustOptions dust = (session.mode == EditMode.WINDOW) ? windowDust : doorDust;
            for (MxLocation mx : list) {
                if (mx == null) continue;
                Location l = mx.getLocation(world);
                if (l == null) continue;
                int bx = l.getBlockX();
                int by = l.getBlockY();
                int bz = l.getBlockZ();
                // Draw an outlined cube for the block with fewer particles
                drawOutlinedCube(world, bx, by, bz, dust);
                // Add a small center marker using the same dust
                world.spawnParticle(Particle.DUST, bx + 0.5, by + 0.5, bz + 0.5, 2, 0, 0, 0, 0, dust);
            }
        }, 0L, 20L); // every second
    }

    private void drawOutlinedCube(World world, int bx, int by, int bz, Particle.DustOptions dust) {
        double x = bx;
        double y = by;
        double z = bz;
        double x1 = x + 1.0;
        double y1 = y + 1.0;
        double z1 = z + 1.0;
        double step = 0.33;
        // bottom square
        drawLine(world, x, y, z, x1, y, z, step, dust);
        drawLine(world, x1, y, z, x1, y, z1, step, dust);
        drawLine(world, x1, y, z1, x, y, z1, step, dust);
        drawLine(world, x, y, z1, x, y, z, step, dust);
        // top square
        drawLine(world, x, y1, z, x1, y1, z, step, dust);
        drawLine(world, x1, y1, z, x1, y1, z1, step, dust);
        drawLine(world, x1, y1, z1, x, y1, z1, step, dust);
        drawLine(world, x, y1, z1, x, y1, z, step, dust);
        // verticals
        drawLine(world, x, y, z, x, y1, z, step, dust);
        drawLine(world, x1, y, z, x1, y1, z, step, dust);
        drawLine(world, x, y, z1, x, y1, z1, step, dust);
        drawLine(world, x1, y, z1, x1, y1, z1, step, dust);
    }

    private void drawLine(World world, double sx, double sy, double sz,
                          double ex, double ey, double ez,
                          double step, Particle.DustOptions dust) {
        double dx = ex - sx;
        double dy = ey - sy;
        double dz = ez - sz;
        double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
        int points = Math.max(2, (int) Math.ceil(dist / step));
        double ix = dx / points;
        double iy = dy / points;
        double iz = dz / points;
        double x = sx;
        double y = sy;
        double z = sz;
        for (int i = 0; i <= points; i++) {
            world.spawnParticle(Particle.DUST, x, y, z, 1, 0, 0, 0, 0, dust);
            x += ix; y += iy; z += iz;
        }
    }

    @Override
    public void execute(Player p, PlayerInteractEvent e) {
        Optional<Preset> optionalPreset = PresetsManager.getInstance().getPresetByWorldUID(e.getPlayer().getWorld().getUID());
        if (optionalPreset.isEmpty()) {
            return;
        }
        Preset preset = optionalPreset.get();
        PresetConfig config = preset.getConfig();

        MxInventoryManager.getInstance().addAndOpenInventory(p, MxDefaultMenuBuilder.create("<gray>Preset Configure-Tool", MxInventorySlots.THREE_ROWS)
                .setItem(MxDefaultItemStackBuilder.create(Material.ENDER_PEARL)
                                .setName("<gray>Verander spawn locatie")
                                .addBlankLore()
                                .addLore("<yellow>Verander de spawn locatie naar je huidige locatie.")
                                .build(),
                        13,
                        (mxInv1, e12) -> {
                            File settings = new File(preset.getDirectory(), "worldsettings.yml");
                            if (settings.exists()) {
                                FileConfiguration fc = YamlConfiguration.loadConfiguration(settings);
                                Location l = p.getLocation();
                                ConfigurationSection section = fc.createSection("spawn");
                                section.set("x", l.getBlockX());
                                section.set("y", l.getBlockY());
                                section.set("z", l.getBlockZ());
                                try {
                                    fc.save(settings);
                                    MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.PRESET_CONFIGURE_TOOL_SPAWN_CHANGED));
                                } catch (IOException ex) {
                                    Logger.logMessage(LogLevel.ERROR, StandardPrefix.CONFIG_FILES, "Could not save file. (" + settings.getAbsolutePath() + ")");
                                    ex.printStackTrace();
                                }
                                p.closeInventory();
                            } else {
                                Logger.logMessage(LogLevel.ERROR, StandardPrefix.CONFIG_FILES, "Could not find file. (" + settings.getAbsolutePath() + ")");
                            }
                        }
                )
                .setItem(getSkull(config),
                        9,
                        (mainInv, clickMain) -> {
                            MxHeadManager mxHeadManager = MxHeadManager.getInstance();
                            ArrayList<Pair<ItemStack, MxItemClicked>> list = new ArrayList<>();
                            MxItemClicked clicked = (mxInv, e1) -> {
                                ItemStack is = e1.getCurrentItem();
                                ItemMeta im = is.getItemMeta();
                                PersistentDataContainer container = im.getPersistentDataContainer();
                                String key = container.get(new NamespacedKey(JavaPlugin.getPlugin(WeerWolven.class), "skull_key"), PersistentDataType.STRING);

                                Optional<MxHeadSection> section = MxHeadManager.getInstance().getHeadSection(key);
                                if (section.isPresent() && section.get().getNameOptional().isPresent()) {
                                    MessageUtil.sendMessageToPlayer(clickMain.getWhoClicked(), WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.PRESET_CONFIGURE_TOOL_SKULL_CHANGED, Collections.singletonList(section.get().getNameOptional().get())));
                                }
                                config.setSkullId(key);
                                config.save();
                                mainInv.getInv().setItem(9, getSkull(config));
                                MxInventoryManager.getInstance().addAndOpenInventory(p, mainInv);

                            };

                            MxHeadManager.getInstance().getAllHeadKeys().forEach(key -> {
                                Optional<MxHeadSection> section = mxHeadManager.getHeadSection(key);
                                section.ifPresent(mxHeadSection -> {
                                    MxSkullItemStackBuilder b = MxSkullItemStackBuilder.create(1)
                                            .setSkinFromHeadsData(key)
                                            .setName("<gray>" + mxHeadSection.getNameOptional().get())
                                            .addBlankLore()
                                            .addLore("<yellow>Klik om de skull te selecteren.")
                                            .addCustomTagString("skull_key", mxHeadSection.getKey());
                                    list.add(new Pair<>(b.build(), clicked));
                                });
                            });

                            MxInventoryManager.getInstance().addAndOpenInventory(p,
                                    MxListInventoryBuilder.create("<gray>Preset Configure-Tool", MxInventorySlots.SIX_ROWS)
                                            .setAvailableSlots(MxInventoryIndex.ROW_ONE_TO_FIVE)
                                            .addListItems(list)
                                            .setItem(MxDefaultItemStackBuilder.create(Material.PAPER)
                                                    .setName("<gray>Info")
                                                    .addBlankLore()
                                                    .addLore("<yellow>Klik op de skull om dat de nieuwe skull van de preset te maken.")
                                                    .build(), 48, null)
                                            .setPrevious(mainInv)
                                            .build());
                        })
                .setItem(getNameItemStack(config),
                        10,
                        (mainInv, clickMain) -> {
                            MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.PRESET_CONFIGURE_TOOL_ENTER_NEW_NAME, WeerWolvenChatPrefix.DEFAULT));
                            p.closeInventory();
                            MxChatInputManager.getInstance().addChatInputCallback(p.getUniqueId(), message -> {
                                config.setName(message);
                                config.save();
                                MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.PRESET_CONFIGURE_TOOL_NAME_CHANGED, Collections.singletonList(message), WeerWolvenChatPrefix.DEFAULT));
                                mainInv.getInv().setItem(10, getNameItemStack(config));
                                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                                    MxInventoryManager.getInstance().addAndOpenInventory(p.getUniqueId(), mainInv);
                                });
                            });
                        })
                .setItem(MxDefaultItemStackBuilder.create(Material.LIGHT_BLUE_SHULKER_BOX)
                                .setName("<gray>Kleuren")
                                .addBlankLore()
                                .addLore("<yellow>Klik hier om de kleuren van de preset te bekijken en te veranderen.")
                                .build(),
                        14,
                        (mainInv, clickMain) -> {
                            openColorsMenu(p, mainInv, preset, config);
                        })
                .setItem(getConfiguredItemStack(config),
                        22,
                        (mainInv, clickMain) -> {
                            config.setConfigured(!config.isConfigured());
                            mainInv.getInv().setItem(22, getConfiguredItemStack(config));
                            config.save();
                        })
                .build());
    }

    @Override
    public void executeOnBreak(Player p, BlockBreakEvent e) {

    }

    private ItemStack getConfiguredItemStack(PresetConfig config) {
        return MxDefaultItemStackBuilder.create(Material.ANVIL)
                .setName("<gray>Toggle configured")
                .addBlankLore()
                .addLore("<gray>Status: " + (config.isConfigured() ? "<green>Geconfigureerd" : "<red>Niet geconfigureerd"))
                .addLore("<yellow>Klik hier om de configuratie te togglen.")
                .addLore("<yellow>Geconfigueerd: Spelers kunnen er een map voor maken.")
                .addLore("<yellow>Niet geconfigueerd: Spelers ziet de preset niet.")
                .build();
    }

    private ItemStack getNameItemStack(PresetConfig config) {
        return MxDefaultItemStackBuilder.create(Material.NAME_TAG)
                .setName("<gray>Verander naam")
                .addBlankLore()
                .addLore("<gray>Status: " + config.getName())
                .addBlankLore()
                .addLore("<yellow>Klik hier om de naam van de preset te veranderen.")
                .addLore("<yellow>Vervolgens moet je in de chat de nieuwe naam sturen.")
                .build();
    }

    private ItemStack getSkull(PresetConfig config) {
        return MxDefaultItemStackBuilder.create(Material.SKELETON_SKULL)
                .setName("<gray>Verander skull")
                .addBlankLore()
                .addLore("<gray>Status: " + (MxHeadManager.getInstance().getHeadSection(config.getSkullId()).isPresent() ? MxHeadManager.getInstance().getHeadSection(config.getSkullId()).get().getNameOptional().get() : "Niet-gevonden"))
                .addBlankLore()
                .addLore("<yellow>Klik hier om de skull van de preset te veranderen.")
                .addLore("<yellow>Je krijgt een lijst met skulls van het commands /skulls.")
                .build();
    }

    private void openColorsMenu(Player p, MxInventory mainInv, Preset preset, PresetConfig config) {
        MxDefaultMenuBuilder builder = MxDefaultMenuBuilder.create("<gray>Preset Configure-Tool", MxInventorySlots.THREE_ROWS)
                .setPrevious(mainInv);
        List<ColorData> colors = config.getColors();
        for (Colors c : Colors.values()) {
            builder.addItem(
                    getColorItemStack(c, config),
                    (mxInv, e) -> {
                        if (preset.containsColor(c)) {
                            Optional<ColorData> optionalColorData = preset.getConfig().getColor(c);
                            if (optionalColorData.isEmpty()) {
                                return;
                            }
                            ColorData colorData = optionalColorData.get();
                            // Remove color
                            MxInventoryManager.getInstance().addAndOpenInventory(p, MxDefaultMenuBuilder.create("<gray>Preset Configure-Tool", MxInventorySlots.THREE_ROWS)
                                    .setPrevious(mxInv)
                                    .setItem(
                                            MxSkullItemStackBuilder.create(1)
                                                    .setSkinFromHeadsData("red-minus")
                                                    .setName("<red>Verwijder kleur")
                                                    .addBlankLore()
                                                    .addLore("<yellow>Klik hier om de kleur te verwijderen.")
                                                    .build(),
                                            11,
                                            (mxInv1, e1) -> {
                                                colors.remove(colorData);
                                                config.save();
                                                p.closeInventory();
                                                MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.PRESET_CONFIGURE_TOOL_COLOR_REMOVED, Collections.singletonList(c.getColor() + c.getDisplayName())));
                                            })
                                    .setItem(
                                            MxDefaultItemStackBuilder.create(Material.ENDER_PEARL)
                                                    .setName("<gray>Verander Spawnpoint")
                                                    .addBlankLore()
                                                    .addLore("<yellow>Klik hier om de spawnpoint aan te passen.")
                                                    .build(),
                                            15,
                                            (mxInv1, e1) -> {
                                                MxLocation location = MxLocation.getFromLocation(p.getLocation());
                                                colorData.setSpawnLocation(location);
                                                config.save();
                                                p.closeInventory();
                                                MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.PRESET_CONFIGURE_TOOL_COLOR_SPAWNPOINT_CHANGED, Collections.singletonList(c.getColor() + c.getDisplayName())));
                                            })
                                    .setItem(
                                            MxDefaultItemStackBuilder.create(Material.COMPASS)
                                                    .setName("<gray>Teleporteer naar Spawnpoint")
                                                    .addBlankLore()
                                                    .addLore("<yellow>Klik hier om naar de spawnpoint te teleporteren.")
                                                    .build(),
                                            13,
                                            (mxInv1, e1) -> {
                                                p.teleport(colorData.getSpawnLocation().getLocation(p.getWorld()));
                                                p.closeInventory();
                                                MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.PRESET_CONFIGURE_TOOL_COLOR_TELEPORTED, Collections.singletonList(c.getColor() + c.getDisplayName())));
                                            })
                                    .setItem(MxDefaultItemStackBuilder.create(Material.GLASS)
                                                    .setName("<gray>Toggle ramen-bewerken")
                                                    .addBlankLore()
                                                    .addLore("<yellow>Klik om ramen-plaatsen te bewerken.")
                                                    .addLore(getEditStatusLore(p.getUniqueId(), preset, c, EditMode.WINDOW))
                                                    .build(),
                                            20,
                                            (mxInv1, e1) -> {
                                                toggleWindowEdit(p, preset, config, c);
                                            })
                                    .setItem(MxDefaultItemStackBuilder.create(Material.OAK_DOOR)
                                                    .setName("<gray>Toggle deuren-bewerken")
                                                    .addBlankLore()
                                                    .addLore("<yellow>Klik om deur-locaties te bewerken.")
                                                    .addLore(getEditStatusLore(p.getUniqueId(), preset, c, EditMode.DOOR))
                                                    .build(),
                                            24,
                                            (mxInv1, e1) -> {
                                                toggleDoorEdit(p, preset, config, c);
                                            })
                                    .setItem(MxSkullItemStackBuilder.create(1)
                                                    .setSkinFromHeadsData(c.getHeadKey())
                                                    .setName(c.getColor() + c.getDisplayName())
                                                    .build(),
                                            22,
                                            null)
                                    .build()
                            );

                        } else {
                            // Add Color
                            MxLocation location = MxLocation.getFromLocation(p.getLocation());
                            colors.add(ColorData.createNew(c,location));
                            config.save();
                            MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.PRESET_CONFIGURE_TOOL_COLOR_ADDED, Collections.singletonList(c.getColor() + c.getDisplayName())));
                            p.closeInventory();
                        }
                    }
            );
        }

        MxInventoryManager.getInstance().addAndOpenInventory(p, builder.build());
    }


    private boolean toggleInList(List<MxLocation> list, MxLocation loc) {
        if (list == null) return false;
        for (Iterator<MxLocation> it = list.iterator(); it.hasNext(); ) {
            MxLocation existing = it.next();
            if (existing != null && existing.equals(loc)) {
                it.remove();
                return false; // removed
            }
        }
        list.add(loc);
        return true; // added
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        EditSession s = EDIT_STATE.get(p.getUniqueId());
        if (s == null || s.mode != EditMode.DOOR) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null) return;
        Material type = e.getClickedBlock().getType();
        if (!isDoor(type)) return;
        Optional<ColorData> cdOpt = s.config.getColor(s.color);
        if (cdOpt.isEmpty()) {
            MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.PRESET_CONFIGURE_TOOL_COLOR_DATA_NOT_FOUND));
            stopEditing(p.getUniqueId(), false);
            return;
        }
        ColorData cd = cdOpt.get();
        MxLocation loc = MxLocation.getFromLocation(e.getClickedBlock().getLocation());
        boolean added = toggleInList(cd.getDoorLocations(), loc);
        s.config.save();
        MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(added ? WeerWolvenLanguageText.PRESET_CONFIGURE_TOOL_DOOR_ADDED : WeerWolvenLanguageText.PRESET_CONFIGURE_TOOL_DOOR_REMOVED));
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        EditSession s = EDIT_STATE.get(p.getUniqueId());
        if (s == null || s.mode != EditMode.WINDOW) return;
        if(s.preset.getMxWorld().isEmpty())
            return;
        if(!s.preset.getMxWorld().get().getWorldUID().equals(e.getBlock().getWorld().getUID()))
            return;
        e.setCancelled(true);
        Optional<ColorData> cdOpt = s.config.getColor(s.color);
        if (cdOpt.isEmpty()) {
            MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.PRESET_CONFIGURE_TOOL_COLOR_DATA_NOT_FOUND));
            stopEditing(p.getUniqueId(), false);
            return;
        }
        ColorData cd = cdOpt.get();
        MxLocation loc = MxLocation.getFromLocation(e.getBlock().getLocation());
        boolean added = toggleInList(cd.getWindowLocations(), loc);
        s.config.save();
        MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(added ? WeerWolvenLanguageText.PRESET_CONFIGURE_TOOL_WINDOW_ADDED : WeerWolvenLanguageText.PRESET_CONFIGURE_TOOL_WINDOW_REMOVED));
    }

    private ItemStack getColorItemStack(Colors c, PresetConfig config) {
        return MxSkullItemStackBuilder.create(1)
                .setSkinFromHeadsData(c.getHeadKey())
                .setName(c.getColor() + c.getDisplayName())
                .addBlankLore()
                .addLore(config.containsColor(c) ? "<green>Kleur is toegevoegd" : "<red>Kleur is niet toegevoegd")
                .addBlankLore()
                .addLore(config.containsColor(c) ? "<yellow>Klik om kleur te beheren" : "<yellow>Klik om kleur toe te voegen")
                .build();
    }
}
