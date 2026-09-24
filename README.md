# pblibrary — Sistema de Gestão de Biblioteca

Sistema de gestão de biblioteca desenvolvido como projeto integrador da disciplina de **Engenharia de Softwares Escaláveis**. O projeto evoluiu deliberadamente de um monólito em camadas para uma arquitetura de microsserviços orientada a eventos, aplicando na prática conceitos de Domain-Driven Design, Spring Cloud e comunicação distribuída.

> 🇬🇧 Read this in English: [README.en.md](README.en.md)

---

## Sobre o projeto

O sistema permite o cadastro de livros e usuários, controle de empréstimos e devoluções, histórico de auditoria de cada empréstimo, e cálculo automático de multas por atraso na devolução — este último implementado como um microsserviço independente, comunicando-se com o monólito de forma **assíncrona**, via RabbitMQ.

O projeto foi construído em quatro entregas progressivas:

1. **Monólito em camadas** — Spring Boot, Controller/Service/Repository, modelagem DDD por domínio, front-end React consumindo a API.
2. **Persistência real** — JPA/Spring Data, histórico de auditoria de empréstimos, testes automatizados completos (unitários, `@DataJpaTest`, `@WebMvcTest`, integração).
3. **Extração de microsserviço** — criação do `fines-api` como serviço independente, com banco de dados próprio, comunicação inicial via Spring Cloud (Eureka + OpenFeign), e resiliência a falhas de rede.
4. **Arquitetura orientada a eventos** — a comunicação síncrona via Feign foi substituída por publicação/consumo de eventos via RabbitMQ, desacoplando `library-api` de `fines-api` também no tempo (não apenas em rede) e implementando Dead Letter Queue e consumidor idempotente. Detalhes completos, incluindo cenários de falha validados, em [`ARQUITETURA-EVENTOS.md`](./ARQUITETURA-EVENTOS.md).

---

## Arquitetura

```
                         ┌─────────────────────┐
                         │   discovery-server  │
                         │   (Eureka Server)   │
                         │      porta 8761     │
                         └───────────┬─────────┘
                                     │  registro
                    ┌────────────────┴─────────────────┐
                    │                                  │
          ┌─────────▼──────────┐             ┌─────────▼──────────┐
          │    library-api     │             │     fines-api      │
          │   (monólito)       │             │  (microsserviço)   │
          │    porta 8080      │             │    porta 8081      │
          └─────────┬──────────┘             └─────────┬──────────┘
                    │                                   │
                    │ publica evento            consome evento
                    │ (loan.returned)                   │
                    └───────────────┬───────────────────┘
                                    ▼
                        ┌───────────────────────┐
                        │       RabbitMQ         │
                        │  exchange: library.events │
                        │  fila: fines.loan-returned │
                        │  DLQ: fines.loan-returned.dlq │
                        └───────────────────────┘

          ┌────────────┐                     ┌────────────┐
          │ PostgreSQL │                     │ PostgreSQL │
          │ library_db │                     │  fines_db  │
          │ porta 5433 │                     │ porta 5434 │
          └────────────┘                     └────────────┘

          ┌─────────────────────┐
          │  library-frontend   │
          │  (React + Vite)     │
          │  porta 5173         │
          └─────────────────────┘
                consome library-api (8080)
                e fines-api (8081) diretamente
```

### Decisões arquiteturais principais

- **Monolith First**: `Book`, `User` e `Loan` permanecem no monólito porque `Loan` depende atomicamente dos outros dois dentro de uma transação (`@Transactional`) — extraí-los geraria problemas reais de consistência distribuída (perda de atomicidade, necessidade de Saga pattern) sem ganho proporcional.
- **Fines como microsserviço**: subdomínio isolado, com gatilho por evento (na devolução do empréstimo), banco de dados próprio e completamente separado, e regra de cálculo de multa encapsulada exclusivamente no próprio serviço — o monólito envia apenas dados brutos (datas), nunca sabe *como* a multa é calculada.
- **Comunicação orientada a eventos (RabbitMQ)**: `library-api` publica um evento de domínio (`LoanReturnedEvent`) em um exchange topic após o commit da devolução do empréstimo; `fines-api` consome esse evento de forma independente. Isso substitui a chamada síncrona via Feign usada até a terceira entrega — a devolução do livro não depende mais da disponibilidade do `fines-api` no mesmo instante. Detalhamento completo (incluindo trade-offs, Dead Letter Queue e idempotência do consumidor) em [`ARQUITETURA-EVENTOS.md`](./ARQUITETURA-EVENTOS.md).
- **Sem biblioteca compartilhada entre serviços**: cada microsserviço recria suas próprias classes de exceção e DTOs (e, agora, sua própria cópia do contrato de evento), mesmo que isso gere pequena duplicação — trade-off consciente para manter os serviços deployáveis de forma independente.

---

## Estrutura do repositório

```
/library
  /library-api          → monólito (Book, User, Loan)
  /fines-api             → microsserviço de cálculo de multas
  /discovery-server        → Eureka Server (Service Discovery)
  /library-frontend          → front-end React + Vite
  docker-compose.yml         → RabbitMQ + banco de dados do fines-api
```

Cada projeto Java é um módulo Maven **independente** (sem `pom.xml` pai agregador), refletindo a filosofia de microsserviços implantáveis separadamente.

---

## Stack técnica

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 4.0.x, Spring Data JPA, Spring Cloud (Netflix Eureka), Spring AMQP |
| Mensageria | RabbitMQ (exchange topic, Dead Letter Queue) |
| Persistência | PostgreSQL (produção), H2 (testes) |
| Testes | JUnit 5, Mockito, AssertJ, MockMvc, `@DataJpaTest`, `@WebMvcTest` |
| Front-end | React 19, Vite, CSS Modules |
| Infraestrutura | Docker Compose (RabbitMQ + banco do `fines-api`), Maven |
| Versionamento | Git + GitFlow, Conventional Commits (mensagens em português) |

---

## Como rodar o projeto

### Pré-requisitos

- Java 21
- Maven (ou usar o wrapper `./mvnw` incluso em cada projeto)
- Node.js e npm
- Docker Desktop
- PostgreSQL rodando localmente na porta `5433` (banco `library_db`)

### 1. Suba a infraestrutura (RabbitMQ + banco do `fines-api`) via Docker Compose

```bash
docker compose up -d
```

Isso sobe o RabbitMQ (portas `5672` e `15672`, painel de administração em **http://localhost:15672**, usuário/senha `guest`/`guest`) e o banco `fines_db` (porta `5434`).

### 2. Suba os serviços na ordem

```bash
# 1. Eureka Server
cd discovery-server && ./mvnw spring-boot:run

# 2. Microsserviço de multas
cd fines-api && ./mvnw spring-boot:run

# 3. Monólito
cd library-api && ./mvnw spring-boot:run
```

Confirme o registro dos serviços em **http://localhost:8761**.

### 3. Suba o front-end

```bash
cd library-frontend
npm install
npm run dev
```

Acesse em **http://localhost:5173**.

---

## Endpoints principais

### `library-api` (porta 8080)

| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/books` | Cadastra um livro |
| GET | `/books` | Lista livros (filtros por `title`/`author`) |
| GET | `/books/{id}` | Busca livro por ID |
| POST | `/users` | Cadastra um usuário |
| GET | `/users` | Lista usuários |
| POST | `/loans` | Cria um empréstimo |
| PATCH | `/loans/{id}/return` | Registra a devolução (publica evento `loan.returned` no RabbitMQ) |
| GET | `/loans/active` | Lista empréstimos ativos |
| GET | `/loans/{id}/history` | Histórico de eventos do empréstimo |

### `fines-api` (porta 8081)

| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/fines` | Calcula e registra uma multa (retorna `204` se não houver atraso) |
| GET | `/fines` | Lista todas as multas |
| GET | `/fines/{id}` | Busca multa por ID |
| GET | `/fines/user/{userId}` | Lista multas de um usuário |
| PATCH | `/fines/{id}/pay` | Marca a multa como paga |

**Regra de negócio:** prazo padrão de empréstimo de 14 dias; multa de R$ 3,00 por dia de atraso, calculada inteiramente pelo `fines-api`.

> A partir da quarta entrega, o principal gatilho de criação de multa passou a ser o consumo do evento `LoanReturnedEvent` via RabbitMQ, publicado por `library-api` após a devolução do empréstimo — em vez da chamada síncrona via Feign usada até a terceira entrega.

---

## Testes automatizados

Cada serviço Java possui suíte própria de testes, seguindo o mesmo padrão: testes unitários de service com Mockito, testes de repositório com `@DataJpaTest` (banco H2 em memória), testes de controller com `@WebMvcTest`, testes de integração transacional onde aplicável, e testes específicos de mensageria (publicação de evento em `library-api`; consumo, idempotência e cenário de falha em `fines-api`).

```bash
cd library-api && ./mvnw test
cd fines-api && ./mvnw test
```

---

## Contexto acadêmico

Projeto desenvolvido para a disciplina de Engenharia de Softwares Escaláveis, com foco pedagógico em: arquitetura em camadas, DDD tático, persistência com Spring Data JPA, testes automatizados, comunicação distribuída com Spring Cloud (Service Discovery), e arquitetura orientada a eventos com RabbitMQ (pub/sub, Dead Letter Queue, consumidor idempotente).
