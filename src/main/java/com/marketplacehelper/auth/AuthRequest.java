package com.marketplacehelper.auth;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
        @NotBlank(message = "Логин обязателен") String username,
        @NotBlank(message = "Пароль обязателен") String password
) {
}
