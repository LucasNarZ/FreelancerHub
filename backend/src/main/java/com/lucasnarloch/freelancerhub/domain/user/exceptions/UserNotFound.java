package com.lucasnarloch.freelancerhub.domain.user.exceptions;

public class UserNotFound extends RuntimeException {
    public UserNotFound() {
        super("User not found");
    }
}
