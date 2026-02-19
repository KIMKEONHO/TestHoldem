package com.holdup.server.gamestate;

/**
 * 한 핸드(Hand) 내 게임 단계. (스트릿)
 */
public enum GamePhase {

    /** 대기(새 핸드 시작 전). */
    WAITING,

    /** 프리플랍 (홀카드만 있음). */
    PREFLOP,

    /** 플랍 (커뮤니티 3장 오픈). */
    FLOP,

    /** 턴 (커뮤니티 4장). */
    TURN,

    /** 리버 (커뮤니티 5장). */
    RIVER,

    /** 쇼다운 (승자 판정). */
    SHOWDOWN,
    ;
}
