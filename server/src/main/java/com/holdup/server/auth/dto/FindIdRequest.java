package com.holdup.server.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FindIdRequest {
    @NotBlank
    @Email
    private String email;
}
