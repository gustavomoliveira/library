# pblibrary — Library Management System

Library management system built as the integrative project for the **Scalable Software Engineering** course. The project was deliberately evolved from a layered monolith into an event-driven microservices architecture, applying Domain-Driven Design, Spring Cloud, and distributed communication concepts in practice.

> 🇧🇷 Leia em português: [README.md](README.md)

---

## About the project

The system supports book and user registration, loan and return management, per-loan audit history, and automatic calculation of overdue fines — the latter implemented as an independent microservice, communicating with the monolith **asynchronously**, via RabbitMQ.

The project was built across four progressive deliveries:

1. **Layered monolith** — Spring Boot, Controller/Service/Repository, domain-driven modeling, React front-end consuming the API.
2. **Real persistence layer** — JPA/Spring Data, loan audit history, full automated test suite (unit, `@DataJpaTest`, `@WebMvcTest`, integration).
3. **Microservice extraction** — creation of `fines-api` as an independent service, with its own database, initially communicating via Spring Cloud (Eureka + OpenFeign), with resilience to network failures.
4. **Event-driven architecture** — the synchronous Feign call was replaced by event publishing/consumption through RabbitMQ, decoupling `library-api` from `fines-api` in time as well as over the network, and adding a Dead Letter Queue and an idempotent consumer. Full details, including validated failure scenarios, in [`ARQUITETURA-EVENTOS.md`](./ARQUITETURA-EVENTOS.md) (Portuguese).

---

## Architecture

```
                         ┌─────────────────────┐
                         │   discovery-server  │
                         │   (Eureka Server)   │
                         │      port 8761      │
                         └───────────┬─────────┘
                                     │  registration
                    ┌────────────────┴─────────────────┐
                    │                                  │
          ┌─────────▼──────────┐             ┌─────────▼──────────┐
          │    library-api     │             │     fines-api      │
          │    (monolith)      │             │   (microservice)   │
          │     port 8080      │             │     port 8081      │
          └─────────┬──────────┘             └─────────┬──────────┘
                    │                                   │
                    │ publishes event           consumes event
                    │ (loan.returned)                   │
                    └───────────────┬───────────────────┘
                                    ▼
                        ┌───────────────────────┐
                        │       RabbitMQ         │
                        │ exchange: library.events │
                        │ queue: fines.loan-returned │
                        │ DLQ: fines.loan-returned.dlq │
                        └───────────────────────┘

          ┌────────────┐                     ┌────────────┐
          │ PostgreSQL │                     │ PostgreSQL │
          │ library_db │                     │  fines_db  │
          │ port 5433  │                     │ port 5434  │
          └────────────┘                     └────────────┘

          ┌─────────────────────┐
          │  library-frontend   │
          │  (React + Vite)     │
          │     port 5173       │
          └─────────────────────┘
              consumes library-api (8080)
              and fines-api (8081) directly
```

### Key architectural decisions

- **Monolith First**: `Book`, `User`, and `Loan` remain in the monolith because `Loan` atomically depends on both within a `@Transactional` boundary — extracting them would introduce real distributed-consistency issues (loss of atomicity, need for a Saga pattern) without a proportional benefit.
- **Fines as a microservice**: an isolated subdomain, event-triggered (on loan return), with its own fully separate database, with the fine-calculation rule encapsulated exclusively inside the service itself — the monolith only sends raw data (dates), never knowing *how* the fine is calculated.
- **Event-driven communication (RabbitMQ)**: `library-api` publishes a domain event (`LoanReturnedEvent`) to a topic exchange after the loan-return transaction commits; `fines-api` consumes that event independently. This replaces the synchronous Feign call used through the third delivery — returning a book no longer depends on `fines-api`'s availability at that exact moment. Full breakdown (trade-offs, Dead Letter Queue, consumer idempotency) in [`ARQUITETURA-EVENTOS.md`](./ARQUITETURA-EVENTOS.md) (Portuguese).
- **No shared library between services**: each microservice re-implements its own exception classes and DTOs (and now its own copy of the event contract), even at the cost of minor duplication — a conscious trade-off to keep services independently deployable.

---

## Repository structure

```
/library
  /library-api          → monolith (Book, User, Loan)
  /fines-api             → fine-calculation microservice
  /discovery-server        → Eureka Server (Service Discovery)
  /library-frontend          → React + Vite front-end
  docker-compose.yml         → RabbitMQ + fines-api database
```

Each Java project is an **independent** Maven module (no parent aggregator `pom.xml`), reflecting the philosophy of independently deployable microservices.

---

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 4.0.x, Spring Data JPA, Spring Cloud (Netflix Eureka), Spring AMQP |
| Messaging | RabbitMQ (topic exchange, Dead Letter Queue) |
| Persistence | PostgreSQL (production), H2 (tests) |
| Testing | JUnit 5, Mockito, AssertJ, MockMvc, `@DataJpaTest`, `@WebMvcTest` |
| Front-end | React 19, Vite, CSS Modules |
| Infrastructure | Docker Compose (RabbitMQ + `fines-api` database), Maven |
| Version control | Git + GitFlow, Conventional Commits |

---

## Running the project

### Prerequisites

- Java 21
- Maven (or the included `./mvnw` wrapper in each project)
- Node.js and npm
- Docker Desktop
- PostgreSQL running locally on port `5433` (database `library_db`)

### 1. Start the infrastructure (RabbitMQ + `fines-api` database) via Docker Compose

```bash
docker compose up -d
```

This starts RabbitMQ (ports `5672` and `15672`, management UI at **http://localhost:15672**, username/password `guest`/`guest`) and the `fines_db` database (port `5434`).

### 2. Start the services in order

```bash
# 1. Eureka Server
cd discovery-server && ./mvnw spring-boot:run

# 2. Fines microservice
cd fines-api && ./mvnw spring-boot:run

# 3. Monolith
cd library-api && ./mvnw spring-boot:run
```

Confirm service registration at **http://localhost:8761**.

### 3. Start the front-end

```bash
cd library-frontend
npm install
npm run dev
```

Access at **http://localhost:5173**.

---

## Main endpoints

### `library-api` (port 8080)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/books` | Registers a book |
| GET | `/books` | Lists books (filterable by `title`/`author`) |
| GET | `/books/{id}` | Finds a book by ID |
| POST | `/users` | Registers a user |
| GET | `/users` | Lists users |
| POST | `/loans` | Creates a loan |
| PATCH | `/loans/{id}/return` | Registers a return (publishes a `loan.returned` event to RabbitMQ) |
| GET | `/loans/active` | Lists active loans |
| GET | `/loans/{id}/history` | Loan event history |

### `fines-api` (port 8081)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/fines` | Calculates and registers a fine (returns `204` if there is no delay) |
| GET | `/fines` | Lists all fines |
| GET | `/fines/{id}` | Finds a fine by ID |
| GET | `/fines/user/{userId}` | Lists a user's fines |
| PATCH | `/fines/{id}/pay` | Marks a fine as paid |

**Business rule:** default 14-day loan period; a R$ 3.00 fine per overdue day, calculated entirely by `fines-api`.

> As of the fourth delivery, the main trigger for fine creation is consuming the `LoanReturnedEvent` via RabbitMQ, published by `library-api` after a loan return — replacing the synchronous Feign call used through the third delivery.

---

## Automated tests

Each Java service has its own test suite, following the same pattern: Mockito-based unit tests for services, `@DataJpaTest` repository tests (in-memory H2 database), `@WebMvcTest` controller tests, transactional integration tests where applicable, and messaging-specific tests (event publishing in `library-api`; consumption, idempotency, and failure scenarios in `fines-api`).

```bash
cd library-api && ./mvnw test
cd fines-api && ./mvnw test
```

---

## Academic context

Developed for the Scalable Software Engineering course, with a pedagogical focus on: layered architecture, tactical DDD, persistence with Spring Data JPA, automated testing, distributed communication with Spring Cloud (Service Discovery), and event-driven architecture with RabbitMQ (pub/sub, Dead Letter Queue, idempotent consumer).
