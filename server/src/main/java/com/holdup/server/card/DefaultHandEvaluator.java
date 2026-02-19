package com.holdup.server.card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * HandEvaluator 기본 구현.
 * 5장 평가: 플러시/스트레이트/페어 등 판별 후 HandRank + keyRanks 생성.
 * 7장 평가: 7C5 조합 중 최선을 선택.
 */
public class DefaultHandEvaluator implements HandEvaluator {

    @Override
    public HandEvaluation evaluateFive(List<Card> fiveCards) {
        if (fiveCards == null || fiveCards.size() != 5) {
            return HandEvaluation.of(HandRank.HIGH_CARD, highCardRanks(fiveCards));
        }
        List<Card> sorted = new ArrayList<>(fiveCards);
        sorted.sort(Card.BY_STRENGTH.reversed());

        boolean flush = isFlush(sorted);
        boolean straight = isStraight(sorted);
        List<Rank> ranks = sorted.stream().map(Card::getRank).collect(Collectors.toList());
        int[] count = rankCounts(ranks);

        if (flush && straight) {
            Rank top = straightTop(sorted);
            if (top == Rank.ACE) return HandEvaluation.of(HandRank.ROYAL_FLUSH, List.of(Rank.ACE));
            return HandEvaluation.of(HandRank.STRAIGHT_FLUSH, List.of(top));
        }
        if (count[4] == 1) return HandEvaluation.of(HandRank.FOUR_OF_A_KIND, keyRanksForQuads(ranks));
        if (count[3] >= 1 && count[2] >= 1) return HandEvaluation.of(HandRank.FULL_HOUSE, keyRanksForFullHouse(ranks));
        if (flush) return HandEvaluation.of(HandRank.FLUSH, ranks);
        if (straight) return HandEvaluation.of(HandRank.STRAIGHT, List.of(straightTop(sorted)));
        if (count[3] >= 1) return HandEvaluation.of(HandRank.THREE_OF_A_KIND, keyRanksForTrips(ranks));
        if (count[2] >= 2) return HandEvaluation.of(HandRank.TWO_PAIR, keyRanksForTwoPair(ranks));
        if (count[2] >= 1) return HandEvaluation.of(HandRank.ONE_PAIR, keyRanksForOnePair(ranks));
        return HandEvaluation.of(HandRank.HIGH_CARD, ranks);
    }

    @Override
    public HandEvaluation evaluateSeven(List<Card> sevenCards) {
        if (sevenCards == null || sevenCards.size() != 7) {
            return HandEvaluation.of(HandRank.HIGH_CARD, highCardRanks(sevenCards));
        }
        HandEvaluation best = null;
        List<List<Card>> combinations = combinations(sevenCards, 5);
        for (List<Card> five : combinations) {
            HandEvaluation e = evaluateFive(five);
            if (best == null || compare(e, best) > 0) best = e;
        }
        return best != null ? best : HandEvaluation.of(HandRank.HIGH_CARD, highCardRanks(sevenCards));
    }

    @Override
    public int compare(HandEvaluation a, HandEvaluation b) {
        int c = Integer.compare(a.getHandRank().getStrength(), b.getHandRank().getStrength());
        if (c != 0) return c;
        List<Rank> ka = a.getKeyRanks();
        List<Rank> kb = b.getKeyRanks();
        for (int i = 0; i < Math.min(ka.size(), kb.size()); i++) {
            int r = Integer.compare(ka.get(i).getStrength(), kb.get(i).getStrength());
            if (r != 0) return r;
        }
        return Integer.compare(ka.size(), kb.size());
    }

    private static List<Rank> highCardRanks(List<Card> cards) {
        if (cards == null || cards.isEmpty()) return List.of();
        return cards.stream()
                .map(Card::getRank)
                .sorted(Comparator.comparingInt(Rank::getStrength).reversed())
                .limit(5)
                .toList();
    }

    private static boolean isFlush(List<Card> cards) {
        if (cards.size() < 5) return false;
        Suit first = cards.get(0).getSuit();
        return cards.stream().allMatch(c -> c.getSuit() == first);
    }

    private static boolean isStraight(List<Card> cards) {
        if (cards.size() != 5) return false;
        List<Integer> strengths = cards.stream()
                .map(c -> c.getRank().getStrength())
                .sorted()
                .distinct()
                .toList();
        if (strengths.size() < 5) return false;
        // A-2-3-4-5 (휠)
        if (strengths.contains(14) && strengths.contains(2) && strengths.contains(3) && strengths.contains(4) && strengths.contains(5)) {
            return true;
        }
        int min = strengths.get(0);
        return strengths.get(4) - min == 4;
    }

    private static Rank straightTop(List<Card> sorted) {
        List<Integer> s = sorted.stream().map(c -> c.getRank().getStrength()).sorted().toList();
        if (s.contains(14) && s.contains(2)) return Rank.FIVE; // 휠: top = 5
        return sorted.get(0).getRank();
    }

    /** [4장 같은 것 개수, 3장 같은 것 개수, 2장 같은 것 개수] */
    private static int[] rankCounts(List<Rank> ranks) {
        int[] count = new int[5];
        for (Rank r : Rank.values()) {
            long c = ranks.stream().filter(x -> x == r).count();
            if (c >= 2) count[(int) c]++;
        }
        return count;
    }

    private static List<Rank> keyRanksForQuads(List<Rank> ranks) {
        Rank quad = ranks.stream().filter(r -> ranks.stream().filter(x -> x == r).count() == 4).findFirst().orElse(null);
        Rank kicker = ranks.stream().filter(r -> r != quad).max(Comparator.comparingInt(Rank::getStrength)).orElse(null);
        return List.of(quad, kicker);
    }

    private static List<Rank> keyRanksForFullHouse(List<Rank> ranks) {
        Rank trips = ranks.stream().filter(r -> ranks.stream().filter(x -> x == r).count() >= 3).max(Comparator.comparingInt(Rank::getStrength)).orElse(null);
        Rank pair = ranks.stream().filter(r -> r != trips && ranks.stream().filter(x -> x == r).count() >= 2).max(Comparator.comparingInt(Rank::getStrength)).orElse(null);
        return List.of(trips, pair);
    }

    private static List<Rank> keyRanksForTrips(List<Rank> ranks) {
        Rank trips = ranks.stream().filter(r -> ranks.stream().filter(x -> x == r).count() >= 3).findFirst().orElse(null);
        List<Rank> kickers = ranks.stream().filter(r -> r != trips).sorted(Comparator.comparingInt(Rank::getStrength).reversed()).limit(2).toList();
        List<Rank> out = new ArrayList<>(List.of(trips));
        out.addAll(kickers);
        return out;
    }

    private static List<Rank> keyRanksForTwoPair(List<Rank> ranks) {
        List<Rank> pairs = new ArrayList<>();
        Rank kicker = null;
        for (Rank r : Rank.values()) {
            long c = ranks.stream().filter(x -> x == r).count();
            if (c >= 2) pairs.add(r);
            else if (c == 1) kicker = (kicker == null || r.getStrength() > kicker.getStrength()) ? r : kicker;
        }
        pairs.sort(Comparator.comparingInt(Rank::getStrength).reversed());
        List<Rank> out = new ArrayList<>(pairs.size() >= 2 ? pairs.subList(0, 2) : pairs);
        if (kicker != null) out.add(kicker);
        return out;
    }

    private static List<Rank> keyRanksForOnePair(List<Rank> ranks) {
        Rank pair = ranks.stream().filter(r -> ranks.stream().filter(x -> x == r).count() >= 2).findFirst().orElse(null);
        List<Rank> kickers = ranks.stream().filter(r -> r != pair).sorted(Comparator.comparingInt(Rank::getStrength).reversed()).limit(3).toList();
        List<Rank> out = new ArrayList<>(List.of(pair));
        out.addAll(kickers);
        return out;
    }

    private static List<List<Card>> combinations(List<Card> list, int k) {
        List<List<Card>> result = new ArrayList<>();
        combine(list, 0, k, new ArrayList<>(), result);
        return result;
    }

    private static void combine(List<Card> list, int start, int k, List<Card> current, List<List<Card>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (int i = start; i < list.size(); i++) {
            current.add(list.get(i));
            combine(list, i + 1, k, current, result);
            current.remove(current.size() - 1);
        }
    }
}
