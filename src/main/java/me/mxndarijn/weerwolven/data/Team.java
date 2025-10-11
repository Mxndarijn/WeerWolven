package me.mxndarijn.weerwolven.data;

import lombok.Getter;

public enum Team {
    SOLO("<yellow>"),
    WEREWOLF("<dark_gray>"),
    CITIZEN("<green>"),
    MURDERER("<red>");


    @Getter
    private final String teamColor;
    Team(String teamColor) {
        this.teamColor = teamColor;
    }

}
