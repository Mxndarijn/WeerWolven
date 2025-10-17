package me.mxndarijn.weerwolven.game.core;

import lombok.Getter;
import me.mxndarijn.weerwolven.WeerWolven;
import me.mxndarijn.weerwolven.data.*;
import me.mxndarijn.weerwolven.game.bus.AutoCloseableGroup;
import me.mxndarijn.weerwolven.game.bus.GameEventBus;
import me.mxndarijn.weerwolven.game.events.minecraft.*;
import me.mxndarijn.weerwolven.game.manager.GameChatManager;
import me.mxndarijn.weerwolven.game.manager.GameHouseManager;
import me.mxndarijn.weerwolven.game.manager.GameVisibilityManager;
import me.mxndarijn.weerwolven.game.manager.GameVoteManager;
import me.mxndarijn.weerwolven.game.phase.Phase;
import me.mxndarijn.weerwolven.game.phase.PhaseExecutor;
import me.mxndarijn.weerwolven.game.phase.PhaseHooks;
import me.mxndarijn.weerwolven.game.phase.DefaultPhaseHooks;
import me.mxndarijn.weerwolven.game.phase.DayNightCycleManager;
import me.mxndarijn.weerwolven.game.runtime.KillQueue;
import me.mxndarijn.weerwolven.game.runtime.LoversChainListener;
import me.mxndarijn.weerwolven.game.runtime.MayorVoteWeightListener;
import me.mxndarijn.weerwolven.game.timer.ActionTimerService;
import me.mxndarijn.weerwolven.game.timer.TimerContext;
import me.mxndarijn.weerwolven.game.timer.TimerFormats;
import me.mxndarijn.weerwolven.managers.*;
import me.mxndarijn.weerwolven.presets.Preset;
import me.mxndarijn.weerwolven.presets.PresetConfig;
import nl.mxndarijn.mxlib.changeworld.MxChangeWorld;
import nl.mxndarijn.mxlib.changeworld.MxChangeWorldManager;
import nl.mxndarijn.mxlib.language.LanguageManager;
import nl.mxndarijn.mxlib.logger.Logger;
import nl.mxndarijn.mxlib.mxscoreboard.MxSupplierScoreBoard;
import nl.mxndarijn.mxlib.mxworld.MxAtlas;
import nl.mxndarijn.mxlib.mxworld.MxWorld;
import nl.mxndarijn.mxlib.util.Functions;
import nl.mxndarijn.mxlib.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static nl.mxndarijn.mxlib.util.Functions.formatGameTime;

@Getter
public class Game {
    private Phase phase = Phase.LOBBY;
    private int dayNumber = 0;

    private final GameInfo gameInfo;
    private final UUID mainHost;

    private final File directory;

    @Nullable
    private MxWorld mxWorld;

    private final ArrayList<UUID> hosts;
    private final PresetConfig config;
    private final List<GamePlayer> gamePlayers;
    private final GameEventBus gameEventBus = new GameEventBus();
    private final ActionTimerService actionTimerService = new ActionTimerService(this);
    private final JavaPlugin plugin;

    private final MxSupplierScoreBoard hostScoreboard;
    private final MxSupplierScoreBoard spectatorScoreboard;

    private final HashMap<UUID, Location> respawnLocations;

    private final List<UUID> spectators;

    private long gameTime = 0;
    private List<Listener> events;
    private boolean firstStart = false;
    private BukkitTask updateGameUpdater;

    private final KillQueue killQueue = new KillQueue();
    private final AutoCloseableGroup busSubs = new AutoCloseableGroup();
    
    // Phase execution system
    private final PhaseHooks phaseHooks = new DefaultPhaseHooks();
    private final PhaseExecutor phaseExecutor;
    private final DayNightCycleManager cycleManager;
    // Orchestration components
    private final me.mxndarijn.weerwolven.game.orchestration.IntentCollector intentCollector = new me.mxndarijn.weerwolven.game.orchestration.IntentCollector();
    private final me.mxndarijn.weerwolven.game.orchestration.OrchestrationConfig orchestrationConfig = new me.mxndarijn.weerwolven.game.orchestration.OrchestrationConfig();
    private final me.mxndarijn.weerwolven.game.orchestration.DefaultDecisionPolicy defaultPolicy = new me.mxndarijn.weerwolven.game.orchestration.SimpleDefaultDecisionPolicy();
    private final me.mxndarijn.weerwolven.game.orchestration.AbilityExecutorRegistry abilityExecs = new me.mxndarijn.weerwolven.game.orchestration.AbilityExecutorRegistry();
    private final me.mxndarijn.weerwolven.game.orchestration.ResolvePolicy resolvePolicy = new me.mxndarijn.weerwolven.game.orchestration.ResolvePolicy();
    private final java.util.concurrent.Executor mainExecutor = Runnable::run; // direct executor for now
    private final me.mxndarijn.weerwolven.game.orchestration.OrchestratorFactory orchestratorFactory =
            new me.mxndarijn.weerwolven.game.orchestration.OrchestratorFactory(
                    intentCollector, orchestrationConfig, defaultPolicy, abilityExecs, resolvePolicy, mainExecutor);
    private volatile boolean phaseLoopRunning = false;

    private void registerDefaultAbilityExecutors() {
        // Register minimal executors so roles have actionable prompts
        abilityExecs.registerSolo(ActionKind.INSPECT, new me.mxndarijn.weerwolven.game.orchestration.SeerInspectExecutor());
    }
    
    private GameHouseManager gameHouseManager;
    private GameChatManager gameChatManager;
    private GameVoteManager gameVoteManager;
    private GameVisibilityManager gameVisibilityManager;

    public Game(UUID mainHost, GameInfo gameInfo, PresetConfig config, MxWorld mxWorld) {
        this.gameInfo = gameInfo;
        this.config = config;
        this.directory = mxWorld.getDir();
        this.mxWorld = mxWorld;
        this.mainHost = mainHost;
        this.plugin = JavaPlugin.getProvidingPlugin(WeerWolven.class);
        this.hosts = new ArrayList<>();
        this.gamePlayers = new ArrayList<>();
        this.config.getColors().forEach(color -> {
           this.gamePlayers.add(new GamePlayer(color, plugin, this));
        });
        this.spectators = new ArrayList<>();
        this.respawnLocations = new HashMap<>();
        
        // Initialize phase execution system
        this.cycleManager = new DayNightCycleManager(this, plugin);
        this.phaseExecutor = new PhaseExecutor(Map.of(), phaseHooks);

        this.hostScoreboard = new MxSupplierScoreBoard(plugin, () -> {
            return ScoreBoard.GAME_HOST.getTitle(new HashMap<>() {{
                put("%%map_name%%", config.getName());
            }});
        }, () -> {

            AtomicInteger alivePlayers = new AtomicInteger();
            AtomicInteger deadPlayers = new AtomicInteger();
            gamePlayers.forEach(g -> {
                if (g.isAlive())
                    alivePlayers.getAndIncrement();
                else
                    deadPlayers.getAndIncrement();
            });
            return ScoreBoard.GAME_HOST.getLines(new HashMap<>() {{
                put("%%map_name%%", config.getName());
                put("%%game_status%%", gameInfo.getStatus().getStatus());
                put("%%game_time%%", formatGameTime(gameTime));
                put("%%players_alive%%", gamePlayers.size() + "");
                put("%%mollen_alive%%", "0");
                put("%%ego_alive%%", "0");
                put("%%alive_players%%", alivePlayers.get() + "");
                put("%%dead_players%%", deadPlayers.get() + "");
                put("%%spectator_count%%", spectators.size() + "");
                put("%%phase%%", phase.getColoredPhase(dayNumber));

            }});
        });
        this.hostScoreboard.setUpdateTimer(10);

        this.spectatorScoreboard = new MxSupplierScoreBoard(plugin, () -> {
            return ScoreBoard.GAME_SPECTATOR.getTitle(new HashMap<>() {{
                put("%%map_name%%", config.getName());
            }});
        }, () -> {
            String host = Bukkit.getOfflinePlayer(getMainHost()).getName();
            return ScoreBoard.GAME_SPECTATOR.getLines(new HashMap<>() {{
                put("%%map_name%%", config.getName());
                put("%%game_status%%", gameInfo.getStatus().getStatus());
                put("%%game_time%%", formatGameTime(gameTime));
                put("%%players_alive%%", gamePlayers.size() + "");
                put("%%mollen_alive%%", "0");
                put("%%ego_alive%%", "0");
                put("%%spectator_count%%", "0");
                put("%%host%%", host);
                put("%%phase%%", phase.getColoredPhase(dayNumber));

            }});
        });

        loadWorld().thenAccept(loaded -> {
            if (!loaded) {
                stopGame();
            }
        });
        this.spectatorScoreboard.setUpdateTimer(10);
        this.registerRuntimeBusListeners();
        // Register built-in ability executors (e.g., Seer scaffold)
        registerDefaultAbilityExecutors();
    }

    public static Optional<Game> createGameFromGameInfo(UUID mainHost, GameInfo gameInfo) {
        Optional<Preset> map = PresetsManager.getInstance().getPresetById(gameInfo.getPresetId());
        if (map.isEmpty() || map.get().getMxWorld().isEmpty()) {
            return Optional.empty();
        }

        File newDir = new File(SpecialDirectories.GAMES_WORLDS.getDirectory() + "");
        Optional<MxWorld> optionalWorld = MxAtlas.getInstance().duplicateMxWorld(map.get().getMxWorld().get(), newDir);

        if (optionalWorld.isEmpty()) {
            return Optional.empty();
        }

        Game g = new Game(mainHost, gameInfo, map.get().getConfig(), optionalWorld.get());
        GameWorldManager.getInstance().addGame(g);

        return Optional.of(g);
    }

    public CompletableFuture<Boolean> loadWorld() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        if (this.mxWorld == null) {
            future.complete(false);
            return future;
        }
        if (this.mxWorld.isLoaded()) {
            future.complete(true);
            return future;
        }
        MxAtlas.getInstance().loadMxWorld(this.mxWorld).thenAccept(loaded -> {
            future.complete(loaded);
            if (loaded) {
                registerEvents();
                updateGame();
                MxChangeWorldManager.getInstance().addWorld(this.mxWorld.getWorldUID(), new MxChangeWorld() {
                    @Override
                    public void enter(Player p, World w, PlayerChangedWorldEvent e) {

                    }

                    @Override
                    public void leave(Player p, World w, PlayerChangedWorldEvent e) {
                        hosts.remove(p.getUniqueId());
                        removePlayer(p.getUniqueId());
                        ScoreBoardManager.getInstance().removePlayerScoreboard(p.getUniqueId(), hostScoreboard);

                        if (hosts.isEmpty()) {
                            setGameStatus(UpcomingGameStatus.FINISHED);
                        }
                    }
                    @Override
                    public void quit(Player p, World w, PlayerQuitEvent e) {
                        // do nothing
                    }
                });
            }
        });
        return future;
    }

    private void registerRuntimeBusListeners() {
        busSubs.close(); // reset if called twice
        // subscribe all runtime listeners that react to game-bus events:
        busSubs.add(LoversChainListener.subscribe(this, gameEventBus));
        busSubs.add(MayorVoteWeightListener.subscribe(this, gameEventBus));
        // busSubs.add(HunterDeathrattleListener.subscribe(this, gameEventBus));
        // busSubs.add(WitchSavePoisonListener.subscribe(this, gameEventBus));
        // etc.
    }

    private void unregisterRuntimeBusListeners() {
        busSubs.close();
    }

    public void removePlayer(UUID playerUUID) {
        //TODO replace player IDK anymore...
        Optional<GamePlayer> player = getGamePlayerOfPlayer(playerUUID);
        if (player.isEmpty())
            return;

        GamePlayer gamePlayer = player.get();
        gamePlayer.setPlayerUUID(null);

        Player p = Bukkit.getPlayer(playerUUID);
        if (p != null) {
            p.teleport(Functions.getSpawnLocation());
            sendMessageToAll(LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_PLAYER_LEAVED, new ArrayList<>(Arrays.asList(p.getName(), gamePlayer.getColorData().getColor().getDisplayName()))));
        }
        sendMessageToAll(LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_PLAYER_LEAVED, new ArrayList<>(Arrays.asList(Bukkit.getOfflinePlayer(playerUUID).getName(), gamePlayer.getColorData().getColor().getDisplayName()))));
        //TODO IDK anymore... Add Scoreboard

    }

    public void updateGame() {
        if (updateGameUpdater != null)
            return;
        final AtomicLong[] lastUpdateTime = {null};
        updateGameUpdater = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (this.mxWorld == null)
                updateGameUpdater.cancel();
            if (lastUpdateTime[0] != null && gameInfo.getStatus() == UpcomingGameStatus.FREEZE)
                lastUpdateTime[0].set(System.currentTimeMillis());
            if (gameInfo.getStatus() != UpcomingGameStatus.PLAYING)
                return;
            if (lastUpdateTime[0] == null)
                lastUpdateTime[0] = new AtomicLong(System.currentTimeMillis());
            long l = System.currentTimeMillis();
            long delta = l - lastUpdateTime[0].get();
            gameTime += delta;

            lastUpdateTime[0].set(l);
        }, 0L, 10L);
    }

    public void setGameStatus(UpcomingGameStatus upcomingGameStatus) {
        //First game start
        if (upcomingGameStatus == UpcomingGameStatus.PLAYING && !firstStart) {
            firstStart = true;
            gameInfo.clearQueue();
            setGameRoles();
            // Register runtime listeners and start the phase loop from Night 1
            registerRuntimeBusListeners();
            startPhaseLoop();
            actionTimerService.start();
        }
        getGameInfo().setStatus(upcomingGameStatus);
        if (upcomingGameStatus == UpcomingGameStatus.FINISHED) {
            stopGame();
        }
        sendMessageToAll(LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_STATUS_CHANGED, Collections.singletonList(upcomingGameStatus.getStatus())));
    }

    private void setGameRoles() {
        // Build flat role list from the selected RoleSet
        List<Roles> roles = new ArrayList<>();
        RoleSet rs = gameInfo.getRoleSet();
        if (rs != null && rs.getRoleSet() != null) {
            rs.getRoleSet().forEach((role, amount) -> {
                if (role != null && amount != null && amount > 0) {
                    for (int i = 0; i < amount; i++) {
                        roles.add(role);
                    }
                }
            });
        }
        // Sort by priority so higher-priority roles are assigned first (HIGHEST ordinal is 0)
        roles.sort(Comparator.comparing(r -> r.getPriority().ordinal()));

        // Prioritize assigning roles to GamePlayers that actually have a player assigned
        List<GamePlayer> ordered = new ArrayList<>(gamePlayers);
        ordered.sort(Comparator.comparing((GamePlayer gp) -> gp.getOptionalPlayerUUID().isEmpty()));

        // Assign roles to game players; default remaining to Villager
        Iterator<Roles> it = roles.iterator();
        for (GamePlayer gp : ordered) {
            Roles r = it.hasNext() ? it.next() : Roles.VILLAGER;
            gp.setRole(r);
        }
    }

    public void addHost(UUID uuid) {
        if(hosts.contains(uuid))
            return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                hosts.add(uuid);
                gameInfo.removePlayerFromQueue(uuid);
                if (this.mxWorld == null || !this.mxWorld.isLoaded()) {
                    AtomicInteger i = new AtomicInteger();
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                        if (this.mxWorld != null && this.mxWorld.isLoaded()) {
                            addHostItems(p);
                        } else {
                            if (i.get() < 100) {
                                i.getAndIncrement();
                                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                                    if (this.mxWorld != null && this.mxWorld.isLoaded()) {
                                        addHostItems(p);
                                    } else {
                                        if (i.get() < 100) {
                                            i.getAndIncrement();
                                        }
                                    }
                                }, 5L);
                            }
                        }
                    }, 10L);
                } else {
                    addHostItems(p);
                }
            }
        });
    }

    public void addHostItems(Player p) {
        if (this.mxWorld == null)
            return;
        World w = Bukkit.getWorld(mxWorld.getWorldUID());
        p.teleport(w.getSpawnLocation());
        ScoreBoardManager.getInstance().setPlayerScoreboard(p.getUniqueId(), hostScoreboard);
        p.setGameMode(GameMode.CREATIVE);
        MessageUtil.sendMessageToPlayer(p, WeerWolvenChatPrefix.DEFAULT + LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_YOU_ARE_NOW_HOST));
        p.getInventory().clear();
        p.getInventory().addItem(Items.PLAYER_MANAGEMENT_ITEM.getItemStack());
        p.getInventory().addItem(Items.HOST_TOOL.getItemStack());
        p.getInventory().addItem(Items.VANISH_ITEM.getItemStack());
    }

    public void stopGame() {
        // stop timers
        try {
            actionTimerService.stop();
        } catch (Exception ignored) {
        }
        // stop phase loop if running
        stopPhaseLoop();
        sendMessageToAll(LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_GAME_STOPPED));
        
        // Cleanup phase systems
        cycleManager.shutdown();
        unregisterRuntimeBusListeners();
        busSubs.close();
        
        getOptionalMxWorld().ifPresent(world -> MxChangeWorldManager.getInstance().removeWorld(world.getWorldUID()));

        hosts.forEach(u -> {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                ScoreBoardManager.getInstance().removePlayerScoreboard(p.getUniqueId(), hostScoreboard);
                VanishManager.getInstance().showPlayerForAll(p);
                p.setHealth(20);
                p.getActivePotionEffects().clear();
                p.setFoodLevel(20);
            }
        });
        spectators.forEach(u -> {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                VanishManager.getInstance().showPlayerForAll(p);
                ScoreBoardManager.getInstance().removePlayerScoreboard(u, spectatorScoreboard);
                p.setHealth(20);
                p.getActivePotionEffects().clear();
                p.setFoodLevel(20);
            }
        });
        gamePlayers.forEach(g -> {
            if (g.getOptionalPlayerUUID().isPresent()) {
                Player p = Bukkit.getPlayer(g.getOptionalPlayerUUID().get());
                if (p != null) {
                    ScoreBoardManager.getInstance().removePlayerScoreboard(p.getUniqueId(), g.getScoreboard());
                    VanishManager.getInstance().showPlayerForAll(p);
                    p.setHealth(20);
                    p.getActivePotionEffects().clear();
                    p.setFoodLevel(20);
                }
            }
        });
        World w = Bukkit.getWorld(getOptionalMxWorld().get().getWorldUID());

        unloadWorld().thenAccept(unloaded -> {
            if (unloaded) this.mxWorld = null;
            GameManager.getInstance().removeUpcomingGame(gameInfo);
            GameWorldManager.getInstance().removeGame(this);
        });
    }

    public CompletableFuture<Boolean> unloadWorld() {
        if (getOptionalMxWorld().isPresent() && Bukkit.getWorld(this.mxWorld.getWorldUID()) != null) {
            World w = Bukkit.getWorld(this.mxWorld.getWorldUID());
            w.getPlayers().forEach(p -> {
                p.teleport(Functions.getSpawnLocation());
            });
        }
        unregisterEvents();

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (getOptionalMxWorld().isEmpty()) {
                future.complete(true);
                return;
            }
            MxAtlas.getInstance().unloadMxWorld(this.mxWorld, true);
            future.complete(true);
        });
        return future;
    }

    public void registerEvents() {
        unregisterEvents();
        gameChatManager = new GameChatManager(this);
        gameHouseManager = new GameHouseManager(this);
        gameVoteManager = new GameVoteManager(this);
        gameVisibilityManager = new GameVisibilityManager(this);

        events = new ArrayList<>(Arrays.asList(
                new GamePreStartEvents(this, plugin),
                new GameFreezeEvents(this, plugin),
                new GamePlayingEvents(this, plugin),
                new GameSpectatorEvents(this, plugin),
                new GameDefaultEvents(this, plugin),
                gameChatManager,
                gameHouseManager,
                gameVoteManager,
                gameVisibilityManager
        ));
    }

    public void unregisterEvents() {
        if (events == null)
            return;
        events.forEach(HandlerList::unregisterAll);
    }

    public Optional<MxWorld> getOptionalMxWorld() {
        return Optional.ofNullable(mxWorld);
    }

    public int getAlivePlayerCount() {
        AtomicInteger i = new AtomicInteger();
        gamePlayers.forEach(c -> {
            if (c.getOptionalPlayerUUID().isPresent() && c.isAlive())
                i.getAndIncrement();
        });

        return i.get();
    }

    public void addSpectator(UUID uuid) {
        spectators.add(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.getInventory().clear();
            ScoreBoardManager.getInstance().setPlayerScoreboard(uuid, spectatorScoreboard);
            MessageUtil.sendMessageToPlayer(player, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_SPECTATOR_JOIN));
            sendMessageToHosts(LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_SPECTATOR_JOINED, Collections.singletonList(player.getName())));
        }
        addSpectatorSettings(uuid);
    }

    public void sendMessageToAll(String message) {
        sendMessageToHosts(message);
        sendMessageToPlayers(message);
        sendMessageToSpectators(message);

    }

    public void sendMessageToHosts(String message) {
        hosts.forEach(host -> {
            Player p = Bukkit.getPlayer(host);
            if (p != null) {
                MessageUtil.sendMessageToPlayer(p, message);
            }
        });
    }

    public void sendMessageToSpectators(String message) {
        spectators.forEach(host -> {
            Player p = Bukkit.getPlayer(host);
            if (p != null) {
                MessageUtil.sendMessageToPlayer(p, message);
            }
        });
    }

    public void sendMessageToPlayers(String message) {
        gamePlayers.forEach(color -> {
            if (color.getOptionalPlayerUUID().isPresent()) {
                Player p = Bukkit.getPlayer(color.getOptionalPlayerUUID().get());
                if (p != null) {
                    MessageUtil.sendMessageToPlayer(p, message);
                }
            }
        });
    }


    public void addSpectatorSettings(UUID uuid) {
        if (mxWorld == null)
            return;
        addSpectatorSettings(uuid, Bukkit.getWorld(mxWorld.getWorldUID()).getSpawnLocation());
    }

    public void addSpectatorSettings(UUID uuid, Location loc) {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) {
            if (!p.isDead()) {
                p.teleport(loc);
            } else {
                respawnLocations.put(uuid, loc);
            }
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                p.getInventory().clear();
                p.getInventory().setItem(0, Items.GAME_SPECTATOR_TELEPORT_ITEM.getItemStack());
                if (spectators.contains(p.getUniqueId())) {
                    p.getInventory().setItem(8, Items.GAME_SPECTATOR_LEAVE_ITEM.getItemStack());
                }
                VanishManager.getInstance().hidePlayerForAll(p);
            }, 40L);
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                VanishManager.getInstance().hidePlayerForAll(p);
            }, 80L);
            p.setHealth(20);
            p.setFoodLevel(20);
            p.setAllowFlight(true);
            VanishManager.getInstance().hidePlayerForAll(p);
        }
    }


    public void removeSpectator(UUID uniqueId, boolean teleport) {
        spectators.remove(uniqueId);
        Player p = Bukkit.getPlayer(uniqueId);
        if (p != null) {
            VanishManager.getInstance().showPlayerForAll(p);
            ScoreBoardManager.getInstance().removePlayerScoreboard(uniqueId, spectatorScoreboard);
            p.setHealth(20);
            p.getActivePotionEffects().clear();
            p.setFoodLevel(20);
            p.setAllowFlight(false);
            p.getInventory().clear();
            if (teleport)
                p.teleport(Functions.getSpawnLocation());
        }
    }

    public void removeSpectator(UUID uniqueId) {
        removeSpectator(uniqueId, true);
    }

    public Optional<GamePlayer> getGamePlayerOfPlayer(UUID uuid) {
        for (GamePlayer color : gamePlayers) {
            if (color.getOptionalPlayerUUID().isPresent() && color.getOptionalPlayerUUID().get().equals(uuid)) {
                return Optional.of(color);
            }
        }
        return Optional.empty();
    }

    public void addPlayer(UUID playerUUID, GamePlayer gamePlayer) {
        //TODO Change Inventory
        Player p = Bukkit.getPlayer(playerUUID);
        if (p == null || mxWorld == null)
            return;

        if (gamePlayer.getOptionalPlayerUUID().isPresent()) {
            removePlayer(gamePlayer.getOptionalPlayerUUID().get());
        }

        gamePlayer.setPlayerUUID(playerUUID);
        gameInfo.removePlayerFromQueue(playerUUID);
        p.getInventory().clear();
        p.teleport(gamePlayer.getColorData().getSpawnLocation().getLocation(Bukkit.getWorld(mxWorld.getWorldUID())));
        p.setHealth(20);
        p.setFoodLevel(20);
        p.setExp(0);
        p.getInventory().addItem(Items.GAME_PLAYER_TOOL.getItemStack());
        p.getInventory().addItem(Items.GAME_PLAYER_VOTE_ITEM.getItemStack());
        p.setGameMode(GameMode.SURVIVAL);
        sendMessageToAll(LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_PLAYER_JOINED, new ArrayList<>(Arrays.asList(p.getName(), gamePlayer.getColorData().getColor().getDisplayName()))));
        ScoreBoardManager.getInstance().setPlayerScoreboard(p.getUniqueId(), gamePlayer.getScoreboard());

    }

    public String getFormattedGameTime() {
        return formatGameTime(gameTime);
    }
    
    /**
     * Advances the game to the next phase with smooth day/night cycle transition.
     * This method:
     * - Updates the phase field
     * - Increments day number if transitioning from DAWN to DAY
     * - Triggers smooth time transition via DayNightCycleManager
     * - Can optionally execute phase actions via PhaseExecutor
     * 
     * @return A CompletableFuture that completes when the time transition is finished
     */
    public CompletableFuture<Void> advancePhase() {
        Phase oldPhase = this.phase;
        Phase newPhase = oldPhase.next();
        
        // Update day number if needed
        if (newPhase.incrementsDayCounterFrom(oldPhase)) {
            this.dayNumber++;
        }
        
        // Update phase
        this.phase = newPhase;
        
        // Broadcast phase change to players
        sendMessageToAll("<gray>De fase verandert naar: " + newPhase.getColoredPhase(dayNumber));
        
        // Trigger smooth time transition and return the future
        CompletableFuture<Void> transitionFuture = cycleManager.transitionToPhase(newPhase);

        // TODO: Execute phase actions via PhaseExecutor when handlers are implemented
        // List<ActionIntent> intents = collectCurrentIntents();
        // phaseExecutor.execute(this, gameEventBus, newPhase, intents);
        
        return transitionFuture;
    }
    
    /**
     * Directly sets the phase without executing phase logic.
     * Useful for administrative commands or game setup.
     * 
     * @param newPhase The phase to set
     * @param smoothTransition Whether to use smooth time transition or instant
     * @return A CompletableFuture that completes when the time transition is finished
     */
    public CompletableFuture<Void> setPhase(Phase newPhase, boolean smoothTransition) {
        Phase oldPhase = this.phase;
        
        // Update day number if needed
        if (newPhase.incrementsDayCounterFrom(oldPhase)) {
            this.dayNumber++;
        }
        
        this.phase = newPhase;
        
        // Broadcast phase change to players
        sendMessageToAll("<gray>De fase is veranderd naar: " + newPhase.getColoredPhase(dayNumber));
        
        // Apply time change and return the appropriate future
        if (smoothTransition) {
            return cycleManager.transitionToPhase(newPhase);
        } else {
            return cycleManager.setPhaseTimeInstant(newPhase);
        }
    }

    // -------- Phase loop orchestration --------
    /** Starts the automatic phase loop from current phase. If in LOBBY, jumps to NIGHT 1. */
    public void startPhaseLoop() {
        if (phaseLoopRunning) return;
        phaseLoopRunning = true;
        if (this.phase == Phase.LOBBY) {
            this.dayNumber = 1; // Night 1
            setPhase(Phase.NIGHT, true).thenRun(() ->
                    Bukkit.getScheduler().runTask(plugin, this::runCurrentPhase)
            );
        } else if (this.phase != Phase.END) {
            // Ensure world time matches current phase, then run
            cycleManager.transitionToPhase(this.phase).thenRun(() ->
                    Bukkit.getScheduler().runTask(plugin, this::runCurrentPhase)
            );
        }
    }

    /** Stops the automatic phase loop; current orchestrator may finish but no further phases will run. */
    public void stopPhaseLoop() { this.phaseLoopRunning = false; }

    /** Executes the orchestrator for the current phase, resolves intents, then advances and recurs. */
    private void runCurrentPhase() {
        Logger.logMessage("Running phase: " + this.phase + "");
        if (!phaseLoopRunning) return;
        if (this.phase == Phase.LOBBY || this.phase == Phase.END) return;

        var orchestrator = orchestratorFactory.create(this, this.phase);
        if (orchestrator == null) { // No orchestration for this phase; just advance
            afterPhaseCollection();
            return;
        }

        orchestrator.runCollection(() -> {
            var intents = intentCollector.drain();
            // Execute collected intents for this phase
            phaseExecutor.execute(this, gameEventBus, intents);
            afterPhaseCollection();
        });
    }

    /** After execution, advance phase (smooth transition) then continue if still running. */
    private void afterPhaseCollection() {
        Logger.logMessage("After phase collection: " + this.phase + "");
        if (!phaseLoopRunning) return;
        advancePhase().thenRun(() -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Logger.logMessage("After phase advance: " + this.phase + "");
                if (phaseLoopRunning && this.phase != Phase.END) {
                    runCurrentPhase();
                }
            });
        });
    }

    public String formatVoteAction(TimerContext ctx) {
        return formatAction(ctx, getGameVoteManager().getVotesCast(), getGameVoteManager().getEligibleVotes(), " votes");
    }

    public String formatAction(TimerContext ctx, int cast, int eligible, String extra) {
        long rem = ctx.remainingMs();
        return ctx.spec().title + " <gray>[" + TimerFormats.mmss(rem) + "] " + " <dark_gray>(<gray>" + cast + "<dark_gray>/<gray>" + eligible + extra + " <dark_gray>)";
    }
}
