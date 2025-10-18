package me.mxndarijn.weerwolven.game.orchestration;

import me.mxndarijn.weerwolven.data.*;
import me.mxndarijn.weerwolven.game.action.RoleAbilityRegistry;
import me.mxndarijn.weerwolven.game.action.RoleAbilityRegistry.AbilityDef;
import me.mxndarijn.weerwolven.game.bus.events.DayVoteWeightEvent;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;
import me.mxndarijn.weerwolven.game.manager.GameChatManager;
import me.mxndarijn.weerwolven.game.manager.GameVisibilityManager;
import me.mxndarijn.weerwolven.game.orchestration.executor.AbilityExecutorRegistry;
import me.mxndarijn.weerwolven.game.phase.Phase;
import me.mxndarijn.weerwolven.game.status.FlagStatus;
import me.mxndarijn.weerwolven.game.status.StatusKey;
import nl.mxndarijn.mxlib.language.LanguageManager;
import me.mxndarijn.weerwolven.game.timer.*;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public final class DayOrchestrator extends PhaseOrchestrator {

    private static final List<ActionKind> DAY_ORDER = List.of(
            ActionKind.WOLF_VOTES_X2
    );

    public DayOrchestrator(Game game,
                           IntentCollector collector,
                           OrchestrationConfig config,
                           DefaultDecisionPolicy defaults,
                           AbilityExecutorRegistry execs,
                           ResolvePolicy resolvePolicy,
                           Executor mainExec) {
        super(game, collector, config, defaults, execs, resolvePolicy, mainExec);
    }

    @Override
    protected List<ActionKind> orderedKinds() {
        return DAY_ORDER;
    }

    @Override
    protected List<GamePlayer> actorsFor(ActionKind kind) {
        return game.getGamePlayers().stream()
                .filter(GamePlayer::isAlive)
                .filter(gp -> hasAbilityAtTiming(gp.getRole(), kind, Timing.DAY))
                .sorted(Comparator.comparing(gp -> gp.getColorData().getColor().getDisplayName()))
                .collect(Collectors.toList());
    }

    /**
     * Override the default collection to run a town-wide nomination vote.+
     * Everyone alive can vote; UI is provided elsewhere. When the vote is
     * closed by another component, the callback will compute weighted results
     * by posting DayVoteWeightEvent per voter, emit DayVoteCompletedEvent,
     * and then continue to the next phase.
     */
    @Override
    public void runCollection(Runnable onDone) {

        //Players can leave their house
        game.getGameHouseManager().setAllPlayersCanOpenDoors(true);
        game.getGameHouseManager().openAllDoors(game.getGamePlayers());
        game.getGameHouseManager().openAllWindows(game.getGamePlayers());
        game.getGameVisibilityManager().setCurrentState(GameVisibilityManager.VisibilityState.everyone());
        game.getGameChatManager().setCurrentState(GameChatManager.ChatState.defaultState());

        // Eligible voters and votable players: all alive players
        List<GamePlayer> alive = game.getGamePlayers().stream()
                .filter(GamePlayer::isAlive)
                .collect(Collectors.toList());
        var eligible = new java.util.HashSet<>(alive);

        // 1) Mayor election at the beginning of the day if no mayor status present
        boolean hasMayor = alive.stream().anyMatch(gp -> gp.getStatusStore().has(StatusKey.VOTE_DOUBLE_MAYOR));
        if (!hasMayor) {
            // Start mayor vote and a group timer bound to it
            String mayorTimerId = "vote:mayor:" + game.getDayNumber();
            long mayorDuration = 600_000L;
            game.getGameVoteManager().startInstantVote(
                    eligible,
                    alive,
                    false, // publicVotes
                    false,  // allowSelfVote
                    false,
                    "<blue>Burgemeester", gp -> 1,
                    finish -> {
                        // cancel timer on completion
                        game.getActionTimerService().cancel(mayorTimerId);
                        if (finish.hasWinner && finish.winner != null) {
                            game.sendMessageToAll(LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.MAYOR_ELECTED,List.of(finish.winner.getColoredName()), WeerWolvenChatPrefix.VOTE));
                            finish.winner.getStatusStore().add(new FlagStatus(
                                    StatusKey.VOTE_DOUBLE_MAYOR,
                                    null,
                                    Integer.MAX_VALUE,
                                    Phase.END
                            ));
                        } else {
                            game.sendMessageToAll(LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.MAYOR_ELECTED_ERROR, WeerWolvenChatPrefix.VOTE));
                        }
                        startLynchVote(alive, eligible, onDone);
                    }
            );
            // Start timer bound to that vote
            {
                var spec = new TimerSpec(mayorTimerId, "<blue>Burgemeester", TimerScope.GROUP, eligible, mayorDuration, game::formatVoteAction, ctx -> {
                    // onTimeout -> resolve vote
                    game.getGameVoteManager().forceResolve();
                }, null, null);
                game.getActionTimerService().addTimer(spec);
            }
            return; // wait for mayor election callback before proceeding
        }

        // If mayor already exists, proceed directly to lynch vote
        startLynchVote(alive, eligible, onDone);
    }

    private void startLynchVote(List<GamePlayer> alive, Set<GamePlayer> eligible, Runnable onDone) {
        // Start a nomination round; allow skip; top-1 nominee by default
        String lynchTimerId = "vote:lynch:" + game.getDayNumber();
        long lynchDuration = 600_000L;
        game.getGameVoteManager().startNominationFlow(
                eligible,
                alive,
                3,      // maxRounds
                true,   // canSkip
                false,  // publicVotes
                false,  // allowSelfVote
                "<blue>Speler uit het dorp stemmen",
                "<gray>Ja/Nee", gp -> {
                    DayVoteWeightEvent e = new DayVoteWeightEvent(gp);
                    game.getGameEventBus().post(e);
                    return e.getWeight();
                },
                finish -> {
                    // cancel timer on completion
                    game.getActionTimerService().cancel(lynchTimerId);

//                    // Emit completion event with details
//                    game.getGameEventBus().post(new DayVoteCompletedEvent(
//                            weighted,
//                            winner,
//                            finish
//                    ));

                    // Continue orchestration
                    onDone.run();
                }
        );
        var spec = new TimerSpec(lynchTimerId, "<blue>Speler uit het dorp stemmen", TimerScope.GROUP, eligible, lynchDuration, game::formatVoteAction, ctx -> {
            // onTimeout -> resolve vote
            game.getGameVoteManager().forceResolve();
        }, null, null);
        game.getActionTimerService().addTimer(spec);
    }

    private boolean hasAbilityAtTiming(Roles role, ActionKind kind, Timing timing) {
        for (AbilityDef def : RoleAbilityRegistry.of(role)) {
            if (def.kind() == kind && def.timing() == timing) return true;
        }
        return false;
    }
}
