package com.lucasnarloch.freelancerhub.domain.user;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.UUID;

@Getter
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "email_confirmed", nullable = false)
    private boolean emailConfirmed = false;

    @Column(name = "google_id", unique = true)
    private String googleId;

    protected User() {
    }

    public User(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public void confirmEmail() {
        this.emailConfirmed = true;
    }

    public void connectGoogleAccount(String googleId) {
        this.googleId = googleId;
    }

    public void rename(String name) {
        this.name = name;
    }
}
