package com.holdup.server.card;

/**
 * 카드 숫자/끗. (2~10, J, Q, K, A)
 * strength: 패 비교 시 사용하는 수치 (2=2 ... A=14).
 */
public enum Rank {
    TWO("2", 2),
    THREE("3", 3),
    FOUR("4", 4),
    FIVE("5", 5),
    SIX("6", 6),
    SEVEN("7", 7),
    EIGHT("8", 8),
    NINE("9", 9),
    TEN("T", 10),
    JACK("J", 11),
    QUEEN("Q", 12),
    KING("K", 13),
    ACE("A", 14);

    private final String code;
    private final int strength;

    Rank(String code, int strength) {
        this.code = code;
        this.strength = strength;
    }

    public String getCode() {
        return code;
    }

    /** 패 비교용 수치 (2=2, A=14). */
    public int getStrength() {
        return strength;
    }

    public static Rank fromCode(String code) {
        if (code == null || code.length() < 1) return null;
        for (Rank r : values()) {
            if (r.code.equalsIgnoreCase(code)) return r;
        }
        return null;
    }
}
