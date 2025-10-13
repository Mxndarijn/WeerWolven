package me.mxndarijn.weerwolven.game;

public enum StatusKey {
    PROTECTED_TONIGHT(false),
    SAVED_BY_WITCH(false),
    JAILED_TONIGHT(false),
    SLEEPS_TONIGHT(false),
    GUARDED_BY_BODYGUARD(false),
    BLESSED_ONCE(false),
    POISON_PENDING(false),
    VOTE_DOUBLE_MAYOR(false),
    VOTE_BONUS_BREAD(true),
    VOTE_WOLVES_HIDDEN_X2(false),
    CHARGES_WITCH_SAVE(false),
    CHARGES_WITCH_POISON(false),
    LOVERS(false);


    public final boolean stackable;
    StatusKey(boolean stackable) { this.stackable = stackable; }
}