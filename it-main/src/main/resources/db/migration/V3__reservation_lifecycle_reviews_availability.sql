ALTER TABLE reviews
    ADD COLUMN reservation_id BIGINT;

ALTER TABLE reviews
    ADD CONSTRAINT fk_reviews_reservation
        FOREIGN KEY (reservation_id) REFERENCES reservations(id);

CREATE UNIQUE INDEX uk_reviews_reservation
    ON reviews(reservation_id)
    WHERE reservation_id IS NOT NULL;

CREATE TABLE cleaner_availability (
    id BIGSERIAL PRIMARY KEY,
    cleaner_email VARCHAR(255) NOT NULL,
    day_of_week VARCHAR(9) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    zone_id VARCHAR(64) NOT NULL DEFAULT 'Europe/Madrid',
    CONSTRAINT fk_cleaner_availability_cleaner
        FOREIGN KEY (cleaner_email) REFERENCES cleaners(email),
    CONSTRAINT chk_cleaner_availability_day CHECK (
        day_of_week IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')
    ),
    CONSTRAINT chk_cleaner_availability_time CHECK (start_time < end_time),
    CONSTRAINT uk_cleaner_availability_slot UNIQUE (
        cleaner_email, day_of_week, start_time, end_time, zone_id
    )
);

CREATE INDEX idx_cleaner_availability_email_day
    ON cleaner_availability(cleaner_email, day_of_week, start_time);
