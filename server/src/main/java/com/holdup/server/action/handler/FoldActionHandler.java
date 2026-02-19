package com.holdup.server.action.handler;

import com.holdup.server.action.ActionHandler;
import com.holdup.server.action.ActionValidation;
import com.holdup.server.action.GameActionType;
import com.holdup.server.action.dto.ActionResult;
import com.holdup.server.action.dto.PlayerActionRequest;
import com.holdup.server.service.GameFlowService;
import com.holdup.server.service.TableManager;
import com.holdup.server.table.Table;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class FoldActionHandler implements ActionHandler {

    private final TableManager tableManager;
    private final GameFlowService gameFlowService;

    public FoldActionHandler(TableManager tableManager, GameFlowService gameFlowService) {
        this.tableManager = tableManager;
        this.gameFlowService = gameFlowService;
    }

    @Override
    public GameActionType getActionType() {
        return GameActionType.FOLD;
    }

    @Override
    public ActionResult handle(PlayerActionRequest request, String playerId) {
        Optional<ActionResult> invalid = ActionValidation.validateTableAndTurn(
                tableManager, request.getTableId(), playerId, GameActionType.FOLD);
        if (invalid.isPresent()) return invalid.get();

        Table table = tableManager.getTable(request.getTableId()).orElseThrow();
        int seatIndex = table.getSeatIndexByPlayerId(playerId);
        table.getSeat(seatIndex).getPlayer().setFolded(true);

        gameFlowService.afterPlayerAction(table.getId(), seatIndex);

        return ActionResult.builder()
                .success(true)
                .actionType(GameActionType.FOLD)
                .playerId(playerId)
                .tableId(table.getId())
                .seatIndex(seatIndex)
                .build();
    }
}
