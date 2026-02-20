package com.holdup.server.gamestate;

import com.holdup.server.card.Card;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 한 핸드(Hand)의 진행 상태. 커뮤니티 카드, 팟, 현재 베팅/턴 등.
 */
public class HandState {

    private GamePhase phase;
    private final List<Card> communityCards;
    /** 메인 팟. */
    private BigDecimal pot;
    /** 현재 스트릿에서의 최고 베팅액 (콜/레이즈 계산용). */
    private BigDecimal currentBet;
    /** 이번 스트릿에서 시트별 베팅 누적 (시트 인덱스 → 금액). */
    private final Map<Integer, BigDecimal> betPerSeatThisStreet;
    /** 딜러 버튼 시트 인덱스. */
    private int dealerSeatIndex;
    /** 현재 액션할 시트 인덱스. */
    private int actingSeatIndex;
    /** 최소 레이즈 금액 (현재 베팅 + 이전 레이즈 크기 등). */
    private BigDecimal minRaise;
    /** 이번 스트릿에서 첫 액션한 시트 인덱스 (스트릿 종료 판단용). */
    private int firstActingSeatIndexThisStreet;
    /** 이번 핸드에 참여한 시트 인덱스 (핸드 시작 시점에 착석한 플레이어만). */
    private Set<Integer> seatIndicesInHand;
    /** 이번 핸드에 참여한 플레이어 ID (핸드 시작 시점 착석자만). 도중 입장/같은 자리 새 플레이어는 제외. */
    private Set<String> playerIdsInHand;

    public HandState() {
        this.phase = GamePhase.WAITING;
        this.communityCards = new ArrayList<>();
        this.pot = BigDecimal.ZERO;
        this.currentBet = BigDecimal.ZERO;
        this.betPerSeatThisStreet = new ConcurrentHashMap<>();
        this.dealerSeatIndex = 0;
        this.actingSeatIndex = 0;
        this.minRaise = BigDecimal.ZERO;
        this.firstActingSeatIndexThisStreet = 0;
        this.seatIndicesInHand = new HashSet<>();
        this.playerIdsInHand = new HashSet<>();
    }

    public Set<Integer> getSeatIndicesInHand() {
        return seatIndicesInHand == null ? Set.of() : Collections.unmodifiableSet(seatIndicesInHand);
    }

    public void setSeatIndicesInHand(Set<Integer> indices) {
        this.seatIndicesInHand = indices != null ? new HashSet<>(indices) : new HashSet<>();
    }

    public Set<String> getPlayerIdsInHand() {
        return playerIdsInHand == null ? Set.of() : Collections.unmodifiableSet(playerIdsInHand);
    }

    public void setPlayerIdsInHand(Set<String> ids) {
        this.playerIdsInHand = ids != null ? new HashSet<>(ids) : new HashSet<>();
    }

    public GamePhase getPhase() {
        return phase;
    }

    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    public List<Card> getCommunityCards() {
        return Collections.unmodifiableList(communityCards);
    }

    public void addCommunityCard(Card card) {
        if (card != null) communityCards.add(card);
    }

    /** 새 핸드 시작 시 커뮤니티 카드 비우기. */
    public void clearCommunityCards() {
        communityCards.clear();
    }

    public void addToPot(BigDecimal amount) {
        if (amount != null && amount.signum() > 0) {
            pot = pot.add(amount);
        }
    }

    public BigDecimal getPot() {
        return pot;
    }

    public void setPot(BigDecimal pot) {
        this.pot = pot != null ? pot : BigDecimal.ZERO;
    }

    public BigDecimal getCurrentBet() {
        return currentBet;
    }

    public void setCurrentBet(BigDecimal currentBet) {
        this.currentBet = currentBet != null ? currentBet : BigDecimal.ZERO;
    }

    public Map<Integer, BigDecimal> getBetPerSeatThisStreet() {
        return Collections.unmodifiableMap(betPerSeatThisStreet);
    }

    public void setBetForSeat(int seatIndex, BigDecimal amount) {
        if (amount != null) betPerSeatThisStreet.put(seatIndex, amount);
    }

    /** 새 스트릿 시작 시 시트별 베팅 초기화. */
    public void clearBetsThisStreet() {
        betPerSeatThisStreet.clear();
        currentBet = BigDecimal.ZERO;
    }

    public int getDealerSeatIndex() {
        return dealerSeatIndex;
    }

    public void setDealerSeatIndex(int dealerSeatIndex) {
        this.dealerSeatIndex = dealerSeatIndex;
    }

    public int getActingSeatIndex() {
        return actingSeatIndex;
    }

    public void setActingSeatIndex(int actingSeatIndex) {
        this.actingSeatIndex = actingSeatIndex;
    }

    public BigDecimal getMinRaise() {
        return minRaise;
    }

    public void setMinRaise(BigDecimal minRaise) {
        this.minRaise = minRaise != null ? minRaise : BigDecimal.ZERO;
    }

    public int getFirstActingSeatIndexThisStreet() {
        return firstActingSeatIndexThisStreet;
    }

    public void setFirstActingSeatIndexThisStreet(int firstActingSeatIndexThisStreet) {
        this.firstActingSeatIndexThisStreet = firstActingSeatIndexThisStreet;
    }
}
