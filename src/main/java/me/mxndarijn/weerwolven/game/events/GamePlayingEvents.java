package me.mxndarijn.weerwolven.game.events;

import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.PlayerSignCommandPreprocessEvent;
import me.mxndarijn.weerwolven.data.Interaction;
import me.mxndarijn.weerwolven.data.ItemTag;
import me.mxndarijn.weerwolven.data.Items;
import me.mxndarijn.weerwolven.data.UpcomingGameStatus;
import me.mxndarijn.weerwolven.data.WeerWolvenLanguageText;
import me.mxndarijn.weerwolven.game.Game;
import me.mxndarijn.weerwolven.game.GamePlayer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import nl.mxndarijn.mxlib.language.LanguageManager;
import nl.mxndarijn.mxlib.util.Functions;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;

import java.util.*;

public class GamePlayingEvents extends GameEvent {


    public GamePlayingEvents(Game g, JavaPlugin plugin) {
        super(g, plugin);
    }

    @EventHandler
    public void damage(PlayerArmorStandManipulateEvent e) {
        if (!validateWorld(e.getPlayer().getWorld()))
            return;
        if (e.getRightClicked().customName() == null)
            return;
        if (Functions.convertComponentToString(e.getRightClicked().customName()).equals("attachment")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void playerKilled(PlayerDeathEvent e) {
        Optional<GamePlayer> gamePlayer = game.getGamePlayerOfPlayer(e.getPlayer().getUniqueId());
        if (gamePlayer.isEmpty())
            return;
        gamePlayer.get().setAlive(false);
        game.addSpectatorSettings(e.getPlayer().getUniqueId(), e.getPlayer().getLocation());

        e.deathMessage(MiniMessage.miniMessage().deserialize("<!i>"));
        game.sendMessageToAll(LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_PLAYER_DIED, Collections.singletonList(e.getPlayer().getName())));
    }

    @EventHandler
    public void breakBlock(BlockBreakEvent e) {
        if (game.getGameInfo().getStatus() != UpcomingGameStatus.PLAYING)
            return;
        if (!validateWorld(e.getPlayer().getWorld()))
            return;

        Optional<GamePlayer> gamePlayer = game.getGamePlayerOfPlayer(e.getPlayer().getUniqueId());
        if (gamePlayer.isEmpty())
            return;
        if (!Items.isItemAGameItem(e.getPlayer().getInventory().getItemInMainHand())) {
            e.setCancelled(true);
        }

    }

    @EventHandler
    public void place(BlockPlaceEvent e) {
        if (game.getGameInfo().getStatus() != UpcomingGameStatus.PLAYING)
            return;
        if (!validateWorld(e.getPlayer().getWorld()))
            return;

        Optional<GamePlayer> gamePlayer = game.getGamePlayerOfPlayer(e.getPlayer().getUniqueId());
        if (gamePlayer.isEmpty())
            return;
        e.setCancelled(true);
    }

    @EventHandler
    public void interactSettings(PlayerInteractEvent e) {
        if (game.getGameInfo().getStatus() != UpcomingGameStatus.PLAYING)
            return;
        if (!validateWorld(e.getPlayer().getWorld()))
            return;

        Optional<GamePlayer> gamePlayer = game.getGamePlayerOfPlayer(e.getPlayer().getUniqueId());
        if (gamePlayer.isEmpty())
            return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.PHYSICAL)
            return;
        Material type = e.getClickedBlock().getType();
        if(!Interaction.isAllowedToInteract(type)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void damageEntity(EntityDamageByEntityEvent e) {
        if (game.getGameInfo().getStatus() != UpcomingGameStatus.PLAYING)
            return;
        if (!validateWorld(e.getEntity().getWorld()))
            return;
        Material type = null;
        if (e.getEntity() instanceof ItemFrame) {
            type = Material.ITEM_FRAME;
        }
        if (e.getEntity() instanceof GlowItemFrame) {
            type = Material.GLOW_ITEM_FRAME;
        }
        if (e.getEntity() instanceof ArmorStand) {
            type = Material.ARMOR_STAND;
        }
        if (type != null) {
            if (game.getGamePlayerOfPlayer(e.getDamager().getUniqueId()).isPresent())
                if(!Interaction.isAllowedToInteract(type)) {
                    e.setCancelled(true);
                }
        }

    }

    @EventHandler
    public void paintingBreak(HangingBreakByEntityEvent e) {
        if (game.getGameInfo().getStatus() != UpcomingGameStatus.PLAYING)
            return;
        if (!validateWorld(e.getEntity().getWorld()))
            return;
        if (game.getGamePlayerOfPlayer(e.getRemover().getUniqueId()).isPresent())
            if (e.getEntity() instanceof ItemFrame || e.getEntity() instanceof GlowItemFrame) {
                e.setCancelled(true);
            }
    }

    @EventHandler
    public void signChangeEvent(SignChangeEvent e) {
        if (game.getGameInfo().getStatus() != UpcomingGameStatus.PLAYING)
            return;
        if (!validateWorld(e.getPlayer().getWorld()))
            return;
        if (game.getGamePlayerOfPlayer(e.getPlayer().getUniqueId()).isPresent())
            e.setCancelled(true);
    }

    @EventHandler
    public void preProcessSignCommand(PlayerSignCommandPreprocessEvent e) {
        if (game.getGameInfo().getStatus() != UpcomingGameStatus.PLAYING)
            return;
        if (!validateWorld(e.getPlayer().getWorld()))
            return;
        if (game.getGamePlayerOfPlayer(e.getPlayer().getUniqueId()).isPresent())
            e.setCancelled(true);
    }

    @EventHandler
    public void onSignClick(PlayerInteractEvent e) {
        if (game.getGameInfo().getStatus() != UpcomingGameStatus.PLAYING)
            return;
        if (!validateWorld(e.getPlayer().getWorld()))
            return;

        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block b = e.getClickedBlock();
        if (b != null) {
            if (b.getType().name().toLowerCase().contains("sign")) {
                if (game.getGamePlayerOfPlayer(e.getPlayer().getUniqueId()).isPresent())
                    e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent e) {
        Player p = e.getPlayer();
        if (game.getGameInfo().getStatus() != UpcomingGameStatus.PLAYING)
            return;
        if (!validateWorld(p.getWorld()))
            return;

        if (e.getRightClicked() instanceof Lectern) {
            ItemStack item = p.getInventory().getItemInMainHand();

            if (item.getType() == Material.WRITTEN_BOOK || item.getType() == Material.BOOK) {
                if (game.getGamePlayerOfPlayer(p.getUniqueId()).isPresent()) {
                    ItemMeta im = item.getItemMeta();
                    PersistentDataContainer container = im.getPersistentDataContainer();
                    String data = container.get(new NamespacedKey(game.getPlugin(), "undroppable"), PersistentDataType.STRING);

                    if (data != null && data.equalsIgnoreCase("true"))
                        e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void paintingBreak(PlayerInteractEntityEvent e) {
        if (game.getGameInfo().getStatus() != UpcomingGameStatus.PLAYING)
            return;
        if (!validateWorld(e.getRightClicked().getWorld()))
            return;
        Material type = null;
        if (e.getRightClicked() instanceof ItemFrame) {
            type = Material.ITEM_FRAME;
        }
        if (e.getRightClicked() instanceof GlowItemFrame) {
            type = Material.GLOW_ITEM_FRAME;
        }
        if (type != null) {
            if (game.getGamePlayerOfPlayer(e.getRightClicked().getUniqueId()).isPresent())
                if(!Interaction.isAllowedToInteract(type)) {
                    e.setCancelled(true);
                }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void chat(AsyncChatEvent e) {
        Player p = e.getPlayer();
        if (e.isCancelled())
            return;
        if (!validateWorld(p.getWorld()))
            return;
        Optional<GamePlayer> optionalGamePlayer = game.getGamePlayerOfPlayer(p.getUniqueId());
        if (optionalGamePlayer.isEmpty())
            return;
        if (!optionalGamePlayer.get().isAlive())
            return;

        e.setCancelled(true);
        GamePlayer gp = optionalGamePlayer.get();
        game.sendMessageToAll(LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_CHAT_PLAYER, Arrays.asList(gp.getColorData().getColor().getDisplayName(), p.getName(), Functions.convertComponentToString(e.message()))));
    }

    @EventHandler
    public void chatHost(AsyncChatEvent e) {
        Player p = e.getPlayer();
        if (!validateWorld(p.getWorld()))
            return;
        if (!game.getHosts().contains(p.getUniqueId()))
            return;

        e.setCancelled(true);
        game.sendMessageToAll(LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_CHAT_HOST, Arrays.asList(p.getName(), Functions.convertComponentToString(e.message()))));
    }

    @EventHandler
    public void playerDeadEvent(PlayerDeathEvent e) {
        Player p = e.getPlayer();
        if (!validateWorld(e.getPlayer().getWorld()))
            return;
        Optional<GamePlayer> optionalGamePlayer = game.getGamePlayerOfPlayer(p.getUniqueId());
        if (optionalGamePlayer.isEmpty())
            return;

        if (game.getHosts().contains(p.getUniqueId())) {
            e.setCancelled(true);
        }

        ArrayList<ItemStack> drops = new ArrayList<>(e.getDrops());
        for (ItemStack item : drops) {
            if (item.getItemMeta() == null)
                continue;
            String vanish = item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, ItemTag.VANISHABLE.getPersistentDataTag()), PersistentDataType.STRING);
            if ((vanish != null && vanish.equalsIgnoreCase("false"))) {
                e.getDrops().remove(item);
            }
        }
    }

    // Rune code
    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        CommandSender sender = event.getSender();

        // Alleen command blocks
        if (!(sender instanceof BlockCommandSender blockSender)) return;

        Location loc = blockSender.getBlock().getLocation();

        String command = event.getCommand();

        if (command.contains("@p")) {
            Player nearest = getNearestPlayer(loc);
            if (nearest != null) {
                String newCommand = command.replace("@p", nearest.getName());
                event.setCommand(newCommand);
            } else {
                // Geen speler in deze wereld
                event.setCancelled(true);
            }
        }
    }

    private Player getNearestPlayer(Location loc) {
        Player nearest = null;
        double minDistSq = Double.MAX_VALUE;

        for (Player player : loc.getWorld().getPlayers()) {
            double distSq = player.getLocation().distanceSquared(loc);
            if (distSq < minDistSq) {
                minDistSq = distSq;
                nearest = player;
            }
        }

        return nearest;
    }

    @EventHandler
    public void onArmorStandInteract(PlayerArmorStandManipulateEvent e) {
        if (!validateWorld(e.getPlayer().getWorld())) return;
        if (game.getGameInfo().getStatus() != UpcomingGameStatus.PLAYING) return;
        Optional<GamePlayer> optionalGamePlayer = game.getGamePlayerOfPlayer(e.getPlayer().getUniqueId());
        if (optionalGamePlayer.isEmpty())
            return;
        if (!optionalGamePlayer.get().isAlive())
            return;

        boolean allowed = Interaction.isAllowedToInteract(Material.ARMOR_STAND);
        if (!allowed) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onArmorStandClick(PlayerInteractAtEntityEvent e) {
        if (!(e.getRightClicked() instanceof ArmorStand)) return;
        if (!validateWorld(e.getPlayer().getWorld())) return;
        if (game.getGameInfo().getStatus() != UpcomingGameStatus.PLAYING) return;
        Optional<GamePlayer> optionalGamePlayer = game.getGamePlayerOfPlayer(e.getPlayer().getUniqueId());
        if (optionalGamePlayer.isEmpty())
            return;
        if (!optionalGamePlayer.get().isAlive())
            return;

        boolean allowed = Interaction.isAllowedToInteract(Material.ARMOR_STAND);

        if (!allowed) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onArmorStandDamageByEntity(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof ArmorStand)) return;
        if (!validateWorld(e.getEntity().getWorld())) return;
        if (game.getGameInfo().getStatus() != UpcomingGameStatus.PLAYING) return;

        Player damagerPlayer = null;

        if (e.getDamager() instanceof Player p) {
            damagerPlayer = p;
        } else if (e.getDamager() instanceof Projectile proj) {
            ProjectileSource shooter = proj.getShooter();
            if (shooter instanceof Player p) {
                damagerPlayer = p;
            }
        }

        if (damagerPlayer == null) return;

        Optional<GamePlayer> optionalGamePlayer = game.getGamePlayerOfPlayer(damagerPlayer.getUniqueId());
        if (optionalGamePlayer.isEmpty()) return;
        if (!optionalGamePlayer.get().isAlive()) return;

        boolean allowed = Interaction.isAllowedToInteract(Material.ARMOR_STAND);

        if (!allowed) {
            e.setCancelled(true);
        }
    }
}
