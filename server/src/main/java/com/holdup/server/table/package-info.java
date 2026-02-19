/**
 * 홀덤 테이블 모델.
 *
 * <h2>디렉토리 구조</h2>
 * <ul>
 *   <li><b>Seat</b> - seatIndex, player, totalBetThisHand. 빈 자리면 player null.</li>
 *   <li><b>Table</b> - id, name, seats, handState, deck, dealer/sb/bb 시트 인덱스,
 *       smallBlindAmount, bigBlindAmount. 한 게임 테이블의 전체 상태.</li>
 * </ul>
 *
 * <p>player, gamestate, card, deck 패키지를 사용합니다.</p>
 */
package com.holdup.server.table;
