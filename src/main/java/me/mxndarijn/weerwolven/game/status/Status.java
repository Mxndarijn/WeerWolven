package me.mxndarijn.weerwolven.game.status;

import me.mxndarijn.weerwolven.game.Game;
import me.mxndarijn.weerwolven.game.Phase;
import me.mxndarijn.weerwolven.game.StatusKey;

import java.util.Optional;
import java.util.UUID;

public interface Status {
    StatusKey key();

    /** Mag null zijn als er geen bron is. */
    UUID source(); // @Nullablethe

    /** Gemak: optionele view op de source. */
    default Optional<UUID> sourceOptional() { return Optional.ofNullable(source()); }

    default boolean isExpired(Game game) { return false; }

    default boolean stackable() { return key().stackable; }
}

