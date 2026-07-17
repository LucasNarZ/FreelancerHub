package com.lucasnarloch.freelancerhub.domain.auth.dtos;

import jakarta.validation.constraints.NotBlank;

public record LoginResponseDto(
        @NotBlank
        String token
) {
}
