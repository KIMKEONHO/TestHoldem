package com.holdup.server.action;

import com.holdup.server.action.dto.ActionResult;
import com.holdup.server.action.dto.PlayerActionRequest;

/**
 * 특정 게임 액션 타입을 처리하는 핸들러 인터페이스.
 */
public interface ActionHandler {

    /** 이 핸들러가 처리하는 액션 타입. */
    GameActionType getActionType();

    /**
     * 액션 실행.
     *
     * @param request 클라이언트 요청
     * @param playerId 요청한 플레이어 ID (세션/Principal 기반)
     * @return 처리 결과 (실패 시 success=false, message 설정)
     */
    ActionResult handle(PlayerActionRequest request, String playerId);
}
