package com.holdup.server.action.handler;

import com.holdup.server.action.ActionHandler;
import com.holdup.server.action.GameActionType;
import com.holdup.server.action.dto.ActionResult;
import com.holdup.server.action.dto.PlayerActionRequest;
import org.springframework.stereotype.Component;

/**
 * 타임아웃 시 자동 폴드 등 시스템에 의한 액션.
 */
@Component
public class TimeoutActionHandler implements ActionHandler {

    @Override
    public GameActionType getActionType() {
        return GameActionType.TIMEOUT;
    }

    @Override
    public ActionResult handle(PlayerActionRequest request, String playerId) {
        // TODO: 타이머/스케줄러에서 호출 또는 클라이언트 타임아웃 신호 처리
        return ActionResult.builder()
                .success(true)
                .actionType(GameActionType.TIMEOUT)
                .playerId(playerId)
                .tableId(request.getTableId())
                .seatIndex(request.getSeatIndex())
                .build();
    }
}
