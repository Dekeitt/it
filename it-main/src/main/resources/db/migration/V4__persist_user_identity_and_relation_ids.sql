CREATE TABLE user_accounts (
    id BIGSERIAL PRIMARY KEY,
    issuer VARCHAR(512) NOT NULL,
    subject VARCHAR(512) NOT NULL,
    email VARCHAR(320),
    display_name VARCHAR(255),
    roles VARCHAR(1000) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_accounts_issuer_subject UNIQUE (issuer, subject)
);

INSERT INTO user_accounts (issuer, subject, email, display_name, roles)
SELECT 'legacy:email', LOWER(source.email), MIN(source.email), NULL, ''
FROM (
    SELECT email FROM cleaners WHERE email IS NOT NULL
    UNION ALL SELECT client_email FROM jobs WHERE client_email IS NOT NULL
    UNION ALL SELECT cleaner_email FROM jobs WHERE cleaner_email IS NOT NULL
    UNION ALL SELECT client_email FROM reservations WHERE client_email IS NOT NULL
    UNION ALL SELECT cleaner_email FROM reservations WHERE cleaner_email IS NOT NULL
    UNION ALL SELECT client_email FROM reviews WHERE client_email IS NOT NULL
    UNION ALL SELECT cleaner_email FROM reviews WHERE cleaner_email IS NOT NULL
    UNION ALL SELECT cleaner_email FROM cleaner_availability WHERE cleaner_email IS NOT NULL
) source
GROUP BY LOWER(source.email);

ALTER TABLE cleaners ADD COLUMN user_id BIGINT;
ALTER TABLE jobs ADD COLUMN client_id BIGINT;
ALTER TABLE jobs ADD COLUMN cleaner_id BIGINT;
ALTER TABLE reservations ADD COLUMN client_id BIGINT;
ALTER TABLE reservations ADD COLUMN cleaner_id BIGINT;
ALTER TABLE reviews ADD COLUMN client_id BIGINT;
ALTER TABLE reviews ADD COLUMN cleaner_id BIGINT;
ALTER TABLE cleaner_availability ADD COLUMN cleaner_id BIGINT;

UPDATE cleaners c
SET user_id = (SELECT u.id FROM user_accounts u WHERE u.issuer = 'legacy:email' AND u.subject = LOWER(c.email));
UPDATE jobs j
SET client_id = (SELECT u.id FROM user_accounts u WHERE u.issuer = 'legacy:email' AND u.subject = LOWER(j.client_email));
UPDATE jobs j
SET cleaner_id = (SELECT u.id FROM user_accounts u WHERE u.issuer = 'legacy:email' AND u.subject = LOWER(j.cleaner_email))
WHERE j.cleaner_email IS NOT NULL;
UPDATE reservations r
SET client_id = (SELECT u.id FROM user_accounts u WHERE u.issuer = 'legacy:email' AND u.subject = LOWER(r.client_email)),
    cleaner_id = (SELECT u.id FROM user_accounts u WHERE u.issuer = 'legacy:email' AND u.subject = LOWER(r.cleaner_email));
UPDATE reviews r
SET client_id = (SELECT u.id FROM user_accounts u WHERE u.issuer = 'legacy:email' AND u.subject = LOWER(r.client_email)),
    cleaner_id = (SELECT u.id FROM user_accounts u WHERE u.issuer = 'legacy:email' AND u.subject = LOWER(r.cleaner_email));
UPDATE cleaner_availability a
SET cleaner_id = (SELECT u.id FROM user_accounts u WHERE u.issuer = 'legacy:email' AND u.subject = LOWER(a.cleaner_email));

ALTER TABLE cleaners ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE jobs ALTER COLUMN client_id SET NOT NULL;
ALTER TABLE reservations ALTER COLUMN client_id SET NOT NULL;
ALTER TABLE reservations ALTER COLUMN cleaner_id SET NOT NULL;
ALTER TABLE reviews ALTER COLUMN client_id SET NOT NULL;
ALTER TABLE reviews ALTER COLUMN cleaner_id SET NOT NULL;
ALTER TABLE cleaner_availability ALTER COLUMN cleaner_id SET NOT NULL;

ALTER TABLE cleaners ADD CONSTRAINT fk_cleaners_user FOREIGN KEY (user_id) REFERENCES user_accounts(id);
ALTER TABLE jobs ADD CONSTRAINT fk_jobs_client_user FOREIGN KEY (client_id) REFERENCES user_accounts(id);
ALTER TABLE jobs ADD CONSTRAINT fk_jobs_cleaner_user FOREIGN KEY (cleaner_id) REFERENCES user_accounts(id);
ALTER TABLE reservations ADD CONSTRAINT fk_reservations_client_user FOREIGN KEY (client_id) REFERENCES user_accounts(id);
ALTER TABLE reservations ADD CONSTRAINT fk_reservations_cleaner_user FOREIGN KEY (cleaner_id) REFERENCES user_accounts(id);
ALTER TABLE reviews ADD CONSTRAINT fk_reviews_client_user FOREIGN KEY (client_id) REFERENCES user_accounts(id);
ALTER TABLE reviews ADD CONSTRAINT fk_reviews_cleaner_user FOREIGN KEY (cleaner_id) REFERENCES user_accounts(id);
ALTER TABLE cleaner_availability ADD CONSTRAINT fk_cleaner_availability_user FOREIGN KEY (cleaner_id) REFERENCES user_accounts(id);

ALTER TABLE cleaner_availability DROP CONSTRAINT fk_cleaner_availability_cleaner;
ALTER TABLE cleaner_availability DROP CONSTRAINT uk_cleaner_availability_slot;
ALTER TABLE reservations DROP CONSTRAINT reservations_no_cleaner_overlap;

ALTER TABLE reservations
    ADD CONSTRAINT reservations_no_cleaner_overlap
    EXCLUDE USING gist (
        cleaner_id WITH =,
        tstzrange(start_at, end_at, '[)') WITH &&
    )
    WHERE (upper(status) <> 'CANCELLED');

CREATE UNIQUE INDEX uk_cleaner_availability_slot_user
    ON cleaner_availability(cleaner_id, day_of_week, start_time, end_time, zone_id);
CREATE INDEX idx_cleaner_availability_user_day
    ON cleaner_availability(cleaner_id, day_of_week, start_time);
CREATE INDEX idx_jobs_client_id ON jobs(client_id);
CREATE INDEX idx_jobs_cleaner_id_status ON jobs(cleaner_id, status);
CREATE INDEX idx_reservations_client_id ON reservations(client_id);
CREATE INDEX idx_reservations_cleaner_id_start ON reservations(cleaner_id, start_at);
CREATE INDEX idx_reviews_cleaner_id ON reviews(cleaner_id);
