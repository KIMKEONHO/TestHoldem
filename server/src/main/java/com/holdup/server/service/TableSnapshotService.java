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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Table 엔티티를 클라이언트용 TableSnapshot으로 변환.
 * viewerPlayerId 기준으로 해당 플레이어에게만 holeCards 노출.
 */
@Service
public class TableSnapshotService {

    /**
     * 쇼다운 시 모든 참여 플레이어의 holeCards를 포함한 스냅샷.
     * phase는 테이블 현재 phase(SHOWDOWN) 그대로 사용.
     */
    public TableSnapshot toSnapshotWithShowdownCards(Table table) {
        if (table == null) return null;
        return toSnapshot(table, null, true);
    }

    /**
     * @param viewerPlayerId 본인 holeCards를 넣을 플레이어 ID. null이면 모든 플레이어 holeCards 미포함(브로드캐스트용).
     * @param includeAllHoleCards true면 쇼다운으로 모든 플레이어의 holeCards 포함.
     */
    public TableSnapshot toSnapshot(Table table, String viewerPlayerId, boolean includeAllHoleCards) {
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
                            .player(toPlayerSnapshot(seat.getPlayer(), viewerPlayerId, includeAllHoleCards))
                            .build();
            seats.add(ss);
        }

        HandState hs = table.getHandState();
        List<String> communityCards = hs.getCommunityCards().stream()
                .map(Card::toString)
                .collect(Collectors.toList());

        Set<Integer> inHandSeatIndices = hs.getSeatIndicesInHand();
        var inHandPlayerIds = hs.getPlayerIdsInHand();
        TableSnapshot.HandStateSnapshot handStateSnapshot = TableSnapshot.HandStateSnapshot.builder()
                .phase(hs.getPhase() != null ? hs.getPhase().name() : GamePhase.WAITING.name())
                .communityCards(communityCards)
                .pot(hs.getPot() != null ? hs.getPot() : BigDecimal.ZERO)
                .currentBet(hs.getCurrentBet() != null ? hs.getCurrentBet() : BigDecimal.ZERO)
                .actingSeatIndex(hs.getActingSeatIndex())
                .minRaise(hs.getMinRaise() != null ? hs.getMinRaise() : BigDecimal.ZERO)
                .inHandSeatIndices(inHandSeatIndices)
                .inHandPlayerIds(inHandPlayerIds)
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

    /**
     * @param viewerPlayerId 본인 holeCards를 넣을 플레이어 ID. null이면 모든 플레이어 holeCards 미포함(브로드캐스트용).
     */
    public TableSnapshot toSnapshot(Table table, String viewerPlayerId) {
        return toSnapshot(table, viewerPlayerId, false);
    }

    private TableSnapshot.PlayerSnapshot toPlayerSnapshot(Player p, String viewerPlayerId, boolean includeAllHoleCards) {
        if (p == null) return null;
        boolean isViewer = viewerPlayerId != null && p.getId() != null && p.getId().equals(viewerPlayerId);
        List<String> holeCards = null;
        if (includeAllHoleCards && p.getHoleCards() != null && !p.getHoleCards().isEmpty()) {
            holeCards = p.getHoleCards().stream().map(Card::toString).collect(Collectors.toList());
        } else if (isViewer && p.getHoleCards() != null) {
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
