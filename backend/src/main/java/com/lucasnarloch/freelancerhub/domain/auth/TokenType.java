package com.lucasnarloch.freelancerhub.domain.auth;

enum TokenType {
    ACCESS("access"),
    REFRESH("refresh");

    private final String value;

    TokenType(String value) {
        this.value = value;
    }

    String value() {
        return value;
    }
}
