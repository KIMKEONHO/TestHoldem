package com.holdup.server.card;

/**
 * 카드 문양. (스페이드, 하트, 다이아몬드, 클럽)
 */
public enum Suit {
    SPADES("s", "♠"),
    HEARTS("h", "♥"),
    DIAMONDS("d", "♦"),
    CLUBS("c", "♣");

    private final String code;
    private final String symbol;

    Suit(String code, String symbol) {
        this.code = code;
        this.symbol = symbol;
    }

    public String getCode() {
        return code;
    }

    public String getSymbol() {
        return symbol;
    }

    public static Suit fromCode(String code) {
        if (code == null || code.length() < 1) return null;
        for (Suit s : values()) {
            if (s.code.equalsIgnoreCase(code)) return s;
        }
        return null;
    }
}
