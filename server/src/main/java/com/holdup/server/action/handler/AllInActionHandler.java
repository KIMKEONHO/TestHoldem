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
public class AllInActionHandler implements ActionHandler {

    private final TableManager tableManager;
    private final GameFlowService gameFlowService;

    public AllInActionHandler(TableManager tableManager, GameFlowService gameFlowService) {
        this.tableManager = tableManager;
        this.gameFlowService = gameFlowService;
    }

    @Override
    public GameActionType getActionType() {
        return GameActionType.ALL_IN;
    }

    @Override
    public ActionResult handle(PlayerActionRequest request, String playerId) {
        Optional<ActionResult> invalid = ActionValidation.validateTableAndTurn(
                tableManager, request.getTableId(), playerId, GameActionType.ALL_IN);
        if (invalid.isPresent()) return invalid.get();

        Table table = tableManager.getTable(request.getTableId()).orElseThrow();
        int seatIndex = table.getSeatIndexByPlayerId(playerId);
        Seat seat = table.getSeat(seatIndex);
        Player player = seat.getPlayer();
        BigDecimal allInAmount = player.getStack();
        if (allInAmount.signum() <= 0) {
            return ActionResult.builder()
                    .success(false)
                    .message("No stack to go all-in")
                    .actionType(GameActionType.ALL_IN)
                    .playerId(playerId)
                    .tableId(table.getId())
                    .seatIndex(seatIndex)
                    .build();
        }

        player.deductStack(allInAmount);
        player.setAllIn(true);
        player.setCurrentBetThisStreet(player.getCurrentBetThisStreet().add(allInAmount));
        table.getHandState().addToPot(allInAmount);
        if (player.getCurrentBetThisStreet().compareTo(table.getHandState().getCurrentBet()) > 0) {
            table.getHandState().setCurrentBet(player.getCurrentBetThisStreet());
        }
        table.getHandState().setBetForSeat(seatIndex, player.getCurrentBetThisStreet());
        seat.addToTotalBetThisHand(allInAmount);

        gameFlowService.afterPlayerAction(table.getId(), seatIndex);

        return ActionResult.builder()
                .success(true)
                .actionType(GameActionType.ALL_IN)
                .playerId(playerId)
                .tableId(table.getId())
                .seatIndex(seatIndex)
                .amount(allInAmount)
                .build();
    }
}
