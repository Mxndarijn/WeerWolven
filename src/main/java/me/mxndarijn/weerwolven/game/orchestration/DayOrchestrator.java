package me.mxndarijn.weerwolven.game.orchestration;

import me.mxndarijn.weerwolven.data.*;
import me.mxndarijn.weerwolven.game.action.RoleAbilityRegistry;
import me.mxndarijn.weerwolven.game.action.RoleAbilityRegistry.AbilityDef;
import me.mxndarijn.weerwolven.game.bus.events.DayVoteCompletedEvent;
import me.mxndarijn.weerwolven.game.bus.events.DayVoteWeightEvent;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;
import me.mxndarijn.weerwolven.game.phase.Phase;
import me.mxndarijn.weerwolven.game.status.FlagStatus;
import me.mxndarijn.weerwolven.game.status.StatusKey;
import nl.mxndarijn.mxlib.language.LanguageKey;
import nl.mxndarijn.mxlib.language.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

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
     * Override the default collection to run a town-wide nomination vote.
     * Everyone alive can vote; UI is provided elsewhere. When the vote is
     * closed by another component, the callback will compute weighted results
     * by posting DayVoteWeightEvent per voter, emit DayVoteCompletedEvent,
     * and then continue to the next phase.
     */
    @Override
    public void runCollection(Runnable onDone) {
        // Eligible voters and votable players: all alive players
        List<GamePlayer> alive = game.getGamePlayers().stream()
                .filter(GamePlayer::isAlive)
                .collect(Collectors.toList());
        var eligible = new java.util.HashSet<>(alive);

        // 1) Mayor election at the beginning of the day if no mayor status present
        boolean hasMayor = alive.stream().anyMatch(gp -> gp.getStatusStore().has(StatusKey.VOTE_DOUBLE_MAYOR));
        if (!hasMayor) {
            game.sendMessageToAll("vote mayor");
            game.getGameVoteManager().startInstantVote(
                    eligible,
                    alive,
                    false, // publicVotes
                    false,  // allowSelfVote
                    false,
                    "<blue>Burgemeester",
                    finish -> {
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
            return; // wait for mayor election callback before proceeding
        }

        // If mayor already exists, proceed directly to lynch vote
        startLynchVote(alive, eligible, onDone);
    }

    private void startLynchVote(List<GamePlayer> alive, Set<GamePlayer> eligible, Runnable onDone) {
        // Start a nomination round; allow skip; top-1 nominee by default
        game.sendMessageToAll("vote player");
        game.getGameVoteManager().startNominationFlow(
                eligible,
                alive,
                3,      // maxRounds
                true,   // canSkip
                false,  // publicVotes
                false,  // allowSelfVote
                "<blue>Speler uit het dorp stemmen.",
                "<gray>Ja/Nee",
                finish -> {
                    // Compute weights per voter and aggregate per candidate using raw nominations
                    Map<GamePlayer, Integer> weighted = new LinkedHashMap<>();
                    for (GamePlayer cand : alive) weighted.put(cand, 0);
                    for (var e : finish.finalNominations.entrySet()) {
                        GamePlayer voter = e.getKey();
                        GamePlayer nominee = e.getValue();
                        if (nominee == null) continue; // skip votes
                        // Ask listeners for weight
                        var evt = new DayVoteWeightEvent(voter);
                        game.getGameEventBus().post(evt);
                        int w = Math.max(0, evt.getWeight());
                        weighted.computeIfPresent(nominee, (k, v) -> v + w);
                    }

                    // Determine winner (highest weighted). If tie or none, winner empty.
                    Optional<GamePlayer> winner = weighted.entrySet().stream()
                            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                            .map(Map.Entry::getKey)
                            .findFirst();
                    if (winner.isPresent()) {
                        int top = weighted.get(winner.get());
                        long same = weighted.values().stream().filter(v -> v == top).count();
                        if (same > 1) winner = Optional.empty();
                    }

                    // Emit completion event with details
                    game.getGameEventBus().post(new DayVoteCompletedEvent(
                            weighted,
                            winner,
                            finish
                    ));

                    // Continue orchestration
                    onDone.run();
                }
        );
    }

    private boolean hasAbilityAtTiming(Roles role, ActionKind kind, Timing timing) {
        for (AbilityDef def : RoleAbilityRegistry.of(role)) {
            if (def.kind() == kind && def.timing() == timing) return true;
        }
        return false;
    }
}
