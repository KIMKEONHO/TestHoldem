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
public class BetActionHandler implements ActionHandler {

    private final TableManager tableManager;
    private final GameFlowService gameFlowService;

    public BetActionHandler(TableManager tableManager, GameFlowService gameFlowService) {
        this.tableManager = tableManager;
        this.gameFlowService = gameFlowService;
    }

    @Override
    public GameActionType getActionType() {
        return GameActionType.BET;
    }

    @Override
    public ActionResult handle(PlayerActionRequest request, String playerId) {
        Optional<ActionResult> invalid = ActionValidation.validateTableAndTurn(
                tableManager, request.getTableId(), playerId, GameActionType.BET);
        if (invalid.isPresent()) return invalid.get();

        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            return ActionResult.builder()
                    .success(false)
                    .message("Bet amount must be positive")
                    .actionType(GameActionType.BET)
                    .playerId(playerId)
                    .tableId(request.getTableId())
                    .build();
        }

        Table table = tableManager.getTable(request.getTableId()).orElseThrow();
        int seatIndex = table.getSeatIndexByPlayerId(playerId);
        Seat seat = table.getSeat(seatIndex);
        Player player = seat.getPlayer();
        BigDecimal amount = request.getAmount().min(player.getStack());
        BigDecimal minRaise = table.getHandState().getMinRaise();
        if (amount.compareTo(minRaise) < 0 && table.getHandState().getCurrentBet().signum() > 0) {
            return ActionResult.builder()
                    .success(false)
                    .message("Bet must be at least " + minRaise)
                    .actionType(GameActionType.BET)
                    .playerId(playerId)
                    .tableId(table.getId())
                    .seatIndex(seatIndex)
                    .build();
        }

        player.deductStack(amount);
        if (player.getStack().signum() == 0) player.setAllIn(true);
        player.setCurrentBetThisStreet(player.getCurrentBetThisStreet().add(amount));
        table.getHandState().addToPot(amount);
        table.getHandState().setCurrentBet(player.getCurrentBetThisStreet());
        table.getHandState().setBetForSeat(seatIndex, player.getCurrentBetThisStreet());
        seat.addToTotalBetThisHand(amount);

        gameFlowService.afterPlayerAction(table.getId(), seatIndex);

        return ActionResult.builder()
                .success(true)
                .actionType(GameActionType.BET)
                .playerId(playerId)
                .tableId(table.getId())
                .seatIndex(seatIndex)
                .amount(amount)
                .build();
    }
}
