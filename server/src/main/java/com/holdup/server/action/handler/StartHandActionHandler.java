package com.holdup.server.action.handler;

import com.holdup.server.action.ActionHandler;
import com.holdup.server.action.GameActionType;
import com.holdup.server.action.dto.ActionResult;
import com.holdup.server.action.dto.PlayerActionRequest;
import com.holdup.server.service.GameFlowService;
import com.holdup.server.service.TableManager;
import org.springframework.stereotype.Component;

@Component
public class StartHandActionHandler implements ActionHandler {

    private final TableManager tableManager;
    private final GameFlowService gameFlowService;

    public StartHandActionHandler(TableManager tableManager, GameFlowService gameFlowService) {
        this.tableManager = tableManager;
        this.gameFlowService = gameFlowService;
    }

    @Override
    public GameActionType getActionType() {
        return GameActionType.START_HAND;
    }

    @Override
    public ActionResult handle(PlayerActionRequest request, String playerId) {
        String tableId = request.getTableId();
        if (tableId == null || tableId.isBlank()) {
            return ActionResult.builder()
                    .success(false)
                    .message("tableId required")
                    .actionType(GameActionType.START_HAND)
                    .playerId(playerId)
                    .build();
        }
        var tableOpt = tableManager.getTable(tableId);
        if (tableOpt.isEmpty()) {
            return ActionResult.builder()
                    .success(false)
                    .message("테이블을 찾을 수 없습니다.")
                    .actionType(GameActionType.START_HAND)
                    .playerId(playerId)
                    .tableId(tableId)
                    .build();
        }
        var table = tableOpt.get();
        boolean started = gameFlowService.startNewHand(tableId);
        String message = null;
        if (!started) {
            long count = table.countOccupiedSeats();
            if (table.getHandState().getPhase() != com.holdup.server.gamestate.GamePhase.WAITING) {
                message = "이미 게임이 진행 중입니다.";
            } else if (count < 2) {
                message = "핸드를 시작하려면 2명 이상이 필요합니다. 같은 방 코드로 친구를 초대해 주세요. (현재 " + count + "명)";
            } else {
                message = "핸드를 시작할 수 없습니다. (2명 이상, 대기 상태 필요)";
            }
        }
        return ActionResult.builder()
                .success(started)
                .message(message)
                .actionType(GameActionType.START_HAND)
                .playerId(playerId)
                .tableId(tableId)
                .build();
    }
}
