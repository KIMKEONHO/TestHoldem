package com.holdup.server.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "아이디를 입력해 주세요.")
    private String username;
    @NotBlank(message = "비밀번호를 입력해 주세요.")
    private String password;
}
