package com.holdup.server.card;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * 패 평가 결과. 등급 + 동점 시 비교용 키 카드 목록.
 */
@Value
@Builder
public class HandEvaluation {

    HandRank handRank;

    /**
     * 동점 시 비교에 사용할 카드 순서 (강한 순).
     * 예: 원페어면 [페어 끗, 나머지 높은 순], 스트레이트면 [스트레이트 최상 끗].
     */
    List<Rank> keyRanks;

    public static HandEvaluation of(HandRank rank, List<Rank> keyRanks) {
        return HandEvaluation.builder()
                .handRank(rank)
                .keyRanks(keyRanks != null ? List.copyOf(keyRanks) : List.of())
                .build();
    }
}
