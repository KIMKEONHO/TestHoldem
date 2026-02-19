package com.holdup.server.card;

/**
 * 홀덤 패 등급 (강한 순).
 * 동일 HandRank일 때는 키 카드(끗)로 비교.
 */
public enum HandRank {

    HIGH_CARD(1),
    ONE_PAIR(2),
    TWO_PAIR(3),
    THREE_OF_A_KIND(4),
    STRAIGHT(5),
    FLUSH(6),
    FULL_HOUSE(7),
    FOUR_OF_A_KIND(8),
    STRAIGHT_FLUSH(9),
    ROYAL_FLUSH(10);

    private final int strength;

    HandRank(int strength) {
        this.strength = strength;
    }

    public int getStrength() {
        return strength;
    }

    public boolean isBetterThan(HandRank other) {
        return this.strength > other.strength;
    }
}
