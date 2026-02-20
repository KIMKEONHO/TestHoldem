package com.holdup.server.auth;

public record UserAccount(
        String username,
        String passwordHash,
        String displayName
) {
}
