package com.holdup.server.table;

import com.holdup.server.card.Card;
import com.holdup.server.deck.Deck;
import com.holdup.server.gamestate.GamePhase;
import com.holdup.server.gamestate.HandState;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 홀덤 테이블. 시트, 현재 핸드 상태, 덱, 블라인드 설정.
 */
public class Table {

    private final String id;
    private final String name;
    private final List<Seat> seats;
    private final HandState handState;
    private Deck deck;
    private int dealerSeatIndex;
    private int smallBlindSeatIndex;
    private int bigBlindSeatIndex;
    private BigDecimal smallBlindAmount;
    private BigDecimal bigBlindAmount;

    public Table(String id, int maxSeats) {
        this.id = id;
        this.name = "Table-" + id;
        this.seats = new ArrayList<>();
        for (int i = 0; i < maxSeats; i++) {
            seats.add(new Seat(i));
        }
        this.handState = new HandState();
        this.smallBlindAmount = BigDecimal.ONE;
        this.bigBlindAmount = BigDecimal.valueOf(2);
    }

    public Table(String id, String name, int maxSeats) {
        this.id = id;
        this.name = name != null ? name : "Table-" + id;
        this.seats = new ArrayList<>();
        for (int i = 0; i < maxSeats; i++) {
            seats.add(new Seat(i));
        }
        this.handState = new HandState();
        this.smallBlindAmount = BigDecimal.ONE;
        this.bigBlindAmount = BigDecimal.valueOf(2);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Seat> getSeats() {
        return Collections.unmodifiableList(seats);
    }

    public Seat getSeat(int seatIndex) {
        if (seatIndex < 0 || seatIndex >= seats.size()) return null;
        return seats.get(seatIndex);
    }

    public int getMaxSeats() {
        return seats.size();
    }

    /** 착석 중인 플레이어 수. */
    public long countOccupiedSeats() {
        return seats.stream().filter(s -> !s.isEmpty()).count();
    }

    public HandState getHandState() {
        return handState;
    }

    public Deck getDeck() {
        return deck;
    }

    public void setDeck(Deck deck) {
        this.deck = deck;
    }

    public int getDealerSeatIndex() {
        return dealerSeatIndex;
    }

    public void setDealerSeatIndex(int dealerSeatIndex) {
        this.dealerSeatIndex = dealerSeatIndex;
    }

    public int getSmallBlindSeatIndex() {
        return smallBlindSeatIndex;
    }

    public void setSmallBlindSeatIndex(int smallBlindSeatIndex) {
        this.smallBlindSeatIndex = smallBlindSeatIndex;
    }

    public int getBigBlindSeatIndex() {
        return bigBlindSeatIndex;
    }

    public void setBigBlindSeatIndex(int bigBlindSeatIndex) {
        this.bigBlindSeatIndex = bigBlindSeatIndex;
    }

    public BigDecimal getSmallBlindAmount() {
        return smallBlindAmount;
    }

    public void setSmallBlindAmount(BigDecimal smallBlindAmount) {
        this.smallBlindAmount = smallBlindAmount != null ? smallBlindAmount : BigDecimal.ZERO;
    }

    public BigDecimal getBigBlindAmount() {
        return bigBlindAmount;
    }

    public void setBigBlindAmount(BigDecimal bigBlindAmount) {
        this.bigBlindAmount = bigBlindAmount != null ? bigBlindAmount : BigDecimal.ZERO;
    }

    /** 현재 커뮤니티 카드 (편의). */
    public List<Card> getCommunityCards() {
        return handState.getCommunityCards();
    }

    /** 현재 게임 단계 (편의). */
    public GamePhase getPhase() {
        return handState.getPhase();
    }

    /** playerId로 시트 인덱스 조회. 없으면 -1. */
    public int getSeatIndexByPlayerId(String playerId) {
        if (playerId == null) return -1;
        for (int i = 0; i < seats.size(); i++) {
            if (!seats.get(i).isEmpty() && playerId.equals(seats.get(i).getPlayer().getId())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * startFrom 부터 시계 방향으로, 착석한 시트 인덱스 목록.
     * 딜링 순서·액션 순서 계산용.
     */
    public List<Integer> getOccupiedSeatIndicesInOrder(int startFrom) {
        int n = seats.size();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int idx = (startFrom + i) % n;
            if (!seats.get(idx).isEmpty()) {
                order.add(idx);
            }
        }
        return order;
    }

    /** 폴드하지 않고 핸드에 참여 중인 시트 수. */
    public long countActiveInHand() {
        return seats.stream()
                .filter(s -> !s.isEmpty() && s.getPlayer() != null && !s.getPlayer().isFolded())
                .count();
    }

    /** 폴드하지 않은 다음 액션 가능 시트 인덱스 (actingSeatIndex 다음부터). 없으면 -1. */
    public int findNextActingSeat(int fromSeatIndex) {
        List<Integer> order = getOccupiedSeatIndicesInOrder((fromSeatIndex + 1) % getMaxSeats());
        for (Integer idx : order) {
            com.holdup.server.player.Player p = getSeat(idx).getPlayer();
            if (p != null && !p.isFolded() && !p.isAllIn()) return idx;
        }
        return -1;
    }
}
