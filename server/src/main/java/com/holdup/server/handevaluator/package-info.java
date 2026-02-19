/**
 * 패 평가 및 승자 판정.
 *
 * <h2>디렉토리 구조</h2>
 * <ul>
 *   <li><b>SeatHandResult</b> - 시트별 평가 결과 (seatIndex, playerId, handEvaluation, rank, winner).</li>
 *   <li><b>ParticipantHand</b> - 승자 판정 입력: seatIndex, playerId, holeCards.</li>
 *   <li><b>WinnerResolver</b> - evaluateWinners(participants, communityCards) → 순위 부여된 SeatHandResult 목록. card.HandEvaluator 사용.</li>
 *   <li><b>HandEvaluatorConfig</b> - HandEvaluator(DefaultHandEvaluator), WinnerResolver 빈 등록.</li>
 * </ul>
 *
 * <p>card 패키지의 HandEvaluator를 사용하며, 쇼다운 시 테이블에서 WinnerResolver를 호출해 승자를 결정합니다.</p>
 */
package com.holdup.server.handevaluator;
