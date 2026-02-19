package com.holdup.server.action.handler;

import com.holdup.server.action.ActionHandler;
import com.holdup.server.action.GameActionType;
import com.holdup.server.action.dto.ActionResult;
import com.holdup.server.action.dto.PlayerActionRequest;
import org.springframework.stereotype.Component;

@Component
public class ReadyActionHandler implements ActionHandler {

    @Override
    public GameActionType getActionType() {
        return GameActionType.READY;
    }

    @Override
    public ActionResult handle(PlayerActionRequest request, String playerId) {
        // TODO: 준비 플래그 설정, 게임 시작 조건이면 시작
        return ActionResult.builder()
                .success(true)
                .actionType(GameActionType.READY)
                .playerId(playerId)
                .tableId(request.getTableId())
                .seatIndex(request.getSeatIndex())
                .build();
    }
}
