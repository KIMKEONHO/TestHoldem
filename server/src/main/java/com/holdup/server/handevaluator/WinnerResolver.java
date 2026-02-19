package com.holdup.server.handevaluator;

import com.holdup.server.card.Card;
import com.holdup.server.card.HandEvaluation;
import com.holdup.server.card.HandEvaluator;
import com.holdup.server.card.HandRank;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 홀카드 + 커뮤니티 카드로 각 시트의 패를 평가하고 승자 순위를 계산.
 * card.HandEvaluator 사용.
 */
public class WinnerResolver {

    private final HandEvaluator handEvaluator;

    public WinnerResolver(HandEvaluator handEvaluator) {
        this.handEvaluator = handEvaluator;
    }

    /**
     * 각 참가자의 홀카드와 커뮤니티 카드로 7장을 만들고, 평가 후 순위 부여.
     *
     * @param participants 시트별 (seatIndex, playerId, holeCards). 폴드된 플레이어는 제외하고 넘김.
     * @param communityCards 커뮤니티 5장
     * @return 순위가 부여된 SeatHandResult 목록 (1등이 rank=1, 동점이면 같은 rank)
     */
    public List<SeatHandResult> evaluateWinners(
            List<ParticipantHand> participants,
            List<Card> communityCards) {

        if (participants == null || participants.isEmpty()) {
            return List.of();
        }
        if (communityCards == null || communityCards.size() != 5) {
            HandEvaluation invalid = HandEvaluation.of(HandRank.HIGH_CARD, List.of());
            return participants.stream()
                    .map(p -> SeatHandResult.of(p.getSeatIndex(), p.getPlayerId(), invalid, 1))
                    .collect(Collectors.toList());
        }

        List<SeatHandResult> evaluated = new ArrayList<>();
        for (ParticipantHand p : participants) {
            List<Card> seven = new ArrayList<>(p.getHoleCards());
            seven.addAll(communityCards);
            HandEvaluation eval = handEvaluator.evaluateSeven(seven);
            evaluated.add(SeatHandResult.of(p.getSeatIndex(), p.getPlayerId(), eval, 0));
        }

        // 순위 매기기: compare로 정렬 후 동점 그룹에 같은 rank 부여
        evaluated.sort((a, b) -> -handEvaluator.compare(a.getHandEvaluation(), b.getHandEvaluation()));

        List<SeatHandResult> ranked = new ArrayList<>();
        int rank = 1;
        for (int i = 0; i < evaluated.size(); i++) {
            SeatHandResult r = evaluated.get(i);
            if (i > 0 && handEvaluator.compare(r.getHandEvaluation(), evaluated.get(i - 1).getHandEvaluation()) != 0) {
                rank = i + 1;
            }
            ranked.add(SeatHandResult.of(r.getSeatIndex(), r.getPlayerId(), r.getHandEvaluation(), rank));
        }
        return ranked;
    }
}
