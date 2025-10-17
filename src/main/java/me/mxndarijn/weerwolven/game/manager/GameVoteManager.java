package me.mxndarijn.weerwolven.game.manager;

import me.mxndarijn.weerwolven.data.WeerWolvenChatPrefix;
import me.mxndarijn.weerwolven.data.WeerWolvenLanguageText;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;
import nl.mxndarijn.mxlib.language.LanguageManager;
import nl.mxndarijn.mxlib.logger.Logger;
import nl.mxndarijn.mxlib.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

/**
 * GameVoteManager
 *
 * Supports two modes:
 * - NOMINATION: up to maxRounds nomination rounds -> per round a single nominee -> Yes/No on the nominee.
 *      * tie on Yes/No = No
 *      * if after final round and canSkip == false -> last nominee is automatically selected (no yes/no)
 *      * previously failed nominees cannot be nominated again in later rounds
 * - INSTANT: one pass; most votes wins (random among ties)
 *
 * Public vote announcements: "player X voted (2/9)"
 */
public class GameVoteManager extends GameManager {

    public GameVoteManager(Game game) {
        super(game);
    }

    public int getEligibleVotes() {
        if(state == null) return 0;
        return state.eligibleCount();
    }

    public int getVotesCast() {
        if(state == null) return 0;
        return state.votesCast();
    }

    public enum Type { NOMINATION, INSTANT }

    // Compatibility types for existing UI (GameVoteItem)
    public enum VoteMode { NOMINATION, YES_NO, INSTANT, DISABLED }
    public static final class CurrentStatus {
        public final VoteMode mode;
        public final List<GamePlayer> voteTargets;
        public final boolean allowSkip;
        public final String titleKey;
        public final boolean allowSelfVote;
        public @Nullable GamePlayer currentNominee;
        public CurrentStatus(VoteMode mode,
                             List<GamePlayer> voteTargets,
                             boolean allowSkip,
                             String titleKey,
                             boolean allowSelfVote,
                             @Nullable GamePlayer currentNominee) {
            this.mode = mode;
            this.voteTargets = voteTargets;
            this.allowSkip = allowSkip;
            this.titleKey = titleKey;
            this.allowSelfVote = allowSelfVote;
            this.currentNominee = currentNominee;
        }
    }

    private final Random rng = new Random();
    private volatile State state;

    // =============================================================================================
    // Public API
    // =============================================================================================

    /**
     * Start a NOMINATION flow.
     *
     * @param eligibleVoters players who may vote
     * @param votablePlayers players who may be voted/nominated
     * @param maxRounds number of nomination rounds (>=1)
     * @param canSkip if false, the final nominee is auto-selected without yes/no
     * @param publicVotes show "player voted (x/y)" messages when true
     * @param allowSelfVote can a player vote for themselves
     * @param nominationTitleKey title key for the nomination step
     * @param yesNoTitleKey title key for the yes/no step
     * @param onFinish callback when the full flow finishes; contains the winner (or empty if none)
     */
    public synchronized void startNominationFlow(Set<GamePlayer> eligibleVoters,
                                                 List<GamePlayer> votablePlayers,
                                                 int maxRounds,
                                                 boolean canSkip,
                                                 boolean publicVotes,
                                                 boolean allowSelfVote,
                                                 String nominationTitleKey,
                                                 String yesNoTitleKey,
                                                 ToIntFunction<GamePlayer> weightProvider,
                                                 Consumer<Finish> onFinish) {

        if (maxRounds < 1) maxRounds = 1;
        State s = new State(Type.NOMINATION, new HashSet<>(eligibleVoters), new ArrayList<>(votablePlayers),
                publicVotes, allowSelfVote, canSkip, maxRounds,
                nominationTitleKey, yesNoTitleKey, weightProvider, onFinish);
        this.state = s;

        notifyPlayersCanVote(s.eligibleVoters, nominationTitleKey);
    }

    /**
     * Start an INSTANT vote.
     */
    public synchronized void startInstantVote(Set<GamePlayer> eligibleVoters,
                                             List<GamePlayer> votablePlayers,
                                             boolean publicVotes,
                                             boolean allowSelfVote,
                                             String titleKey,
                                             ToIntFunction<GamePlayer> weightProvider,
                                             Consumer<Finish> onFinish) {

        startInstantVote(eligibleVoters, votablePlayers, publicVotes, allowSelfVote, true, titleKey, weightProvider, onFinish);
    }

    public synchronized void startInstantVote(Set<GamePlayer> eligibleVoters,
                                              List<GamePlayer> votablePlayers,
                                              boolean publicVotes,
                                              boolean allowSelfVote,
                                              boolean canSkip,
                                              String titleKey,
                                              ToIntFunction<GamePlayer> weightProvider,
                                              Consumer<Finish> onFinish) {

        State s = new State(Type.INSTANT, new HashSet<>(eligibleVoters), new ArrayList<>(votablePlayers),
                publicVotes, allowSelfVote, canSkip, 1, titleKey, titleKey, weightProvider, onFinish);
        this.state = s;

        notifyPlayersCanVote(s.eligibleVoters, titleKey);
    }

    /**
     * Player submits a candidate (NOMINATION round) or a vote (INSTANT).
     * For NOMINATION, pass null to skip (if allowed).
     */
    public boolean submitNomination(GamePlayer voter, @Nullable GamePlayer targetOrNull) {
        State s = state;
        if (s == null) return false;

        boolean accepted = s.submitNomination(voter, targetOrNull);
        if (accepted) {
            announcePlayerVotedTo(s.eligibleVoters, voter, s.votesCast(), s.eligibleCount());
        }

        // If everyone voted, resolve the step immediately.
        if (accepted) s.tryResolveAfterAllVotes(rng);
        return accepted;
    }

    /**
     * YES/NO vote for the current nominee; pass null to skip (if allowed).
     */
    public boolean submitYesNo(GamePlayer voter, @Nullable Boolean yes) {
        State s = state;
        if (s == null) return false;
        boolean accepted = s.submitYesNo(voter, yes);
        if (accepted) {
            announcePlayerVotedTo(s.eligibleVoters, voter, s.votesCast(), s.eligibleCount());
        }
        if (accepted) s.tryResolveAfterAllVotes(rng);
        return accepted;
    }

    /** Force-resolve the current step (useful on timeouts). */
    public synchronized void forceResolve() {
        State s = state;
        if (s != null) s.forceResolve(rng);
    }

    public synchronized boolean isActive() {
        return state != null && !state.finished;
    }

    // Compatibility accessors for UI
    public Optional<CurrentStatus> getCurrentStatus() {
        State s = state;
        if (s == null || s.finished) return Optional.empty();
        VoteMode mode;
        if (s.type == Type.INSTANT) mode = VoteMode.INSTANT;
        else if (s.inYesNoPhase) mode = VoteMode.YES_NO;
        else mode = VoteMode.NOMINATION;

        List<GamePlayer> targets;
        if (s.inYesNoPhase && s.currentNominee != null) targets = List.of(s.currentNominee);
        else targets = List.copyOf(s.allVotable);

        String title = s.inYesNoPhase ? s.yesNoTitleKey : s.nominationTitleKey;
        boolean allowSkip = s.inYesNoPhase ? false : s.canSkip;
        return Optional.of(new CurrentStatus(mode, targets, allowSkip, title, s.allowSelfVote, s.currentNominee));
    }

    public boolean canVote(GamePlayer voter) {
        State s = state;
        return s != null && !s.finished && s.canVote(voter);
    }

    // =============================================================================================
    // Internal State
    // =============================================================================================

    private static final class State {

        final Type type;

        // common
        final Set<GamePlayer> eligibleVoters;
        final List<GamePlayer> allVotable;
        final boolean publicVotes;
        final boolean allowSelfVote;

        // nomination specific
        final boolean canSkip;
        int round;
        final int maxRounds;
        final Set<GamePlayer> triedNominees = new HashSet<>();
        @Nullable GamePlayer currentNominee;

        // titles
        final String nominationTitleKey;
        final String yesNoTitleKey;

        // callbacks
        final Consumer<Finish> onFinish;

        // weights
        final ToIntFunction<GamePlayer> weightProvider;
        // stored at submission time
        final Map<GamePlayer, Integer> nominationWeights = new HashMap<>(); // voter -> weight snapshot at nomination submit
        final Map<GamePlayer, Integer> yesNoWeights = new HashMap<>();      // voter -> weight snapshot at yes/no submit

        // tallies (per step)
        final Map<GamePlayer, GamePlayer> nominations = new HashMap<>(); // voter -> nominee (null means skip)
        final Map<GamePlayer, Boolean> yesNo = new HashMap<>();          // voter -> yes/no (cannot be skipped)

        boolean inYesNoPhase = false;
        boolean finished = false;

        State(Type type,
              Set<GamePlayer> eligibleVoters,
              List<GamePlayer> votable,
              boolean publicVotes,
              boolean allowSelfVote,
              boolean canSkip,
              int maxRounds,
              String nominationTitleKey,
              String yesNoTitleKey,
              ToIntFunction<GamePlayer> weightProvider,
              Consumer<Finish> onFinish) {
            this.type = type;
            this.eligibleVoters = eligibleVoters;
            this.allVotable = votable;
            this.publicVotes = publicVotes;
            this.allowSelfVote = allowSelfVote;
            this.canSkip = canSkip;
            this.maxRounds = Math.max(1, maxRounds);
            this.nominationTitleKey = nominationTitleKey;
            this.yesNoTitleKey = yesNoTitleKey;
            this.onFinish = onFinish;
            this.weightProvider = weightProvider;

            this.round = 1;
        }

        public int eligibleCount() {
            int c = 0;
            for (GamePlayer gp : eligibleVoters) if (gp.getOptionalPlayerUUID().isPresent()) c++;
            return c;
        }

        public int votesCast() {
            return inYesNoPhase ? yesNo.size() : nominations.size();
        }

        boolean canVote(GamePlayer voter) {
            if (!eligibleVoters.contains(voter)) return false;
            return !inYesNoPhase ? !nominations.containsKey(voter) : !yesNo.containsKey(voter);
        }

        boolean submitNomination(GamePlayer voter, @Nullable GamePlayer target) {
            if (finished || inYesNoPhase) return false;
            if (!eligibleVoters.contains(voter)) return false;
            if (nominations.containsKey(voter)) return false;

            if (target == null) {
                if (!canSkip) return false;
                // Snapshot weight at submission time (0 for skip)
                nominationWeights.put(voter, 0);
                nominations.put(voter, null);
                return true;
            }

            if (!allVotable.contains(target)) return false;
            if (!allowSelfVote && voter.equals(target)) return false;
            if (triedNominees.contains(target)) return false; // cannot re-nominate failed nominee in later rounds

            // Snapshot weight at submission time
            int w = Math.max(0, weightProvider.applyAsInt(voter));
            nominationWeights.put(voter, w);
            nominations.put(voter, target);
            return true;
        }

        boolean submitYesNo(GamePlayer voter, @Nullable Boolean yesValue) {
            if (finished || !inYesNoPhase) return false;
            if (!eligibleVoters.contains(voter)) return false;
            if (yesNo.containsKey(voter)) return false;

            if (yesValue == null) return false; // YES/NO cannot be skipped
            // Snapshot weight at submission time for YES/NO
            int w = Math.max(0, weightProvider.applyAsInt(voter));
            yesNoWeights.put(voter, w);
            yesNo.put(voter, yesValue);
            return true;
        }

        synchronized void tryResolveAfterAllVotes(Random rng) {
            if (finished) return;
            if (votesCast() < eligibleCount()) return;
            // all eligible have voted (or we consider we've collected all intended votes)
            forceResolve(rng);
        }

        synchronized void forceResolve(Random rng) {
            if (finished) return;

            if (type == Type.INSTANT) {
                resolveInstant(rng);
                return;
            }

            // NOMINATION flow
            if (!inYesNoPhase) {
                // pick nominee for this round
                currentNominee = pickTopNominee(rng);
                if (currentNominee == null) {
                    // nobody nominated anything meaningful -> if this is last round and cannot skip, we cannot auto-pick -> finish with no winner
                    // (Alternatively you could random-pick from remaining; spec didn't request it.)
                    finish(Finish.noWinner(Type.NOMINATION, round, maxRounds,
                                                new HashMap<>(nominations), Map.of(),
                                                new HashMap<>(nominationWeights), new HashMap<>(yesNoWeights),
                                                computeNominationScores(), computeYesNoScores()));
                    return;
                }

                // final round & cannot skip => auto-select nominee (no yes/no)
                if (round == maxRounds && !canSkip) {
                    finish(Finish.winner(currentNominee, Type.NOMINATION, round, maxRounds,
                            new HashMap<>(nominations), Map.of(),
                            new HashMap<>(nominationWeights), new HashMap<>(yesNoWeights),
                            computeNominationScores(), computeYesNoScores()));
                    return;
                }

                // otherwise: move to YES/NO phase
                inYesNoPhase = true;
                yesNo.clear();
                notifyPlayersCanVote(eligibleVoters, yesNoTitleKey + " " + currentNominee.getColoredName());
                return;
            }

            // in YES/NO phase
            boolean accepted = computeYesNoAccepts(currentNominee);
            if (accepted) {
                finish(Finish.winner(currentNominee, Type.NOMINATION, round, maxRounds,
                        new HashMap<>(nominations), new HashMap<>(yesNo),
                        new HashMap<>(nominationWeights), new HashMap<>(yesNoWeights),
                        computeNominationScores(), computeYesNoScores()));
                return;
            } else {
                // No (or tie) -> prepare next round if any
                triedNominees.add(currentNominee);
                round++;
                if (round > maxRounds) {
                    // ran out of rounds -> if canSkip==false we would have auto-selected before entering yes/no
                    finish(Finish.noWinner(Type.NOMINATION, maxRounds, maxRounds,
                                                new HashMap<>(nominations), new HashMap<>(yesNo),
                                                new HashMap<>(nominationWeights), new HashMap<>(yesNoWeights),
                                                computeNominationScores(), computeYesNoScores()));
                    return;
                }

                // reset for new round (excluding tried nominee)
                nominations.clear();
                inYesNoPhase = false;
                currentNominee = null;

                notifyPlayersCanVote(eligibleVoters, nominationTitleKey);
            }
        }

        // ---------------------------------------------
        // Helpers
        // ---------------------------------------------

        private Map<GamePlayer, Integer> computeNominationScores() {
            Map<GamePlayer, Integer> tally = new HashMap<>();
            // initialize only votable and not tried when in nomination phase, else include all votable
            for (GamePlayer gp : allVotable) {
                if (!triedNominees.contains(gp)) {
                    tally.put(gp, 0);
                } else {
                    tally.putIfAbsent(gp, 0);
                }
            }
            for (Map.Entry<GamePlayer, GamePlayer> e : nominations.entrySet()) {
                GamePlayer voter = e.getKey();
                GamePlayer nominee = e.getValue();
                if (nominee == null) continue;
                int w = nominationWeights.getOrDefault(voter, 0);
                tally.computeIfPresent(nominee, (k, v) -> v + Math.max(0, w));
            }
            return tally;
        }

        private Map<Boolean, Integer> computeYesNoScores() {
            int yes = 0, no = 0;
            for (Map.Entry<GamePlayer, Boolean> e : yesNo.entrySet()) {
                Boolean b = e.getValue();
                if (b == null) continue;
                int w = yesNoWeights.getOrDefault(e.getKey(), 0);
                if (Boolean.TRUE.equals(b)) yes += Math.max(0, w); else no += Math.max(0, w);
            }
            Map<Boolean, Integer> map = new HashMap<>();
            map.put(Boolean.TRUE, yes);
            map.put(Boolean.FALSE, no);
            return map;
        }

        private void finish(Finish result) {
            finished = true;
            try {
                onFinish.accept(result);
            } catch (Exception e) {
                Logger.logMessage("[Vote] onFinish threw: " + e.getMessage());
            }
        }

        private void notifyPlayersCanVote(Set<GamePlayer> voters, String titleKey) {
            String msg = LanguageManager.getInstance().getLanguageString(
                    WeerWolvenLanguageText.VOTE_CAN_VOTE,
                    List.of(titleKey),
                    WeerWolvenChatPrefix.VOTE
            );
            for (GamePlayer voter : voters) {
                voter.getOptionalPlayerUUID().ifPresent(uuid -> {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) MessageUtil.sendMessageToPlayer(p, msg);
                });
            }
        }

        private GamePlayer pickTopNominee(Random rng) {
            Map<GamePlayer, Integer> tally = new HashMap<>();
            for (GamePlayer gp : allVotable) {
                if (!triedNominees.contains(gp)) {
                    tally.put(gp, 0);
                }
            }
            for (Map.Entry<GamePlayer, GamePlayer> e : nominations.entrySet()) {
                GamePlayer voter = e.getKey();
                GamePlayer nominee = e.getValue();
                if (nominee != null && tally.containsKey(nominee)) {
                    int w = nominationWeights.containsKey(voter)
                            ? Math.max(0, nominationWeights.get(voter))
                            : Math.max(0, weightProvider.applyAsInt(voter));
                    tally.put(nominee, tally.get(nominee) + w);
                }
            }
            int max = 0;
            for (int v : tally.values()) max = Math.max(max, v);
            if (max <= 0) return null;

            List<GamePlayer> top = new ArrayList<>();
            for (Map.Entry<GamePlayer, Integer> e2 : tally.entrySet()) {
                if (e2.getValue() == max) top.add(e2.getKey());
            }
            if (top.isEmpty()) return null;

            return top.get(rng.nextInt(top.size())); // random tie-break
        }

        private boolean computeYesNoAccepts(@Nullable GamePlayer candidate) {
            if (candidate == null) return false;
            int yes = 0, no = 0;
            for (Map.Entry<GamePlayer, Boolean> e : yesNo.entrySet()) {
                Boolean b = e.getValue();
                if (b == null) continue; // skip (shouldn't happen in yes/no)
                int w = yesNoWeights.containsKey(e.getKey())
                        ? Math.max(0, yesNoWeights.get(e.getKey()))
                        : Math.max(0, weightProvider.applyAsInt(e.getKey()));
                if (Boolean.TRUE.equals(b)) yes += w; else no += w;
            }
            if (yes == no) return false; // tie = no
            return yes > no;
        }

        private void resolveInstant(Random rng) {
            Map<GamePlayer, Integer> tally = new HashMap<>();
            for (GamePlayer gp : allVotable) tally.put(gp, 0);
            for (Map.Entry<GamePlayer, GamePlayer> e : nominations.entrySet()) {
                GamePlayer voter = e.getKey();
                GamePlayer target = e.getValue();
                if (target != null && tally.containsKey(target)) {
                    int w = nominationWeights.containsKey(voter)
                            ? Math.max(0, nominationWeights.get(voter))
                            : Math.max(0, weightProvider.applyAsInt(voter));
                    tally.put(target, tally.get(target) + w);
                }
            }
            int max = 0;
            for (int v : tally.values()) max = Math.max(max, v);

            if (max <= 0) {
                finish(Finish.noWinner(Type.INSTANT, 1, 1,
                                            new HashMap<>(nominations), Map.of(),
                                            new HashMap<>(nominationWeights), new HashMap<>(yesNoWeights),
                                            computeNominationScores(), computeYesNoScores()));
                return;
            }
            List<GamePlayer> top = new ArrayList<>();
            for (Map.Entry<GamePlayer, Integer> e2 : tally.entrySet()) {
                if (e2.getValue() == max) top.add(e2.getKey());
            }
            GamePlayer winner = top.get(rng.nextInt(top.size())); // random among ties
            finish(Finish.winner(winner, Type.INSTANT, 1, 1,
                                        new HashMap<>(nominations), Map.of(),
                                        new HashMap<>(nominationWeights), new HashMap<>(yesNoWeights),
                                        computeNominationScores(), computeYesNoScores()));
        }
    }

    // =============================================================================================
    // Results + Utilities
    // =============================================================================================

    public static final class Finish {
        public final Type type;
        public final boolean hasWinner;
        public final @Nullable GamePlayer winner;

        // For auditing / debugging
        public final int round;       // round that ended the flow
        public final int maxRounds;
        public final Map<GamePlayer, GamePlayer> finalNominations; // last nomination map collected
        public final Map<GamePlayer, Boolean> finalYesNo;          // last yes/no map collected (if any)

        // New: weight snapshots and aggregated scores
        public final Map<GamePlayer, Integer> nominationWeights;   // voter -> weight at nomination submit
        public final Map<GamePlayer, Integer> yesNoWeights;        // voter -> weight at yes/no submit
        public final Map<GamePlayer, Integer> nominationScoresByTarget; // target -> total weighted nominations
        public final Map<Boolean, Integer> yesNoScores;            // true -> total YES weight, false -> total NO weight

        private Finish(Type type,
                       boolean hasWinner,
                       @Nullable GamePlayer winner,
                       int round,
                       int maxRounds,
                       Map<GamePlayer, GamePlayer> finalNominations,
                       Map<GamePlayer, Boolean> finalYesNo,
                       Map<GamePlayer, Integer> nominationWeights,
                       Map<GamePlayer, Integer> yesNoWeights,
                       Map<GamePlayer, Integer> nominationScoresByTarget,
                       Map<Boolean, Integer> yesNoScores) {
            this.type = type;
            this.hasWinner = hasWinner;
            this.winner = winner;
            this.round = round;
            this.maxRounds = maxRounds;
            this.finalNominations = finalNominations;
            this.finalYesNo = finalYesNo;
            this.nominationWeights = nominationWeights;
            this.yesNoWeights = yesNoWeights;
            this.nominationScoresByTarget = nominationScoresByTarget;
            this.yesNoScores = yesNoScores;
        }

        public static Finish winner(GamePlayer gp, Type type, int round, int maxRounds,
                                    Map<GamePlayer, GamePlayer> noms,
                                    Map<GamePlayer, Boolean> yn,
                                    Map<GamePlayer, Integer> nominationWeights,
                                    Map<GamePlayer, Integer> yesNoWeights,
                                    Map<GamePlayer, Integer> nominationScoresByTarget,
                                    Map<Boolean, Integer> yesNoScores) {
            return new Finish(type, true, gp, round, maxRounds, noms, yn, nominationWeights, yesNoWeights, nominationScoresByTarget, yesNoScores);
        }

        public static Finish noWinner(Type type, int round, int maxRounds,
                                      Map<GamePlayer, GamePlayer> noms,
                                      Map<GamePlayer, Boolean> yn,
                                      Map<GamePlayer, Integer> nominationWeights,
                                      Map<GamePlayer, Integer> yesNoWeights,
                                      Map<GamePlayer, Integer> nominationScoresByTarget,
                                      Map<Boolean, Integer> yesNoScores) {
            return new Finish(type, false, null, round, maxRounds, noms, yn, nominationWeights, yesNoWeights, nominationScoresByTarget, yesNoScores);
        }
    }

    // =============================================================================================
    // Messaging
    // =============================================================================================

    private void notifyPlayersCanVote(Set<GamePlayer> eligibleVoters, String titleKey) {
        String msg = LanguageManager.getInstance().getLanguageString(
                WeerWolvenLanguageText.VOTE_CAN_VOTE,
                List.of(titleKey),
                WeerWolvenChatPrefix.VOTE
        );
        for (GamePlayer voter : eligibleVoters) {
            voter.getOptionalPlayerUUID().ifPresent(uuid -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) MessageUtil.sendMessageToPlayer(p, msg);
            });
        }
        for(UUID host : game.getHosts()) {
            Player p = Bukkit.getPlayer(host);
            if (p != null)
                MessageUtil.sendMessageToPlayer(p, LanguageManager.getInstance().getLanguageString(
                        WeerWolvenLanguageText.VOTE_PLAYERS_CAN_NOW_VOTE,
                        List.of(titleKey),
                        WeerWolvenChatPrefix.VOTE
                ));
        }
    }

    private void announcePlayerVoted(GamePlayer voter, int votedCount, int total) {
        String name = voter.getOptionalPlayerUUID()
                .map(Bukkit::getPlayer)
                .map(Player::getName)
                .orElse("Unknown");
        String color = voter.getColorData().getColor().getDisplayName();
        String msg = LanguageManager.getInstance().getLanguageString(
                WeerWolvenLanguageText.GAME_PLAYER_VOTED,
                new ArrayList<>(Arrays.asList(name, color, String.valueOf(votedCount), String.valueOf(total)))
        );
        this.game.sendMessageToAll(msg);
    }

    private void announcePlayerVotedTo(Set<GamePlayer> eligibleVoters, GamePlayer voter, int votedCount, int total) {
        String name = voter.getOptionalPlayerUUID()
                .map(Bukkit::getPlayer)
                .map(Player::getName)
                .orElse("Unknown");
        String color = voter.getColorData().getColor().getDisplayName();
        String msg = LanguageManager.getInstance().getLanguageString(
                WeerWolvenLanguageText.GAME_PLAYER_VOTED,
                new ArrayList<>(Arrays.asList(name, color, String.valueOf(votedCount), String.valueOf(total))),
                WeerWolvenChatPrefix.VOTE
        );
        // Send to eligible voters
        for (GamePlayer gp : eligibleVoters) {
            gp.getOptionalPlayerUUID().ifPresent(uuid -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) MessageUtil.sendMessageToPlayer(p, msg);
            });
        }
        // Send to hosts
        for (UUID host : game.getHosts()) {
            Player p = Bukkit.getPlayer(host);
            if (p != null) MessageUtil.sendMessageToPlayer(p, msg);
        }
    }
}
