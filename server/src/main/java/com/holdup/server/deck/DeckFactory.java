package com.holdup.server.deck;

import com.holdup.server.card.Card;

import java.util.List;

/**
 * 덱 생성 팩토리. 표준 52장 또는 제외 카드 지정.
 */
public final class DeckFactory {

    private DeckFactory() {}

    /** 표준 52장 덱 생성. */
    public static Deck createStandard() {
        return new Deck();
    }

    /** 지정 카드를 제외한 덱 생성 (이미 딜된 카드 제외 시나리오 등). */
    public static Deck createExcluding(List<Card> exclude) {
        return new Deck(exclude);
    }

    /** 표준 덱 생성 후 셔플까지 한 번에. */
    public static Deck createShuffled() {
        Deck deck = new Deck();
        deck.shuffle();
        return deck;
    }

    /** 표준 덱 생성 후 지정 셔플 전략 적용. */
    public static Deck createShuffled(ShuffleStrategy strategy) {
        Deck deck = new Deck();
        deck.shuffle(strategy);
        return deck;
    }
}
