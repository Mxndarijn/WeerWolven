package me.mxndarijn.weerwolven.game;

import lombok.Getter;

/**
 * Finite State Machine voor het potje.
 * Gebruik Phase.next() voor het standaardverloop en helper-methodes voor checks.
 */
@Getter
public enum Phase {
    LOBBY("Samenkomst", false, "<blue>", 0),      // setup & rolverdeling
    NIGHT("Nacht", true, "<blue>", 13000),      // nachtacties (prevent/protect/info/team kill/reactions)  
    DAWN("Zonsopkomst", false, "<blue>", 23000),       // onthul nachtresultaten + aftermath + cleanup
    DAY("Overdag", false, "<blue>", 0),        // discussies & stemmen van burgers
    DUSK("Zonsondergang", false, "<blue>", 12000),       // lynch + aftermath + cleanup
    END("Slot", false, "<blue>", 0);        // gewonnen/verloren

    private final String displayName;
    private final boolean shouldMentionDayNumber;
    private final String color;
    private int minecraftTime;

    Phase(String displayName, boolean isStarry, String color, int minecraftTime) {
        this.displayName = displayName;
        this.shouldMentionDayNumber = isStarry;
        this.color = color;
        this.minecraftTime = minecraftTime;
    }

    /**
     * Default overgang. Je mag hiervan afwijken in je Game-loop indien nodig.
     */
    public Phase next() {
        return switch (this) {
            case LOBBY, DUSK -> NIGHT;
            case NIGHT -> DAWN;
            case DAWN -> DAY;
            case DAY -> DUSK;
            case END -> END;
        };
    }


    public boolean isNightLike() {
        return this == NIGHT;
    }

    public boolean isDayLike() {
        return this == DAY;
    }

    public boolean isAfter(Phase other) {
        return this.ordinal() > other.ordinal();
    }

    public String getColoredPhase(int dayNumber) {
        if (shouldMentionDayNumber) {
            return color + displayName + " <dark_gray>(<gray>" + dayNumber + "<dark_gray>)";
        } else {
            return color + displayName;
        }
    }
}