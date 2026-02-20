package com.holdup.server.auth.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthResponse {
    boolean success;
    String message;
    String token;
    String username;
    String displayName;
}
