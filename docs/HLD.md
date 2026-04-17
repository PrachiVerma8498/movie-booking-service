# High Level Design (HLD)

## 1. Document purpose
This document describes the high-level architecture for the `movie-booking-service` project and aligns it with Problem Statement v1.3.3 for a B2B + B2C online movie ticket booking platform.

It includes:
- Current implementation (as-is)
- Target architecture (to-be, extensible)
- Functional and non-functional requirement coverage
- Platform provisioning, release, observability, and governance recommendations

## 2. Problem statement summary (v1.3.3)
XYZ needs a platform that:
- Onboards theatre partners (B2B)
- Lets customers browse and book tickets (B2C)
- Supports offers and smooth booking experience
- Scales across cities/countries with high availability
- Integrates with payment providers and theatre systems

## 3. Scope and assumptions
### In scope for current codebase
- Show creation with seat inventory (`POST /api/shows`)
- Browse shows by city and date (`GET /api/shows`)
- Book seats with discount rules (`POST /api/bookings`)
- Fetch and cancel booking (`GET /api/bookings/{id}`, `POST /api/bookings/{id}/cancel`)
- Concurrency protection for seat booking using database locking

### Out of scope in current codebase (recommended for next phases)
- Identity, authentication, and role-based access control
- Payment gateway integration
- Theatre onboarding workflow and partner APIs
- Localization by language/genre metadata models
- Notifications (email/SMS/push)
- Multi-region active-active deployment

## 4. Functional requirement traceability
| Requirement | Current status | Notes |
|---|---|---|
| Browse theatres running selected movie by city/date | Partial | Browse by city/date exists; movie-level filtering can be added as query param. |
| Offers: 50% discount on third ticket | Implemented | In `BookingService.bookTickets`. |
| Offers: 20% discount for afternoon show | Implemented | Afternoon defined as 12:00-17:59. |
| Book tickets by theatre/timing/seats | Implemented | Via show id + seat list. |
| Theatre create/update/delete shows | Partial | Create exists; update/delete pending. |
| Bulk booking and cancellation | Gap | Not yet available as batch APIs. |
| Theatre allocate/update seat inventory | Partial | Initial seat allocation exists via show creation; updates pending. |

## 5. Architecture overview
### 5.1 As-is (implemented)
- Architecture style: layered monolith
- Runtime: Spring Boot service exposing REST APIs
- Persistence: PostgreSQL via Spring Data JPA + Flyway
- Concurrency control: `PESSIMISTIC_WRITE` lock on requested seats

Logical layers:
1. API layer (`api`): controllers + exception mapping
2. Application/service layer (`service`): business rules and transactions
3. Domain model (`domain`): entities and enums
4. Data access (`repository`): JPA repositories
5. Data migration (`db/migration`): schema + sample data

### 5.2 To-be (target at scale)
Evolve from monolith to modular services behind an API gateway:
- `catalog-service` (movies, theatres, shows, search)
- `inventory-service` (seat map, hold/lock, capacity)
- `booking-service` (booking state machine)
- `pricing-offers-service` (campaigns, discount engine)
- `payment-service` (payment intent, webhook handling)
- `partner-integration-service` (theatre APIs, adapters)
- `notification-service` (email/SMS/push)

Use event-driven integration (Kafka/PubSub) for cross-service state propagation.

## 6. Key design decisions
1. **Transactional seat booking**: lock target seats in DB transaction to avoid oversell.
2. **Simple offer engine in service layer**: deterministic pricing for v1.
3. **Flyway-managed schema**: repeatable and versioned database evolution.
4. **REST-first contract**: easy integration with Postman and partner systems.
5. **Monolith-first delivery**: faster MVP; clear path to service decomposition.

## 7. Data architecture (high-level)
Primary database: PostgreSQL

Core entities:
- `shows` (movie, theatre, city, show_time, base_price)
- `seats` (show_id, seat_number, status, version)
- `bookings` (show_id, seat list, price breakdown, booking status)

Data consistency:
- ACID transaction for booking/cancellation
- Seat uniqueness per show using DB unique constraint `(show_id, seat_number)`

## 8. Transactional scenarios and consistency
### Scenario A: book seats
1. Validate request and normalize seat numbers.
2. Load show.
3. Lock requested seats for update.
4. Verify all seats exist and are available.
5. Mark seats as booked.
6. Compute discounts and final amount.
7. Persist booking and commit.

Guarantee: no double-booking for the same seat under concurrent requests.

### Scenario B: cancel booking
1. Validate booking exists and not already cancelled.
2. Lock seats from booking.
3. Mark seats available.
4. Mark booking cancelled.
5. Commit.

## 9. Non-functional architecture
### 9.1 Scalability and availability (target 99.99%)
- Stateless API instances behind load balancer
- Horizontal scaling by city/traffic patterns
- Database: managed PostgreSQL with read replicas, PITR backups
- Caching for browse queries (Redis)
- Circuit breakers/retries/timeouts for external integrations
- Multi-AZ active-passive at minimum, multi-region for mature stage

### 9.2 Security and OWASP controls
- OAuth2/OIDC authentication, JWT-based authz
- RBAC roles: `PARTNER_ADMIN`, `PARTNER_OPERATOR`, `CUSTOMER`, `OPS_ADMIN`
- Input validation (already present via Bean Validation)
- Rate limiting and bot protection
- Parameterized queries via JPA (avoid SQL injection)
- Secrets in vault (not in source control)
- TLS in transit + encryption at rest
- Security testing: SAST, DAST, dependency CVE scanning

### 9.3 Compliance
Recommended baseline:
- PCI DSS scope reduction via tokenized payment gateway integration
- GDPR/DPDP-ready retention and deletion workflows
- Audit logs for booking status and partner actions

## 10. Integration architecture
### Theatre partner integration
- Adapter pattern in partner integration module
- Two modes:
  - API push/pull for digital theatres
  - CSV/SFTP batch ingestion for legacy theatres
- Canonical model for show and seat payloads

### Payment integration
- Payment intent creation before final confirmation
- Webhook-based asynchronous finalization
- Idempotency keys for retry-safe payment callbacks

## 11. Monetization model
Potential streams:
- Per-ticket platform fee
- Partner subscription tiers for analytics and ad placement
- Sponsored listing/promotions for theatres and movies
- Premium convenience fee for priority seat lock window

## 12. Platform provisioning and sizing (baseline)
### Suggested cloud stack
- Compute: Kubernetes or managed app service
- DB: managed PostgreSQL
- Cache: Redis
- Messaging: Kafka or managed pub/sub
- Edge: API Gateway + WAF + CDN for public APIs
- CI/CD: GitHub Actions or Jenkins + artifact registry

### Initial sizing assumptions
- 2-4 app instances for non-peak; auto-scale on CPU + RPS + p95 latency
- DB primary + 1 read replica
- Cache sized to keep 24h browse catalog hot set

## 13. Release strategy across geos
- Trunk-based development with feature flags
- Canary rollout per geography/city
- Blue-green for high-risk releases
- Backward-compatible API versioning (`/v1`, `/v2`)
- Localization strategy: locale-specific metadata and currency support

## 14. Monitoring, logging, and KPIs
### Monitoring and logs
- Metrics: Prometheus/OpenTelemetry
- Dashboards: Grafana
- Logs: ELK/OpenSearch with request correlation id
- Tracing: distributed traces for booking and payment paths

### Product and platform KPIs
- Booking success rate
- Seat lock contention rate
- Payment success/failure ratio
- p95/p99 API latency
- Availability SLA/SLO attainment
- Cancellation and refund turnaround time

## 15. High-level project plan and estimates
| Phase | Duration | Outcomes |
|---|---:|---|
| Phase 1 MVP hardening | 2-3 weeks | Fix edge cases, add update/delete show, improve tests, API docs |
| Phase 2 Partner + payment | 4-6 weeks | Partner onboarding APIs, payment integration, idempotency |
| Phase 3 Scale + observability | 3-4 weeks | Caching, metrics, alerts, performance tests |
| Phase 4 Multi-geo readiness | 4-6 weeks | Internationalization, regional rollout patterns |

## 16. Risks and mitigations
- **Race conditions at peak load** -> keep pessimistic lock for critical writes; run concurrency tests.
- **Payment inconsistency** -> implement booking state machine with pending/confirmed/failed states.
- **Partner data quality issues** -> schema validation and quarantine queue for bad payloads.
- **Release regressions** -> contract tests, canary, and rollback automation.

## 17. Candidate Solution / Panel feedback placeholders
### Candidate Solution: Refer to applicant solution

### Panel Feedback
- Summary (max three lines):
- Strengths (max two lines):
- Gaps (max two lines):
- Proficiency Level (Low/Med/High):

