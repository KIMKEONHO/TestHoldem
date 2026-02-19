package com.holdup.server.action.handler;

import com.holdup.server.action.ActionHandler;
import com.holdup.server.action.GameActionType;
import com.holdup.server.action.dto.ActionResult;
import com.holdup.server.action.dto.PlayerActionRequest;
import com.holdup.server.player.Player;
import com.holdup.server.service.TableManager;
import com.holdup.server.table.Table;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SitActionHandler implements ActionHandler {

    private final TableManager tableManager;

    public SitActionHandler(TableManager tableManager) {
        this.tableManager = tableManager;
    }

    @Override
    public GameActionType getActionType() {
        return GameActionType.SIT;
    }

    @Override
    public ActionResult handle(PlayerActionRequest request, String playerId) {
        String tableId = request.getTableId();
        if (tableId == null || request.getSeatIndex() == null) {
            return ActionResult.builder()
                    .success(false)
                    .message("tableId and seatIndex required")
                    .actionType(GameActionType.SIT)
                    .playerId(playerId)
                    .build();
        }
        Optional<Table> opt = tableManager.getTable(tableId);
        if (opt.isEmpty()) {
            return ActionResult.builder()
                    .success(false)
                    .message("Table not found")
                    .actionType(GameActionType.SIT)
                    .playerId(playerId)
                    .tableId(tableId)
                    .build();
        }
        Table table = opt.get();
        int seatIndex = request.getSeatIndex();
        if (seatIndex < 0 || seatIndex >= table.getMaxSeats()) {
            return ActionResult.builder()
                    .success(false)
                    .message("Invalid seatIndex")
                    .actionType(GameActionType.SIT)
                    .playerId(playerId)
                    .tableId(tableId)
                    .build();
        }
        if (!table.getSeat(seatIndex).isEmpty()) {
            return ActionResult.builder()
                    .success(false)
                    .message("Seat occupied")
                    .actionType(GameActionType.SIT)
                    .playerId(playerId)
                    .tableId(tableId)
                    .seatIndex(seatIndex)
                    .build();
        }
        // 이미 다른 자리에 앉아 있으면 그 자리 비우고 플레이어는 유지
        int currentSeat = table.getSeatIndexByPlayerId(playerId);
        Player player = null;
        if (currentSeat >= 0) {
            player = table.getSeat(currentSeat).getPlayer();
            table.getSeat(currentSeat).setPlayer(null);
        }
        if (player == null) {
            String displayName = request.getPlayerId() != null && !request.getPlayerId().isBlank()
                    ? request.getPlayerId()
                    : ("Player-" + seatIndex);
            player = new Player(playerId, displayName, JoinTableConstants.DEFAULT_STACK);
        }
        table.getSeat(seatIndex).setPlayer(player);
        return ActionResult.builder()
                .success(true)
                .actionType(GameActionType.SIT)
                .playerId(playerId)
                .tableId(tableId)
                .seatIndex(seatIndex)
                .build();
    }
}
