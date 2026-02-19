package com.holdup.server.handevaluator;

import com.holdup.server.card.Card;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * 승자 판정 시 한 참가자의 홀카드 정보.
 */
@Value
@Builder
public class ParticipantHand {

    int seatIndex;
    String playerId;
    List<Card> holeCards;

    public List<Card> getHoleCards() {
        return holeCards != null ? List.copyOf(holeCards) : List.of();
    }
}
