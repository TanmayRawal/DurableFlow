# DurableFlow

DurableFlow is a fault-tolerant workflow execution engine for developer-defined DAGs. Definitions and task state are persisted; work is coordinated through expiring leases, bounded retries, and recovery of abandoned tasks.

## What it does

This repository provides a real Spring Boot API and operations dashboard for creating, validating, and running workflow definitions.

- Validates unique node identifiers.
- Rejects edges that point to nonexistent nodes.
- Rejects cyclic workflows using depth-first graph traversal.
- Persists immutable, versioned definitions in PostgreSQL through Flyway migrations.
- Exposes OpenAPI-ready JSON endpoints.
- Creates a persisted task for each node and marks only dependency-free nodes `READY`.
- Progresses dependent tasks to `READY` only after every predecessor succeeds.
- Requires idempotency keys for run creation and returns the existing run for a duplicate request.
- Uses expiring worker leases, heartbeats, exponential retry backoff, and dead-lettering for failed work.
- Uses a transactional outbox to publish task-ready events to Redis Streams after database state is committed.
- Includes an opt-in Redis Stream worker with a deterministic `noop` handler for end-to-end demos; production handlers are registered explicitly by handler type.

## Architecture now

```text
React operations dashboard
             |
       REST / WebSocket
             |
 Spring Boot modular monolith
     | workflow-definition module
     | orchestration module (Milestone 2)
     | worker-runtime module (Milestone 3)
             |
       PostgreSQL + Flyway
             |
       transactional outbox -> Redis Streams
```

The first release intentionally uses a modular monolith. A single deployable process makes transactions, debugging, and local development straightforward; its clear module boundaries allow the scheduler and worker runtime to split into services only if scale requires it.

## Run locally

Prerequisites: Java 21, Maven 3.9+, Docker Desktop.

```bash
docker compose up --build
```

This starts PostgreSQL, Redis, the API + worker at `http://localhost:8080`, and the dashboard at `http://localhost:8081`. Use `noop` as a node handler type for the first end-to-end workflow; it is the intentionally deterministic demo handler. Stop the stack with `docker compose down`.

### Dashboard

```bash
cd frontend
pnpm install
pnpm dev
```

The Vite development server proxies `/api` to the Spring Boot service on port 8080. Paste a workflow run ID into the console to inspect its live task state and safely claim, heartbeat, or complete work as the console operator. The console subscribes to `/topic/runs/{runId}` over STOMP/WebSocket and updates after committed state transitions.

Create a definition, then start a run:

```bash
curl -X POST http://localhost:8080/api/workflows \
  -H "Content-Type: application/json" \
  -d '{
    "name": "order-fulfilment",
    "description": "Example dependency graph",
    "graph": {
      "nodes": [
        {"key": "validate", "handlerType": "noop"},
        {"key": "charge", "handlerType": "noop"},
        {"key": "receipt", "handlerType": "noop"}
      ],
      "edges": [
        {"from": "validate", "to": "charge"},
        {"from": "charge", "to": "receipt"}
      ]
    }
  }'
```

Copy the returned definition `id` into this request. The returned run `id` is the value to open in the dashboard.

```bash
curl -X POST http://localhost:8080/api/workflows/<definition-id>/runs \
  -H "Idempotency-Key: demo-order-001" \
  -H "Content-Type: application/json" \
  -d '{"orderId":"demo-001"}'
```

## API

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/workflows` | Validate and store version 1 of a workflow definition |
| `GET` | `/api/workflows/{id}` | Read a stored definition |
| `POST` | `/api/workflows/validate` | Validate a graph without storing it |
| `POST` | `/api/workflows/{id}/runs` | Start a persisted workflow run |
| `POST` | `/api/runs/{runId}/tasks/{taskId}/claim` | Transition a ready task to running |
| `POST` | `/api/runs/{runId}/tasks/{taskId}/complete` | Complete a task and schedule eligible dependents |
| `POST` | `/api/runs/{runId}/tasks/{taskId}/heartbeat` | Extend the lease for the owning worker |
| `POST` | `/api/runs/{runId}/tasks/{taskId}/fail` | Retry or dead-letter a failed task |
| `GET` | `/api/runs/{runId}` | Read task states for one workflow run |

## Verification

The deterministic unit suite covers DAG cycle validation and task state-machine transitions. A Testcontainers integration test covers persisted workflow execution when Docker Desktop is running:

```bash
cd backend
mvn test
```

The dashboard type-checks and builds as a production bundle:

```bash
cd frontend
pnpm build
```

## Design decisions

- **Transactional outbox:** a task is stored and its dispatch intent is written in the same database transaction; a scheduler publishes that intent to Redis Streams after commit.
- **At-least-once workers:** workers must tolerate duplicate deliveries. The lease owner guards task transitions; abandoned leases become retries with exponential backoff and eventually dead-letter.
- **Idempotent starts:** a definition ID and idempotency key uniquely identify a run request, preventing accidental duplicate workflow instances.
- **Modular monolith:** workflow definition, orchestration, dispatch, worker runtime, and realtime modules are separable without premature distributed-system complexity.
