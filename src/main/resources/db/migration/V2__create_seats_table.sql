-- V2: Create seats table
CREATE TYPE seat_status AS ENUM ('AVAILABLE', 'BOOKED');

CREATE TABLE seats (
    id          BIGSERIAL    PRIMARY KEY,
    show_id     BIGINT       NOT NULL REFERENCES shows (id) ON DELETE CASCADE,
    seat_number VARCHAR(20)  NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',
    version     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_seat_show UNIQUE (show_id, seat_number)
);

CREATE INDEX idx_seats_show_id_status ON seats (show_id, status);

