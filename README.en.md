# pblibrary — Library Management System

Library management system built as the integrative project for the **Scalable Software Engineering** course (Instituto Infnet), progressively evolved, across five deliveries, from a simple monolith into an event-driven, containerized, observable microservices architecture.

> 🇧🇷 Leia em português: [README.md](README.md)

## Table of contents

- [Architecture overview](#architecture-overview)
- [Services and ports](#services-and-ports)
- [Tech stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Running with Docker Compose](#running-with-docker-compose)
- [Running with Kubernetes](#running-with-kubernetes-practice-exercise)
- [Monitoring](#monitoring)
- [CI/CD](#cicd)
- [Repository structure](#repository-structure)
- [Main endpoints](#main-endpoints)
- [Project history](#project-history)
- [Known limitations](#known-limitations)

## Architecture overview

The system is made up of two business domains (`library-api` and `fines-api`), communicating asynchronously via RabbitMQ, with the supporting infrastructure typical of a Spring Cloud microservices architecture: service discovery, centralized configuration, and an API Gateway as the single entry point.

```
                         ┌──────────────────┐
                         │  library-frontend │  (React)
                         └─────────┬────────┘
                                   │ HTTP
                                   ▼
                         ┌──────────────────┐
                         │    api-gateway    │  (Spring Cloud Gateway)
                         └─────────┬────────┘
                     ┌─────────────┴─────────────┐
                     ▼                           ▼
             ┌───────────────┐           ┌───────────────┐
             │  library-api  │           │   fines-api   │
             │  (monolith)   │──events──▶│ (microservice)│
             └───────┬───────┘  RabbitMQ └───────┬───────┘
                     │                           │
                     ▼                           ▼
             ┌───────────────┐           ┌───────────────┐
             │  library-db   │           │   fines-db    │
             │  (Postgres)   │           │  (Postgres)   │
             └───────────────┘           └───────────────┘

        Infrastructure shared by all the services above:
        ┌──────────────────┐   ┌──────────────────┐
        │  discovery-server │   │   config-server   │
        │   (Eureka)         │   │ (Spring Cloud Config,│
        │                    │   │  Git-backed)         │
        └──────────────────┘   └──────────────────┘
```

`library-api` is the system's original monolith — book, user, and loan management — which stays deliberately monolithic (`Book`, `User`, and `Loan` are strongly, transactionally coupled; splitting them apart would require Saga patterns without a proportional benefit at this scale). `fines-api` was extracted as the system's only microservice, responsible for calculating and managing overdue fines, reacting to loan events published by `library-api` via RabbitMQ — there is no synchronous call between the two business domains.

`discovery-server` (Netflix Eureka) lets services discover each other by name, with no fixed hostports. `config-server` (Spring Cloud Config) centralizes non-sensitive configuration for all services — ports, other services' URLs, feature flags — in its own Git repository (`config-repo/`), served at runtime; passwords and other credentials never live in that repository. `api-gateway` (Spring Cloud Gateway Server WebMVC) is the system's single external HTTP entry point: it routes `/books`, `/users`, and `/loans` to `library-api`, and `/fines` to `fines-api`, resolving each request's actual destination via Eureka.

## Services and ports

| Service | Port | Description |
|---|---|---|
| `library-frontend` | 5173 | Web UI (React) |
| `api-gateway` | 8082 | Single HTTP entry point |
| `library-api` | 8080 | Monolith: books, users, loans |
| `fines-api` | 8081 | Microservice: fines |
| `discovery-server` | 8761 | Eureka — service discovery |
| `config-server` | 8888 | Centralized configuration |
| `library-db` | 5435 → 5432 | Postgres for `library-api` |
| `fines-db` | 5434 → 5432 | Postgres for `fines-api` |
| `rabbitmq` | 5672 / 15672 | Messaging / admin dashboard |
| `zipkin` | 9411 | Distributed tracing |
| `loki` | 3100 | Log storage |
| `grafana` | 3000 | Log and metrics visualization |

## Tech stack

- **Backend:** Java 21, Spring Boot 4, Spring Cloud (Config, Gateway, Netflix Eureka, OpenFeign where applicable), Spring Data JPA, Hibernate, PostgreSQL, RabbitMQ (Spring AMQP)
- **Frontend:** React, Vite
- **Observability:** Micrometer Tracing + Zipkin, Grafana + Loki + Promtail
- **Infrastructure:** Docker, Docker Compose, Kubernetes (hand-written manifests, no Helm)
- **CI:** GitHub Actions
- **Build:** Maven (backend), npm (frontend)

## Prerequisites

- Docker Desktop (with Kubernetes enabled, only if you're going to use that part)
- Java 21 and Maven, only to run a service outside a container
- Node 22, only to run the frontend outside a container

## Running with Docker Compose

This is the main, recommended way to run the whole system locally.

```bash
docker compose up -d --build
```

This brings up the system's 12 containers (6 application services + 2x Postgres, RabbitMQ, Zipkin, Loki, Promtail, Grafana) in the correct dependency order — `config-server` must respond on its health endpoint before the other services try to fetch their configuration.

After a few seconds, check that everything came up:

```bash
docker compose ps
```

All services should show status `Up` (`config-server` should show `(healthy)`).

Access the application at **http://localhost:5173**.

To tear everything down:

```bash
docker compose down
```

## Running with Kubernetes (practice exercise)

The manifests under `k8s/` reproduce the same architecture above on a local Kubernetes cluster (tested with the Kubernetes bundled in Docker Desktop). This part isn't used for any graded delivery — it was built as a practice exercise for this skill.

```bash
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/rabbitmq.yaml
kubectl apply -f k8s/fines-db.yaml
kubectl apply -f k8s/library-db.yaml
kubectl apply -f k8s/config-server.yaml
kubectl apply -f k8s/discovery-server.yaml
kubectl apply -f k8s/fines-api.yaml
kubectl apply -f k8s/library-api.yaml
kubectl apply -f k8s/api-gateway.yaml
kubectl apply -f k8s/library-frontend.yaml
```

```bash
kubectl get pods
```

The frontend is exposed via `NodePort` at **http://localhost:30080**. The other services are internal to the cluster (`ClusterIP`) and, for manual inspection, can be reached via `kubectl port-forward service/<name> <port>:<port>`.

Every image used in the manifests (`library-config-server`, `library-discovery-server`, etc.) needs to already exist locally — built via `docker compose build` — since the manifests use `imagePullPolicy: Never` (no image is published to an external registry).

## Monitoring

Monitoring runs via Docker Compose (not replicated in Kubernetes).

**Distributed tracing:** access Zipkin at **http://localhost:9411**. Every request that crosses `api-gateway`, `library-api`, and `fines-api` is traced (100% sampling, suitable for a development/demo environment). Use "Find a trace" to query by service.

**Log aggregation:** access Grafana at **http://localhost:3000** (no login required — anonymous auth is enabled for this local environment). Go to **Explore**, select the **Loki** data source, and use the `container` label to filter logs for any service in the system. Promtail automatically collects logs from every running Docker container, with no per-service configuration needed.

## CI/CD

The repository uses GitHub Actions with one workflow per service (`.github/workflows/ci-*.yml`), triggered by a push or pull request that changes files in that specific service. Each workflow:

1. Runs the service's automated tests and Maven build (or `npm run build`, for the frontend).
2. Confirms that the service's `Dockerfile` builds successfully.

There is no continuous deployment (CD) stage: the pipeline stops at build validation, without publishing images to any registry — this delivery does not include cloud infrastructure to receive an automated deployment.

## Repository structure

```
library/
├── config-server/       # Spring Cloud Config Server
├── config-repo/         # Own Git repository with the configs served by config-server
├── discovery-server/    # Eureka
├── api-gateway/         # Spring Cloud Gateway
├── library-api/         # Monolith: books, users, loans
├── fines-api/           # Microservice: fines
├── library-frontend/    # React frontend
├── k8s/                 # Kubernetes manifests
├── .github/workflows/   # CI workflows
├── docker-compose.yml
├── promtail-config.yml
└── docs/                # Architecture documentation and evidence from earlier deliveries
```

## Main endpoints

All accessed through `api-gateway` (port 8082):

| Method | Route | Description |
|---|---|---|
| `GET` | `/books` | List books |
| `POST` | `/books` | Create a book |
| `GET` | `/users` | List users |
| `POST` | `/users` | Create a user |
| `GET` | `/loans` | List loans |
| `POST` | `/loans` | Register a loan |
| `GET` | `/fines` | List fines |

## Project history

This repository documents the evolution of the course's integrative project across five successive deliveries:

1. **Simple monolith** — layered Spring Boot, first domain modeling.
2. **Real persistence layer** — JPA/Hibernate, data history, automated tests.
3. **Building a microservice** — `fines-api` extraction, synchronous communication via Feign.
4. **Event-driven architecture** — replacing Feign with asynchronous events via RabbitMQ (see `ARQUITETURA-EVENTOS.md`).
5. **Deployment and operations** (this delivery) — containerization with Docker, Kubernetes (practice), monitoring (tracing and logs), and CI.

## Known limitations

- The containerized databases (`library-db`, `fines-db`) start empty — data used during local development was not migrated into the containers.
- There is no Bean Validation, pagination, Swagger/OpenAPI, or Spring Security implemented — out of scope for the deliveries completed so far.
- The Transactional Outbox pattern (to guarantee event publication even if RabbitMQ is unavailable) was identified as a future improvement, but not implemented.
