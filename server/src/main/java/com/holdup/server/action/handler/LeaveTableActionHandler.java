package com.holdup.server.action.handler;

import com.holdup.server.action.ActionHandler;
import com.holdup.server.action.GameActionType;
import com.holdup.server.action.dto.ActionResult;
import com.holdup.server.action.dto.PlayerActionRequest;
import com.holdup.server.service.TableManager;
import com.holdup.server.table.Table;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class LeaveTableActionHandler implements ActionHandler {

    private final TableManager tableManager;

    public LeaveTableActionHandler(TableManager tableManager) {
        this.tableManager = tableManager;
    }

    @Override
    public GameActionType getActionType() {
        return GameActionType.LEAVE_TABLE;
    }

    @Override
    public ActionResult handle(PlayerActionRequest request, String playerId) {
        String tableId = request.getTableId();
        if (tableId == null || tableId.isBlank()) {
            return ActionResult.builder()
                    .success(false)
                    .message("tableId required")
                    .actionType(GameActionType.LEAVE_TABLE)
                    .playerId(playerId)
                    .build();
        }
        Optional<Table> opt = tableManager.getTable(tableId);
        if (opt.isEmpty()) {
            return ActionResult.builder()
                    .success(false)
                    .message("Table not found")
                    .actionType(GameActionType.LEAVE_TABLE)
                    .playerId(playerId)
                    .tableId(tableId)
                    .build();
        }
        Table table = opt.get();
        int seatIndex = table.getSeatIndexByPlayerId(playerId);
        if (seatIndex < 0) {
            return ActionResult.builder()
                    .success(true)
                    .message("Not at table")
                    .actionType(GameActionType.LEAVE_TABLE)
                    .playerId(playerId)
                    .tableId(tableId)
                    .build();
        }
        table.getSeat(seatIndex).setPlayer(null);
        return ActionResult.builder()
                .success(true)
                .actionType(GameActionType.LEAVE_TABLE)
                .playerId(playerId)
                .tableId(tableId)
                .seatIndex(seatIndex)
                .build();
    }
}
