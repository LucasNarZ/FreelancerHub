package com.lucasnarloch.freelancerhub.domain.user.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequestDto(

        @NotBlank
        @Size(max = 255)
        String name,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 8, max = 72)
        String password

) {
}