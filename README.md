# Movie Booking Service (Spring Boot)

This is a standalone Spring Boot project for movie ticket booking with:

- Theatre show creation
- Browse shows by city/date
- Ticket booking by seat selection
- Seat availability protection via transactional locking
- Offers:
  - 50% discount on the third ticket
  - 20% discount for afternoon shows (12:00-17:59)
- Booking cancellation with seat release

## Tech stack

- Java 17
- Spring Boot 3.3
- Spring Web + Validation
- Spring Data JPA
- PostgreSQL
- JUnit 5

## Prerequisites

Start a local PostgreSQL instance and create the database:

```sql
CREATE DATABASE "showBooking";
```

Default connection config (update `application.yml` if different):

| Property | Value |
|----------|-------|
| Host     | localhost |
| Port     | 5432 |
| Database | showBooking |
| Username | postgres |
| Password | postgres |

## Assumed discount behavior

Both discounts are applied cumulatively:

1. Third-ticket discount is applied first (50% of one ticket price, if ticket count >= 3)
2. Afternoon discount is then applied on the remaining subtotal

## API endpoints

- `POST /api/shows` - create a show with seat inventory
- `GET /api/shows?city={city}&date={yyyy-MM-dd}` - browse shows
- `POST /api/bookings` - book seats
- `GET /api/bookings/{bookingId}` - fetch booking
- `POST /api/bookings/{bookingId}/cancel` - cancel booking

## Sample payloads

### Create show

```json
{
  "movieName": "Inception",
  "theatreName": "PVR Downtown",
  "city": "Bangalore",
  "showTime": "2026-03-30T14:00:00",
  "basePrice": 200.00,
  "seatNumbers": ["A1", "A2", "A3", "A4", "B1"]
}
```

### Book tickets

```json
{
  "showId": 1,
  "seatNumbers": ["A1", "A2", "A3"]
}
```

## Run locally

```powershell
cd "C:\Users\PrachiVerma\Documents\preps\Preparation\movie-booking-service"
mvn spring-boot:run
```

## Run tests

```powershell
cd "C:\Users\PrachiVerma\Documents\preps\Preparation\movie-booking-service"
mvn test
```


