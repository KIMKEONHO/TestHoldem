package com.holdup.server.action.handler;

import com.holdup.server.action.ActionHandler;
import com.holdup.server.action.GameActionType;
import com.holdup.server.action.dto.ActionResult;
import com.holdup.server.action.dto.PlayerActionRequest;
import com.holdup.server.player.Player;
import com.holdup.server.service.TableManager;
import com.holdup.server.table.Table;
import org.springframework.stereotype.Component;

@Component
public class JoinTableActionHandler implements ActionHandler {

    private final TableManager tableManager;

    public JoinTableActionHandler(TableManager tableManager) {
        this.tableManager = tableManager;
    }

    @Override
    public GameActionType getActionType() {
        return GameActionType.JOIN_TABLE;
    }

    @Override
    public ActionResult handle(PlayerActionRequest request, String playerId) {
        String tableId = request.getTableId();
        if (tableId == null || tableId.isBlank()) {
            return ActionResult.builder()
                    .success(false)
                    .message("tableId required")
                    .actionType(GameActionType.JOIN_TABLE)
                    .playerId(playerId)
                    .build();
        }
        Table table = tableManager.createTable(tableId, 9);
        if (table.getSeatIndexByPlayerId(playerId) >= 0) {
            return ActionResult.builder()
                    .success(true)
                    .message("Already at table")
                    .actionType(GameActionType.JOIN_TABLE)
                    .playerId(playerId)
                    .tableId(tableId)
                    .seatIndex(table.getSeatIndexByPlayerId(playerId))
                    .build();
        }
        Integer seatIndex = request.getSeatIndex();
        if (seatIndex == null || seatIndex < 0 || seatIndex >= table.getMaxSeats()) {
            // 빈 자리 아무거나 찾기
            for (int i = 0; i < table.getMaxSeats(); i++) {
                if (table.getSeat(i).isEmpty()) {
                    seatIndex = i;
                    break;
                }
            }
        }
        if (seatIndex == null || !table.getSeat(seatIndex).isEmpty()) {
            return ActionResult.builder()
                    .success(false)
                    .message("No empty seat")
                    .actionType(GameActionType.JOIN_TABLE)
                    .playerId(playerId)
                    .tableId(tableId)
                    .build();
        }
        // playerId = 연결 고유 ID(세션), 표시명 = 클라이언트가 보낸 닉네임
        String displayName = request.getPlayerId() != null && !request.getPlayerId().isBlank()
                ? request.getPlayerId()
                : ("Player-" + seatIndex);
        Player player = new Player(playerId, displayName, JoinTableConstants.DEFAULT_STACK);
        table.getSeat(seatIndex).setPlayer(player);
        return ActionResult.builder()
                .success(true)
                .actionType(GameActionType.JOIN_TABLE)
                .playerId(playerId)
                .tableId(tableId)
                .seatIndex(seatIndex)
                .build();
    }
}
