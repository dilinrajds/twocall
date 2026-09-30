-- =============================================================================
-- V1: Initial Database Schema for Two-Partner Private Messenger
-- Strict Isolation: Two devices per pair, UUID keys, Indexed Foreign Keys
-- =============================================================================

CREATE TABLE IF NOT EXISTS pairs (
    id UUID PRIMARY KEY,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS devices (
    id UUID PRIMARY KEY,
    pair_id UUID NOT NULL REFERENCES pairs(id) ON DELETE CASCADE,
    device_fingerprint VARCHAR(128) NOT NULL,
    public_identity_key TEXT,
    device_label VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_active_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_device_pair_fingerprint UNIQUE(pair_id, device_fingerprint)
);
CREATE INDEX IF NOT EXISTS idx_devices_pair_id ON devices(pair_id);

CREATE TABLE IF NOT EXISTS pairing_codes (
    id UUID PRIMARY KEY,
    code_hash VARCHAR(64) NOT NULL UNIQUE,
    pair_id UUID NOT NULL REFERENCES pairs(id) ON DELETE CASCADE,
    creator_device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    attempts_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_pairing_codes_hash ON pairing_codes(code_hash);
CREATE INDEX IF NOT EXISTS idx_pairing_codes_pair_id ON pairing_codes(pair_id);

CREATE TABLE IF NOT EXISTS device_sessions (
    id UUID PRIMARY KEY,
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    refresh_token_hash VARCHAR(64) NOT NULL UNIQUE,
    access_token_id VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_device_sessions_device_id ON device_sessions(device_id);
CREATE INDEX IF NOT EXISTS idx_device_sessions_refresh_hash ON device_sessions(refresh_token_hash);

CREATE TABLE IF NOT EXISTS attachments (
    id UUID PRIMARY KEY,
    pair_id UUID NOT NULL REFERENCES pairs(id) ON DELETE CASCADE,
    uploader_device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    sha256_hash VARCHAR(64) NOT NULL,
    encrypted_key_material TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_attachments_pair_id ON attachments(pair_id);

CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY,
    pair_id UUID NOT NULL REFERENCES pairs(id) ON DELETE CASCADE,
    sender_device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    client_message_id VARCHAR(64) NOT NULL,
    ciphertext_payload TEXT NOT NULL,
    iv VARCHAR(64) NOT NULL,
    ephemeral_public_key TEXT,
    message_type VARCHAR(32) NOT NULL,
    reply_to_message_id UUID REFERENCES messages(id) ON DELETE SET NULL,
    media_attachment_id UUID REFERENCES attachments(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SENT',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    edited_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_message_client_id_pair UNIQUE (pair_id, client_message_id)
);
CREATE INDEX IF NOT EXISTS idx_messages_pair_created ON messages(pair_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_messages_sender_device ON messages(sender_device_id);

CREATE TABLE IF NOT EXISTS message_reactions (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    emoji VARCHAR(16) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_message_reaction_device UNIQUE (message_id, device_id)
);
CREATE INDEX IF NOT EXISTS idx_message_reactions_message ON message_reactions(message_id);

CREATE TABLE IF NOT EXISTS delivery_receipts (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_receipt_message_device UNIQUE (message_id, device_id)
);
CREATE INDEX IF NOT EXISTS idx_delivery_receipts_message ON delivery_receipts(message_id);

CREATE TABLE IF NOT EXISTS push_tokens (
    id UUID PRIMARY KEY,
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    fcm_token TEXT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_device_push_token UNIQUE (device_id)
);

CREATE TABLE IF NOT EXISTS call_sessions (
    id UUID PRIMARY KEY,
    pair_id UUID NOT NULL REFERENCES pairs(id) ON DELETE CASCADE,
    caller_device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    call_type VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_call_sessions_pair ON call_sessions(pair_id);
