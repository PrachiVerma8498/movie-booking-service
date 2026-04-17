# Low Level Design (LLD)

## 1. Purpose
This document captures implementation-level design for the current `movie-booking-service` codebase and identifies extensions needed to fully satisfy Problem Statement v1.3.3.

## 2. Code structure
```
src/main/java/com/preparation/moviebooking
  api/
    BookingController.java
    ShowController.java
    ApiExceptionHandler.java
    dto/
  service/
    BookingService.java
    ShowService.java
  domain/
    Show.java
    Seat.java
    Booking.java
    BookingStatus.java
    SeatStatus.java
  repository/
    ShowRepository.java
    SeatRepository.java
    BookingRepository.java
  exception/
    BookingValidationException.java
    ResourceNotFoundException.java
```

## 3. Module responsibilities
- `ShowController`: create show and browse shows APIs.
- `BookingController`: book seats, fetch booking, cancel booking APIs.
- `ShowService`: input normalization, show persistence, browse queries.
- `BookingService`: booking validation, locking, pricing, cancellation.
- `SeatRepository`: seat lookup with `PESSIMISTIC_WRITE` lock.
- `ApiExceptionHandler`: maps exceptions to structured HTTP errors.

## 4. Data model and schema mapping
### 4.1 `shows` table (`V1__create_shows_table.sql`)
- `id BIGSERIAL PK`
- `movie_name VARCHAR(255) NOT NULL`
- `theatre_name VARCHAR(255) NOT NULL`
- `city VARCHAR(255) NOT NULL`
- `show_time TIMESTAMP NOT NULL`
- `base_price NUMERIC(10,2) NOT NULL`
- Index: `(city, show_time)`

### 4.2 `seats` table (`V2__create_seats_table.sql`)
- `id BIGSERIAL PK`
- `show_id BIGINT NOT NULL FK -> shows(id)`
- `seat_number VARCHAR(20) NOT NULL`
- `status VARCHAR(20) NOT NULL` (`AVAILABLE`, `BOOKED`)
- `version BIGINT NOT NULL` for optimistic versioning metadata
- Unique constraint: `(show_id, seat_number)`
- Index: `(show_id, status)`

### 4.3 `bookings` table (`V3__create_bookings_table.sql`)
- `id BIGSERIAL PK`
- `show_id BIGINT NOT NULL FK -> shows(id)`
- `seat_numbers_csv VARCHAR(1000) NOT NULL`
- `ticket_count INT NOT NULL`
- `gross_amount NUMERIC(10,2) NOT NULL`
- `discount_amount NUMERIC(10,2) NOT NULL`
- `final_amount NUMERIC(10,2) NOT NULL`
- `status VARCHAR(20) NOT NULL` (`CONFIRMED`, `CANCELLED`)
- `created_at TIMESTAMP NOT NULL`
- `cancelled_at TIMESTAMP NULL`

## 5. API contracts
### 5.1 Create show
- Method: `POST /api/shows`
- Request DTO: `CreateShowRequest`
  - `movieName` (required)
  - `theatreName` (required)
  - `city` (required)
  - `showTime` (required, future)
  - `basePrice` (required, >= 0.01)
  - `seatNumbers` (required, non-empty)
- Response DTO: `ShowSummaryResponse`

### 5.2 Browse shows
- Method: `GET /api/shows?city={city}&date={yyyy-MM-dd}`
- Response: `List<ShowSummaryResponse>` ordered by show time ascending

### 5.3 Book tickets
- Method: `POST /api/bookings`
- Request DTO: `BookTicketsRequest`
  - `showId` (required)
  - `seatNumbers` (required, non-empty)
- Response DTO: `BookingResponse`

### 5.4 Get booking
- Method: `GET /api/bookings/{bookingId}`
- Response DTO: `BookingResponse`

### 5.5 Cancel booking
- Method: `POST /api/bookings/{bookingId}/cancel`
- Response DTO: `BookingResponse`

### 5.6 Error model
`ApiExceptionHandler` returns:
- `timestamp`
- `status`
- `error`
- `message`

Status mappings:
- `404`: `ResourceNotFoundException`
- `400`: `BookingValidationException`, bean validation errors

## 6. Core business rules
1. Seat numbers are normalized to uppercase and trimmed.
2. Duplicate seat numbers in one request are rejected.
3. Booking fails if any requested seat is missing or already booked.
4. Pricing:
   - Gross = `basePrice * ticketCount`
   - Third ticket discount = `basePrice * 0.50` when `ticketCount >= 3`
   - Afternoon discount = `20%` of subtotal after third-ticket discount when show hour in `[12, 18)`
   - Final = `gross - totalDiscount`
5. Cancellation reopens all seats in booking.

## 7. Detailed flow design
### 7.1 Book tickets sequence
1. Controller validates JSON and calls `BookingService.bookTickets`.
2. Service normalizes seats and validates non-empty list.
3. Service fetches show by id.
4. Service calls `SeatRepository.findAllForUpdateByShowIdAndSeatNumbers` with `PESSIMISTIC_WRITE` lock.
5. Service checks seat count match and seat status.
6. Service marks seats as `BOOKED`.
7. Service computes discount and final amount.
8. Service creates booking row with `CONFIRMED` state.
9. Transaction commits and response is returned.

### 7.2 Cancel booking sequence
1. Controller calls `BookingService.cancelBooking`.
2. Service fetches booking and validates it is not already cancelled.
3. Service locks booked seats using same seat lock query.
4. Service sets seats to `AVAILABLE`.
5. Service marks booking `CANCELLED`, sets cancellation timestamp.
6. Transaction commits and response is returned.

### 7.3 Browse shows sequence
1. Controller receives `city` and `date`.
2. Service computes start and end timestamps for date.
3. Repository returns city/date filtered shows.
4. Service maps each show to summary and counts available seats.

## 8. Concurrency and thread safety design
- Critical write path uses DB row-level locking (`PESSIMISTIC_WRITE`).
- Isolation is enforced by transaction boundaries (`@Transactional`).
- Unique seat constraint prevents duplicate seat identity per show.
- This protects against concurrent double booking for same seat set.

## 9. Current design gaps and recommended LLD enhancements
1. **Duplicate methods in `BookingService`**
   - `bookMovieTickets` and `bookTickets` overlap.
   - `cancelBookedTicket` and `cancelBooking` overlap.
   - Recommendation: keep one canonical implementation for each use case.
2. **Bug-prone check in `bookMovieTickets`**
   - Size check is inverted there (`==` instead of `!=`).
   - Recommendation: remove deprecated method path.
3. **Seat list in booking persistence**
   - CSV works for MVP; a join table `booking_seats` is better for analytics and integrity.
4. **No explicit idempotency for booking API**
   - Add `Idempotency-Key` header and dedupe table.
5. **No update/delete show APIs yet**
   - Add endpoints with partner authorization checks.

## 10. Suggested interface extensions (v1.3.3 alignment)
### 10.1 Partner show management
- `PUT /api/shows/{showId}` update show metadata and pricing
- `DELETE /api/shows/{showId}` soft delete/cancel future inventory
- `PATCH /api/shows/{showId}/seats` adjust seat statuses or allocations

### 10.2 Offers service abstraction
Define `PricingPolicy` interface:
- `BigDecimal apply(Show show, List<Seat> seats, BigDecimal gross)`

Implementations:
- `ThirdTicketDiscountPolicy`
- `AfternoonShowDiscountPolicy`
- `CompositePricingPolicy`

### 10.3 Payment orchestration
Introduce booking states:
- `PENDING_PAYMENT`, `CONFIRMED`, `PAYMENT_FAILED`, `CANCELLED`, `REFUNDED`

## 11. Test design mapping
Existing tests in `BookingServiceIntegrationTest` cover:
- Discount calculations
- Double booking prevention
- Seat release after cancellation

Additional recommended tests:
- Concurrent booking race test with two threads and same seat
- Validation tests for blank/duplicate seat numbers
- API contract tests for error payload format
- Flyway migration smoke test against clean DB

## 12. Dev and run notes
- DB migrations run via Flyway at startup.
- Sample data exists in `V4__seed_sample_data.sql`.
- Base local run command in project README.

## 13. Candidate Solution / Panel feedback placeholders
### Candidate Solution: Refer to applicant solution

### Panel Feedback
- Summary (max three lines):
- Strengths (max two lines):
- Gaps (max two lines):
- Proficiency Level (Low/Med/High):

