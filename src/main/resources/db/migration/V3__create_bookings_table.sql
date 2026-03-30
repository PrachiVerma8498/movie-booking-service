-- V3: Create bookings table
CREATE TABLE bookings (
    id               BIGSERIAL       PRIMARY KEY,
    show_id          BIGINT          NOT NULL REFERENCES shows (id),
    seat_numbers_csv VARCHAR(1000)   NOT NULL,
    ticket_count     INT             NOT NULL,
    gross_amount     NUMERIC(10, 2)  NOT NULL,
    discount_amount  NUMERIC(10, 2)  NOT NULL,
    final_amount     NUMERIC(10, 2)  NOT NULL,
    status           VARCHAR(20)     NOT NULL,
    created_at       TIMESTAMP       NOT NULL,
    cancelled_at     TIMESTAMP
);

CREATE INDEX idx_bookings_show_id ON bookings (show_id);
CREATE INDEX idx_bookings_status  ON bookings (status);

