package me.mxndarijn.weerwolven.game.core.win;

import lombok.Getter;

@Getter
public enum WinConditionText {
    WEREWOLF("<red>Weerwolven", "<gray>hebben gewonnen."),
    JESTER("<yellow>Dorpsgek", "<gray>heeft gewonnen."),
    VILLAGERS("<green>Burgers", "<gray>hebben gewonnen."),
    LOVERS("<aqua>Geliefdes", "<gray>hebben gewonnen." ),
    NO_ONE("<gray>Niemand", "<gray>heeft gewonnen." ),;

    private final String prefix;
    private final String suffix;

    WinConditionText(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }


}
