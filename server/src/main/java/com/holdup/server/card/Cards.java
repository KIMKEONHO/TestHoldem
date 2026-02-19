package com.holdup.server.card;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 카드 목록 직렬화/역직렬화. (클라이언트·API 교환용)
 * 형식: "As Kh Tc" 처럼 공백 구분 코드 문자열.
 */
public final class Cards {

    private Cards() {}

    /** 한 장 코드: 끗 1글자 + 문양 1글자. 예: As, Kh, 2d */
    public static String toCode(Card card) {
        return card == null ? "" : card.toString();
    }

    public static Card fromCode(String code) {
        return Card.of(code);
    }

    /** 여러 장을 공백 구분 문자열로. 예: "As Kh 2c" */
    public static String toCodeList(List<Card> cards) {
        if (cards == null || cards.isEmpty()) return "";
        return cards.stream().map(Card::toString).collect(Collectors.joining(" "));
    }

    /** 공백 구분 문자열을 카드 목록으로. */
    public static List<Card> fromCodeList(String codeList) {
        if (codeList == null || codeList.isBlank()) return List.of();
        List<Card> out = new ArrayList<>();
        for (String part : codeList.trim().split("\\s+")) {
            Card c = Card.of(part.trim());
            if (c != null) out.add(c);
        }
        return out;
    }
}
