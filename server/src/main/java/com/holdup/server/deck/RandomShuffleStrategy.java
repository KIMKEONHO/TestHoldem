package com.holdup.server.deck;

import com.holdup.server.card.Card;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 기본 무작위 셔플.
 */
public class RandomShuffleStrategy implements ShuffleStrategy {

    @Override
    public void shuffle(List<Card> cards) {
        Collections.shuffle(cards, ThreadLocalRandom.current());
    }
}
