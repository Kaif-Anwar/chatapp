-- Baseline schema (IF NOT EXISTS for safe deploy on existing databases)

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    avatar_url VARCHAR(512),
    created_at DATETIME(6),
    updated_at DATETIME(6)
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(512) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expiry_date DATETIME(6) NOT NULL,
    revoked BIT(1) NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    type VARCHAR(20) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(6)
);

CREATE TABLE IF NOT EXISTS conversation_participants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    joined_at DATETIME(6),
    last_read_at DATETIME(6),
    is_admin BIT(1) NOT NULL DEFAULT 0,
    UNIQUE KEY uk_conv_user (conversation_id, user_id)
);

CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    status VARCHAR(20) NOT NULL DEFAULT 'SENT',
    created_at DATETIME(6),
    edited_at DATETIME(6),
    is_deleted BIT(1) NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT,
    is_read BIT(1) NOT NULL DEFAULT 0,
    ref_id BIGINT,
    created_at DATETIME(6)
);
