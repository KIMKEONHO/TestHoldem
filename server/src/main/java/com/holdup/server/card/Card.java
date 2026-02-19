package com.holdup.server.card;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Comparator;

/**
 * 한 장의 플레잉 카드. (문양 + 끗)
 * 불변(immutable)으로 사용.
 */
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class Card {

    private Suit suit;
    private Rank rank;

    /** 강도 순 정렬용 비교기 (끗 오름차순, 동일 끗이면 문양). */
    public static final Comparator<Card> BY_STRENGTH = Comparator
            .comparingInt((Card c) -> c.rank.getStrength())
            .thenComparing(c -> c.suit.ordinal());

    @Override
    public String toString() {
        return rank.getCode() + suit.getCode();
    }

    /**
     * 코드 문자열으로 생성. 예: "As", "Kh", "Tc"
     */
    public static Card of(String code) {
        if (code == null || code.length() < 2) return null;
        Rank r = Rank.fromCode(code.substring(0, 1));
        Suit s = Suit.fromCode(code.substring(1, 2));
        return (r != null && s != null) ? new Card(s, r) : null;
    }
}
