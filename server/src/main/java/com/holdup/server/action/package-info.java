/**
 * 홀덤 게임 플레이어 액션 패키지.
 *
 * <h2>디렉토리 구조</h2>
 * <ul>
 *   <li><b>GameActionType</b> - 액션 타입 enum (FOLD, CHECK, CALL, BET, RAISE, ALL_IN, JOIN_TABLE 등)</li>
 *   <li><b>ActionHandler</b> - 액션별 처리 인터페이스</li>
 *   <li><b>ActionDispatcher</b> - 요청을 해당 타입의 핸들러로 라우팅</li>
 *   <li><b>dto/</b> - PlayerActionRequest(요청), ActionResult(응답)</li>
 *   <li><b>handler/</b> - 각 GameActionType별 구현체 (FoldActionHandler, BetActionHandler 등)</li>
 * </ul>
 *
 * <p>클라이언트는 /app/action 으로 PlayerActionRequest를 보내고,
 * /topic/actions (또는 테이블별 /topic/table/{tableId}) 구독으로 ActionResult를 받습니다.</p>
 */
package com.holdup.server.action;
