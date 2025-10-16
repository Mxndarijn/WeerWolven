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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

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
                                                 Consumer<Finish> onFinish) {

        if (maxRounds < 1) maxRounds = 1;
        State s = new State(Type.NOMINATION, new HashSet<>(eligibleVoters), new ArrayList<>(votablePlayers),
                publicVotes, allowSelfVote, canSkip, maxRounds,
                nominationTitleKey, yesNoTitleKey, onFinish);
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
                                              Consumer<Finish> onFinish) {

        startInstantVote(eligibleVoters, votablePlayers, publicVotes, allowSelfVote, true, titleKey, onFinish);
    }

    public synchronized void startInstantVote(Set<GamePlayer> eligibleVoters,
                                              List<GamePlayer> votablePlayers,
                                              boolean publicVotes,
                                              boolean allowSelfVote,
                                              boolean canSkip,
                                              String titleKey,
                                              Consumer<Finish> onFinish) {

        State s = new State(Type.INSTANT, new HashSet<>(eligibleVoters), new ArrayList<>(votablePlayers),
                publicVotes, allowSelfVote, canSkip, 1, titleKey, titleKey, onFinish);
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

            this.round = 1;
        }

        int eligibleCount() {
            int c = 0;
            for (GamePlayer gp : eligibleVoters) if (gp.getOptionalPlayerUUID().isPresent()) c++;
            return c;
        }

        int votesCast() {
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
                nominations.put(voter, null);
                return true;
            }

            if (!allVotable.contains(target)) return false;
            if (!allowSelfVote && voter.equals(target)) return false;
            if (triedNominees.contains(target)) return false; // cannot re-nominate failed nominee in later rounds

            nominations.put(voter, target);
            return true;
        }

        boolean submitYesNo(GamePlayer voter, @Nullable Boolean yesValue) {
            if (finished || !inYesNoPhase) return false;
            if (!eligibleVoters.contains(voter)) return false;
            if (yesNo.containsKey(voter)) return false;

            if (yesValue == null) return false; // YES/NO cannot be skipped
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
                    finish(Finish.noWinner(Type.NOMINATION, round, maxRounds, new HashMap<>(nominations), Map.of()));
                    return;
                }

                // final round & cannot skip => auto-select nominee (no yes/no)
                if (round == maxRounds && !canSkip) {
                    finish(Finish.winner(currentNominee, Type.NOMINATION, round, maxRounds,
                            new HashMap<>(nominations), Map.of()));
                    return;
                }

                // otherwise: move to YES/NO phase
                inYesNoPhase = true;
                yesNo.clear();
                notifyPlayersCanVote(eligibleVoters, yesNoTitleKey);
                return;
            }

            // in YES/NO phase
            boolean accepted = computeYesNoAccepts(currentNominee);
            if (accepted) {
                finish(Finish.winner(currentNominee, Type.NOMINATION, round, maxRounds,
                        new HashMap<>(nominations), new HashMap<>(yesNo)));
                return;
            } else {
                // No (or tie) -> prepare next round if any
                triedNominees.add(currentNominee);
                round++;
                if (round > maxRounds) {
                    // ran out of rounds -> if canSkip==false we would have auto-selected before entering yes/no
                    finish(Finish.noWinner(Type.NOMINATION, maxRounds, maxRounds, new HashMap<>(nominations), new HashMap<>(yesNo)));
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
            for (GamePlayer v : nominations.values()) {
                if (v != null && tally.containsKey(v)) {
                    tally.put(v, tally.get(v) + 1);
                }
            }
            int max = 0;
            for (int v : tally.values()) max = Math.max(max, v);
            if (max <= 0) return null;

            List<GamePlayer> top = new ArrayList<>();
            for (Map.Entry<GamePlayer, Integer> e : tally.entrySet()) {
                if (e.getValue() == max) top.add(e.getKey());
            }
            if (top.isEmpty()) return null;

            return top.get(rng.nextInt(top.size())); // random tie-break
        }

        private boolean computeYesNoAccepts(@Nullable GamePlayer candidate) {
            if (candidate == null) return false;
            int yes = 0, no = 0;
            for (Boolean b : yesNo.values()) {
                if (b == null) continue; // skip
                if (Boolean.TRUE.equals(b)) yes++; else no++;
            }
            if (yes == no) return false; // tie = no
            return yes > no;
        }

        private void resolveInstant(Random rng) {
            Map<GamePlayer, Integer> tally = new HashMap<>();
            for (GamePlayer gp : allVotable) tally.put(gp, 0);
            for (GamePlayer v : nominations.values()) {
                if (v != null && tally.containsKey(v)) {
                    tally.put(v, tally.get(v) + 1);
                }
            }
            int max = 0;
            for (int v : tally.values()) max = Math.max(max, v);

            if (max <= 0) {
                finish(Finish.noWinner(Type.INSTANT, 1, 1, new HashMap<>(nominations), Map.of()));
                return;
            }
            List<GamePlayer> top = new ArrayList<>();
            for (Map.Entry<GamePlayer, Integer> e : tally.entrySet()) {
                if (e.getValue() == max) top.add(e.getKey());
            }
            GamePlayer winner = top.get(rng.nextInt(top.size())); // random among ties
            finish(Finish.winner(winner, Type.INSTANT, 1, 1, new HashMap<>(nominations), Map.of()));
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

        private Finish(Type type,
                       boolean hasWinner,
                       @Nullable GamePlayer winner,
                       int round,
                       int maxRounds,
                       Map<GamePlayer, GamePlayer> finalNominations,
                       Map<GamePlayer, Boolean> finalYesNo) {
            this.type = type;
            this.hasWinner = hasWinner;
            this.winner = winner;
            this.round = round;
            this.maxRounds = maxRounds;
            this.finalNominations = finalNominations;
            this.finalYesNo = finalYesNo;
        }

        public static Finish winner(GamePlayer gp, Type type, int round, int maxRounds,
                                    Map<GamePlayer, GamePlayer> noms,
                                    Map<GamePlayer, Boolean> yn) {
            return new Finish(type, true, gp, round, maxRounds, noms, yn);
        }

        public static Finish noWinner(Type type, int round, int maxRounds,
                                      Map<GamePlayer, GamePlayer> noms,
                                      Map<GamePlayer, Boolean> yn) {
            return new Finish(type, false, null, round, maxRounds, noms, yn);
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
