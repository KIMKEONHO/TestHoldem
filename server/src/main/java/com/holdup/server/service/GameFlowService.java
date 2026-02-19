package com.holdup.server.service;

import com.holdup.server.deck.DeckFactory;
import com.holdup.server.gamestate.GamePhase;
import com.holdup.server.gamestate.HandState;
import com.holdup.server.handevaluator.ParticipantHand;
import com.holdup.server.handevaluator.SeatHandResult;
import com.holdup.server.handevaluator.WinnerResolver;
import com.holdup.server.player.Player;
import com.holdup.server.table.Seat;
import com.holdup.server.table.Table;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 한 핸드 시작·스트릿 전환·쇼다운·팟 분배를 담당.
 */
@Service
public class GameFlowService {

    private final TableManager tableManager;
    private final WinnerResolver winnerResolver;

    @Value("${holdup.game.min-players-to-start:2}")
    private int minPlayersToStart;

    public GameFlowService(TableManager tableManager, WinnerResolver winnerResolver) {
        this.tableManager = tableManager;
        this.winnerResolver = winnerResolver;
    }

    /**
     * 새 핸드 시작. WAITING 상태이고 착석 수가 min-players-to-start 이상일 때만 성공.
     */
    public boolean startNewHand(String tableId) {
        Optional<Table> opt = tableManager.getTable(tableId);
        if (opt.isEmpty()) return false;
        Table table = opt.get();
        HandState state = table.getHandState();

        if (state.getPhase() != GamePhase.WAITING) return false;
        if (table.countOccupiedSeats() < minPlayersToStart) return false;

        // 플레이어·시트 핸드 초기화
        for (Seat seat : table.getSeats()) {
            if (!seat.isEmpty()) {
                seat.getPlayer().resetForNewHand();
            }
            seat.clearTotalBetThisHand();
        }

        state.clearCommunityCards();
        state.setPot(BigDecimal.ZERO);
        state.clearBetsThisStreet();

        // 덱 셔플
        table.setDeck(DeckFactory.createShuffled());

        // 딜러/SB/BB 결정: 기존 딜러 다음 착석자부터 순서 [SB, BB, UTG, ...]
        List<Integer> order = table.getOccupiedSeatIndicesInOrder(state.getDealerSeatIndex() + 1);
        if (order.size() < 2) return false;

        int dealer, sbSeat, bbSeat;
        if (order.size() == 2) {
            dealer = order.get(0);
            sbSeat = order.get(0);
            bbSeat = order.get(1);
        } else {
            dealer = order.get(0);
            sbSeat = order.get(1);
            bbSeat = order.get(2);
        }
        table.setDealerSeatIndex(dealer);
        table.setSmallBlindSeatIndex(sbSeat);
        table.setBigBlindSeatIndex(bbSeat);
        state.setDealerSeatIndex(dealer);

        // 홀카드 딜링 (딜러 다음부터 시계방향)
        for (Integer idx : order) {
            Seat seat = table.getSeat(idx);
            if (!seat.isEmpty() && table.getDeck() != null) {
                seat.getPlayer().setHoleCards(table.getDeck().deal(2));
            }
        }

        // 블라인드 포스팅
        BigDecimal sb = table.getSmallBlindAmount();
        BigDecimal bb = table.getBigBlindAmount();
        postBlind(table, sbSeat, sb);
        postBlind(table, bbSeat, bb);

        state.setCurrentBet(bb);
        state.setMinRaise(bb);
        state.setPhase(GamePhase.PREFLOP);

        // 프리플랍 첫 액션: 2명이면 BB, 3명 이상이면 UTG
        int firstActing = order.size() == 2 ? order.get(1) : order.get(2);
        state.setFirstActingSeatIndexThisStreet(firstActing);
        state.setActingSeatIndex(firstActing);

        return true;
    }

    private void postBlind(Table table, int seatIndex, BigDecimal amount) {
        Seat seat = table.getSeat(seatIndex);
        if (seat == null || seat.isEmpty() || amount == null || amount.signum() <= 0) return;
        Player p = seat.getPlayer();
        BigDecimal actual = amount.min(p.getStack());
        p.deductStack(actual);
        if (actual.compareTo(p.getStack()) >= 0 && p.getStack().signum() == 0) p.setAllIn(true);
        table.getHandState().addToPot(actual);
        seat.addToTotalBetThisHand(actual);
        p.setCurrentBetThisStreet(actual);
        table.getHandState().setBetForSeat(seatIndex, actual);
    }

    /**
     * 플레이어 액션 후: 다음 턴으로 이동 또는 스트릿/핸드 종료 처리.
     *
     * @param tableId 테이블 ID
     * @param seatIndexWhoActed 액션한 시트 인덱스
     * @return true if state was advanced (next acting or street/hand ended)
     */
    public boolean afterPlayerAction(String tableId, int seatIndexWhoActed) {
        Optional<Table> opt = tableManager.getTable(tableId);
        if (opt.isEmpty()) return false;
        Table table = opt.get();
        HandState state = table.getHandState();

        // 1명만 남으면 즉시 승자 처리
        if (table.countActiveInHand() <= 1) {
            awardPotToLastStanding(table);
            return true;
        }

        // 다음 액션 가능 시트로 이동
        int next = table.findNextActingSeat(seatIndexWhoActed);
        if (next >= 0) {
            state.setActingSeatIndex(next);
            // 한 바퀴 돌아서 첫 액션자까지 왔고, 모두 베팅 맞춤이면 스트릿 종료
            if (next == state.getFirstActingSeatIndexThisStreet() && allHaveMatchedOrAllIn(table)) {
                advanceStreetOrShowdown(table);
            }
            return true;
        }

        // 다음 액션 없음 = 전원 올인 또는 모두 매칭됨
        if (allHaveMatchedOrAllIn(table)) {
            advanceStreetOrShowdown(table);
        }
        return true;
    }

    private boolean allHaveMatchedOrAllIn(Table table) {
        HandState state = table.getHandState();
        BigDecimal currentBet = state.getCurrentBet();
        for (Seat seat : table.getSeats()) {
            if (seat.isEmpty()) continue;
            Player p = seat.getPlayer();
            if (p.isFolded()) continue;
            if (p.isAllIn()) continue;
            if (p.getCurrentBetThisStreet().compareTo(currentBet) < 0) return false;
        }
        return true;
    }

    private void advanceStreetOrShowdown(Table table) {
        HandState state = table.getHandState();
        GamePhase phase = state.getPhase();

        if (phase == GamePhase.RIVER) {
            runShowdown(table);
            return;
        }

        // 스트릿 진행: 커뮤니티 카드 추가, 베팅 초기화
        state.clearBetsThisStreet();
        for (Seat seat : table.getSeats()) {
            if (!seat.isEmpty()) seat.getPlayer().clearBetThisStreet();
        }
        state.setCurrentBet(BigDecimal.ZERO);

        List<Integer> order = table.getOccupiedSeatIndicesInOrder(state.getDealerSeatIndex() + 1);
        int firstActing = -1;
        for (Integer idx : order) {
            Player p = table.getSeat(idx).getPlayer();
            if (p != null && !p.isFolded() && !p.isAllIn()) {
                firstActing = idx;
                break;
            }
        }

        var deck = table.getDeck();
        if (phase == GamePhase.PREFLOP) {
            state.setPhase(GamePhase.FLOP);
            for (int i = 0; i < 3 && deck != null; i++) {
                state.addCommunityCard(deck.dealOne());
            }
        } else if (phase == GamePhase.FLOP) {
            state.setPhase(GamePhase.TURN);
            if (deck != null) state.addCommunityCard(deck.dealOne());
        } else if (phase == GamePhase.TURN) {
            state.setPhase(GamePhase.RIVER);
            if (deck != null) state.addCommunityCard(deck.dealOne());
        }

        state.setFirstActingSeatIndexThisStreet(firstActing >= 0 ? firstActing : order.get(0));
        state.setActingSeatIndex(firstActing >= 0 ? firstActing : order.get(0));
    }

    private void runShowdown(Table table) {
        List<ParticipantHand> participants = new ArrayList<>();
        for (Seat seat : table.getSeats()) {
            if (seat.isEmpty()) continue;
            Player p = seat.getPlayer();
            if (p.isFolded()) continue;
            participants.add(ParticipantHand.builder()
                    .seatIndex(seat.getSeatIndex())
                    .playerId(p.getId())
                    .holeCards(p.getHoleCards())
                    .build());
        }

        if (participants.isEmpty()) return;
        if (participants.size() == 1) {
            table.getSeat(participants.get(0).getSeatIndex()).getPlayer()
                    .addToStack(table.getHandState().getPot());
            table.getHandState().setPot(BigDecimal.ZERO);
            table.getHandState().setPhase(GamePhase.WAITING);
            table.setDeck(null);
            return;
        }

        List<SeatHandResult> results = winnerResolver.evaluateWinners(participants, table.getHandState().getCommunityCards());
        BigDecimal pot = table.getHandState().getPot();
        long winnerCount = results.stream().filter(SeatHandResult::isWinner).count();
        if (winnerCount == 0) winnerCount = 1;
        BigDecimal share = pot.divide(BigDecimal.valueOf(winnerCount), 0, RoundingMode.DOWN);
        BigDecimal remainder = pot.subtract(share.multiply(BigDecimal.valueOf(winnerCount)));

        int paid = 0;
        for (SeatHandResult r : results) {
            if (r.getRank() != 1) continue;
            Table t = table;
            Seat s = t.getSeat(r.getSeatIndex());
            if (s != null && !s.isEmpty()) {
                s.getPlayer().addToStack(share);
                paid++;
            }
        }
        if (remainder.signum() > 0 && paid > 0) {
            table.getSeat(results.get(0).getSeatIndex()).getPlayer().addToStack(remainder);
        }

        table.getHandState().setPot(BigDecimal.ZERO);
        table.getHandState().setPhase(GamePhase.WAITING);
        table.setDeck(null);
    }

    private void awardPotToLastStanding(Table table) {
        for (Seat seat : table.getSeats()) {
            if (seat.isEmpty()) continue;
            Player p = seat.getPlayer();
            if (p.isFolded()) continue;
            p.addToStack(table.getHandState().getPot());
            break;
        }
        table.getHandState().setPot(BigDecimal.ZERO);
        table.getHandState().setPhase(GamePhase.WAITING);
        table.setDeck(null);
    }
}
