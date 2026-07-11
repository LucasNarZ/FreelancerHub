# FreelanceHub - SaaS for Freelancers

## Overview

FreelanceHub is a SaaS platform built to help freelancers manage their business operations in one place. The initial focus is on lead management, client communication, budget/quote creation, and a dashboard with insights derived from the freelancer's own data. Future iterations will add WhatsApp automation and Google Places API integration to help freelancers find and reach potential clients.

## Target Audience

Freelancers who need a lightweight, centralized tool to:
- Track and organize leads
- Communicate with prospects and clients efficiently
- Create and send budgets/quotes
- Understand their business performance through data

## Product Scope

### MVP (Phase 1)

- **Authentication** - User registration and login with JWT (access token + refresh token with rotation)
- **Lead Management** - Create, update, categorize, and track leads through a pipeline (e.g., New, Contacted, Negotiating, Won, Lost)
- **Message Templates** - Create reusable message templates to send to leads/clients (manual copy/send in MVP, no automation yet)
- **Budget/Quote Builder** - Create, edit, and export budgets/quotes tied to a lead or client
- **Dashboard** - Basic analytics and insights based on the user's own data (e.g., conversion rate, leads by status, revenue estimates from quotes)

### Phase 2 (Post-MVP)

- **WhatsApp Integration** - Automate message sending and receiving via WhatsApp Business API (e.g., Twilio, Meta Cloud API, or similar), including automated follow-ups and notifications
- **Google Places API Integration** - Allow freelancers to search for potential leads/businesses by location and category directly within the platform
- Additional automation and enrichment features to be defined as the product evolves

## Tech Stack

### Backend
- **Java + Spring Boot** - REST API
- **Spring Security + JWT** - Authentication and authorization
    - Access token (short-lived)
    - Refresh token (long-lived, stored and rotated on each use to prevent reuse/replay)
- **PostgreSQL** - Primary relational database

### Frontend
- **React** - UI library
- **Vite** - Build tool and dev server

### Infrastructure
- **Docker / Docker Compose** - Containerization for all services (backend, frontend, database, nginx)
- **Nginx** - Reverse proxy, serving the frontend and routing API requests to the backend
- **Redis** - Storage for refresh tokens (and access token blacklist/revocation checks if needed)

### CI/CD
- **GitHub Actions** - Pipeline for running tests, building images, and deploying
- **Deployment** - Self-managed VPS (deploy via SSH/Docker Compose triggered from the pipeline)

### Testing
- **Unit tests** - Backend business logic (services, utilities, token rotation logic, etc.)
- **Integration tests** - Backend endpoints and database interactions (e.g., using Testcontainers with PostgreSQL/Redis)

### Observability
- **Prometheus** - Metrics collection from the backend
- **Grafana** - Dashboards and visualization of metrics/logs
- **Loki** - Log aggregation, integrated with Grafana

## Architecture Overview

```
                        +----------------+
                        |     Nginx      |
                        | (reverse proxy)|
                        +--------+-------+
                                 |
                +----------------+----------------+
                |                                 |
        +-------v-------+                 +-------v-------+
        |  React + Vite  |                |  Spring Boot   |
        |   (frontend)   |                |   (backend)    |
        +---------------+                 +-------+--------+
                                                    |
                                    +---------------+---------------+
                                    |               |               |
                            +-------v------+ +------v------+ +------v------+
                            |  PostgreSQL   | |    Redis     | |  Prometheus  |
                            | (main data)   | | (tokens)     | |  (metrics)   |
                            +---------------+ +-------------+ +------+------+
                                                                      |
                                                              +-------v-------+
                                                              |    Grafana    |
                                                              | (dashboards)  |
                                                              +---------------+
                                                                      ^
                                                              +-------+-------+
                                                              |     Loki      |
                                                              |    (logs)     |
                                                              +---------------+
```

Future integrations (WhatsApp API, Google Places API) will be consumed by the backend as external services, without changing the core architecture.

## Suggested Project Structure

```
freelancehub/
  backend/
    src/
      main/
        java/...
        resources/
          application.yml
    Dockerfile
    pom.xml
  frontend/
    src/
    public/
    Dockerfile
    vite.config.js
    package.json
  nginx/
    nginx.conf
  docker-compose.yml
  .env.example
```

## Core Domain Entities (initial draft)

- **User** - Freelancer account (id, name, email, password hash, created_at)
- **RefreshToken** - Refresh tokens with rotation control, stored in Redis with TTL matching token expiration (key: token_hash or user/session id, value: user_id, expires_at, revoked, replaced_by)
- **Lead** - Potential client (id, user_id, name, contact_info, status, source, notes, created_at, updated_at)
- **MessageTemplate** - Reusable message content (id, user_id, title, content, category)
- **Budget/Quote** - Proposal sent to a lead/client (id, user_id, lead_id, items, total_value, status, created_at)
- **BudgetItem** - Line item within a budget (id, budget_id, description, quantity, unit_price)

## Authentication Flow (JWT with Refresh Token Rotation)

1. User logs in with credentials
2. Backend issues an access token (short-lived, e.g., 15 minutes) and a refresh token (long-lived, e.g., 7 days), storing the refresh token in Redis with a matching TTL
3. Access token is used to authenticate requests to protected endpoints
4. When the access token expires, the client uses the refresh token to request a new pair
5. On each refresh, the old refresh token is invalidated in Redis (rotated) and a new one is issued and stored
6. If a revoked/used refresh token is presented again, the session is invalidated as a security measure (possible token theft detection)

## CI/CD Pipeline (GitHub Actions)

1. On push/PR - run backend unit tests and integration tests
2. On merge to main - build Docker images for backend and frontend
3. Push images to a container registry (e.g., GitHub Container Registry)
4. Deploy to the VPS - connect via SSH and run `docker-compose pull && docker-compose up -d` (or an equivalent deploy script) to update the running containers

## Observability

- **Prometheus** scrapes metrics exposed by the Spring Boot backend (e.g., via Micrometer/Actuator)
- **Grafana** consumes Prometheus as a data source for metrics dashboards, and Loki as a data source for logs
- **Loki** collects and aggregates application logs, queried through Grafana

## Roadmap Summary

| Phase | Features |
|---|---|
| MVP | Auth (JWT + refresh rotation), Lead management, Message templates, Budget/Quote builder, Dashboard |
| Phase 2 | WhatsApp API integration (automated messaging) |
| Phase 2 | Google Places API integration (lead discovery) |
| Future | To be defined based on user feedback |

## Local Development (planned)

- `docker-compose up` should spin up: PostgreSQL, Redis, Spring Boot backend, React/Vite frontend, Nginx as reverse proxy, and the observability stack (Prometheus, Grafana, Loki)
- Environment variables managed via `.env` file (database credentials, JWT secrets, token expiration times)

## Notes for Task Planning

This README is meant to serve as the base reference for breaking down the project into actionable development tasks, covering:
- Infrastructure setup (Docker, Docker Compose, Nginx config)
- Backend setup (Spring Boot project structure, security config, database migrations)
- Authentication module (JWT issuance, refresh token rotation, revocation logic)
- Lead management module (CRUD, pipeline/status logic)
- Message template module (CRUD)
- Budget/Quote module (CRUD, PDF/export if needed later)
- Dashboard module (aggregation queries, charts on frontend)
- Frontend project setup (Vite + React scaffolding, folder structure, linting/formatting config)
- Frontend routing setup (React Router, protected routes based on auth state)
- Frontend authentication (login/register pages, token storage, axios/fetch interceptor for access token refresh)
- Frontend state management setup (context API, Zustand, or Redux, depending on preference)
- Frontend API integration layer (base API client, error handling, request/response typing)
- Lead management UI (list view, create/edit form, pipeline/status view, filters)
- Message templates UI (list, create/edit, copy-to-clipboard action)
- Budget/Quote UI (create/edit form, item list, export/print view)
- Dashboard UI (charts and summary cards consuming backend analytics endpoints)
- Shared UI components (buttons, inputs, modals, tables, layout/navigation)
- Frontend responsive design and styling setup (Tailwind, CSS modules, or styled-components, depending on preference)
- Redis setup (refresh token storage and rotation logic)
- Observability setup (Prometheus metrics, Grafana dashboards, Loki log aggregation)
- Backend unit tests (services, utilities, token rotation logic)
- Backend integration tests (endpoints, database, Redis interactions)
- CI/CD pipeline setup (GitHub Actions workflows for tests, build, and deploy)
- VPS deployment setup (server provisioning, Docker Compose deploy, SSH-based deploy script)
- Phase 2: WhatsApp API integration
- Phase 2: Google Places API integration
