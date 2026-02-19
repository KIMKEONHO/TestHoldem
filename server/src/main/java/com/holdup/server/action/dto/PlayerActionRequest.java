package com.holdup.server.action.dto;

import com.holdup.server.action.GameActionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 클라이언트 → 서버: 플레이어가 수행하려는 액션 요청.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerActionRequest {

    @NotNull
    private GameActionType actionType;

    /** BET, RAISE, ALL_IN 시 금액 (칩 단위). */
    private BigDecimal amount;

    /** JOIN_TABLE 등에서 테이블/방 식별자. */
    private String tableId;

    /** 선택: 시트 번호 등. */
    private Integer seatIndex;

    /** Principal 없을 때 클라이언트가 보내는 플레이어 식별자(닉네임). */
    private String playerId;
}
