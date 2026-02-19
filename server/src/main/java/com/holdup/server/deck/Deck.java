package com.holdup.server.deck;

import com.holdup.server.card.Card;
import com.holdup.server.card.Rank;
import com.holdup.server.card.Suit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 52장 덱. 셔플 후 카드를 딜링.
 */
public class Deck {

    private final List<Card> cards;

    public Deck() {
        this.cards = new ArrayList<>(52);
        for (Suit s : Suit.values()) {
            for (Rank r : Rank.values()) {
                cards.add(new Card(s, r));
            }
        }
    }

    /** 이미 사용 중인 카드를 제외한 덱 생성 (예: 플레이어 홀카드 제외). */
    public Deck(List<Card> exclude) {
        this();
        if (exclude != null) {
            cards.removeAll(exclude);
        }
    }

    /** 무작위 셔플. */
    public void shuffle() {
        Collections.shuffle(cards, ThreadLocalRandom.current());
    }

    /** 셔플 전략 주입 시 사용. */
    public void shuffle(ShuffleStrategy strategy) {
        if (strategy != null) {
            strategy.shuffle(cards);
        } else {
            shuffle();
        }
    }

    /** 맨 위에서 n장 뽑기. 없으면 있는 만큼만 반환. */
    public List<Card> deal(int n) {
        int take = Math.min(n, cards.size());
        List<Card> dealt = new ArrayList<>(cards.subList(0, take));
        for (int i = 0; i < take; i++) {
            cards.remove(0);
        }
        return dealt;
    }

    /** 한 장 뽑기. 없으면 null. */
    public Card dealOne() {
        return cards.isEmpty() ? null : cards.remove(0);
    }

    public int remaining() {
        return cards.size();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }
}
