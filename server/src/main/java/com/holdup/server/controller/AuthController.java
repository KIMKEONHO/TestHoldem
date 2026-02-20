package com.holdup.server.controller;

import com.holdup.server.auth.InMemoryUserAccountService;
import com.holdup.server.auth.JwtTokenService;
import com.holdup.server.auth.dto.AuthRequest;
import com.holdup.server.auth.dto.AuthResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final InMemoryUserAccountService accountService;
    private final JwtTokenService jwtTokenService;

    public AuthController(InMemoryUserAccountService accountService, JwtTokenService jwtTokenService) {
        this.accountService = accountService;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody AuthRequest request) {
        String displayName = (request.getDisplayName() == null || request.getDisplayName().isBlank())
                ? request.getUsername()
                : request.getDisplayName().trim();
        try {
            var account = accountService.register(request.getUsername().trim(), request.getPassword(), displayName);
            String token = jwtTokenService.createToken(account);
            return ResponseEntity.ok(AuthResponse.builder()
                    .success(true)
                    .message("회원가입 완료")
                    .token(token)
                    .username(account.username())
                    .displayName(account.displayName())
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AuthResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return accountService.authenticate(request.getUsername().trim(), request.getPassword())
                .map(account -> ResponseEntity.ok(AuthResponse.builder()
                        .success(true)
                        .message("로그인 성공")
                        .token(jwtTokenService.createToken(account))
                        .username(account.username())
                        .displayName(account.displayName())
                        .build()))
                .orElseGet(() -> ResponseEntity.status(401).body(AuthResponse.builder()
                        .success(false)
                        .message("아이디 또는 비밀번호가 올바르지 않습니다.")
                        .build()));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtTokenService.TokenPrincipal principal)) {
            return ResponseEntity.status(401).body(AuthResponse.builder().success(false).message("인증 필요").build());
        }
        return ResponseEntity.ok(AuthResponse.builder()
                .success(true)
                .username(principal.username())
                .displayName(principal.displayName())
                .build());
    }
}
