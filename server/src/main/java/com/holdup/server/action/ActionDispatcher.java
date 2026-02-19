package com.holdup.server.action;

import com.holdup.server.action.dto.ActionResult;
import com.holdup.server.action.dto.PlayerActionRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 클라이언트 액션 요청을 해당 타입의 ActionHandler로 라우팅.
 */
@Component
public class ActionDispatcher {

    private final Map<GameActionType, ActionHandler> handlersByType;

    public ActionDispatcher(List<ActionHandler> handlers) {
        this.handlersByType = handlers.stream()
                .collect(Collectors.toMap(ActionHandler::getActionType, Function.identity()));
    }

    /**
     * 요청의 actionType에 맞는 핸들러를 찾아 실행.
     *
     * @param request  클라이언트 요청
     * @param playerId 요청 플레이어 ID
     * @return 처리 결과 (해당 타입 핸들러 없으면 success=false)
     */
    public ActionResult dispatch(PlayerActionRequest request, String playerId) {
        if (request == null || request.getActionType() == null) {
            return ActionResult.builder()
                    .success(false)
                    .message("Invalid request or missing actionType")
                    .build();
        }
        ActionHandler handler = handlersByType.get(request.getActionType());
        if (handler == null) {
            return ActionResult.builder()
                    .success(false)
                    .message("Unsupported action: " + request.getActionType())
                    .actionType(request.getActionType())
                    .build();
        }
        return handler.handle(request, playerId);
    }
}
