# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Status

This repo is a technical interview challenge: a dispatch window booking system with geographic coverage. All 9 build order steps are done (skeleton, compose, Flyway schema + reference data, entities, repositories, commune coverage, window seeder, availability, booking service, concurrency tests, REST API with problem+json errors and Swagger, demo sold-out day, Thymeleaf + HTMX booking UI at `/`). README verified by a dry run on a fresh copy with empty Maven caches.

- `docs-requirement.pdf` (in Spanish) is the official challenge statement and the source of truth for requirements. Page 2 is a reference screenshot the UI and seed data mirror. `prompt.txt` is an English paraphrase that adds a few personal preferences (Swagger, human-voice README).
- `IMPLEMENTATION_PLAN.md` holds the agreed design decisions (section 1 "Locked decisions") and the build order (section 11). Follow it. If something in the code would deviate from it, ask first and then update the plan.
- Scope is exactly what the PDF asks for, plus cancel. Every feature must trace to a row in the plan's section 12. Ideas beyond that (holds/TTL, idempotency keys, pickup, auth) go in the README's "What I left out" section, not in code.

## Stack and commands

Java 21, Spring Boot 4.1.2 (modular starters, e.g. `spring-boot-starter-webmvc`; springdoc must be its 3.x line, currently 3.2.0), Maven wrapper, PostgreSQL 16 (no H2 anywhere), Flyway, springdoc-openapi, Thymeleaf + HTMX (HTMX bundled locally, no Node), Testcontainers.

- Run: `./mvnw spring-boot:run`. Spring Boot Docker Compose support starts Postgres from `compose.yaml` on host port 5433 (db/user/password all `dispatch`).
- Reset the local database: `docker compose down -v`.
- Tests: `./mvnw test` (needs Docker for Testcontainers; about 30 s).
- One class: `./mvnw test -Dtest=BookingConcurrencyTests`
- One method: `./mvnw test -Dtest=SchemaMigrationTests#databaseRejectsBookedCountAboveCapacity`
- App on `http://localhost:8080`, Swagger UI at `/swagger-ui.html`, spec at `/v3/api-docs`.

## Architecture essentials

- Address = street, number, commune. A zone is a set of communes (`commune.zone_id`, null means not covered), so zones can't overlap by construction.
- `dispatch_window` (zone + date + time block) carries `capacity` and `booked_count`. A DB `CHECK (booked_count <= capacity)` is the last line of defence against over-booking.
- Booking is one step: CONFIRMED, optionally CANCELLED, no holds. In one transaction, insert the booking (partial unique index on `order_id` where CONFIRMED), then `UPDATE ... SET booked_count = booked_count + 1 WHERE booked_count < capacity`. Zero rows means sold out and the insert rolls back. Never replace this with read-then-write.
- Cancel is a guarded `UPDATE booking ... WHERE status = 'CONFIRMED'`, and capacity is released only if that changed a row.
- Book and cancel both touch the booking row before the window row. Keep that order to avoid deadlocks.
- Flyway owns the schema (`db/migration/V1__schema.sql`, `V2__reference_data.sql` for zones, communes and time blocks). Hibernate runs with `ddl-auto=validate`, so an entity change needs a matching migration change.
- Windows for the 7-day horizon (`dispatch.horizon-days`) are created by `WindowSeeder` with `INSERT ... ON CONFLICT DO NOTHING` at startup and by a daily cron at 00:05 Santiago time. The demo sold-out day is created through `BookingService`, so `booked_count` always matches confirmed bookings.
- `DemoDataSeeder` (`dispatch.demo-data.enabled=true`) only runs on an empty `booking` table and must run after `WindowSeeder` (`@Order(1)` / `@Order(2)` on the startup listeners). Tests switch it off in `src/test/resources/config/application.properties`, which Spring Boot layers over the main file; `DemoDataSeederTests` turns it back on for itself.
- Time comes from an injected `Clock` (`DispatchConfig.TIME_ZONE`, America/Santiago). Pass the current time into queries instead of relying on SQL `now()`, so the "block already started" rule is testable.
- REST controllers (`api`, under `/api/v1`) and Thymeleaf/HTMX controllers (`web`) both call the same services. Services throw domain exceptions (`service/*Exception`); `api.ApiExceptionHandler` maps them to RFC 7807 `problem+json` with stable codes (`WINDOW_SOLD_OUT`, `ADDRESS_NOT_COVERED`, ...). The endpoint list and the code-to-status table are in plan section 6. DTOs are records; entities are never exposed.
- The UI (`web.BookingPageController`) returns fragments of `templates/fragments.html` that HTMX swaps into `#app`. Expected outcomes (sold out, not covered, already booked) come back as 200 fragments with a message; only unexpected errors are non-2xx. HTMX is bundled at `static/js/htmx.min.js` (2.0.11), no CDN.

## Conventions

- Nothing is released yet, so migrations are edited in place rather than adding V3+. After changing an applied migration, a local database fails Flyway's checksum check; reset it with `docker compose down -v`. Testcontainers databases are always fresh.
- Comment policy is in `IMPLEMENTATION_PLAN.md` section 7.1: comment the "why" for concurrency, DB-specific behaviour and hidden business rules only. No boilerplate Javadoc, banners or change narration.
- Integration tests `@Import(TestcontainersConfiguration.class)` for a shared Postgres container (`@ServiceConnection`), plus `FixedClockConfiguration` when time matters. It pins the clock to Monday 2030-03-04 16:30 Santiago, so expected statuses in tests are relative to that instant.
- Test names describe behaviour (`bookingFailsWhenWindowIsSoldOut`). Concurrency tests must also assert `booked_count` equals the confirmed booking count per window.
- API, code and UI labels are all in English (the UI follows the reference screen's layout, not its Spanish wording). Commune, region and zone names are proper names and stay as they are.
- README is written in a plain first-person voice; see section 10 of the plan.
