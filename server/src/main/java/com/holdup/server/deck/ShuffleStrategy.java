package com.holdup.server.deck;

import com.holdup.server.card.Card;

import java.util.List;

/**
 * 덱 셔플 전략. 테스트용 고정 시드 또는 커스텀 알고리즘 주입 가능.
 */
@FunctionalInterface
public interface ShuffleStrategy {

    void shuffle(List<Card> cards);
}
