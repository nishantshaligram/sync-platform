# Sync Platform

A microservices-based platform for syncing commerce data (e.g. **Shopify**) with accounting platforms (e.g. **QuickBooks Online**), with built-in authentication, subscription/billing, and scheduled + manual sync orchestration.

Built with **Spring Boot 3 / Spring Cloud** (Java 21) on the backend and **React + Vite** on the frontend.

---

## Architecture

```
                         ┌──────────────┐
                         │   Frontend    │  React + Vite (http://localhost:5173)
                         └──────┬───────┘
                                │
                         ┌──────▼───────┐
                         │   Gateway     │  Spring Cloud Gateway (http://localhost:8080)
                         └──────┬───────┘
        ┌──────────┬────────────┼────────────┬──────────────┐
        │          │            │             │              │
 ┌──────▼─────┐ ┌──▼─────────┐ ┌▼───────────┐ ┌▼────────────┐ ┌▼───────────────┐
 │ auth-service│ │subscription│ │shopify-conn│ │qbo-connector│ │ sync-core-service│
 │  (JWT/JWKS) │ │  -service  │ │  -service  │ │  -service   │ │ (connections,    │
 └─────────────┘ └────────────┘ └────────────┘ └─────────────┘ │  sync history)   │
                                                                  └────────┬─────────┘
                                                                           │
                                                                  ┌────────▼─────────┐
                                                                  │ scheduler-service │
                                                                  │ (cron -> Kafka)   │
                                                                  └───────────────────┘

  Service discovery: Eureka Server (http://localhost:8761)
  Centralized config: Config Server (http://localhost:8888), backed by ./config-repo

  Infra: PostgreSQL, MongoDB, Redis, Kafka + Zookeeper, Elasticsearch
```

### Services

| Service                     | Port | Responsibility                                              |
|------------------------------|------|--------------------------------------------------------------|
| `config-server`              | 8888 | Spring Cloud Config server, serves `./config-repo`           |
| `eureka-server`               | 8761 | Service discovery / registry                                  |
| `gateway`                      | 8080 | Single entry point, routes requests to services by path       |
| `auth-service`                | 8081 | Signup/login, JWT issuance, JWKS endpoint                      |
| `subscription-service`        | 8082 | Plans, limits, Stripe checkout & webhooks                      |
| `shopify-connector-service`    | 8083 | Shopify OAuth connection + webhook ingestion                  |
| `qbo-connector-service`        | 8084 | QuickBooks Online OAuth connection                             |
| `sync-core-service`            | 8085 | Sync connections, manual sync, sync history/search             |
| `scheduler-service`            | —    | Polls due sync schedules and publishes Kafka events            |
| `frontend`                     | 5173 | React SPA (login, dashboard, connection wizard, details)       |

### Infrastructure dependencies

| Service        | Port  | Used for                                  |
|----------------|-------|--------------------------------------------|
| PostgreSQL     | 5432  | Primary relational data (users, connections, subscriptions...) |
| MongoDB        | 27017 | Sync run documents / event history          |
| Redis          | 6379  | Caching, locks, quota counters              |
| Kafka + Zookeeper | 9092 / 29092 / 2181 | Async sync-run events between services |
| Elasticsearch  | 9200  | Searchable sync run history                 |

### Gateway routes (`config-repo/gateway.yml`)

| Path prefix        | Routes to                  |
|---------------------|-----------------------------|
| `/auth/**`           | `auth-service`              |
| `/subscriptions/**`  | `subscription-service`      |
| `/shopify/**`        | `shopify-connector-service`  |
| `/qbo/**`            | `qbo-connector-service`      |
| `/connections/**`    | `sync-core-service`          |

---

## Prerequisites

- **Docker** and **Docker Compose**
- **Java 21** and **Maven** (only needed if building services outside Docker)
- **Node.js 18+** and **npm** (for the frontend dev server)

---

## Configuration

1. Copy the example env file and fill in your own credentials:

   ```bash
   cp .env.example .env
   ```

2. Edit `.env` with your sandbox/test credentials:

   | Variable | Description |
   |---|---|
   | `QBO_CLIENT_ID` / `QBO_CLIENT_SECRET` / `QBO_REDIRECT_URI` | QuickBooks Online developer app credentials |
   | `SHOPIFY_CLIENT_ID` / `SHOPIFY_CLIENT_SECRET` / `SHOPIFY_REDIRECT_URI` | Shopify custom app credentials |
   | `STRIPE_SECRET_KEY` / `STRIPE_WEBHOOK_SECRET` | Stripe test-mode API key and webhook signing secret |

   The platform will still boot without real values (placeholders are used as defaults in `docker-compose.yml`), but OAuth and billing flows will fail until real test credentials are supplied.

3. Shared, non-secret config for all services lives in [`config-repo/`](config-repo) (`application.yml`, `gateway.yml`) and is served by `config-server`.

---

## Running the platform

### Option A — Quick start (recommended for day-to-day use)

```powershell
.\start.ps1
```

This runs `docker compose up -d` for the backend, waits for the gateway to report healthy, and starts the frontend dev server (`npm run dev`).

```powershell
.\stop.ps1
```

Stops the frontend dev server and runs `docker compose stop` for the backend.

### Option B — Full manual bring-up (staged startup)

If you're starting from a clean/stale environment (e.g. leftover Kafka/Zookeeper volumes causing cluster-ID mismatches on restart), bring the stack up in stages so each dependency is healthy before the next layer starts.

1. **Tear down and clear stale Kafka/Zookeeper state:**

   ```bash
   docker-compose down
   docker volume rm sync-platform_kafka_data sync-platform_zookeeper_data
   ```

2. **Start Zookeeper and wait ~20s** for it to initialize:

   ```bash
   docker-compose up -d zookeeper
   ```

3. **Start the remaining infrastructure and wait ~30s:**

   ```bash
   docker-compose up -d kafka postgres mongodb redis elasticsearch
   ```

4. **Start config & discovery and wait ~30s:**

   ```bash
   docker-compose up -d config-server eureka-server
   ```

5. **Build and start all application services:**

   ```bash
   docker-compose up -d --build gateway auth-service subscription-service shopify-connector-service qbo-connector-service sync-core-service scheduler-service
   ```

6. **Verify everything is up:**

   ```bash
   docker-compose ps
   ```

7. **Start the frontend dev server:**

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

> Why the staged approach? Kafka depends on Zookeeper having a stable cluster/broker registration before it starts, and the application services depend on `config-server` + `eureka-server` being registered and healthy before they can fetch config and register themselves. Bringing everything up at once after a `down` can leave Kafka or the app services in a crash-loop until Zookeeper/Eureka are ready — removing the old Kafka/Zookeeper volumes avoids stale cluster-ID errors after a `down`.

---

## Accessing the app

| What | URL |
|---|---|
| Frontend (SPA) | http://localhost:5173 |
| API Gateway | http://localhost:8080 |
| Eureka dashboard | http://localhost:8761 |
| Config Server | http://localhost:8888 |

### Typical user flow

1. **Sign up** via the frontend (or `POST /auth/signup`). A verification link is logged to the `auth-service` container logs (`docker logs auth-service`) — MVP behavior, no real email is sent.
2. **Verify** the account via the logged link (`GET /auth/verify?token=...`).
3. **Log in** (`POST /auth/login`) to receive a JWT, used as a Bearer token for all other services.
4. **Activate a plan** — `POST /subscriptions/demo` for the free demo plan, or `POST /subscriptions/checkout` to start a Stripe-based upgrade.
5. **Connect platforms** — use the Shopify/QBO OAuth authorize endpoints (`/shopify/oauth/authorize`, `/qbo/oauth/authorize`) to link source/destination accounts.
6. **Create a sync connection** (`POST /connections`) pairing a source and destination account, with a sync interval and backfill window.
7. **Trigger syncs** manually (`POST /connections/{id}/sync`) or let `scheduler-service` trigger them automatically based on each connection's schedule.
8. **View history** via `GET /connections/{id}/sync-history` and `/sync-history/search`.

---

## API testing

A ready-to-import Postman collection covering the full flow above (auth, subscriptions, connector OAuth, and sync connections, with automatic JWT/ID chaining) is available at:

```
postman/sync-platform.postman_collection.json
```

Import it into Postman and run requests top-to-bottom within each folder.

---

## Project structure

```
sync-platform/
├── auth-service/              # Signup/login, JWT issuance, JWKS
├── config-repo/               # Shared Spring Cloud Config files
├── config-server/             # Spring Cloud Config Server
├── eureka-server/              # Service registry
├── gateway/                     # Spring Cloud Gateway (entry point)
├── qbo-connector-service/      # QuickBooks Online OAuth + integration
├── shopify-connector-service/  # Shopify OAuth, webhooks + integration
├── subscription-service/        # Plans, limits, Stripe billing
├── sync-core-service/           # Sync connections, orchestration, history
├── scheduler-service/           # Polls due schedules, publishes Kafka events
├── frontend/                     # React + Vite SPA
├── postman/                       # Postman collection for API testing
├── docker-compose.yml           # Infra + service orchestration
├── start.ps1 / stop.ps1          # Convenience scripts (Windows)
└── .env.example                  # Required environment variables template
```

---

## Tech stack

- **Backend:** Java 21, Spring Boot 3.3, Spring Cloud 2023.0.2 (Gateway, Config, Eureka, OAuth2 Resource Server)
- **Frontend:** React 19, Vite
- **Data stores:** PostgreSQL, MongoDB, Redis, Elasticsearch
- **Messaging:** Apache Kafka + Zookeeper
- **Integrations:** Shopify OAuth/Webhooks, QuickBooks Online OAuth, Stripe Billing
