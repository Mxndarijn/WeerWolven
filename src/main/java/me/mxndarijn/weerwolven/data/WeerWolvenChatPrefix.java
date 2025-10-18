package me.mxndarijn.weerwolven.data;


import lombok.Getter;
import nl.mxndarijn.mxlib.chatprefix.ChatPrefixType;

@Getter
public enum WeerWolvenChatPrefix implements ChatPrefixType {
    DEFAULT("<gold>WeerWolven"),
    VOTE("<blue>Stemmen"),
    HOST_LOG("<dark_gray>Host-Log"),
    WEREWOLF_CHAT("<aqua>WW-Chat"),
    LOVERS_CHAT("<aqua>Geliefdes-Chat"),;

    private final String prefix;
    private final String name;

    WeerWolvenChatPrefix(String prefix) {
        this.prefix = prefix + "<yellow> \u00BB <gray>";
        this.name = prefix;
    }

    @Override
    public String toString() {
        return prefix;
    }

    @Override
    public String prefix() {
        return this.prefix;
    }
}
