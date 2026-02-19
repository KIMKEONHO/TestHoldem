package com.holdup.server.action.dto;

import com.holdup.server.action.GameActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 서버 → 클라이언트: 액션 처리 결과 (브로드캐스트용).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionResult {

    private boolean success;
    private String message;
    private GameActionType actionType;
    private String playerId;
    private String tableId;
    private Integer seatIndex;

    /** 적용된 금액 (콜/베팅/레이즈/올인). */
    private BigDecimal amount;

    /** 추가 메타데이터 (핸 상태, 포트 등). */
    private Map<String, Object> payload;
}
