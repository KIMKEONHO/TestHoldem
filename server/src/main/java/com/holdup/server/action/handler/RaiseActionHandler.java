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
public class RaiseActionHandler implements ActionHandler {

    private final TableManager tableManager;
    private final GameFlowService gameFlowService;

    public RaiseActionHandler(TableManager tableManager, GameFlowService gameFlowService) {
        this.tableManager = tableManager;
        this.gameFlowService = gameFlowService;
    }

    @Override
    public GameActionType getActionType() {
        return GameActionType.RAISE;
    }

    @Override
    public ActionResult handle(PlayerActionRequest request, String playerId) {
        Optional<ActionResult> invalid = ActionValidation.validateTableAndTurn(
                tableManager, request.getTableId(), playerId, GameActionType.RAISE);
        if (invalid.isPresent()) return invalid.get();

        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            return ActionResult.builder()
                    .success(false)
                    .message("Raise amount must be positive")
                    .actionType(GameActionType.RAISE)
                    .playerId(playerId)
                    .tableId(request.getTableId())
                    .build();
        }

        Table table = tableManager.getTable(request.getTableId()).orElseThrow();
        int seatIndex = table.getSeatIndexByPlayerId(playerId);
        Seat seat = table.getSeat(seatIndex);
        Player player = seat.getPlayer();
        BigDecimal currentBet = table.getHandState().getCurrentBet();
        BigDecimal alreadyIn = player.getCurrentBetThisStreet();
        BigDecimal toCall = currentBet.subtract(alreadyIn);
        BigDecimal minRaise = table.getHandState().getMinRaise();
        // amount = 추가로 걸 금액 (콜분 + 레이즈분)
        BigDecimal addAmount = request.getAmount().min(player.getStack());
        if (addAmount.compareTo(toCall.add(minRaise)) < 0 && addAmount.compareTo(player.getStack()) < 0) {
            return ActionResult.builder()
                    .success(false)
                    .message("Raise must be at least " + minRaise + " more than current bet")
                    .actionType(GameActionType.RAISE)
                    .playerId(playerId)
                    .tableId(table.getId())
                    .seatIndex(seatIndex)
                    .build();
        }
        if (addAmount.compareTo(toCall) < 0) {
            return ActionResult.builder()
                    .success(false)
                    .message("Amount must at least call " + toCall)
                    .actionType(GameActionType.RAISE)
                    .playerId(playerId)
                    .tableId(table.getId())
                    .seatIndex(seatIndex)
                    .build();
        }
        player.deductStack(addAmount);
        if (player.getStack().signum() == 0) player.setAllIn(true);
        player.setCurrentBetThisStreet(player.getCurrentBetThisStreet().add(addAmount));
        table.getHandState().addToPot(addAmount);
        table.getHandState().setCurrentBet(player.getCurrentBetThisStreet());
        table.getHandState().setBetForSeat(seatIndex, player.getCurrentBetThisStreet());
        seat.addToTotalBetThisHand(addAmount);

        gameFlowService.afterPlayerAction(table.getId(), seatIndex);

        return ActionResult.builder()
                .success(true)
                .actionType(GameActionType.RAISE)
                .playerId(playerId)
                .tableId(table.getId())
                .seatIndex(seatIndex)
                .amount(addAmount)
                .build();
    }
}
