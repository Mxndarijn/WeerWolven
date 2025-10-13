package me.mxndarijn.weerwolven.game;

import lombok.Getter;

@Getter
public enum Phase {
    LOBBY("Samenkomst", false, "<blue>",   0),
    NIGHT("Nacht",       true,  "<blue>", 13000),
    DAWN("Zonsopkomst",  false, "<blue>", 23000),
    DAY("Overdag",       false, "<blue>",     0),
    DUSK("Zonsondergang",false, "<blue>", 12000),
    END("Slot",          false, "<blue>",     0);

    private final String displayName;
    private final boolean shouldMentionDayNumber;
    private final String color;
    private final int minecraftTime;

    Phase(String displayName, boolean shouldMentionDayNumber, String color, int minecraftTime) {
        this.displayName = displayName;
        this.shouldMentionDayNumber = shouldMentionDayNumber;
        this.color = color;
        this.minecraftTime = minecraftTime;
    }

    /** Default transition; your Game loop can override when needed. */
    public Phase next() {
        return switch (this) {
            case LOBBY, DUSK -> NIGHT;
            case NIGHT -> DAWN;
            case DAWN  -> DAY;
            case DAY   -> DUSK;
            case END   -> END;
        };
    }

    /** Stable index independent of enum ordinal (in case you reorder constants). */
    public int index() {
        return switch (this) {
            case LOBBY -> 0;
            case NIGHT -> 1;
            case DAWN  -> 2;
            case DAY   -> 3;
            case DUSK  -> 4;
            case END   -> 5;
        };
    }

    /** Compare using the stable index. */
    public boolean isAfter(Phase other) { return this.index() > other.index(); }

    public boolean isNightLike() { return this == NIGHT; }
    public boolean isDayLike()   { return this == DAY; }
    public boolean isTransition(){ return this == DAWN || this == DUSK; }

    /** Helper for your scoreboard text. */
    public String getColoredPhase(int dayNumber) {
        if (shouldMentionDayNumber) {
            return color + displayName + " <dark_gray>(<gray>" + dayNumber + "<dark_gray>)";
        }
        return color + displayName;
    }

    /** Whether moving into this phase should increment the day counter. */
    public boolean incrementsDayCounterFrom(Phase previous) {
        return previous == DAWN && this == DAY;
    }
}
