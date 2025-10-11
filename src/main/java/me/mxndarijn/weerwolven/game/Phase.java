package me.mxndarijn.weerwolven.game;

/**
 * Finite State Machine voor het potje.
 * Gebruik Phase.next() voor het standaardverloop en helper-methodes voor checks.
 */
public enum Phase {
    LOBBY,      // setup & rolverdeling
    NIGHT,      // nachtacties (prevent/protect/info/team kill/reactions)
    DAWN,       // onthul nachtresultaten + aftermath + cleanup
    DAY,        // discussies & stemmen van burgers
    DUSK,       // lynch + aftermath + cleanup
    END;        // gewonnen/verloren

    /** Default overgang. Je mag hiervan afwijken in je Game-loop indien nodig. */
    public Phase next() {
        return switch (this) {
            case LOBBY -> NIGHT;
            case NIGHT -> DAWN;
            case DAWN  -> DAY;
            case DAY   -> DUSK;
            case DUSK  -> NIGHT;
            case END   -> END;
        };
    }

    public boolean isNightLike() { return this == NIGHT; }
    public boolean isDayLike()   { return this == DAY; }

    /** Handig voor expiries (bv. status tot DAWN). */
    public boolean isAfter(Phase other) {
        return this.ordinal() > other.ordinal();
    }
}
