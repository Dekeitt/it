CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE reservations
    ADD COLUMN end_at TIMESTAMPTZ,
    ADD COLUMN agreed_amount_cents BIGINT,
    ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'eur',
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE reservations
SET end_at = start_at + duration_minutes * INTERVAL '1 minute'
WHERE end_at IS NULL;

UPDATE reservations r
SET agreed_amount_cents = j.price_cents
FROM jobs j
WHERE r.job_id = j.id
  AND r.agreed_amount_cents IS NULL
  AND j.price_cents > 0;

ALTER TABLE reservations
    ALTER COLUMN end_at SET NOT NULL,
    ADD CONSTRAINT chk_reservation_end_after_start CHECK (end_at > start_at),
    ADD CONSTRAINT chk_reservation_agreed_amount CHECK (
        agreed_amount_cents IS NULL OR agreed_amount_cents > 0
    ),
    ADD CONSTRAINT chk_reservation_currency CHECK (currency ~ '^[a-z]{3}$');

ALTER TABLE reservations
    ADD CONSTRAINT reservations_no_cleaner_overlap
    EXCLUDE USING gist (
        cleaner_email WITH =,
        tstzrange(start_at, end_at, '[)') WITH &&
    )
    WHERE (upper(status) <> 'CANCELLED');

ALTER TABLE payments
    ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'eur',
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE payments
    ADD CONSTRAINT uk_payments_reservation UNIQUE (reservation_id),
    ADD CONSTRAINT chk_payment_currency CHECK (currency ~ '^[a-z]{3}$');

ALTER TABLE payment_events
    ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'PROCESSED',
    ADD COLUMN event_created_at TIMESTAMPTZ,
    ADD COLUMN claimed_at TIMESTAMPTZ,
    ADD COLUMN failure_reason VARCHAR(1000);

UPDATE payment_events
SET claimed_at = COALESCE(processed_at, NOW())
WHERE claimed_at IS NULL;

ALTER TABLE payment_events
    ALTER COLUMN claimed_at SET NOT NULL,
    ALTER COLUMN processed_at DROP NOT NULL;

CREATE INDEX idx_payment_events_status_claimed
    ON payment_events(status, claimed_at);
