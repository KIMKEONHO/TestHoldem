package com.holdup.server.player;

import com.holdup.server.card.Card;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 테이블에 앉은 플레이어 상태. (한 테이블·한 핸드 기준)
 */
public class Player {

    private final String id;
    private String displayName;
    private BigDecimal stack;
    private int seatIndex;
    private final List<Card> holeCards;
    private boolean folded;
    private boolean allIn;
    /** 이번 스트릿에서 이미 건 금액 (콜/레이즈 계산용). */
    private BigDecimal currentBetThisStreet;

    public Player(String id) {
        this.id = id;
        this.stack = BigDecimal.ZERO;
        this.holeCards = new ArrayList<>();
        this.currentBetThisStreet = BigDecimal.ZERO;
    }

    public Player(String id, String displayName, BigDecimal initialStack) {
        this.id = id;
        this.displayName = displayName != null ? displayName : id;
        this.stack = initialStack != null ? initialStack : BigDecimal.ZERO;
        this.holeCards = new ArrayList<>();
        this.currentBetThisStreet = BigDecimal.ZERO;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public BigDecimal getStack() {
        return stack;
    }

    public void setStack(BigDecimal stack) {
        this.stack = stack != null ? stack : BigDecimal.ZERO;
    }

    /** 스택에서 금액 차감 (베팅/콜/레이즈). */
    public void deductStack(BigDecimal amount) {
        if (amount != null && amount.signum() > 0) {
            stack = stack.subtract(amount);
            if (stack.signum() < 0) stack = BigDecimal.ZERO;
        }
    }

    /** 팟에서 승리 금액 추가. */
    public void addToStack(BigDecimal amount) {
        if (amount != null && amount.signum() > 0) {
            stack = stack.add(amount);
        }
    }

    public int getSeatIndex() {
        return seatIndex;
    }

    public void setSeatIndex(int seatIndex) {
        this.seatIndex = seatIndex;
    }

    public List<Card> getHoleCards() {
        return Collections.unmodifiableList(holeCards);
    }

    public void setHoleCards(List<Card> cards) {
        holeCards.clear();
        if (cards != null) holeCards.addAll(cards);
    }

    public void clearHoleCards() {
        holeCards.clear();
    }

    public boolean isFolded() {
        return folded;
    }

    public void setFolded(boolean folded) {
        this.folded = folded;
    }

    public boolean isAllIn() {
        return allIn;
    }

    public void setAllIn(boolean allIn) {
        this.allIn = allIn;
    }

    public BigDecimal getCurrentBetThisStreet() {
        return currentBetThisStreet;
    }

    public void setCurrentBetThisStreet(BigDecimal currentBetThisStreet) {
        this.currentBetThisStreet = currentBetThisStreet != null ? currentBetThisStreet : BigDecimal.ZERO;
    }

    /** 이번 스트릿 시작 시 베팅 초기화. */
    public void clearBetThisStreet() {
        this.currentBetThisStreet = BigDecimal.ZERO;
    }

    /** 다음 핸드용 상태 초기화 (폴드/올인/스트릿 베팅만, 스택·자리는 유지). */
    public void resetForNewHand() {
        holeCards.clear();
        folded = false;
        allIn = false;
        currentBetThisStreet = BigDecimal.ZERO;
    }
}
