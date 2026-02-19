/**
 * 홀덤 카드·덱·패 평가 패키지.
 *
 * <h2>디렉토리 구조 및 역할</h2>
 * <ul>
 *   <li><b>Suit</b> - 문양 enum (SPADES, HEARTS, DIAMONDS, CLUBS), 코드(s/h/d/c)</li>
 *   <li><b>Rank</b> - 끗 enum (2~A), strength(2=2, A=14)로 패 비교</li>
 *   <li><b>Card</b> - 한 장의 카드 (suit + rank), 불변. Card.of("As") 로 생성</li>
 *   <li><b>HandRank</b> - 패 등급 (HIGH_CARD ~ ROYAL_FLUSH)</li>
 *   <li><b>HandEvaluation</b> - 평가 결과 (HandRank + 동점 비교용 keyRanks)</li>
 *   <li><b>HandEvaluator</b> - 5장/7장 패 평가 인터페이스</li>
 *   <li><b>DefaultHandEvaluator</b> - 구현체: 플러시·스트레이트·페어 판별, 7C5 최선 조합</li>
 *   <li><b>Cards</b> - 카드(목록) ↔ 문자열 직렬화 (예: "As Kh 2c")</li>
 * </ul>
 *
 * <p>덱은 {@link com.holdup.server.deck} 패키지의 Deck 사용. 게임 로직에서는 Deck으로 딜링하고,
 * HandEvaluator.evaluateSeven(홀카드2+커뮤니티5)로 승자를 결정할 때 사용합니다.</p>
 */
package com.holdup.server.card;
