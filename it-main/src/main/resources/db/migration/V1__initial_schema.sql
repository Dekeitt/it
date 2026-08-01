CREATE TABLE cleaners (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255),
    rating DOUBLE PRECISION,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE jobs (
    id BIGSERIAL PRIMARY KEY,
    client_email VARCHAR(255) NOT NULL,
    title VARCHAR(120),
    cleaner_email VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    description TEXT,
    price_cents BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_jobs_price_non_negative CHECK (price_cents IS NULL OR price_cents >= 0)
);

CREATE TABLE reservations (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL,
    client_email VARCHAR(255) NOT NULL,
    cleaner_email VARCHAR(255) NOT NULL,
    start_at TIMESTAMPTZ NOT NULL,
    duration_minutes INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    payment_intent_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_reservations_job FOREIGN KEY (job_id) REFERENCES jobs(id),
    CONSTRAINT chk_reservation_duration CHECK (duration_minutes BETWEEN 30 AND 1440)
);

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    amount_cents BIGINT NOT NULL,
    stripe_payment_intent_id VARCHAR(255),
    client_secret VARCHAR(255),
    raw_json TEXT,
    status VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_payments_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id),
    CONSTRAINT chk_payment_amount_positive CHECK (amount_cents > 0)
);

CREATE TABLE payment_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(255),
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    cleaner_email VARCHAR(255) NOT NULL,
    client_email VARCHAR(255) NOT NULL,
    rating INTEGER NOT NULL,
    comment VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_jobs_status ON jobs(status);
CREATE INDEX idx_jobs_cleaner_status ON jobs(cleaner_email, status);
CREATE INDEX idx_reservations_cleaner_start ON reservations(cleaner_email, start_at);
CREATE INDEX idx_reservations_client ON reservations(client_email);
CREATE INDEX idx_payments_reservation ON payments(reservation_id);
CREATE UNIQUE INDEX idx_payments_stripe_intent ON payments(stripe_payment_intent_id) WHERE stripe_payment_intent_id IS NOT NULL;
CREATE INDEX idx_reviews_cleaner ON reviews(cleaner_email);
