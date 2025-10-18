package me.mxndarijn.weerwolven.game.core;

import lombok.Getter;
import me.mxndarijn.weerwolven.WeerWolven;
import me.mxndarijn.weerwolven.data.*;
import me.mxndarijn.weerwolven.game.action.ActionHandler;
import me.mxndarijn.weerwolven.game.action.InspectLogHandler;
import me.mxndarijn.weerwolven.game.action.JailLogHandler;
import me.mxndarijn.weerwolven.game.bus.AutoCloseableGroup;
import me.mxndarijn.weerwolven.game.bus.GameEventBus;
import me.mxndarijn.weerwolven.game.bus.Priority;
import me.mxndarijn.weerwolven.game.bus.events.GameWonEvent;
import me.mxndarijn.weerwolven.game.core.win.*;
import me.mxndarijn.weerwolven.game.events.minecraft.*;
import me.mxndarijn.weerwolven.game.manager.*;
import me.mxndarijn.weerwolven.game.orchestration.executor.AbilityExecutorRegistry;
import me.mxndarijn.weerwolven.game.orchestration.executor.SeerInspectExecutor;
import me.mxndarijn.weerwolven.game.orchestration.executor.WerewolfEliminateExecutor;
import me.mxndarijn.weerwolven.game.phase.Phase;
import me.mxndarijn.weerwolven.game.phase.PhaseExecutor;
import me.mxndarijn.weerwolven.game.phase.PhaseHooks;
import me.mxndarijn.weerwolven.game.phase.DefaultPhaseHooks;
import me.mxndarijn.weerwolven.game.phase.DayNightCycleManager;
import me.mxndarijn.weerwolven.game.runtime.EliminateQueue;
import me.mxndarijn.weerwolven.game.runtime.LoversChainListener;
import me.mxndarijn.weerwolven.game.runtime.MayorVoteWeightListener;
import me.mxndarijn.weerwolven.game.timer.ActionTimerService;
import me.mxndarijn.weerwolven.game.timer.TimerContext;
import me.mxndarijn.weerwolven.game.timer.TimerFormats;
import me.mxndarijn.weerwolven.managers.*;
import me.mxndarijn.weerwolven.presets.Preset;
import me.mxndarijn.weerwolven.presets.PresetConfig;
import net.kyori.adventure.title.Title;
import nl.mxndarijn.mxlib.changeworld.MxChangeWorld;
import nl.mxndarijn.mxlib.changeworld.MxChangeWorldManager;
import nl.mxndarijn.mxlib.language.LanguageManager;
import nl.mxndarijn.mxlib.logger.Logger;
import nl.mxndarijn.mxlib.mxscoreboard.MxSupplierScoreBoard;
import nl.mxndarijn.mxlib.mxworld.MxAtlas;
import nl.mxndarijn.mxlib.mxworld.MxWorld;
import nl.mxndarijn.mxlib.util.Functions;
import nl.mxndarijn.mxlib.util.MessageUtil;
import org.bukkit.*;
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

/**
 * The Game class represents a dynamic game model facilitating multi-phase gameplay,
 * player management, host interactions, and orchestration of game events.
 * <p>
 * This class is designed to handle various aspects of game mechanics,
 * including player interactions, event lifecycle, custom game configuration, and phase transitions.
 */
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

    private final EliminateQueue eliminateQueue = new EliminateQueue();
    private final AutoCloseableGroup busSubs = new AutoCloseableGroup();
    
    // Phase execution system
    private final PhaseHooks phaseHooks = new DefaultPhaseHooks();
    private final PhaseExecutor phaseExecutor;
    private final DayNightCycleManager cycleManager;
    // Orchestration components
    private final me.mxndarijn.weerwolven.game.orchestration.IntentCollector intentCollector = new me.mxndarijn.weerwolven.game.orchestration.IntentCollector();
    private final me.mxndarijn.weerwolven.game.orchestration.OrchestrationConfig orchestrationConfig = new me.mxndarijn.weerwolven.game.orchestration.OrchestrationConfig();
    private final me.mxndarijn.weerwolven.game.orchestration.DefaultDecisionPolicy defaultPolicy = new me.mxndarijn.weerwolven.game.orchestration.SimpleDefaultDecisionPolicy();
    private final AbilityExecutorRegistry abilityExecs = new AbilityExecutorRegistry();
    private final me.mxndarijn.weerwolven.game.orchestration.ResolvePolicy resolvePolicy = new me.mxndarijn.weerwolven.game.orchestration.ResolvePolicy();
    private final java.util.concurrent.Executor mainExecutor = Runnable::run; // direct executor for now
    private final me.mxndarijn.weerwolven.game.orchestration.OrchestratorFactory orchestratorFactory =
            new me.mxndarijn.weerwolven.game.orchestration.OrchestratorFactory(
                    intentCollector, orchestrationConfig, defaultPolicy, abilityExecs, resolvePolicy, mainExecutor);
    private volatile boolean phaseLoopRunning = false;

    private void registerDefaultAbilityExecutors() {
        // Register minimal executors so roles have actionable prompts
        abilityExecs.registerSolo(ActionKind.INSPECT, new SeerInspectExecutor());
        abilityExecs.registerTeam(ActionKind.TEAM_ELIMINATE, new WerewolfEliminateExecutor());
        abilityExecs.registerSolo(ActionKind.JAIL, new me.mxndarijn.weerwolven.game.orchestration.executor.JailerJailExecutor());
    }
    
    private GameHouseManager gameHouseManager;
    private GameChatManager gameChatManager;
    private GameVoteManager gameVoteManager;
    private GameVisibilityManager gameVisibilityManager;

    private final WinCheckService winChecks = new WinCheckService(
            this,
            this.gameEventBus,
            List.of(
                    new LoversLastTwoCondition(),
                    new WerewolfParityCondition(),
                    new CitizensEliminateThreatsCondition()
            )
    );

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
        var handlers = new EnumMap<ActionKind, ActionHandler>(ActionKind.class);

        handlers.put(ActionKind.INSPECT, new InspectLogHandler());
        handlers.put(ActionKind.JAIL, new JailLogHandler());

        this.phaseExecutor = new PhaseExecutor(handlers, phaseHooks);

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
                stopGame(new WinResult(Team.SOLO, List.of(), WinConditionText.NO_ONE));
            }
        });
        this.spectatorScoreboard.setUpdateTimer(10);
        this.registerRuntimeBusListeners();
        // Register built-in ability executors (e.g., Seer scaffold)
        registerDefaultAbilityExecutors();
        // Configure orchestration settings
        setupOrchestration();
    }

    /**
     * Configures the orchestration settings for the game by defining specific timeouts
     * for actions and applying resolution policies.
     *
     * This method sets the following action-specific configurations:
     * - Sets a timeout of 45,000 milliseconds for the INSPECT and JAIL actions.
     * - Sets a timeout of 300,000 milliseconds for the TEAM_ELIMINATE action.
     *
     * Additionally, this method configures the TEAM_ELIMINATE action to use team-based aggregation
     * for resolving policies.
     *
     * This setup ensures synchronization between game mechanics and orchestration logic.
     */
    private void setupOrchestration() {
        // Ensure orchestrator timeout for INSPECT matches Seer UI timer
        this.orchestrationConfig.setTimeout(ActionKind.INSPECT, 45_000L);
        // Timeout for Jailer jail selection
        this.orchestrationConfig.setTimeout(ActionKind.JAIL, 45_000L);
        this.orchestrationConfig.setTimeout(ActionKind.TEAM_ELIMINATE, 300_000L);
        // Configure TEAM_ELIMINATE to use team aggregation
        this.resolvePolicy.aggregated(ActionKind.TEAM_ELIMINATE);
    }

    /**
     * Creates a new game instance from the provided game information and main host UUID.
     *
     * @param mainHost the UUID of the main host of the game
     * @param gameInfo the information about the game, including configuration and preset ID
     * @return an {@code Optional} containing the created {@code Game} instance if successful,
     * or an empty {@code Optional} if the game creation fails
     */
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

    /**
     * Loads the associated MxWorld asynchronously.
     * If the MxWorld is null, or the MxWorld is already loaded, the method completes the future accordingly.
     * If the MxWorld is not loaded, it triggers the world loading process and registers necessary game events.
     *
     * @return a CompletableFuture that completes with a boolean value:
     * - true if the world is successfully loaded,
     * - false if the world cannot be loaded or is null.
     */
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
                            stopGame(new WinResult(Team.SOLO, List.of(), WinConditionText.NO_ONE));
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

    /**
     * Registers runtime event listeners for the active game session on the {@code gameEventBus}.
     * This method subscribes relevant listeners that respond to in-game events
     * during the current game runtime.
     *
     * Before adding new subscriptions, the method resets any existing subscriptions
     * by calling {@code busSubs.close()} to avoid duplicate listener registrations.
     *
     * The following listeners are subscribed to the {@code gameEventBus}:
     * - {@code LoversChainListener}: Reacts to player elimination events and handles the logic
     *   associated with the 'lovers' mechanic, where the elimination of one player may trigger
     *   the elimination of their partner.
     * - {@code MayorVoteWeightListener}: Handles the doubling of vote weights during
     *   the day phase for players with the mayor status*/
    private void registerRuntimeBusListeners() {
        busSubs.close(); // reset if called twice
        // subscribe all runtime listeners that react to game-bus events:
        busSubs.add(LoversChainListener.subscribe(this, gameEventBus));
        busSubs.add(MayorVoteWeightListener.subscribe(this, gameEventBus));
        busSubs.add(JesterInstantWinListener.subscribe(this, gameEventBus));
        busSubs.add(gameEventBus.subscribe(
                GameWonEvent.class,
                Priority.NORMAL,
                evt -> {
                    if (getGameInfo().getStatus() == UpcomingGameStatus.FINISHED) {
                        return;
                    }
                    stopGame(evt.result());
                }
        ));
        // busSubs.add(HunterDeathrattleListener.subscribe(this, gameEventBus));
        // busSubs.add(WitchSavePoisonListener.subscribe(this, gameEventBus));
        // etc.
    }

    /**
     * Unregisters and closes all runtime bus listeners associated with the game.
     * This method is responsible for ensuring that all subscriptions tied to the bus are
     * properly closed and the underlying resources are released.
     * It utilizes the {@code busSubs.close()} method to gracefully handle the closing
     * of all active listeners.
     */
    private void unregisterRuntimeBusListeners() {
        busSubs.close();
    }

    /**
     * Removes a player from the game based on their unique identifier (UUID).
     * This method handles the player's removal, updates their state, and sends notifications
     * to all participants about the departure.
     *
     * @param playerUUID The unique identifier of the player to be removed from the game.
     */
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

    /**
     * Updates the state of the game periodically. This method schedules a repeating task
     * using the Bukkit scheduler to handle updates based on game status and elapsed time.
     *
     * Key behaviors:
     * - If the `updateGameUpdater` is already set, the method exits to prevent duplicate tasks.
     * - Initializes the `lastUpdateTime` if not already set.
     * - Cancels the update task if `mxWorld` is null, which indicates the game world is unavailable.
     * - Updates the `gameTime` only if the game's status is `PLAYING`.
     * - If the game's status is `FREEZE`, sets the `lastUpdateTime` to the current system time.
     *
     * The task runs at a fixed interval of 10 ticks (0.5 seconds in typical Bukkit configurations).
     * It ensures the game's time progression is accurately tracked and reacts to changes
     * in the game status*/
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

    /**
     * Updates the current status of the game and performs necessary actions
     * based on the new status, such as starting or stopping the game, managing
     * game roles, and notifying players of the status change.
     *
     * @param upcomingGameStatus the status to set for the game; it can affect
     *                           the flow of the game such as initializing roles,
     *                           starting phase loops, or stopping the game.
     */
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
        sendMessageToAll(LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_STATUS_CHANGED, Collections.singletonList(upcomingGameStatus.getStatus())));
    }

    /**
     * Assigns roles to all game players based on the selected role set while ensuring
     * that roles are distributed based on priority and availability.
     *
     * The method performs the following steps:
     * 1. Retrieves the role set from the game information and constructs a flat list of roles
     *    based on the role configuration. Each role is added to the list according to its specified amount.
     * 2. Sorts the roles by priority, ensuring that higher-priority roles are assigned first.
     * 3. Prioritizes assigning roles to game players who currently have an associated player.
     * 4. Iterates over the game players and assigns roles in the sorted order. If roles run out,
     *    defaults the remaining game players to the {@code Roles.VILLAGER} role.
     */
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

    /**
     * Adds a player, identified by their UUID, to the list of game hosts.
     * If the player is already a host, the method does nothing.
     * This method ensures that the player is assigned hosting capabilities, is
     * removed from the game queue, and is equipped with host-specific items.
     * The addition process is handled asynchronously and checks are performed
     * to ensure the relevant world is loaded.
     *
     * @param uuid the unique identifier of the player to be added as a host
     */
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

    /**
     * Assigns host items and settings to the specified player, enabling them to perform host-related actions
     * and configuring their inventory, game mode, and scoreboard accordingly.
     *
     * @param p the player to be configured as a host
     */
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

    /**
     * Stops the ongoing game instance and performs all necessary cleanup processes.
     *
     * This method handles multiple tasks required to terminate the game properly,
     * including stopping game mechanisms, notifying participants, restoring player
     * states, unloading the game world, and updating managers for the game's state
     * and lifecycle.
     *
     * Key behavior includes:
     * - Stopping the action timer service and the phase loop.
     * - Sending a notification to all players that the game has stopped.
     * - Shutting down and unregistering runtime components such as cycle manager or event listeners.
     * - Cleaning up player states (e.g., health, potion effects, and visibility).
     * - Managing scoreboard and vanish states for hosts, spectators, and game players.
     * - Removing the associated world from the world manager and unloading it asynchronously.
     * - Removing the game instance from relevant managers within the system.
     *
     * This method ensures all resources used during the*/
    public void stopGame(WinResult result) {
        new GameEndManager(this).endGame(result);
    }

    /**
     * Unloads the current world associated with the game instance. If the world
     * is currently loaded, it teleports all players within the world to a
     * designated spawn location and then proceeds to unregister event listeners
     * related to the game. The world's unloading process is scheduled
     * asynchronously, ensuring that the necessary clean-up operations are
     * performed before completion.
     *
     * @return a CompletableFuture that resolves to {@code true} if the world
     * unloading process completes successfully.
     */
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

    /**
     * Registers all game-related event handlers and managers.
     *
     * This method initializes and assigns the game managers (e.g., GameChatManager,
     * GameHouseManager, GameVoteManager, and GameVisibilityManager) and creates a
     * list of event handlers required for various game states and functionalities.
     * The method begins by unregistering any previously registered events
     * to ensure no duplicate handlers are retained.
     *
     * The event handlers added include:
     * - GamePreStartEvents: Handles events prior to game start.
     * - GameFreezeEvents: Handles events during game freeze mode.
     * - GamePlayingEvents: Handles events during the game-playing phase.
     * - GameSpectatorEvents: Handles events specific to spectators.
     * - GameDefaultEvents: Manages default game events.
     * - Game managers: Handles*/
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

    /**
     * Unregisters all event handlers associated with the game.
     *
     * This method ensures that any previously registered event handlers are
     * removed to prevent unintended behavior or resource leaks. If no events have
     * been registered, the method performs no actions.
     */
    public void unregisterEvents() {
        if (events == null)
            return;
        events.forEach(HandlerList::unregisterAll);
    }

    /**
     * Retrieves an Optional containing the MxWorld instance if it is present.
     *
     * @return an Optional containing the MxWorld instance or an empty Optional if mxWorld is null
     */
    public Optional<MxWorld> getOptionalMxWorld() {
        return Optional.ofNullable(mxWorld);
    }

    /**
     * Retrieves the number of alive players in the game.
     * A player is considered alive if their unique identifier is present and their alive status is true.
     *
     * @return the count of alive players currently in the game
     */
    public int getAlivePlayerCount() {
        AtomicInteger i = new AtomicInteger();
        gamePlayers.forEach(c -> {
            if (c.getOptionalPlayerUUID().isPresent() && c.isAlive())
                i.getAndIncrement();
        });

        return i.get();
    }

    /**
     * Adds a player to the list of spectators and updates their status accordingly.
     * The player's inventory will be cleared, their scoreboard will be updated to the spectator scoreboard,
     * and a notification message will be sent to the player and hosts. Spectator-specific settings are also applied.
     *
     * @param uuid the unique identifier of the player to be added as a spectator
     */
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

    /**
     * Sends a message to all participants in the game, including hosts, players, and spectators.
     *
     * @param message the message to send to all participants
     */
    public void sendMessageToAll(String message) {
        sendMessageToHosts(message);
        sendMessageToPlayers(message);
        sendMessageToSpectators(message);

    }

    /**
     * Sends a message to all hosts in the game.
     * Hosts are identified from the list of host UUIDs and converted to Player instances.
     * If a player corresponding to a host UUID is online, the specified message is sent to them.
     *
     * @param message The message to send to each host.
     */
    public void sendMessageToHosts(String message) {
        hosts.forEach(host -> {
            Player p = Bukkit.getPlayer(host);
            if (p != null) {
                MessageUtil.sendMessageToPlayer(p, message);
            }
        });
    }

    /**
     * Sends a message to all spectators in the game.
     *
     * Iterates through the list of spectators, retrieves the corresponding
     * Player object if available, and sends the provided message to the player.
     *
     * @param message The message to be sent to all spectators.
     *                This should be a non-null string containing the message content.
     */
    public void sendMessageToSpectators(String message) {
        spectators.forEach(host -> {
            Player p = Bukkit.getPlayer(host);
            if (p != null) {
                MessageUtil.sendMessageToPlayer(p, message);
            }
        });
    }

    /**
     * Sends a message to all players currently involved in the game. Each player's UUID
     * is retrieved from the game players, and the message is sent if they are online.
     *
     * @param message the message to be sent to all players in the game
     */
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


    /**
     * Adds spectator settings for a player identified by their UUID.
     * If the game world is not loaded, the method does nothing.
     * The spectator is set up with default settings and teleported to the world's spawn location.
     *
     * @param uuid the UUID of the player to be set as a spectator
     */
    public void addSpectatorSettings(UUID uuid) {
        if (mxWorld == null)
            return;
        addSpectatorSettings(uuid, Bukkit.getWorld(mxWorld.getWorldUID()).getSpawnLocation());
    }

    /**
     * Configures and applies spectator settings for a player identified by their UUID,
     * including teleportation, inventory updates, and visibility settings.
     *
     * @param uuid the unique identifier of the player to be set as a spectator
     * @param loc the location where the player should be teleported as a spectator
     */
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


    /**
     * Removes a spectator from the current game, restoring their visibility, health, inventory, and other settings.
     * Optionally teleports the player to the spawn location after removal.
     *
     * @param uniqueId The unique identifier of the spectator to be removed.
     * @param teleport If true, the player will be teleported to the spawn location upon removal.
     */
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

    /**
     * Removes the specified spectator from the game. This method ensures that the spectator's
     * relevant game states and properties are reset. The spectator is also removed from
     * the list of spectators in the game.
     *
     * @param uniqueId The unique identifier of the player to be removed as a spectator.
     */
    public void removeSpectator(UUID uniqueId) {
        removeSpectator(uniqueId, true);
    }

    /**
     * Retrieves the {@link GamePlayer} associated with the given player's UUID, if present.
     *
     * @param uuid the UUID of the player to find the corresponding {@link GamePlayer} for
     * @return an {@link Optional} containing the {@link GamePlayer} if the player is found; otherwise, an empty {@link Optional}
     */
    public Optional<GamePlayer> getGamePlayerOfPlayer(UUID uuid) {
        for (GamePlayer color : gamePlayers) {
            if (color.getOptionalPlayerUUID().isPresent() && color.getOptionalPlayerUUID().get().equals(uuid)) {
                return Optional.of(color);
            }
        }
        return Optional.empty();
    }

    /**
     * Adds a player to the game, initializes their inventory, teleports them to their spawn location,
     * and sets the necessary game settings for the player.
     *
     * @param playerUUID The unique identifier of the player to be added.
     * @param gamePlayer The {@link GamePlayer} instance associated with the player.
     */
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

    /**
     * Retrieves the game time in a formatted string representation.
     *
     * @return a string representing the formatted game time
     */
    public String getFormattedGameTime() {
        return formatGameTime(gameTime);
    }

    /**
     * Advances the current phase of the game to the next phase.
     * Updates the phase and optionally increments the day counter
     * if the transition involves moving from dawn to day.
     * Also broadcasts a message to all players about the phase change
     * and triggers a smooth time transition for the new phase.
     *
     * @return a CompletableFuture representing the completion of the smooth time transition.
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
     * Updates the current game phase and optionally transitions to the new phase smoothly.
     * If the new phase requires a day counter increment (e.g., transitioning from DAWN to DAY),
     * it updates the day number accordingly. Additionally, sends a broadcast message to all players
     * indicating the change in phase.
     *
     * @param newPhase The new game phase to transition to.
     * @param smoothTransition Whether the transition to the new phase should be smooth or immediate.
     * @return A CompletableFuture that completes when the phase change has been fully applied.
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

    /**
     * Starts the main phase loop if it is not already running. The phase loop is responsible
     * for transitioning through game phases and executing the logic for each phase.
     *
     * If the current phase is {@link Phase#LOBBY}, the day counter is initialized to 1, and
     * the phase transitions to {@link Phase#DAY} with a smooth transition. Once this transition
     * is complete, the current phase logic is executed.
     *
     * If the current phase is not {@link Phase#END}, the game transitions to the current phase's
     * world time, ensuring consistency, and then executes the current phase's logic.
     */
    public void startPhaseLoop() {
        if (phaseLoopRunning) return;
        phaseLoopRunning = true;
        if (this.phase == Phase.LOBBY) {
            this.dayNumber = 1;
            setPhase(Phase.DAY, true).thenRun(() ->
                    Bukkit.getScheduler().runTask(plugin, this::runCurrentPhase)
            );
        } else if (this.phase != Phase.END) {
            // Ensure world time matches current phase, then run
            cycleManager.transitionToPhase(this.phase).thenRun(() ->
                    Bukkit.getScheduler().runTask(plugin, this::runCurrentPhase)
            );
        }
    }

    /**
     * Stops the ongoing phase loop by setting the {@code phaseLoopRunning} flag to false.
     * This method is used to halt the execution of phase-related operations within the game's cycle.
     */
    public void stopPhaseLoop() {
        this.phaseLoopRunning = false;
    }

    /**
     * Executes the current game phase logic and orchestrates the corresponding actions.
     *
     * The method first logs the phase being executed. If the phase loop is not running
     * or the current phase is either the LOBBY or END, the method exits early.
     *
     * It uses the `orchestratorFactory` to create a `PhaseOrchestrator` for the current phase.
     * If no orchestrator is available for the given phase, the method calls `afterPhaseCollection`
     * to advance the phase and returns.
     *
     * If a valid orchestrator is created, it runs its collection logic. During this step,
     * it collects intents using the `intentCollector`, processes them via the `phaseExecutor`,
     * and performs any necessary actions to complete the current phase by calling
     * `afterPhaseCollection`.
     */
    private void runCurrentPhase() {
        Logger.logMessage("Running phase: " + this.phase);
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

    /**
     * Executes actions after phase collection is completed and manages the transition
     * to the subsequent phase in the game lifecycle. This method ensures that the game
     * progresses smoothly between phases while performing necessary checks and updates.
     *
     * Key Actions:
     * - Logs the current phase once phase collection is complete.
     * - Checks if the phase loop is currently running; if not, the method exits early.
     * - Advances to the next game phase using the {@link #advancePhase()} method,
     *   and upon completion, updates the server thread to potentially start the next phase.
     * - Ensures that phase-specific logic (via {@link #runCurrentPhase()}) executes
     *   if the game is still running and the current phase has not reached the end of the game.
     */
    private void afterPhaseCollection() {
        Logger.logMessage("After phase collection: " + this.phase);
        if (!phaseLoopRunning) return;
        advancePhase().thenRun(() -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Logger.logMessage("After phase advance: " + this.phase);
                if (phaseLoopRunning && this.phase != Phase.END) {
                    runCurrentPhase();
                }
            });
        });
    }

    /**
     * Formats the vote action by retrieving the number of votes cast and eligible votes
     * from the current game context and appending a descriptor string.
     *
     * @param ctx the {@code TimerContext} containing the game context and timing information
     * @return a formatted string representing the vote action, including the current timer,
     *         votes cast, eligible votes, and a descriptor
     */
    public String formatVoteAction(TimerContext ctx) {
        return formatAction(ctx, getGameVoteManager().getVotesCast(), getGameVoteManager().getEligibleVotes(), " votes");
    }

    /**
     * Formats an action string using the provided TimerContext and additional parameters.
     *
     * @param ctx the TimerContext containing game-specific timing and specification details
     * @param cast the number of actions or attempts made
     * @param eligible the total number of eligible actions or attempts
     * @param extra additional string information to append to the formatted action
     * @return a formatted string combining the timer specifications, time remaining,
     *         cast count, eligible count, and the extra string
     */
    public String formatAction(TimerContext ctx, int cast, int eligible, String extra) {
        long rem = ctx.remainingMs();
        return ctx.spec().title + " <gray>[" + TimerFormats.mmss(rem) + "] " + " <dark_gray>(<gray>" + cast + "<dark_gray>/<gray>" + eligible + extra + "<dark_gray>)";
    }
}
