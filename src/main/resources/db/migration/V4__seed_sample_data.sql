-- V4: Seed sample data for local Postman scenarios

-- Two shows in Bangalore on the same date so browse + booking flows are easy to test.
INSERT INTO shows (id, movie_name, theatre_name, city, show_time, base_price)
VALUES
    (1001, 'Inception', 'PVR Downtown', 'Bangalore', '2026-04-10 14:00:00', 200.00),
    (1002, 'Interstellar', 'PVR Downtown', 'Bangalore', '2026-04-10 20:00:00', 250.00);

-- Seats for show 1001 (A1 and A2 are already booked via booking 9001).
INSERT INTO seats (id, show_id, seat_number, status, version)
VALUES
    (2001, 1001, 'A1', 'BOOKED', 0),
    (2002, 1001, 'A2', 'BOOKED', 0),
    (2003, 1001, 'A3', 'AVAILABLE', 0),
    (2004, 1001, 'A4', 'AVAILABLE', 0),
    (2005, 1001, 'A5', 'AVAILABLE', 0),
    (2006, 1001, 'B1', 'AVAILABLE', 0),
    (2007, 1001, 'B2', 'AVAILABLE', 0),
    (2008, 1001, 'B3', 'AVAILABLE', 0),
    (2009, 1001, 'B4', 'AVAILABLE', 0),
    (2010, 1001, 'B5', 'AVAILABLE', 0);

-- Seats for show 1002 (all currently available).
INSERT INTO seats (id, show_id, seat_number, status, version)
VALUES
    (2011, 1002, 'A1', 'AVAILABLE', 0),
    (2012, 1002, 'A2', 'AVAILABLE', 0),
    (2013, 1002, 'A3', 'AVAILABLE', 0),
    (2014, 1002, 'A4', 'AVAILABLE', 0),
    (2015, 1002, 'A5', 'AVAILABLE', 0),
    (2016, 1002, 'B1', 'AVAILABLE', 0),
    (2017, 1002, 'B2', 'AVAILABLE', 0),
    (2018, 1002, 'B3', 'AVAILABLE', 0),
    (2019, 1002, 'B4', 'AVAILABLE', 0),
    (2020, 1002, 'B5', 'AVAILABLE', 0);

-- A confirmed booking for show 1001 and a historical cancelled booking for show 1002.
INSERT INTO bookings (
    id,
    show_id,
    seat_numbers_csv,
    ticket_count,
    gross_amount,
    discount_amount,
    final_amount,
    status,
    created_at,
    cancelled_at
)
VALUES
    (9001, 1001, 'A1,A2', 2, 400.00, 80.00, 320.00, 'CONFIRMED', '2026-04-09 10:00:00', NULL),
    (9002, 1002, 'B1,B2', 2, 500.00, 0.00, 500.00, 'CANCELLED', '2026-04-09 11:00:00', '2026-04-09 12:00:00');

-- Keep sequences in sync after explicit IDs.
SELECT setval('shows_id_seq', (SELECT MAX(id) FROM shows));
SELECT setval('seats_id_seq', (SELECT MAX(id) FROM seats));
SELECT setval('bookings_id_seq', (SELECT MAX(id) FROM bookings));
