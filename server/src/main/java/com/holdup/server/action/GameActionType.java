package com.holdup.server.action;

/**
 * 홀덤 게임에서 플레이어가 수행할 수 있는 액션 타입.
 */
public enum GameActionType {

    // 테이블/방 관련
    JOIN_TABLE,   // 테이블 입장
    LEAVE_TABLE,  // 테이블 퇴장
    SIT,          // 자리 앉기
    SIT_OUT,      // 자리에서 일어남 (다음 핸드부터 미참여)
    READY,        // 준비 완료 (다음 게임 참여 의사)

    // 베팅 액션
    FOLD,         // 폴드
    CHECK,        // 체크 (베팅 없음, 가능할 때만)
    CALL,         // 콜 (현재 최고 베팅액에 맞춤)
    BET,          // 베팅 (첫 베팅 또는 액션)
    RAISE,        // 레이즈 (기존 베팅보다 더 베팅)
    ALL_IN,       // 올인

    // 시스템
    START_HAND,   // 새 핸드 시작 (WAITING 상태에서만)
    TIMEOUT,      // 타임아웃 (자동 폴드 등)
    CHAT,         // 채팅 (선택 구현)
    ;
}
