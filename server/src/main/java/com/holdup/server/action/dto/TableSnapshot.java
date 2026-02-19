package com.holdup.server.action.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 클라이언트에 브로드캐스트할 테이블 상태 스냅샷.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableSnapshot {

    private String tableId;
    private String tableName;
    private List<SeatSnapshot> seats;
    private HandStateSnapshot handState;
    private BigDecimal smallBlindAmount;
    private BigDecimal bigBlindAmount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatSnapshot {
        private int seatIndex;
        private PlayerSnapshot player; // null이면 빈 자리
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerSnapshot {
        private String id;
        private String displayName;
        private BigDecimal stack;
        private boolean folded;
        private boolean allIn;
        private BigDecimal currentBetThisStreet;
        /** 본인만 볼 수 있음 (다른 플레이어는 null). */
        private List<String> holeCards;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HandStateSnapshot {
        private String phase;
        private List<String> communityCards;
        private BigDecimal pot;
        private BigDecimal currentBet;
        private Integer actingSeatIndex;
        private BigDecimal minRaise;
    }
}
