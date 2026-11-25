CREATE TABLE identity_users (
    id UUID PRIMARY KEY,
    normalized_email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE identity_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES identity_users(id),
    grade_id VARCHAR(80),
    grade_label VARCHAR(120),
    cycle VARCHAR(40),
    locale VARCHAR(10) NOT NULL DEFAULT 'fr',
    avatar_url VARCHAR(2048),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE identity_user_roles (
    user_id UUID NOT NULL REFERENCES identity_users(id),
    role VARCHAR(40) NOT NULL,
    PRIMARY KEY (user_id, role)
);

CREATE TABLE refresh_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES identity_users(id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    replaced_by_hash VARCHAR(64)
);

CREATE TABLE token_records (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES identity_users(id),
    access_token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_refresh_sessions_user_status ON refresh_sessions(user_id, status);
CREATE INDEX idx_token_records_user ON token_records(user_id);
