package com.holdup.server.action.handler;

import com.holdup.server.action.ActionHandler;
import com.holdup.server.action.GameActionType;
import com.holdup.server.action.dto.ActionResult;
import com.holdup.server.action.dto.PlayerActionRequest;
import org.springframework.stereotype.Component;

@Component
public class SitOutActionHandler implements ActionHandler {

    @Override
    public GameActionType getActionType() {
        return GameActionType.SIT_OUT;
    }

    @Override
    public ActionResult handle(PlayerActionRequest request, String playerId) {
        // TODO: 현재 핸드 종료 후 다음부터 미참여 처리
        return ActionResult.builder()
                .success(true)
                .actionType(GameActionType.SIT_OUT)
                .playerId(playerId)
                .tableId(request.getTableId())
                .seatIndex(request.getSeatIndex())
                .build();
    }
}
