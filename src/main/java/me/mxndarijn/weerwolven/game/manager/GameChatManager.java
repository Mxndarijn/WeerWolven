package me.mxndarijn.weerwolven.game.manager;

import io.papermc.paper.event.player.AsyncChatEvent;
import lombok.Getter;
import me.mxndarijn.weerwolven.data.WeerWolvenChatPrefix;
import me.mxndarijn.weerwolven.data.WeerWolvenLanguageText;
import me.mxndarijn.weerwolven.game.core.Game;
import me.mxndarijn.weerwolven.game.core.GamePlayer;
import me.mxndarijn.weerwolven.data.Roles;
import me.mxndarijn.weerwolven.game.status.StatusKey;
import nl.mxndarijn.mxlib.language.LanguageManager;
import nl.mxndarijn.mxlib.util.Functions;
import nl.mxndarijn.mxlib.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

/**
 * Manages in-game chat routing based on a configurable ChatState.
 * - Hosts can always chat in the current chat.
 * - If a host starts the message with '!' it is broadcast to everyone in the game (hosts, players, spectators).
 * - ChatState determines who can send and who can receive, and carries a prefix used when formatting messages.
 * - By default: empty prefix, everyone can read, players can chat, spectators cannot chat.
 */
@Getter
public class GameChatManager extends GameManager {

    private volatile ChatState currentState;

    public GameChatManager(Game game) {
        super(game);
        this.currentState = ChatState.defaultState();
    }

    public void setCurrentState(ChatState state) {
        if (state != null) this.currentState = state;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncChat(AsyncChatEvent event) {
        Player sender = event.getPlayer();
        ChatState state = this.currentState;

        boolean isHost = game.getHosts().contains(sender.getUniqueId());
        boolean isSpectator = game.getSpectators().contains(sender.getUniqueId());
        Optional<GamePlayer> gpOpt = game.getGamePlayerOfPlayer(sender.getUniqueId());

        if (!isHost && !isSpectator && gpOpt.isEmpty())
            return;

        String message = Functions.convertComponentToString(event.message());

        // Host global shout: leading '!' -> send to everyone
        if (isHost && message.startsWith("!")) {
            String content = message.substring(1).trim();
            if (!content.isEmpty()) {
                String shout = LanguageManager.getInstance().getLanguageString(
                        WeerWolvenLanguageText.GAME_CHAT_HOST,
                        Arrays.asList(sender.getName(), content)
                );
                game.sendMessageToAll(shout);
            }
            event.setCancelled(true);
            return;
        }

        // Determine permission to send
        boolean canSend;
        if (isHost) {
            canSend = true; // hosts can always chat in the current chat
        } else if (isSpectator) {
            canSend = state.spectatorCanSend.test(sender.getUniqueId());
        } else {
            canSend = gpOpt.map(state.canSend::test).orElse(false);
        }

        if (!canSend) {
            // Inform spectators or dead players they cannot chat; otherwise silently block
            if (isSpectator || !gpOpt.get().isAlive()) {
                MessageUtil.sendMessageToPlayer(sender,
                        LanguageManager.getInstance().getLanguageString(WeerWolvenLanguageText.GAME_SPECTATOR_TRY_CHAT));
            }
            // Block message from appearing globally
            event.setCancelled(true);
            return;
        }

        // Build recipient set according to canReceive rules. Hosts always receive.
        Set<Player> recipients = new LinkedHashSet<>();
        // Hosts
        for (UUID hostId : game.getHosts()) {
            Player p = Bukkit.getPlayer(hostId);
            if (p != null) recipients.add(p);
        }
        // Game players
        for (GamePlayer gp : game.getGamePlayers()) {
            gp.getOptionalPlayerUUID().ifPresent(uuid -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && state.canReceive.test(gp)) {
                    recipients.add(p);
                }
            });
        }
        // Spectators
        for (UUID specId : game.getSpectators()) {
            Player p = Bukkit.getPlayer(specId);
            if (p != null && state.spectatorCanReceive.test(specId)) {
                recipients.add(p);
            }
        }

        String formatted;
        String pfx = resolvePrefix(state.getPrefix());
        if (isHost) {
            if(state.getPrefix() == null) {
                formatted = LanguageManager.getInstance().getLanguageString(
                        WeerWolvenLanguageText.GAME_CHAT_HOST,
                        Arrays.asList(sender.getName(), message)
                );
            } else {
                formatted = LanguageManager.getInstance().getLanguageString(
                        WeerWolvenLanguageText.GAME_CHAT_HOST_WITH_PREFIX,
                        Arrays.asList(pfx, sender.getName(), message)
                );
            }
        } else if (isSpectator) {
            if( state.getPrefix() == null) {
                formatted = LanguageManager.getInstance().getLanguageString(
                        WeerWolvenLanguageText.GAME_CHAT_SPECTATOR,
                        Arrays.asList(sender.getName(), message)
                );
            } else {
                formatted = LanguageManager.getInstance().getLanguageString(
                        WeerWolvenLanguageText.GAME_CHAT_SPECTATOR_WITH_PREFIX,
                        Arrays.asList(pfx, sender.getName(), message)
                );
            }
        } else {
            GamePlayer gp = gpOpt.get();
            if(state.getPrefix() == null) {
            formatted = LanguageManager.getInstance().getLanguageString(
                    WeerWolvenLanguageText.GAME_CHAT_PLAYER,
                    Arrays.asList(gp.getColorData().getColor().getDisplayName(), sender.getName(), message)
            );

            } else {
                formatted = LanguageManager.getInstance().getLanguageString(
                        WeerWolvenLanguageText.GAME_CHAT_PLAYER_WITH_PREFIX,
                        Arrays.asList(pfx,gp.getColorData().getColor().getDisplayName(), sender.getName(), message)
                );
            }
        }
        for (Player target : recipients) {
            MessageUtil.sendMessageToPlayer(target, formatted);
        }

        event.setCancelled(true);
    }

    private static String resolvePrefix(WeerWolvenChatPrefix prefix) {
        return prefix == null ? "" : prefix.prefix();
    }

    // ChatState definition and helpers
    public static class ChatState {
        @Nullable
        private final WeerWolvenChatPrefix prefix;
        private final Predicate<GamePlayer> canSend;
        private final Predicate<GamePlayer> canReceive;
        private final Predicate<UUID> spectatorCanSend;
        private final Predicate<UUID> spectatorCanReceive;

        public ChatState(@Nullable WeerWolvenChatPrefix prefix,
                         Predicate<GamePlayer> canSend,
                         Predicate<GamePlayer> canReceive,
                         Predicate<UUID> spectatorCanSend,
                         Predicate<UUID> spectatorCanReceive) {
            this.prefix = prefix;
            this.canSend = Objects.requireNonNullElse(canSend, gp -> true);
            this.canReceive = Objects.requireNonNullElse(canReceive, gp -> true);
            this.spectatorCanSend = Objects.requireNonNullElse(spectatorCanSend, id -> false);
            this.spectatorCanReceive = Objects.requireNonNullElse(spectatorCanReceive, id -> true);
        }

        @Nullable
        public WeerWolvenChatPrefix getPrefix() {
            return prefix;
        }

        public boolean canSend(GamePlayer gp) {
            return canSend.test(gp);
        }

        public boolean canReceive(GamePlayer gp) {
            return canReceive.test(gp);
        }

        public boolean spectatorCanSend(UUID id) {
            return spectatorCanSend.test(id);
        }

        public boolean spectatorCanReceive(UUID id) {
            return spectatorCanReceive.test(id);
        }

        public static ChatState defaultState() {
            // Everyone can read, players can chat, spectators cannot chat. Empty prefix by default (null)
            return new ChatState(
                    null,
                    gp -> true,
                    gp -> true,
                    id -> false,
                    id -> true
            );
        }

        public static ChatState everyoneWithPrefix(WeerWolvenChatPrefix prefix) {
            return new ChatState(prefix, gp -> true, gp -> true, id -> false, id -> true);
        }

        public static ChatState onlyRoleCanChat(WeerWolvenChatPrefix prefix, Roles role, boolean spectatorsMaySend) {
            Predicate<GamePlayer> canSend = gp -> gp != null && gp.getRole() == role;
            Predicate<UUID> specSend = spectatorsMaySend ? id -> true : id -> false;
            return new ChatState(prefix, canSend, gp -> true, specSend, id -> true);
        }

        public static ChatState predicate(WeerWolvenChatPrefix prefix,
                                          Predicate<GamePlayer> canSend,
                                          Predicate<GamePlayer> canReceive,
                                          Predicate<UUID> spectatorCanSend,
                                          Predicate<UUID> spectatorCanReceive) {
            return new ChatState(prefix, canSend, canReceive, spectatorCanSend, spectatorCanReceive);
        }

        public static ChatState onlyWithStatus(WeerWolvenChatPrefix prefix, StatusKey key) {
            Predicate<GamePlayer> hasStatus = gp -> gp != null && gp.getStatusStore().has(key);
            // Only players with the status can send and receive. Spectators cannot send or receive.
            return new ChatState(prefix, hasStatus, hasStatus, id -> false, id -> false);
        }
    }
}
