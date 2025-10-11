package me.mxndarijn.weerwolven.game;

import me.mxndarijn.weerwolven.game.status.IntStatus;
import me.mxndarijn.weerwolven.game.status.Status;

import java.util.*;
import java.util.stream.Collectors;

public final class StatusStore {
    private final Map<StatusKey, List<Status>> byKey = new EnumMap<>(StatusKey.class);

    public void add(Status s) {
        var list = byKey.computeIfAbsent(s.key(), k -> new ArrayList<>());
        if (!s.stackable()) list.clear();
        list.add(s);
    }

    public void remove(StatusKey key) { byKey.remove(key); }

    public boolean has(StatusKey key) {
        var list = byKey.get(key);
        return list != null && !list.isEmpty();
    }

    public Optional<Status> getOne(StatusKey key) {
        var list = byKey.get(key);
        return (list == null || list.isEmpty()) ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<Status> getAll(StatusKey key) { return byKey.getOrDefault(key, List.of()); }

    public int sumInt(StatusKey key) {
        return getAll(key).stream()
                .filter(s -> s instanceof IntStatus i).mapToInt(s -> ((IntStatus) s).amount())
                .sum();
    }

    public List<Status> cleanupExpired(Game game) {
        List<Status> removed = new ArrayList<>();
        for (var entry : new ArrayList<>(byKey.entrySet())) {
            var kept = entry.getValue().stream()
                    .filter(s -> !s.isExpired(game))
                    .collect(Collectors.toCollection(ArrayList::new));
            if (kept.size() != entry.getValue().size()) removed.addAll(entry.getValue());
            if (kept.isEmpty()) byKey.remove(entry.getKey()); else entry.setValue(kept);
        }
        return removed;
    }

    public Map<StatusKey, List<Status>> view() { return Collections.unmodifiableMap(byKey); }
}

