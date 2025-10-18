package me.mxndarijn.weerwolven.game.timer;

import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;
import me.mxndarijn.weerwolven.data.UpcomingGameStatus;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class ActionTimerService {
    private final Game game;
    private BukkitTask loopTask;

    private final Map<String, TimerSpec> byId = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> perPlayerActiveIds = new ConcurrentHashMap<>();

    // Cache last shown seconds to reduce spam per player
    private final Map<UUID, String> lastPlayerShown = new ConcurrentHashMap<>();
    private String lastHostShownSecondKey = null;

    // When frozen, timers should not progress. We record when the freeze started
    // and use that fixed time as the effective clock. Upon unfreeze we shift
    // all timers forward by the frozen duration so they resume where they left off.
    private Long freezeStartMs = null;

    public ActionTimerService(Game game) { this.game = game; }

    public synchronized void start() {
        if (loopTask != null) return;
        loopTask = Bukkit.getScheduler().runTaskTimer(game.getPlugin(), this::tick, 0L, 10L); // 0.5s
    }

    public synchronized void stop() {
        if (loopTask != null) loopTask.cancel();
        loopTask = null;
        byId.clear();
        perPlayerActiveIds.clear();
        lastPlayerShown.clear();
        lastHostShownSecondKey = null;
    }

    public void addTimer(TimerSpec spec) {
        spec.startedAtMs = System.currentTimeMillis();
        byId.put(spec.id, spec);
        for (GamePlayer gp : spec.audience) {
            gp.getOptionalPlayerUUID().ifPresent(uuid ->
                    perPlayerActiveIds.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(spec.id)
            );
        }
    }

    public void cancel(String id) {
        TimerSpec spec = byId.remove(id);
        if (spec == null) return;
        for (GamePlayer gp : spec.audience) {
            gp.getOptionalPlayerUUID().ifPresent(uuid -> {
                var set = perPlayerActiveIds.get(uuid);
                if (set != null) set.remove(id);
            });
        }
        if (spec.onCancel != null) spec.onCancel.accept(new TimerContext(game, spec));
    }

    private void tick() {
        long now = System.currentTimeMillis();
        boolean frozen = game.getGameInfo().getStatus() == UpcomingGameStatus.FREEZE;
        long effectiveNow = now;

        if (frozen) {
            if (freezeStartMs == null) freezeStartMs = now;
            effectiveNow = freezeStartMs;
        } else {
            if (freezeStartMs != null) {
                long frozenDuration = now - freezeStartMs;
                // Shift all timers forward so elapsed time excludes frozen duration
                for (TimerSpec spec : byId.values()) {
                    spec.startedAtMs += frozenDuration;
                }
                freezeStartMs = null;
            }
        }

        List<TimerSpec> expired = new ArrayList<>();

        // Render per player action bar (only if mm:ss changed)
        for (UUID uuid : perPlayerActiveIds.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            TimerSpec best = pickBestFor(uuid, effectiveNow);
            if (best == null) continue;
            long remaining = Math.max(0, best.durationMs - (effectiveNow - best.startedAtMs));
            String secondsKey = best.id + ":" + (remaining / 1000);
            String last = lastPlayerShown.get(uuid);
            if (!secondsKey.equals(last)) {
                String line = best.renderForPlayer.apply(new TimerContext(game, best, effectiveNow, remaining));
                sendActionBar(p, line);
                lastPlayerShown.put(uuid, secondsKey);
            }
        }

        // Check expiration and callbacks
        for (TimerSpec spec : byId.values()) {
            long elapsed = effectiveNow - spec.startedAtMs;
            long remaining = Math.max(0, spec.durationMs - elapsed);
            if (spec.onTick != null) safeAccept(spec.onTick, new TimerContext(game, spec, effectiveNow, remaining));
            // Do not expire timers while the game is frozen
            if (remaining <= 0 && !frozen) {
                expired.add(spec);
            }
        }

        // Handle expirations after iteration
        for (TimerSpec spec : expired) {
            cancel(spec.id);
            safeAccept(spec.onTimeout, new TimerContext(game, spec, effectiveNow, 0));
        }

        // Host rotation line (only once per second)
        String hostKey = Long.toString(effectiveNow / 1000);
        if (!hostKey.equals(lastHostShownSecondKey)) {
            String hostLine = renderForHost(effectiveNow);
            if (hostLine != null) {
                for (UUID host : game.getHosts()) {
                    Player hp = Bukkit.getPlayer(host);
                    if (hp != null) sendActionBar(hp, hostLine);
                }
            }
            lastHostShownSecondKey = hostKey;
        }
    }

    private TimerSpec pickBestFor(UUID uuid, long now) {
        var ids = perPlayerActiveIds.get(uuid);
        if (ids == null || ids.isEmpty()) return null;
        TimerSpec best = null; long bestRemaining = Long.MAX_VALUE;
        for (String id : ids) {
            var spec = byId.get(id);
            if (spec == null) continue;
            long remaining = Math.max(0, spec.durationMs - (now - spec.startedAtMs));
            if (remaining < bestRemaining) { bestRemaining = remaining; best = spec; }
        }
        return best;
    }

    private String renderForHost(long now) {
        if (byId.isEmpty()) return null;
        List<TimerSpec> specs = new ArrayList<>(byId.values());
        specs.sort(Comparator.comparing(s -> s.id));
        int idx = (int)((now / 1000) % specs.size());
        TimerSpec s = specs.get(idx);
        long remaining = Math.max(0, s.durationMs - (now - s.startedAtMs));
        return s.renderForPlayer.apply(new TimerContext(game, s, now, remaining));
    }

    private static void sendActionBar(Player p, String line) {
        if (line == null || line.isEmpty()) return;
        p.sendActionBar(MiniMessage.miniMessage().deserialize(line));
    }

    private static void safeAccept(Consumer<TimerContext> c, TimerContext ctx) {
        try { c.accept(ctx); } catch (Exception ignored) {}
    }

    public List<TimerSpec> getTimers() {
        return byId.values().stream().toList();
    }

    public void forceEnd(TimerSpec spec) {
        spec.durationMs = 0;
    }
}
