package com.holdup.server.table;

import com.holdup.server.player.Player;

import java.math.BigDecimal;

/**
 * 테이블의 한 자리. 시트 인덱스와 착석한 플레이어(없으면 빈 자리).
 */
public class Seat {

    private final int seatIndex;
    private Player player;
    /** 이 핸드에서 이 시트에 걸린 총 베팅 (사이드팟 계산용). */
    private BigDecimal totalBetThisHand;

    public Seat(int seatIndex) {
        this.seatIndex = seatIndex;
        this.totalBetThisHand = BigDecimal.ZERO;
    }

    public int getSeatIndex() {
        return seatIndex;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
        if (player != null) {
            player.setSeatIndex(seatIndex);
        }
    }

    public boolean isEmpty() {
        return player == null;
    }

    public BigDecimal getTotalBetThisHand() {
        return totalBetThisHand;
    }

    public void setTotalBetThisHand(BigDecimal totalBetThisHand) {
        this.totalBetThisHand = totalBetThisHand != null ? totalBetThisHand : BigDecimal.ZERO;
    }

    public void addToTotalBetThisHand(BigDecimal amount) {
        if (amount != null && amount.signum() > 0) {
            totalBetThisHand = totalBetThisHand.add(amount);
        }
    }

    /** 새 핸드 시작 시 초기화. */
    public void clearTotalBetThisHand() {
        this.totalBetThisHand = BigDecimal.ZERO;
    }
}
