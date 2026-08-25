ALTER TABLE user_accounts
    ADD COLUMN blocked_at TIMESTAMPTZ,
    ADD COLUMN blocked_reason VARCHAR(1000),
    ADD COLUMN blocked_by_user_id BIGINT REFERENCES user_accounts(id);
CREATE INDEX idx_user_accounts_blocked ON user_accounts(blocked_at) WHERE blocked_at IS NOT NULL;

ALTER TABLE reviews
    ADD COLUMN moderation_status VARCHAR(32) NOT NULL DEFAULT 'VISIBLE',
    ADD COLUMN moderated_at TIMESTAMPTZ,
    ADD COLUMN moderated_by_user_id BIGINT REFERENCES user_accounts(id),
    ADD COLUMN moderation_reason VARCHAR(1000);
CREATE INDEX idx_reviews_moderation_status ON reviews(moderation_status);

CREATE TABLE admin_audit_log (
    id BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT NOT NULL REFERENCES user_accounts(id),
    action VARCHAR(96) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id VARCHAR(160) NOT NULL,
    idempotency_key VARCHAR(255),
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_admin_audit_idempotency UNIQUE(idempotency_key)
);
CREATE INDEX idx_admin_audit_target ON admin_audit_log(target_type, target_id, created_at DESC);
CREATE INDEX idx_admin_audit_actor ON admin_audit_log(actor_user_id, created_at DESC);

CREATE TABLE cleaner_connect_accounts (
    id BIGSERIAL PRIMARY KEY,
    cleaner_id BIGINT NOT NULL REFERENCES user_accounts(id),
    stripe_account_id VARCHAR(255) NOT NULL,
    country_code VARCHAR(2),
    onboarding_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    details_submitted BOOLEAN NOT NULL DEFAULT FALSE,
    charges_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    payouts_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    requirements_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_cleaner_connect_cleaner UNIQUE(cleaner_id),
    CONSTRAINT uk_cleaner_connect_stripe_account UNIQUE(stripe_account_id)
);
CREATE INDEX idx_cleaner_connect_status ON cleaner_connect_accounts(onboarding_status, charges_enabled, payouts_enabled);

ALTER TABLE payments
    ADD COLUMN platform_fee_cents BIGINT,
    ADD COLUMN provider_amount_cents BIGINT,
    ADD COLUMN stripe_destination_account VARCHAR(255),
    ADD COLUMN stripe_charge_id VARCHAR(255),
    ADD COLUMN stripe_transfer_id VARCHAR(255),
    ADD COLUMN stripe_application_fee_id VARCHAR(255),
    ADD CONSTRAINT chk_payment_platform_fee_nonnegative CHECK (platform_fee_cents IS NULL OR platform_fee_cents >= 0),
    ADD CONSTRAINT chk_payment_provider_amount_nonnegative CHECK (provider_amount_cents IS NULL OR provider_amount_cents >= 0);
CREATE INDEX idx_payments_destination_account ON payments(stripe_destination_account);
CREATE UNIQUE INDEX uk_payments_stripe_charge ON payments(stripe_charge_id) WHERE stripe_charge_id IS NOT NULL;
CREATE UNIQUE INDEX uk_payments_stripe_transfer ON payments(stripe_transfer_id) WHERE stripe_transfer_id IS NOT NULL;

CREATE TABLE marketplace_settlements (
    id BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT NOT NULL REFERENCES reservations(id),
    payment_id BIGINT NOT NULL REFERENCES payments(id),
    cleaner_id BIGINT NOT NULL REFERENCES user_accounts(id),
    connect_account_id BIGINT NOT NULL REFERENCES cleaner_connect_accounts(id),
    gross_cents BIGINT NOT NULL,
    platform_fee_cents BIGINT NOT NULL,
    provider_amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    stripe_payment_intent_id VARCHAR(255),
    stripe_charge_id VARCHAR(255),
    stripe_transfer_id VARCHAR(255),
    stripe_application_fee_id VARCHAR(255),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_marketplace_settlement_reservation UNIQUE(reservation_id),
    CONSTRAINT uk_marketplace_settlement_payment UNIQUE(payment_id),
    CONSTRAINT chk_settlement_amounts CHECK (
        gross_cents > 0 AND platform_fee_cents >= 0 AND provider_amount_cents >= 0
        AND platform_fee_cents + provider_amount_cents = gross_cents
    )
);
CREATE INDEX idx_marketplace_settlement_cleaner ON marketplace_settlements(cleaner_id, created_at DESC);
CREATE INDEX idx_marketplace_settlement_status ON marketplace_settlements(status, updated_at);

CREATE TABLE connect_events (
    id BIGSERIAL PRIMARY KEY,
    stripe_event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(160) NOT NULL,
    stripe_account_id VARCHAR(255),
    event_created_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PROCESSING',
    attempts INTEGER NOT NULL DEFAULT 1,
    claimed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ,
    failure_reason VARCHAR(1000),
    CONSTRAINT uk_connect_events_stripe_event UNIQUE(stripe_event_id)
);
CREATE INDEX idx_connect_events_status ON connect_events(status, claimed_at);

CREATE TABLE stripe_payouts (
    id BIGSERIAL PRIMARY KEY,
    connect_account_id BIGINT NOT NULL REFERENCES cleaner_connect_accounts(id),
    stripe_payout_id VARCHAR(255) NOT NULL,
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    arrival_at TIMESTAMPTZ,
    failure_code VARCHAR(160),
    raw_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_stripe_payout_id UNIQUE(stripe_payout_id)
);
CREATE INDEX idx_stripe_payout_account ON stripe_payouts(connect_account_id, created_at DESC);
CREATE INDEX idx_stripe_payout_status ON stripe_payouts(status, updated_at);
