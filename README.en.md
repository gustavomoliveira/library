# pblibrary — Library Management System

Library management system built as the integrative project for the **Scalable Software Engineering** course. The project was deliberately evolved from a layered monolith into a microservices architecture, applying Domain-Driven Design, Spring Cloud, and distributed communication concepts in practice.

> 🇧🇷 Leia em português: [README.md](./README.md)

---

## About the project

The system supports book and user registration, loan and return management, per-loan audit history, and automatic calculation of overdue fines — the latter implemented as an independent microservice, communicating with the monolith through Service Discovery (Eureka) and a Feign Client.

The project was built across three progressive deliveries:

1. **Layered monolith** — Spring Boot, Controller/Service/Repository, domain-driven modeling, React front-end consuming the API.
2. **Real persistence layer** — JPA/Spring Data, loan audit history, full automated test suite (unit, `@DataJpaTest`, `@WebMvcTest`, integration).
3. **Microservice extraction** — creation of `fines-api` as an independent service, with its own database, communicating via Spring Cloud (Eureka + OpenFeign), with resilience to network failures.

---

## Architecture

```
                         ┌─────────────────────┐
                         │   discovery-server  │
                         │   (Eureka Server)   │
                         │      porta 8761     │
                         └───────────┬─────────┘
                                     │  registro / descoberta
                    ┌────────────────┴─────────────────┐
                    │                                  │
          ┌─────────▼──────────┐             ┌─────────▼──────────┐
          │    library-api     │──Feign────▶ │     fines-api      │
          │   (monólito)       │  (multas)   │  (microsserviço)   │
          │    porta 8080      │             │    porta 8081      │
          └─────────┬──────────┘             └─────────┬──────────┘
                    │                                   │
          ┌─────────▼──────────┐             ┌─────────▼──────────┐
          │  PostgreSQL        │             │  PostgreSQL        │
          │  library_db        │             │  fines_db (Docker) │
          │  porta 5433        │             │  porta 5434        │
          └────────────────────┘             └────────────────────┘

          ┌─────────────────────┐
          │  library-frontend   │
          │  (React + Vite)     │
          │  porta 5173         │
          └─────────────────────┘
                consome library-api (8080)
                e fines-api (8081) diretamente
```

### Key architectural decisions

- **Monolith First**: `Book`, `User`, and `Loan` remain in the monolith because `Loan` atomically depends on both within a `@Transactional` boundary — extracting them would introduce real distributed-consistency issues (loss of atomicity, need for a Saga pattern) without a proportional benefit.
- **Fines as a microservice**: an isolated subdomain, event-triggered (on loan return), with its own fully separate database, with the fine-calculation rule encapsulated exclusively inside the service itself — the monolith only sends raw data (dates), never knowing *how* the fine is calculated.
- **Resilience**: the call from the monolith to `fines-api` is wrapped in exception handling — if `fines-api` is unavailable, the book return (the core functionality) still completes normally; the failure is only logged.
- **No shared library between services**: each microservice re-implements its own exception classes and DTOs, even at the cost of minor duplication — a conscious trade-off to keep services independently deployable.

---

## Repository structure

```
/library
  /library-api          → monolith (Book, User, Loan)
  /fines-api             → fine-calculation microservice
  /discovery-server        → Eureka Server (Service Discovery)
  /library-frontend          → React + Vite front-end
```

Each Java project is an **independent** Maven module (no parent aggregator `pom.xml`), reflecting the philosophy of independently deployable microservices.

---

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 4.0.x, Spring Data JPA, Spring Cloud (Netflix Eureka, OpenFeign) |
| Persistence | PostgreSQL (production), H2 (tests) |
| Testing | JUnit 5, Mockito, AssertJ, MockMvc, `@DataJpaTest`, `@WebMvcTest` |
| Front-end | React 19, Vite, CSS Modules |
| Infrastructure | Docker (`fines-api` database), Maven |
| Version control | Git + GitFlow, Conventional Commits |

---

## Running the project

### Prerequisites

- Java 21
- Maven (or the included `./mvnw` wrapper in each project)
- Node.js and npm
- Docker Desktop
- PostgreSQL running locally on port `5433` (database `library_db`)

### 1. Start the `fines-api` database via Docker

```bash
docker run --name fines-postgres \
  -e POSTGRES_DB=fines_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5434:5432 \
  -v fines-postgres-data:/var/lib/postgresql/data \
  -d postgres:17
```

On subsequent runs, just use `docker start fines-postgres`.

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
| PATCH | `/loans/{id}/return` | Registers a return (triggers a notification to `fines-api` when overdue) |
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

**Business rule:** default 14-day loan period; a R$ 3.00 fine per overdue day, calculated entirely by `fines-api` from the `loanDate` and `returnDate` received from the monolith.

---

## Automated tests

Each Java service has its own test suite, following the same pattern: Mockito-based unit tests for services, `@DataJpaTest` repository tests (in-memory H2 database), `@WebMvcTest` controller tests, and transactional integration tests where applicable.

```bash
cd library-api && ./mvnw test
cd fines-api && ./mvnw test
```

---

## Academic context

Developed for the Scalable Software Engineering course, with a pedagogical focus on: layered architecture, tactical DDD, persistence with Spring Data JPA, automated testing, and distributed communication with Spring Cloud (Service Discovery and Feign Client).
