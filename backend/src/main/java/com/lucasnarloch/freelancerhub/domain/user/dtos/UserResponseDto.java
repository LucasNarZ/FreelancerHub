package com.lucasnarloch.freelancerhub.domain.user.dtos;

import com.lucasnarloch.freelancerhub.domain.user.User;

import java.util.UUID;

public record UserResponseDto(
        UUID id,
        String name,
        String email,
        boolean emailConfirmed,
        boolean googleConnected
) {

    public static UserResponseDto from(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.isEmailConfirmed(),
                user.getGoogleId() != null
        );
    }
}