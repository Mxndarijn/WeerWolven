package me.mxndarijn.weerwolven.data;


import lombok.Getter;
import nl.mxndarijn.mxlib.chatprefix.ChatPrefixType;

@Getter
public enum WeerWolvenChatPrefix implements ChatPrefixType {
    DEFAULT("<gold>WeerWolven");

    private final String prefix;
    private final String name;

    WeerWolvenChatPrefix(String prefix) {
        this.prefix = prefix + "<dark_green> \u00BB <gray>";
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
