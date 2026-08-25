CREATE TABLE notification_preferences (
    user_id BIGINT PRIMARY KEY REFERENCES user_accounts(id) ON DELETE CASCADE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE notification_outbox (
    id BIGSERIAL PRIMARY KEY,
    event_key VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(80) NOT NULL,
    recipient_user_id BIGINT NOT NULL REFERENCES user_accounts(id),
    channel VARCHAR(16) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    available_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    claimed_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,
    last_error VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_notification_channel CHECK (channel IN ('EMAIL','WEB_PUSH')),
    CONSTRAINT chk_notification_status CHECK (status IN ('PENDING','PROCESSING','SENT','FAILED','SKIPPED')),
    CONSTRAINT chk_notification_attempts CHECK (attempts >= 0)
);
CREATE INDEX idx_notification_outbox_claim ON notification_outbox(status, available_at, claimed_at, id);
CREATE INDEX idx_notification_outbox_recipient ON notification_outbox(recipient_user_id, created_at DESC);

CREATE TABLE web_push_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES user_accounts(id) ON DELETE CASCADE,
    endpoint VARCHAR(2048) NOT NULL UNIQUE,
    p256dh VARCHAR(255) NOT NULL,
    auth_secret VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_success_at TIMESTAMPTZ,
    disabled_at TIMESTAMPTZ
);
CREATE INDEX idx_web_push_subscriptions_user ON web_push_subscriptions(user_id, disabled_at);
