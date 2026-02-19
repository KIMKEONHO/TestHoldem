package com.holdup.server.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * WebSocket 게임 메시지 핸들러.
 * 클라이언트는 /app/... 로 메시지를 보내고, /topic/... 구독으로 응답을 받습니다.
 */
@Controller
public class GameController {

    @MessageMapping("/hello")
    @SendTo("/topic/greetings")
    public Map<String, String> hello(Map<String, String> payload) {
        return Map.of(
                "message", "Hello, " + payload.getOrDefault("name", "Guest") + "!"
        );
    }
}
