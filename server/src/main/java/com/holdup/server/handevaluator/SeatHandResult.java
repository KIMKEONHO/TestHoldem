package com.holdup.server.handevaluator;

import com.holdup.server.card.HandEvaluation;

import lombok.Builder;
import lombok.Value;

/**
 * 한 시트(플레이어)의 패 평가 결과 및 순위.
 */
@Value
@Builder
public class SeatHandResult {

    /** 시트 인덱스. */
    int seatIndex;

    /** 플레이어 ID (선택). */
    String playerId;

    /** 7장(홀2+커뮤니티5)으로 평가한 결과. */
    HandEvaluation handEvaluation;

    /**
     * 순위. 1 = 1등(승자), 2 = 2등(스플릿 시 동점 그룹) 등.
     * 동점이면 같은 rank 부여.
     */
    int rank;

    /** 1등 여부. */
    boolean winner;

    public static SeatHandResult of(int seatIndex, String playerId, HandEvaluation handEvaluation, int rank) {
        return SeatHandResult.builder()
                .seatIndex(seatIndex)
                .playerId(playerId)
                .handEvaluation(handEvaluation)
                .rank(rank)
                .winner(rank == 1)
                .build();
    }
}
