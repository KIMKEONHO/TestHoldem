package com.holdup.server.action.handler;

import com.holdup.server.action.ActionHandler;
import com.holdup.server.action.ActionValidation;
import com.holdup.server.action.GameActionType;
import com.holdup.server.action.dto.ActionResult;
import com.holdup.server.action.dto.PlayerActionRequest;
import com.holdup.server.player.Player;
import com.holdup.server.service.GameFlowService;
import com.holdup.server.service.TableManager;
import com.holdup.server.table.Seat;
import com.holdup.server.table.Table;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class CallActionHandler implements ActionHandler {

    private final TableManager tableManager;
    private final GameFlowService gameFlowService;

    public CallActionHandler(TableManager tableManager, GameFlowService gameFlowService) {
        this.tableManager = tableManager;
        this.gameFlowService = gameFlowService;
    }

    @Override
    public GameActionType getActionType() {
        return GameActionType.CALL;
    }

    @Override
    public ActionResult handle(PlayerActionRequest request, String playerId) {
        Optional<ActionResult> invalid = ActionValidation.validateTableAndTurn(
                tableManager, request.getTableId(), playerId, GameActionType.CALL);
        if (invalid.isPresent()) return invalid.get();

        Table table = tableManager.getTable(request.getTableId()).orElseThrow();
        int seatIndex = table.getSeatIndexByPlayerId(playerId);
        Seat seat = table.getSeat(seatIndex);
        Player player = seat.getPlayer();
        BigDecimal currentBet = table.getHandState().getCurrentBet();
        BigDecimal toCall = currentBet.subtract(player.getCurrentBetThisStreet());
        if (toCall.signum() <= 0) {
            return ActionResult.builder()
                    .success(false)
                    .message("Nothing to call")
                    .actionType(GameActionType.CALL)
                    .playerId(playerId)
                    .tableId(table.getId())
                    .seatIndex(seatIndex)
                    .build();
        }
        BigDecimal actual = toCall.min(player.getStack());
        player.deductStack(actual);
        if (player.getStack().signum() == 0) player.setAllIn(true);
        player.setCurrentBetThisStreet(player.getCurrentBetThisStreet().add(actual));
        table.getHandState().addToPot(actual);
        table.getHandState().setBetForSeat(seatIndex, player.getCurrentBetThisStreet());
        seat.addToTotalBetThisHand(actual);

        gameFlowService.afterPlayerAction(table.getId(), seatIndex);

        return ActionResult.builder()
                .success(true)
                .actionType(GameActionType.CALL)
                .playerId(playerId)
                .tableId(table.getId())
                .seatIndex(seatIndex)
                .amount(actual)
                .build();
    }
}
