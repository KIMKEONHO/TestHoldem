/**
 * 홀덤 덱 패키지.
 *
 * <h2>디렉토리 구조</h2>
 * <ul>
 *   <li><b>Deck</b> - 52장 덱. shuffle(), deal(n), dealOne(), remaining(). card 패키지(Card, Suit, Rank) 사용.</li>
 *   <li><b>DeckFactory</b> - createStandard(), createExcluding(cards), createShuffled() 등 덱 생성.</li>
 *   <li><b>ShuffleStrategy</b> - 셔플 전략 인터페이스 (테스트 시드·커스텀 알고리즘 주입용).</li>
 *   <li><b>RandomShuffleStrategy</b> - 기본 무작위 셔플 구현.</li>
 * </ul>
 *
 * <p>게임 시작 시 DeckFactory.createShuffled() 또는 new Deck() 후 shuffle()으로 덱을 준비하고,
 * deal(2)로 홀카드, deal(3)·deal(1)·deal(1)로 플랍·턴·리버를 딜링합니다.</p>
 */
package com.holdup.server.deck;
