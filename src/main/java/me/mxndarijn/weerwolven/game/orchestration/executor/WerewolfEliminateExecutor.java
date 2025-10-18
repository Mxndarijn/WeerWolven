package me.mxndarijn.weerwolven.game.orchestration.executor;

import me.mxndarijn.weerwolven.data.*;
import me.mxndarijn.weerwolven.game.action.ActionIntent;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;
import me.mxndarijn.weerwolven.game.manager.GameChatManager;
import me.mxndarijn.weerwolven.game.manager.GameVisibilityManager;
import me.mxndarijn.weerwolven.game.timer.TimerFormats;
import me.mxndarijn.weerwolven.game.timer.TimerScope;
import me.mxndarijn.weerwolven.game.timer.TimerSpec;
import nl.mxndarijn.mxlib.language.LanguageManager;
import nl.mxndarijn.mxlib.logger.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Sound;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class WerewolfEliminateExecutor implements TeamAbilityExecutor {
    
    @Override
    public CompletableFuture<List<ActionIntent>> executeTeam(Game game, List<GamePlayer> actors, long timeoutMs) {
        Logger.logMessage("exuting werewolf eliminate");
        CompletableFuture<List<ActionIntent>> future = new CompletableFuture<>();

        // Set up werewolf chat
        game.getGameChatManager().setCurrentState(new GameChatManager.ChatState(
                WeerWolvenChatPrefix.WEREWOLF_CHAT,
                actors::contains,
                actors::contains,
                uuid -> false,
                uuid -> false
        ));
        
        // Open doors for werewolves to allow free movement
        actors.forEach(gp -> {
            game.getGameHouseManager().openHouseDoor(gp, null);
            game.getGameHouseManager().openHouseWindows(gp);
            game.getGameHouseManager().setCanOpenDoor(gp, true);
        });

        String timerId = "werewolf:eliminate:" + game.getDayNumber();
        
        // Start vote with 5-minute timer (300,000 ms)
        game.getGameVoteManager().startInstantVote(
                new HashSet<>(actors),
                game.getGamePlayers().stream().filter(gp -> !actors.contains(gp) && gp.isAlive()).toList(),
                true,
                false,
                true, // Allow skip
                "<red>Weerwolven Eliminatie",
                gp -> 1,
                finish -> {
                    // Cancel timer when vote completes
                    game.getActionTimerService().cancel(timerId);
                    
                    // Get the vote result
                    Optional<GamePlayer> targetOpt = Optional.ofNullable(finish.winner);
                    resetToBefore(game);
                    List<ActionIntent> intents;

                    intents = targetOpt.map(gamePlayer -> complete(game, actors, gamePlayer)).orElseGet(ArrayList::new);

                    String homeTimerId = "werewolf:eliminate:home:" + game.getDayNumber();
                    AbilityExecutorHelper.goHome(actors, game, homeTimerId).thenAccept(success -> {
                        game.getGameVisibilityManager().setCurrentState(GameVisibilityManager.VisibilityState.noOne());
                        future.complete(intents);
                    });
                }
        );

        // Start a 5-minute timer (300,000 ms)
        var spec = new TimerSpec(
                timerId,
                "<blue>Weerwolven Eliminatie",
                TimerScope.GROUP,
                new HashSet<>(actors),
                240_000L,
                game::formatVoteAction,
                ctx -> {
                    if (!future.isDone()) {
                        Bukkit.getScheduler().runTask(game.getPlugin(), () -> {
                            game.getGameVoteManager().forceResolve();
                        });
                    }
                },
                null,
                null
        );
        game.getActionTimerService().addTimer(spec);

        return future;
    }

    //Should never execute
    @Override
    public List<ActionIntent> defaultExecute(Game game, List<GamePlayer> actors, long timeoutMs) {
        // Select a random non-werewolf player as target
        List<GamePlayer> candidates = new ArrayList<>();
        for (GamePlayer gp : game.getGamePlayers()) {
            if (gp.getRole().getTeam() != Team.WEREWOLF && gp.isAlive()) {
                candidates.add(gp);
            }
        }
        
        if (candidates.isEmpty()) {
            return List.of();
        }
        
        GamePlayer target = candidates.get(new Random().nextInt(candidates.size()));
        
        // Host log for default execution
        game.sendMessageToHosts(LanguageManager.getInstance().getLanguageString(
                WeerWolvenLanguageText.GAME_HOST_LOG_EXECUTING_DEFAULT_TIMEOUT,
                List.of("<blue>Weerwolven", "<gray>Weerwolven")));
        return complete(game, actors, target);
    }

    private List<ActionIntent> complete(Game game, List<GamePlayer> actors, GamePlayer target) {

        AbilityExecutorHelper.playSoundInRadiusSmooth(target, Sound.ENTITY_WOLF_GROWL, 1.5f, 0.8f, 30);
        AbilityExecutorHelper.playSoundAtPlayers(actors, Sound.ENTITY_WOLF_GROWL, 1.2f, 0.8f);
        ActionIntent intent = new ActionIntent(
                actors,
                ActionKind.TEAM_ELIMINATE,
                Timing.NIGHT,
                Map.of("target", target),
                50

        );
        return List.of(intent);
    }

    private void resetToBefore(Game game) {
        game.getGameChatManager().setCurrentState(GameChatManager.ChatState.noOne());
        game.getGameVoteManager().stopVote();
    }
}
