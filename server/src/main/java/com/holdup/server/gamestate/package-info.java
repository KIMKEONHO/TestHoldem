/**
 * 한 핸드(Hand)의 게임 진행 상태.
 *
 * <h2>디렉토리 구조</h2>
 * <ul>
 *   <li><b>GamePhase</b> - WAITING, PREFLOP, FLOP, TURN, RIVER, SHOWDOWN</li>
 *   <li><b>HandState</b> - phase, communityCards, pot, currentBet, betPerSeatThisStreet,
 *       dealerSeatIndex, actingSeatIndex, minRaise. 테이블에서 한 핸드 진행 시 사용.</li>
 * </ul>
 */
package com.holdup.server.gamestate;
