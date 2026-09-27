# pblibrary — Sistema de Gestão de Biblioteca

Sistema de gestão de biblioteca desenvolvido como projeto integrador da disciplina de **Engenharia de Softwares Escaláveis** (Instituto Infnet), evoluindo progressivamente, em cinco entregas, de um monólito simples até uma arquitetura de microsserviços orientada a eventos, conteinerizada e observável.

> 🇬🇧 Read this in English: [README.en.md](README.en.md)

## Sumário

- [Visão geral da arquitetura](#visão-geral-da-arquitetura)
- [Serviços e portas](#serviços-e-portas)
- [Stack tecnológica](#stack-tecnológica)
- [Pré-requisitos](#pré-requisitos)
- [Como rodar com Docker Compose](#como-rodar-com-docker-compose)
- [Como rodar com Kubernetes](#como-rodar-com-kubernetes-exercício-de-prática)
- [Monitoramento](#monitoramento)
- [CI/CD](#cicd)
- [Estrutura do repositório](#estrutura-do-repositório)
- [Endpoints principais](#endpoints-principais)
- [Histórico evolutivo do projeto](#histórico-evolutivo-do-projeto)
- [Limitações conhecidas](#limitações-conhecidas)

## Visão geral da arquitetura

O sistema é composto por dois domínios de negócio (`library-api` e `fines-api`), comunicando-se de forma assíncrona via RabbitMQ, com toda a infraestrutura de suporte típica de uma arquitetura de microsserviços em Spring Cloud: descoberta de serviços, configuração centralizada e um API Gateway como ponto único de entrada.

```
                         ┌──────────────────┐
                         │ library-frontend │  (React)
                         └─────────┬────────┘
                                   │ HTTP
                                   ▼
                         ┌──────────────────┐
                         │    api-gateway   │  (Spring Cloud Gateway)
                         └─────────┬────────┘
                     ┌─────────────┴─────────────┐
                     ▼                           ▼
             ┌───────────────┐           ┌───────────────┐
             │  library-api  │           │   fines-api   │
             │  (monólito)   │──eventos─▶│(microsserviço)│
             └───────┬───────┘  RabbitMQ └───────┬───────┘
                     │                           │
                     ▼                           ▼
             ┌───────────────┐           ┌───────────────┐
             │  library-db   │           │   fines-db    │
             │  (Postgres)   │           │  (Postgres)   │
             └───────────────┘           └───────────────┘

        Infraestrutura compartilhada por todos os serviços acima:
        ┌──────────────────┐   ┌──────────────────┐
        │ discovery-server │   │   config-server  │
        │   (Eureka)       │   │(Spring Cloud Config,│
        │                  │   │  Git-backed)     │
        └──────────────────┘   └──────────────────┘
```

`library-api` é o monólito original do sistema — cadastro de livros, usuários e empréstimos — que permanece monolítico deliberadamente (`Book`, `User` e `Loan` têm forte acoplamento transacional entre si; decompor exigiria Saga patterns sem benefício proporcional nesta escala). `fines-api` foi extraído como o único microsserviço do sistema, responsável pelo cálculo e gestão de multas por atraso, reagindo a eventos de empréstimo publicados pelo `library-api` via RabbitMQ — não há chamada síncrona entre os dois domínios de negócio.

`discovery-server` (Netflix Eureka) permite que os serviços se descubram entre si pelo nome, sem hostports fixos. `config-server` (Spring Cloud Config) centraliza a configuração não sensível de todos os serviços — portas, URLs de outros serviços, feature flags — num repositório Git próprio (`config-repo/`), servida em tempo de execução; senhas e outras credenciais nunca ficam nesse repositório. `api-gateway` (Spring Cloud Gateway Server WebMVC) é o único ponto de entrada HTTP do sistema visto de fora: roteia `/books`, `/users`, `/loans` para o `library-api` e `/fines` para o `fines-api`, resolvendo o destino real de cada requisição via Eureka.

## Serviços e portas

| Serviço | Porta | Descrição |
|---|---|---|
| `library-frontend` | 5173 | Interface web (React) |
| `api-gateway` | 8082 | Ponto único de entrada HTTP |
| `library-api` | 8080 | Monólito: livros, usuários, empréstimos |
| `fines-api` | 8081 | Microsserviço: multas |
| `discovery-server` | 8761 | Eureka — descoberta de serviços |
| `config-server` | 8888 | Configuração centralizada |
| `library-db` | 5435 → 5432 | Postgres do `library-api` |
| `fines-db` | 5434 → 5432 | Postgres do `fines-api` |
| `rabbitmq` | 5672 / 15672 | Mensageria / painel de administração |
| `zipkin` | 9411 | Rastreamento distribuído (tracing) |
| `loki` | 3100 | Armazenamento de logs |
| `grafana` | 3000 | Visualização de logs e métricas |

## Stack tecnológica

- **Backend:** Java 21, Spring Boot 4, Spring Cloud (Config, Gateway, Netflix Eureka, OpenFeign onde aplicável), Spring Data JPA, Hibernate, PostgreSQL, RabbitMQ (Spring AMQP)
- **Frontend:** React, Vite
- **Observabilidade:** Micrometer Tracing + Zipkin, Grafana + Loki + Promtail
- **Infraestrutura:** Docker, Docker Compose, Kubernetes (manifests próprios, sem Helm)
- **CI:** GitHub Actions
- **Build:** Maven (backend), npm (frontend)

## Pré-requisitos

- Docker Desktop (com Kubernetes habilitado, apenas se for usar essa parte)
- Java 21 e Maven, apenas para rodar algum serviço fora de container
- Node 22, apenas para rodar o frontend fora de container

## Como rodar com Docker Compose

Esta é a forma principal e recomendada de executar o sistema completo localmente.

```bash
docker compose up -d --build
```

Isso sobe os 12 containers do sistema (6 serviços da aplicação + Postgres x2, RabbitMQ, Zipkin, Loki, Promtail, Grafana), na ordem correta de dependência — o `config-server` precisa responder no endpoint de saúde antes que os demais serviços tentem buscar sua configuração.

Após alguns segundos, valide a subida:

```bash
docker compose ps
```

Todos os serviços devem aparecer com status `Up` (o `config-server` deve mostrar `(healthy)`).

Acesse a aplicação em **http://localhost:5173**.

Para derrubar tudo:

```bash
docker compose down
```

## Como rodar com Kubernetes (exercício de prática)

Os manifests em `k8s/` reproduzem a mesma arquitetura acima num cluster Kubernetes local (testado com o Kubernetes embutido no Docker Desktop). Esta parte não é usada para nenhuma entrega ou avaliação formal — foi construída como exercício de prática dessa competência.

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

O frontend é exposto via `NodePort` em **http://localhost:30080**. Os demais serviços são internos ao cluster (`ClusterIP`) e, para inspeção manual, podem ser alcançados via `kubectl port-forward service/<nome> <porta>:<porta>`.

Cada imagem usada nos manifests (`library-config-server`, `library-discovery-server`, etc.) precisa já existir localmente — construída via `docker compose build` — já que os manifests usam `imagePullPolicy: Never` (nenhuma imagem é publicada em registry externo).

## Monitoramento

O monitoramento roda via Docker Compose (não replicado em Kubernetes).

**Rastreamento distribuído (tracing):** acesse o Zipkin em **http://localhost:9411**. Todas as requisições que atravessam o `api-gateway`, `library-api` e `fines-api` são rastreadas (amostragem de 100%, adequada para ambiente de desenvolvimento/demonstração). Use "Find a trace" para consultar por serviço.

**Agregação de logs:** acesse o Grafana em **http://localhost:3000** (sem necessidade de login — autenticação anônima habilitada para este ambiente local). Vá em **Explore**, selecione a fonte de dados **Loki**, e use o rótulo `container` para filtrar os logs de qualquer serviço do sistema. O Promtail coleta automaticamente os logs de todos os containers Docker em execução, sem necessidade de configuração por serviço.

## CI/CD

O repositório usa GitHub Actions com um workflow por serviço (`.github/workflows/ci-*.yml`), disparado por push ou pull request que altere arquivos daquele serviço específico. Cada workflow:

1. Roda os testes automatizados e o build Maven (ou `npm run build`, no caso do frontend) do serviço.
2. Confirma que o `Dockerfile` do serviço builda com sucesso.

Não há etapa de deploy contínuo (CD): a esteira termina na validação de build, sem publicar imagens em nenhum registry — esta entrega não inclui infraestrutura de nuvem para receber um deploy automatizado.

## Estrutura do repositório

```
library/
├── config-server/       # Spring Cloud Config Server
├── config-repo/         # Repositório Git próprio com as configs servidas pelo config-server
├── discovery-server/    # Eureka
├── api-gateway/         # Spring Cloud Gateway
├── library-api/         # Monólito: livros, usuários, empréstimos
├── fines-api/           # Microsserviço: multas
├── library-frontend/    # Frontend React
├── k8s/                 # Manifests Kubernetes
├── .github/workflows/   # Workflows de CI
├── docker-compose.yml
├── promtail-config.yml
└── docs/                # Documentação de arquitetura e evidências das entregas anteriores
```

## Endpoints principais

Todos acessados através do `api-gateway` (porta 8082):

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/books` | Lista livros |
| `POST` | `/books` | Cadastra livro |
| `GET` | `/users` | Lista usuários |
| `POST` | `/users` | Cadastra usuário |
| `GET` | `/loans` | Lista empréstimos |
| `POST` | `/loans` | Registra empréstimo |
| `GET` | `/fines` | Lista multas |

## Histórico evolutivo do projeto

Este repositório documenta a evolução do projeto integrador da disciplina ao longo de cinco entregas sucessivas:

1. **Monólito simples** — Spring Boot em camadas, primeira modelagem de domínio.
2. **Camada de persistência real** — JPA/Hibernate, histórico de dados, testes automatizados.
3. **Criação de um microsserviço** — extração do `fines-api`, comunicação síncrona via Feign.
4. **Arquitetura orientada a eventos** — substituição do Feign por eventos assíncronos via RabbitMQ (ver `ARQUITETURA-EVENTOS.md`).
5. **Implantação e operação** (esta entrega) — conteinerização com Docker, Kubernetes (prática), monitoramento (tracing e logs) e CI.

## Limitações conhecidas

- Os bancos de dados conteinerizados (`library-db`, `fines-db`) iniciam vazios — os dados usados durante o desenvolvimento em ambiente local não foram migrados para os containers.
- Não há Bean Validation, paginação, Swagger/OpenAPI ou Spring Security implementados — fora do escopo definido para as entregas realizadas.
- O padrão Transactional Outbox (para garantir a publicação de eventos mesmo em caso de indisponibilidade do RabbitMQ) foi identificado como melhoria futura, mas não implementado.
