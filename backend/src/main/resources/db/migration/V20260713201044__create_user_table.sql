CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    email_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    google_id VARCHAR(255),

    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_google_id UNIQUE (google_id)
);