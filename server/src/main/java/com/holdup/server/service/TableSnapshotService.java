package com.holdup.server.service;

import com.holdup.server.action.dto.TableSnapshot;
import com.holdup.server.card.Card;
import com.holdup.server.gamestate.GamePhase;
import com.holdup.server.gamestate.HandState;
import com.holdup.server.player.Player;
import com.holdup.server.table.Seat;
import com.holdup.server.table.Table;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Table 엔티티를 클라이언트용 TableSnapshot으로 변환.
 * viewerPlayerId 기준으로 해당 플레이어에게만 holeCards 노출.
 */
@Service
public class TableSnapshotService {

    /**
     * @param viewerPlayerId 본인 holeCards를 넣을 플레이어 ID. null이면 모든 플레이어 holeCards 미포함(브로드캐스트용).
     */
    public TableSnapshot toSnapshot(Table table, String viewerPlayerId) {
        if (table == null) return null;

        List<TableSnapshot.SeatSnapshot> seats = new ArrayList<>();
        for (Seat seat : table.getSeats()) {
            TableSnapshot.SeatSnapshot ss = seat.isEmpty()
                    ? TableSnapshot.SeatSnapshot.builder()
                            .seatIndex(seat.getSeatIndex())
                            .player(null)
                            .build()
                    : TableSnapshot.SeatSnapshot.builder()
                            .seatIndex(seat.getSeatIndex())
                            .player(toPlayerSnapshot(seat.getPlayer(), viewerPlayerId))
                            .build();
            seats.add(ss);
        }

        HandState hs = table.getHandState();
        List<String> communityCards = hs.getCommunityCards().stream()
                .map(Card::toString)
                .collect(Collectors.toList());

        TableSnapshot.HandStateSnapshot handStateSnapshot = TableSnapshot.HandStateSnapshot.builder()
                .phase(hs.getPhase() != null ? hs.getPhase().name() : GamePhase.WAITING.name())
                .communityCards(communityCards)
                .pot(hs.getPot() != null ? hs.getPot() : BigDecimal.ZERO)
                .currentBet(hs.getCurrentBet() != null ? hs.getCurrentBet() : BigDecimal.ZERO)
                .actingSeatIndex(hs.getActingSeatIndex())
                .minRaise(hs.getMinRaise() != null ? hs.getMinRaise() : BigDecimal.ZERO)
                .build();

        return TableSnapshot.builder()
                .tableId(table.getId())
                .tableName(table.getName())
                .seats(seats)
                .handState(handStateSnapshot)
                .smallBlindAmount(table.getSmallBlindAmount())
                .bigBlindAmount(table.getBigBlindAmount())
                .build();
    }

    private TableSnapshot.PlayerSnapshot toPlayerSnapshot(Player p, String viewerPlayerId) {
        if (p == null) return null;
        boolean isViewer = p.getId() != null && p.getId().equals(viewerPlayerId);
        List<String> holeCards = null;
        if (isViewer && p.getHoleCards() != null) {
            holeCards = p.getHoleCards().stream().map(Card::toString).collect(Collectors.toList());
        }
        return TableSnapshot.PlayerSnapshot.builder()
                .id(p.getId())
                .displayName(p.getDisplayName())
                .stack(p.getStack() != null ? p.getStack() : BigDecimal.ZERO)
                .folded(p.isFolded())
                .allIn(p.isAllIn())
                .currentBetThisStreet(p.getCurrentBetThisStreet() != null ? p.getCurrentBetThisStreet() : BigDecimal.ZERO)
                .holeCards(holeCards)
                .build();
    }
}
