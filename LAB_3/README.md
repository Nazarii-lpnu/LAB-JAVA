# Car Service Laboratory 3

## Purpose

This project extends the Laboratory 2 car-service domain with Spring Boot,
Spring Data JPA, PostgreSQL persistence, Flyway migrations, transactions,
atomic batch work insertion, and optimistic locking.

The original domain rules remain in the domain model. `RepairOrder` is the
aggregate root for service works, mechanic assignment, scheduling, lifecycle
transitions, pricing data, and completion.

## Technologies

- Java 27
- Spring Boot 4.1.0
- Spring Data JPA and Hibernate
- PostgreSQL
- Flyway
- Maven
- JUnit 5
- Mockito
- Testcontainers
- JaCoCo

## Environment requirements

- JDK 27
- Maven
- PostgreSQL for application execution
- Internet access on the first Maven build unless dependencies are cached

The Maven compiler is configured with Java release 27.

## PostgreSQL configuration

The application reads database settings from environment variables:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/car_service
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
```

Start PostgreSQL with Docker Compose:

```bash
docker compose up -d
```

The compose file creates the `car_service` database and exposes PostgreSQL on
port 5432. Set `DB_PASSWORD` if a different local password is required. Do not
commit passwords or other secrets. Flyway applies migrations automatically, and
Hibernate validates the migrated schema with
`spring.jpa.hibernate.ddl-auto=validate`.

## Running the application

```bash
mvn clean compile
mvn spring-boot:run
```

The Spring Boot entry point is
`com.example.carservice.CarServiceApplication`.

`CarServiceDemo` is a Spring-managed `CommandLineRunner`. It creates a
customer, car, mechanic, repair order, and service works, loads the persisted
order, completes it, prints the final commercial price and status, and
demonstrates an expected invalid operation. The fixed demo VIN prevents
duplicate demo orders on subsequent starts.

To disable the demo runner:

```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--app.demo.enabled=false"
```

Run the application a second time against the same PostgreSQL database to
observe that the existing demo order is loaded instead of being created again.

## Running tests

```bash
mvn clean test
```

All integration tests run against PostgreSQL 18 in a shared Testcontainers
container and therefore require Docker. Docker-unavailable test execution fails
loudly rather than silently skipping integration tests. The suite covers JPA
persistence, relationships, constraints, transaction rollback, completion,
batch updates, optimistic locking, pricing, demo idempotency, and native
PostgreSQL queries.

The latest `mvn clean verify` run executed **71 tests**, with **0 failures**,
**0 errors**, and no skipped tests.

The JaCoCo report is generated at:

```text
target/site/jacoco/index.html
```

## Package structure

```text
src/main/java/com/example/carservice
├── app
├── domain
├── exception
├── pricing
├── repository
└── service
```

- `app`: Spring Boot entry point, configuration, and `CommandLineRunner`.
- `domain`: entities, value objects, enums, aggregate rules, and converters.
- `exception`: domain-specific runtime exceptions.
- `pricing`: pricing policy, rule engine, rules, and pricing value objects.
- `repository`: Spring Data repositories with derived, JPQL, and native queries.
- `service`: entity-oriented application services and repair-order commands.

## Application layers

Services own transactions, business rules, repository access, explicit entity
loading, existence checks, and translation of missing or duplicate entities
into domain exceptions. Repositories use inherited Spring Data CRUD methods for
standard operations.
Derived methods are used for simple entity lookups, while JPQL and native SQL
are reserved for custom fetch plans and database-specific queries.

## Persistence model

### Entities

- `Customer`
  - Primary key: generated BIGINT identity.
  - Owns cars through a one-to-many relationship.
- `Car`
  - Primary key: VIN.
  - References its customer through a mandatory foreign key.
- `Mechanic`
  - Primary key: generated BIGINT identity.
  - Stores specialization.
- `RepairOrder`
  - Primary key: generated BIGINT identity.
  - References a car and optionally a mechanic.
  - Owns service works.
  - Stores status, priority, planned slot, final price, completion time,
    and optimistic-locking version.
- `ServiceWork`
  - Primary key: generated BIGINT identity.
  - References its repair order through a mandatory foreign key.
- `TimeSlot`
  - Embeddable value object stored in `repair_orders`.

Relationships are lazy by default. `RepairOrderRepository` uses entity
graphs and fetch joins when a complete order with car, mechanic, and works is
needed. Monetary values use `BigDecimal` and `DECIMAL(19,2)`. Durations are
stored as seconds through `DurationAttributeConverter`.

## Flyway migrations

`src/main/resources/db/migration/V1__create_schema.sql` creates:

- `customers`
- `cars`
- `mechanics`
- `repair_orders`
- `service_works`

It also defines primary keys, foreign keys, indexes, not-null constraints,
check constraints, completion columns, and the `repair_orders.version` column.
Because V1 was changed to generated BIGINT identifiers,
reset a local database before starting the application again:

```bash
docker compose down -v && docker compose up -d
```

This removes the old Flyway schema history and avoids a checksum mismatch.
Future changes to an already-applied schema must use a new versioned migration
instead of editing an applied migration.

## Business rules

The normal order lifecycle is:

```text
CREATED -> DIAGNOSED -> APPROVED -> IN_PROGRESS -> COMPLETED
```

Cancellation is allowed from `CREATED`, `DIAGNOSED`, and `APPROVED`.
Completed and cancelled orders cannot be modified.

Other preserved rules include:

- An approved order must contain at least one work.
- A work ID cannot be duplicated within an order.
- A mechanic must be compatible with every work.
- A general mechanic can perform every specialization.
- An order cannot complete while work remains unfinished.
- Base price is parts plus labor.
- Urgent orders apply the existing labor multiplier.
- Pricing rules execute in priority and ID order.
- Monetary results use scale 2 and `RoundingMode.HALF_UP`.
- Scheduling rejects unavailable or overlapping mechanic assignments.

## Transactions and atomicity

`RepairOrderService.complete(long)` is transactional. It loads the order,
validates its state and work status, records completion, calculates and stores
the commercial price, and flushes the update. Unchecked validation, pricing,
or persistence exceptions escape the method and cause rollback.

`RepairOrderService.addWorks(long, Collection<ServiceWork>)` is also
transactional. Each work is passed to the existing `RepairOrder.addWork`
validation. If any work is null, duplicated, incompatible, or otherwise
rejected, the exception escapes and the whole batch is rolled back. The
integration test verifies that earlier valid items do not remain persisted.

## Optimistic locking

`RepairOrder` contains a JPA `@Version` field mapped to
`repair_orders.version`. Hibernate includes the version in update predicates
and increments it after a successful update. If two persistence contexts load
the same version, the first update succeeds and the stale second update
raises an optimistic-locking exception instead of overwriting newer data.

The integration test verifies the conflict at flush/commit time and confirms
the first update remains in the database.

`RepairOrderService` translates this persistence conflict into
`ConcurrentModificationException` with a retry-oriented message. The demo
prints the conflict and confirms that the first update remains persisted.

## N+1 query detection and prevention

The test profile enables Hibernate statistics. A deliberately lazy query that
loads 10 orders with their cars joined but accesses each order's lazy works
collection executes **11 SQL statements**: 1 order query plus 10 collection
queries (the N+1 problem). The optimized repository method uses an
`@EntityGraph` for `car`, `mechanic`, and `works`, reducing the same access
pattern to **1 SQL statement** in the integration test.

## Latest verification

The application was run successfully against PostgreSQL 18 using:

```bash
DB_URL=jdbc:postgresql://localhost:5433/car_service \
DB_USERNAME=postgres DB_PASSWORD=postgres mvn spring-boot:run
```

Port `5433` was used because the default host port `5432` was already
occupied. The demo loaded the existing order, continued after the expected
invalid operation, and printed the optimistic-lock conflict.
