CREATE TABLE service_types (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(1000),
    minimum_duration_minutes INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_service_type_min_duration CHECK (minimum_duration_minutes BETWEEN 30 AND 1440)
);

INSERT INTO service_types(code, name, description, minimum_duration_minutes) VALUES
    ('STANDARD', 'Limpieza estándar', 'Mantenimiento habitual de la vivienda.', 60),
    ('DEEP_CLEAN', 'Limpieza a fondo', 'Limpieza intensiva de cocina, baños y superficies.', 120),
    ('MOVE_OUT', 'Fin de alquiler', 'Limpieza profunda para entrega o cambio de vivienda.', 120);

CREATE TABLE cleaner_service_offerings (
    id BIGSERIAL PRIMARY KEY,
    cleaner_id BIGINT NOT NULL REFERENCES user_accounts(id),
    service_type_id BIGINT NOT NULL REFERENCES service_types(id),
    hourly_rate_cents BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_cleaner_service_offering UNIQUE(cleaner_id, service_type_id),
    CONSTRAINT chk_cleaner_service_rate CHECK (hourly_rate_cents > 0)
);
CREATE INDEX idx_cleaner_service_offering_service ON cleaner_service_offerings(service_type_id, active);

CREATE TABLE cleaner_service_areas (
    id BIGSERIAL PRIMARY KEY,
    cleaner_id BIGINT NOT NULL REFERENCES user_accounts(id),
    country_code VARCHAR(2) NOT NULL,
    postal_code_prefix VARCHAR(16) NOT NULL,
    CONSTRAINT uk_cleaner_service_area UNIQUE(cleaner_id, country_code, postal_code_prefix)
);
CREATE INDEX idx_cleaner_service_area_lookup ON cleaner_service_areas(country_code, postal_code_prefix);

CREATE TABLE user_addresses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES user_accounts(id),
    label VARCHAR(80) NOT NULL,
    line1 VARCHAR(255) NOT NULL,
    line2 VARCHAR(255),
    postal_code VARCHAR(32) NOT NULL,
    city VARCHAR(160) NOT NULL,
    region VARCHAR(160),
    country_code VARCHAR(2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_user_addresses_user ON user_addresses(user_id, created_at DESC);

ALTER TABLE jobs ADD COLUMN source VARCHAR(32) NOT NULL DEFAULT 'OPEN_MARKET';
ALTER TABLE reservations ADD COLUMN service_type_id BIGINT REFERENCES service_types(id);
ALTER TABLE reservations ADD COLUMN address_id BIGINT REFERENCES user_addresses(id);
CREATE INDEX idx_reservations_service_type ON reservations(service_type_id);
CREATE INDEX idx_reservations_address ON reservations(address_id);
