-- V1: Create shows table
CREATE TABLE shows (
    id           BIGSERIAL       PRIMARY KEY,
    movie_name   VARCHAR(255)    NOT NULL,
    theatre_name VARCHAR(255)    NOT NULL,
    city         VARCHAR(255)    NOT NULL,
    show_time    TIMESTAMP       NOT NULL,
    base_price   NUMERIC(10, 2)  NOT NULL
);

CREATE INDEX idx_shows_city_show_time ON shows (city, show_time);

