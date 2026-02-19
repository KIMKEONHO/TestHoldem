package com.holdup.server.card;

import java.util.List;

/**
 * 5장 또는 7장 카드로 홀덤 패 등급을 판정.
 * 홀덤에서는 7장(홀카드 2 + 커뮤니티 5) 중 5장 조합의 최선을 선택.
 */
public interface HandEvaluator {

    /**
     * 5장으로 패 등급과 키 카드 반환.
     */
    HandEvaluation evaluateFive(List<Card> fiveCards);

    /**
     * 7장 중 최선의 5장 조합을 찾아 평가. (홀덤용)
     */
    HandEvaluation evaluateSeven(List<Card> sevenCards);

    /**
     * 두 평가 결과 비교. a가 더 강하면 양수, b가 더 강하면 음수, 동점이면 0.
     */
    int compare(HandEvaluation a, HandEvaluation b);
}
