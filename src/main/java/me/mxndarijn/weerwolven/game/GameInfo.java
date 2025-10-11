package me.mxndarijn.weerwolven.game;

import lombok.Getter;
import lombok.Setter;
import me.mxndarijn.weerwolven.data.UpcomingGameStatus;
import me.mxndarijn.weerwolven.data.WeerWolvenLanguageText;
import me.mxndarijn.weerwolven.data.WeerWolvenPermissions;
import me.mxndarijn.weerwolven.managers.GameManager;
import me.mxndarijn.weerwolven.managers.PresetsManager;
import me.mxndarijn.weerwolven.managers.RoleSetManager;
import me.mxndarijn.weerwolven.presets.Preset;
import nl.mxndarijn.mxlib.configfiles.ConfigService;
import nl.mxndarijn.mxlib.configfiles.StandardConfigFile;
import nl.mxndarijn.mxlib.item.MxSkullItemStackBuilder;
import nl.mxndarijn.mxlib.language.LanguageManager;
import nl.mxndarijn.mxlib.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GameInfo {

    private final List<UUID> queue;
    @Getter
    private String presetId;
    @Getter
    private String roleSetId;

    @Getter
    private UUID host;
    @Getter
    private LocalDateTime time;
    @Setter
    @Getter
    private UpcomingGameStatus status;

    private GameInfo() {
        queue = new ArrayList<>();
        status = UpcomingGameStatus.WAITING;
    }

    public static GameInfo create(Preset preset, UUID host, LocalDateTime time, RoleSet roleSet) {
        GameInfo game = new GameInfo();
        game.host = host;
        game.presetId = preset.getDirectory().getName();
        game.roleSetId = roleSet != null ? roleSet.getId() : null;
        game.time = time;

        return game;
    }

    public static Optional<GameInfo> loadFromFile(java.util.Map<String, Object> map) {
        GameInfo game = new GameInfo();
        game.host = UUID.fromString((String) map.get("host"));
        game.presetId = (String) map.get("presetId");
        game.roleSetId = (String) map.get("roleSetId");
        game.time = LocalDateTime.parse((String) map.get("time"));

        if (game.time.isBefore(LocalDateTime.now()))
            return Optional.empty();

        Optional<Preset> optionalPreset = PresetsManager.getInstance().getPresetById(game.presetId);
        if (optionalPreset.isEmpty()) {
            return Optional.empty();
        }
        if (game.roleSetId != null && !game.roleSetId.isEmpty()) {
            if (RoleSetManager.getInstance().getById(game.roleSetId).isEmpty()) {
                return Optional.empty();
            }
        }

        return Optional.of(game);
    }

    public java.util.Map<String, Object> getDataForSaving() {
        java.util.Map<String, Object> map = new HashMap<>();
        map.put("host", host.toString());
        map.put("presetId", presetId);
        if (roleSetId != null) map.put("roleSetId", roleSetId);
        map.put("time", time.toString());

        return map;
    }

    public ItemStack getItemStack(Player p) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        String formattedTime = time.format(formatter);
        Optional<Preset> optionalPreset = PresetsManager.getInstance().getPresetById(presetId);
        if (optionalPreset.isEmpty()) {
            GameManager.getInstance().removeUpcomingGame(this);
            throw new IllegalStateException("Preset not found");
        }
        Preset preset = optionalPreset.get();
        MxSkullItemStackBuilder builder = MxSkullItemStackBuilder.create(1)
                .setSkinFromHeadsData(preset.getConfig().getSkullId())
                .setName("<gray>" + preset.getConfig().getName())
                .addBlankLore()
                .addLore("<gray>Status: " + status.getStatus())
                .addLore("<gray>Host: " + Bukkit.getOfflinePlayer(host).getName());

        Duration duration = Duration.between(time, LocalDateTime.now());
        long minutes = Math.abs(duration.toMinutes());
        if (status.isCanJoinQueue()) {
            if (minutes < ConfigService.getInstance().get(StandardConfigFile.MAIN_CONFIG).getCfg().getInt("time-before-queue-is-open-in-hours") * 60L) {
                if(time.isAfter(LocalDateTime.now())) {
                    builder.addBlankLore()
                            .addLore("<gray>Begint om: " + formattedTime + " (Over " + minutes + (minutes > 1 ? " minuten)" : " minuut)"))
                            .addLore("<gray>Aantal wachtend: " + queue.size());

                } else {
                    builder.addBlankLore()
                            .addLore("<gray>Begon om: " + formattedTime + "<red> (Tijd al geweest).")
                            .addLore("<gray>Aantal wachtend: " + queue.size());
                }

                if(queue.contains(p.getUniqueId())) {
                    builder.addLore("<yellow>Klik hier om uit de wachtrij te gaan.");
                } else {
                    builder.addLore("<yellow>Klik hier om in de wachtrij te komen.");
                }
                if (host == p.getUniqueId() || p.hasPermission(WeerWolvenPermissions.ITEM_GAMES_MANAGE_OTHER_GAMES.getPermission())) {
                    builder.addLore("<yellow>Shift-Klik om de game te beheren.");
                }
            } else {
                builder.addBlankLore();
                DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("EEEE HH:mm", Locale.forLanguageTag("nl-NL"));
                String formattedTime1 = time.format(formatter1);
                builder.addLore("<gray>Begint om: " + formattedTime1);
            }
        } else {
            builder.addLore("<yellow>Klik hier om de game te spectaten.");
            if(host == p.getUniqueId() || p.hasPermission(WeerWolvenPermissions.ITEM_GAMES_MANAGE_OTHER_GAMES.getPermission())) {
                builder.addLore("<yellow>Shift-Klik om host te worden.");
            }
        }

        return builder.build();

    }

    public void addPlayerToQueue(@NotNull UUID uniqueId) {
        // Snapshot order BEFORE mutation
        List<UUID> oldOrder = new ArrayList<>(queue);
        queue.add(uniqueId);
        reorderQueueAndNotify(oldOrder);
    }

    public void removePlayerFromQueue(@NotNull UUID uniqueId) {
        // Snapshot order BEFORE mutation
        List<UUID> oldOrder = new ArrayList<>(queue);
        if (queue.remove(uniqueId)) {
            reorderQueueAndNotify(oldOrder);
            Player pl = Bukkit.getPlayer(uniqueId);
        }
    }

    private void reorderQueueAndNotify(List<UUID> oldOrder) {
        if (queue.isEmpty()) return;
        // Map old positions for players that are STILL in queue
        java.util.Map<UUID, Integer> oldPos = new HashMap<>();
        for (int i = 0; i < oldOrder.size(); i++) {
            UUID u = oldOrder.get(i);
            if (queue.contains(u)) {
                oldPos.put(u, i);
            }
        }
        // Compute new order using same comparator as getOrderedQueue
        List<UUID> newOrder = computeOrderedQueue();
        // Notify players whose position changed
        for (int i = 0; i < newOrder.size(); i++) {
            UUID u = newOrder.get(i);
            Integer old = oldPos.get(u);
            if (old != null && old != i) {
                Player pl = Bukkit.getPlayer(u);
                if (pl != null) {
                    MessageUtil.sendMessageToPlayer(pl, LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAMES_POSITION_CHANGED_QUEUE, List.of("" + (old + 1),"" +( i + 1))));
                }
            }
        }
        // Apply new order to stored queue
        queue.clear();
        queue.addAll(newOrder);
    }

    private List<UUID> computeOrderedQueue() {
//        long now = System.currentTimeMillis();
//        long twoDaysMillis = ChronoUnit.DAYS.getDuration().toMillis() * 2;
//        java.util.Map<UUID, Integer> orderIndex = new HashMap<>();
//        for (int i = 0; i < queue.size(); i++) {
//            orderIndex.put(queue.get(i), i);
//        }
//        Comparator<UUID> comparator = Comparator
//                .comparingInt((UUID u) -> {
//                    PlayerData pd = DatabaseManager.getInstance().getPlayerData(u);
//                    boolean isNew = pd != null && pd.getData(PlayerData.UserDataType.GAMESPLAYED) == 0;
//                    if (isNew) return 0;
//
//                    long last = DatabaseManager.getInstance().getLastGameTime(u);
//                    if (last == 0L) return 1;
//                    long diff = now - last;
//                    return (diff >= twoDaysMillis) ? 1 : 2;
//                })
//                .thenComparingInt(u -> orderIndex.getOrDefault(u, Integer.MAX_VALUE));
        List<UUID> copy = new ArrayList<>(queue);
//        copy.sort(comparator);
        return copy;
    }

    /**
     * Returns a dynamically ordered view of the queue based on the following priority buckets:
     *  1) Players who have never participated before (gamesplayed == 0)
     *  2) Players who have not played in the past two days (lastgame >= 2 days ago or 0)
     *  3) Recent players (played within the past two days)
     * Within each bucket, original join order is preserved.
     */
    public @NotNull List<UUID> getOrderedQueue() {
        // Queue is kept ordered at all times; return a copy for read-only usage
        return new ArrayList<>(queue);
    }

    public int getQueuePositionOfPlayer(UUID player) {
        return queue.indexOf(player);
    }
    public int getQueueSize() {
        return queue.size();
    }

    public void clearQueue() {
        this.queue.clear();
    }
}
