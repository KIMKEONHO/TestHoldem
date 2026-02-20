package com.holdup.server.controller;

import com.holdup.server.auth.JwtTokenService;
import com.holdup.server.auth.UserAccountService;
import com.holdup.server.auth.dto.AuthRequest;
import com.holdup.server.auth.dto.AuthResponse;
import com.holdup.server.auth.dto.FindIdRequest;
import com.holdup.server.auth.dto.ResetPasswordRequest;
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

    private final UserAccountService accountService;
    private final JwtTokenService jwtTokenService;

    public AuthController(UserAccountService accountService, JwtTokenService jwtTokenService) {
        this.accountService = accountService;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody AuthRequest request) {
        String displayName = (request.getDisplayName() == null || request.getDisplayName().isBlank())
                ? request.getUsername()
                : request.getDisplayName().trim();
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(AuthResponse.builder().success(false).message("이메일은 필수입니다.").build());
        }

        try {
            var account = accountService.register(
                    request.getUsername().trim(),
                    request.getPassword(),
                    displayName,
                    request.getEmail().trim().toLowerCase()
            );
            String token = jwtTokenService.createToken(account);
            return ResponseEntity.ok(AuthResponse.builder()
                    .success(true)
                    .message("회원가입 완료")
                    .token(token)
                    .username(account.username())
                    .displayName(account.displayName())
                    .email(account.email())
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
                        .email(account.email())
                        .build()))
                .orElseGet(() -> ResponseEntity.status(401).body(AuthResponse.builder()
                        .success(false)
                        .message("아이디 또는 비밀번호가 올바르지 않습니다.")
                        .build()));
    }

    @PostMapping("/find-id")
    public ResponseEntity<AuthResponse> findId(@Valid @RequestBody FindIdRequest request) {
        return accountService.findIdByEmail(request.getEmail().trim().toLowerCase())
                .map(username -> ResponseEntity.ok(AuthResponse.builder()
                        .success(true)
                        .message("가입된 아이디는 " + username + " 입니다.")
                        .username(username)
                        .build()))
                .orElseGet(() -> ResponseEntity.status(404).body(AuthResponse.builder()
                        .success(false)
                        .message("해당 이메일로 등록된 계정을 찾을 수 없습니다.")
                        .build()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        boolean ok = accountService.resetPassword(
                request.getUsername().trim(),
                request.getEmail().trim().toLowerCase(),
                request.getNewPassword()
        );
        if (!ok) {
            return ResponseEntity.badRequest().body(AuthResponse.builder()
                    .success(false)
                    .message("아이디/이메일 정보가 일치하지 않습니다.")
                    .build());
        }
        return ResponseEntity.ok(AuthResponse.builder().success(true).message("비밀번호가 재설정되었습니다.").build());
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtTokenService.TokenPrincipal principal)) {
            return ResponseEntity.status(401).body(AuthResponse.builder().success(false).message("인증 필요").build());
        }
        return accountService.findByUsername(principal.username())
                .map(account -> ResponseEntity.ok(AuthResponse.builder()
                        .success(true)
                        .username(account.username())
                        .displayName(account.displayName())
                        .email(account.email())
                        .build()))
                .orElseGet(() -> ResponseEntity.status(404).body(AuthResponse.builder().success(false).message("사용자 없음").build()));
    }
}
