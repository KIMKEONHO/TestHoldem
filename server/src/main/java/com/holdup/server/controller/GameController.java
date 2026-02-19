package com.holdup.server.controller;

import com.holdup.server.action.ActionDispatcher;
import com.holdup.server.action.dto.ActionResult;
import com.holdup.server.action.dto.PlayerActionRequest;
import com.holdup.server.action.dto.TableSnapshot;
import com.holdup.server.service.TableManager;
import com.holdup.server.service.TableSnapshotService;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket 게임 메시지 핸들러.
 * 클라이언트는 /app/... 로 메시지를 보내고, /topic/... 구독으로 응답을 받습니다.
 * 액션 결과는 tableId가 있으면 /topic/table/{tableId}, 없으면 /topic/actions 로 전송.
 */
@Controller
public class GameController {

    private final ActionDispatcher actionDispatcher;
    private final SimpMessagingTemplate messagingTemplate;
    private final TableManager tableManager;
    private final TableSnapshotService tableSnapshotService;

    public GameController(ActionDispatcher actionDispatcher, SimpMessagingTemplate messagingTemplate,
                         TableManager tableManager, TableSnapshotService tableSnapshotService) {
        this.actionDispatcher = actionDispatcher;
        this.messagingTemplate = messagingTemplate;
        this.tableManager = tableManager;
        this.tableSnapshotService = tableSnapshotService;
    }

    @MessageMapping("/hello")
    @SendTo("/topic/greetings")
    public Map<String, String> hello(Map<String, String> payload) {
        return Map.of(
                "message", "Hello, " + payload.getOrDefault("name", "Guest") + "!"
        );
    }

    /**
     * 플레이어 게임 액션: /app/action 요청.
     * tableId가 있으면 /topic/table/{tableId} 로만 전송 (해당 테이블 구독자만 수신).
     */
    @MessageMapping("/action")
    public void action(@Valid PlayerActionRequest request, Principal principal) {
        // 핸드셰이크 시 부여된 연결별 고유 ID (UniqueUserHandshakeHandler)
        String playerId = principal != null && principal.getName() != null
                ? principal.getName()
                : (request.getPlayerId() != null ? request.getPlayerId() : "anonymous");
        ActionResult result = actionDispatcher.dispatch(request, playerId);
        String tableId = result.getTableId();
        if (tableId != null && !tableId.isBlank()) {
            tableManager.getTable(tableId).ifPresent(table -> {
                TableSnapshot snapshot = tableSnapshotService.toSnapshot(table, null);
                Map<String, Object> payload = result.getPayload() != null ? new HashMap<>(result.getPayload()) : new HashMap<>();
                payload.put("tableState", snapshot);
                result.setPayload(payload);
                messagingTemplate.convertAndSend("/topic/table/" + tableId, result);

                // 각 플레이어에게 본인 홀카드가 포함된 스냅샷 전송 (/user/queue/table-state)
                for (var seat : table.getSeats()) {
                    if (!seat.isEmpty()) {
                        String pid = seat.getPlayer().getId();
                        TableSnapshot mySnapshot = tableSnapshotService.toSnapshot(table, pid);
                        Map<String, Object> myPayload = new HashMap<>(payload);
                        myPayload.put("tableState", mySnapshot);
                        ActionResult myResult = ActionResult.builder()
                                .success(result.isSuccess())
                                .message(result.getMessage())
                                .actionType(result.getActionType())
                                .playerId(result.getPlayerId())
                                .tableId(result.getTableId())
                                .seatIndex(result.getSeatIndex())
                                .amount(result.getAmount())
                                .payload(myPayload)
                                .build();
                        messagingTemplate.convertAndSendToUser(pid, "/queue/table-state", myResult);
                    }
                }
            });
        } else {
            messagingTemplate.convertAndSend("/topic/actions", result);
        }
    }
}
