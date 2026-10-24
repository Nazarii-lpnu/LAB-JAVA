# Car Service Laboratory 2

## Purpose

Laboratory 2 is a Maven-based Java application for managing automotive repair
orders. It preserves the confirmed Laboratory 1 domain behavior and adds
repository-based persistence, application services, injectable commercial
pricing, and a reusable pricing-rule engine.

## Technologies

- Java 17
- Maven
- JUnit 5
- Mockito
- Maven Surefire Plugin
- JaCoCo Maven Plugin

## Environment requirements

- JDK 17 or newer with `java` and `javac` available on `PATH`
- Maven available as `mvn`
- Internet access on the first Maven build, unless dependencies are already cached

The project is configured for Java release 17 in `pom.xml`.

## Running the console demonstration

From the `LAB_2` directory:

```text
mvn clean compile
java -cp target/classes com.example.carservice.app.CarServiceDemo
```

The demonstration creates and saves an order, retrieves it, adds work, prints
base and commercial prices, completes a valid order flow, rejects a modification
after completion, and continues execution after the expected error.

## Running automated tests

```text
mvn clean test
```

Surefire discovers JUnit 5 tests. After a successful Maven run, the JaCoCo
report is generated at:

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
│   └── inmemory
└── service
```

- `app`: `CarServiceDemo`, the console entry point.
- `domain`: `RepairOrder`, `ServiceWork`, `Car`, `Customer`, `Mechanic`,
  `TimeSlot`, gifts, statuses, priorities, and specializations.
- `exception`: domain-specific runtime exceptions.
- `pricing`: pricing policies, immutable pricing DTOs/context, rules, and
  `PricingRuleEngine`.
- `repository`: `RepairOrderRepository`, the persistence abstraction.
- `repository.inmemory`: `InMemoryRepairOrderRepository`.
- `service`: `RepairOrderService` and the Laboratory 1-compatible `CarService`
  facade.

## Main architecture

### RepairOrder

`RepairOrder` is the aggregate root for an order. It owns service works,
mechanic assignment, planned scheduling, order status, priority, and lifecycle
invariants. It supports adding/removing work, assigning a compatible mechanic,
planning a `TimeSlot`, diagnosing, approving, starting, completing work,
completing the order, cancelling, calculating base cost, and selecting a gift.

The allowed lifecycle is:

```text
CREATED -> DIAGNOSED -> APPROVED -> IN_PROGRESS -> COMPLETED
```

Cancellation is supported from `CREATED`, `DIAGNOSED`, and `APPROVED`.
Completed and cancelled orders cannot be modified.

### RepairOrderService

`RepairOrderService` coordinates application operations. It receives a
`RepairOrderRepository` and a `PricingPolicy` through its constructor. It
retrieves orders, invokes domain operations, validates mechanic scheduling
against repository data, and persists successful updates.

The service does not duplicate order lifecycle or pricing calculations.

### RepairOrderRepository

`RepairOrderRepository` defines saving, retrieval, updating, searching by car
VIN, searching by mechanic, and listing all orders. Missing IDs are represented
by `Optional.empty()` at repository level; `RepairOrderService.getById` converts
that result to `EntityNotFoundException`.

### In-memory persistence

`InMemoryRepairOrderRepository` stores orders in a private `HashMap<UUID,
RepairOrder>`. `save` rejects duplicate IDs, `update` requires an existing ID,
and searches return matching orders sorted by creation time. Each repository
instance owns its own map, so instances are isolated.

## Pricing

### Base price

`RepairOrder.calculateBaseCost()` adds every work item's spare-parts cost and
labor cost using `BigDecimal`. The result is rounded to scale 2 with
`RoundingMode.HALF_UP`.

Example:

```text
Parts: 25.00 + 60.00 = 85.00
Labor: 40.00 + 80.00 = 120.00
Base price: 205.00
```

### Commercial price

`CommercialPricingPolicy` starts with the base components. For an urgent order,
it applies the 1.20 labor multiplier while leaving spare-parts cost unchanged.
The resulting amount is then passed to `PricingRuleEngine`.

For example, with parts `100.00` and labor `50.00`:

```text
Normal commercial price: 150.00
Urgent commercial price: 100.00 + (50.00 × 1.20) = 160.00
```

### PricingRule and PricingRuleEngine

`PricingRule` is the common extension point. Each rule provides:

- a unique ID;
- an integer priority;
- an immutable `PricingContext` transformation.

The current built-in rules are:

- `DiscountPricingRule`: applies a percentage from 0 through 100.
- `MinimumPriceRule`: raises a result to a configured non-negative floor.

`PricingRuleEngine` is independent of `RepairOrder`, repositories, and console
classes. It accepts immutable `PricingInput` and returns immutable
`PricingResult`. It rejects null, negative, malformed, duplicate, and invalid
rule data. It never mutates caller-owned input.

Rules execute in ascending priority order, then by rule ID. Discounts use
priority 100 and minimum-price rules use priority 1000, so discounts execute
before the minimum-price floor. Ordering changes the result: a 20% discount and
a 90.00 floor applied to 100.00 produce 90.00 when the discount runs first,
but 72.00 if the floor runs first.

Final results use scale 2 and `RoundingMode.HALF_UP`. Repeated invocations with
the same input and configuration are deterministic.

To add a rule, implement `PricingRule` and return a new context:

```java
return context.withPrice(id(), newPrice);
```

Pass the rule to `PricingRuleEngine`; the engine algorithm does not change.

## SOLID principles in the implementation

- **Single Responsibility:** `RepairOrder` owns domain invariants,
  `RepairOrderService` coordinates use cases, repositories store orders, and
  pricing classes calculate prices.
- **Open/Closed:** new pricing behavior is added through `PricingRule` without
  changing `PricingRuleEngine`.
- **Liskov Substitution:** any `PricingRule` can participate in the engine, and
  any `RepairOrderRepository` can be supplied to the service.
- **Interface Segregation:** the repository and pricing interfaces expose only
  operations required by their clients.
- **Dependency Inversion:** `RepairOrderService` depends on
  `RepairOrderRepository` and `PricingPolicy` abstractions, not concrete
  persistence or pricing implementations.

## Verification status

The test source currently contains:

- 101 unique test methods;
- 9 parameterized test methods;
- 123 executed test cases in the latest direct JUnit Platform run;
- 123 passed, 0 failed, 0 skipped.

The latest direct run used cached JUnit 5 and Mockito dependencies. The command
`mvn clean test` was also attempted, but Maven was unavailable on the audit
machine, so no Maven/Surefire result or JaCoCo percentage is claimed.

## Known limitations

- Persistence is in-memory only and is lost when the process exits.
- There is no database, REST API, GUI, authentication, or Spring integration.
- Maven must be installed before the documented Maven commands can execute.
- The official Laboratory 2 specification was not present in the workspace;
  implementation and documentation are based on the requested requirements and
  confirmed Laboratory 1 behavior.
