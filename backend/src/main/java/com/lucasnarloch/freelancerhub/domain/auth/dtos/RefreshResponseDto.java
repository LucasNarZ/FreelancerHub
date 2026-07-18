package com.lucasnarloch.freelancerhub.domain.auth.dtos;

import jakarta.validation.constraints.NotBlank;

public record RefreshResponseDto (
        @NotBlank
        String accessToken,

        @NotBlank
        String refreshToken
) {
}
