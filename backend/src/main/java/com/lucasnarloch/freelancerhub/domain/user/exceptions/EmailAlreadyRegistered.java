package com.lucasnarloch.freelancerhub.domain.user.exceptions;

public class EmailAlreadyRegistered extends RuntimeException {
    public EmailAlreadyRegistered() {
        super("Email already registered");
    }
}
