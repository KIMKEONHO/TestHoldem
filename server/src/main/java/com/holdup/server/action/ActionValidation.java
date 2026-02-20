package com.holdup.server.action;

import com.holdup.server.action.dto.ActionResult;
import com.holdup.server.gamestate.GamePhase;
import com.holdup.server.service.TableManager;
import com.holdup.server.table.Table;

import java.util.Optional;
import java.util.Set;

/**
 * 액션 핸들러 공통: 테이블 조회·턴 검증.
 */
public final class ActionValidation {

    private ActionValidation() {}

    /**
     * 테이블 존재·플레이어 착석·현재 턴 여부 검증.
     * @return 실패 시 ActionResult, 성공 시 empty
     */
    public static Optional<ActionResult> validateTableAndTurn(
            TableManager tableManager,
            String tableId,
            String playerId,
            GameActionType actionType) {

        if (tableId == null || tableId.isBlank()) {
            return Optional.of(ActionResult.builder()
                    .success(false)
                    .message("tableId required")
                    .actionType(actionType)
                    .playerId(playerId)
                    .build());
        }
        Optional<Table> opt = tableManager.getTable(tableId);
        if (opt.isEmpty()) {
            return Optional.of(ActionResult.builder()
                    .success(false)
                    .message("Table not found: " + tableId)
                    .actionType(actionType)
                    .playerId(playerId)
                    .tableId(tableId)
                    .build());
        }
        Table table = opt.get();
        int seatIndex = table.getSeatIndexByPlayerId(playerId);
        if (seatIndex < 0) {
            return Optional.of(ActionResult.builder()
                    .success(false)
                    .message("You are not at this table")
                    .actionType(actionType)
                    .playerId(playerId)
                    .tableId(tableId)
                    .build());
        }
        // 게임 진행 중인데 이번 핸드 참가자가 아니면(도중 입장) 액션 거부
        var handState = table.getHandState();
        if (handState.getPhase() != GamePhase.WAITING) {
            Set<String> playerIdsInHand = handState.getPlayerIdsInHand();
            if (!playerIdsInHand.isEmpty() && !playerIdsInHand.contains(playerId)) {
                return Optional.of(ActionResult.builder()
                        .success(false)
                        .message("이번 게임에는 참가하지 않습니다. 다음 게임까지 대기해 주세요.")
                        .actionType(actionType)
                        .playerId(playerId)
                        .tableId(tableId)
                        .seatIndex(seatIndex)
                        .build());
            }
        }
        if (handState.getActingSeatIndex() != seatIndex) {
            return Optional.of(ActionResult.builder()
                    .success(false)
                    .message("Not your turn")
                    .actionType(actionType)
                    .playerId(playerId)
                    .tableId(tableId)
                    .seatIndex(seatIndex)
                    .build());
        }
        return Optional.empty();
    }
}
