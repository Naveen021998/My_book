# Ledger App (Khatabook-Style) Full Implementation Plan

## Document Purpose
This document converts the current blueprint scope into an execution plan for building a production-grade Khatabook-style ledger application with:

- Spring Boot backend services (auth + user + core/resource)
- React Native mobile app (Android-first)
- Relational database (MySQL 8)
- Customer/supplier ledgers, credit/debit entries, settlements
- Reminders and basic reporting/export
- Secure deployment and CI/CD

This plan is written as a delivery document. It focuses on what to build, in what order, with what boundaries, and how to de-risk execution.

Repo Backlog:
- Single documentation file lives in `docs/WORK_LOG.md` (backend + UI + run steps + troubleshooting).
## Current Status Snapshot (Repo Reality)

- Phases 2/3/4/5 are implemented in this repo as of 2026-04-07.
- Phase 7/8 baseline hardening + pilot checklist are now completed (2026-04-07).
- OTP verification is implemented in dev mode (register -> verify OTP -> issue tokens). Wallet/QR/payments, scheduler, and advanced notifications are not implemented yet.
- The single source of truth for run steps and changes is `docs/WORK_LOG.md`.

## Planning Assumptions
- The product is a ledger app to track credit/debit transactions per party (customer/supplier) and compute balances.
- Phase 1 (create/list users) is already completed in this repository.
- Core stack:
  - Backend: Java 17+ (target 21 later), Spring Boot, Spring Security, Spring Data JPA, Flyway, MySQL 8
  - Mobile: React Native, TypeScript
  - Infra: Docker, CI pipeline, centralized logging
- External integrations (optional later): SMS/WhatsApp reminders, export to S3, analytics.

## Product Goals
- Allow users to register/login and manage a set of parties (customers/suppliers).
- Allow users to add ledger entries (gave credit / received payment) and see running balances.
- Provide searchable history, basic reports, and export (CSV/PDF later).
- Maintain security, auditability, and operational visibility.

## Non-Goals for MVP
- Full accounting system (GST/tax, invoicing, stock)
- Multi-branch/enterprise workflows
- Complex offline reconciliation (offline mode can be a later phase)

## High-Level System Architecture
The platform should be split into four main applications from the start:

1. Authorization Service
   - registration and login
   - OTP and token issuance
   - password/PIN management
   - refresh token lifecycle
   - device/session validation

2. User Service
   - user CRUD (phase 1)
   - user profile and settings (me endpoints)
   - user settings and preferences

3. Core Application Service (Resource/Ledger Service)
   - party management (customers/suppliers)
   - ledger entries and balance computation
   - reminders and notifications
   - reporting/export hooks

4. Mobile UI Application
   - onboarding and auth screens
   - create/list user flows for phase 1
   - party list, party details, add-entry flows
   - reports and reminders

Supporting platform components:

4. Data Layer
   - MySQL 8 for transactional data
   - Redis optional later for rate limiting, caching, idempotency
   - Object storage optional later for exports and attachments

5. Integrations
   - payment gateway/banking integration
   - KYC provider
   - OTP/SMS provider
   - push notification provider
   - monitoring/alerting stack

## Service Split
### 1. Authorization Service
Responsibilities:
- user authentication
- access token and refresh token management
- OTP generation and verification
- password/PIN hashing and validation
- session and device trust logic

Recommended modules:
- `auth`
- `otp`
- `token`
- `session`
- `security`
- `common`

Suggested starter endpoints:
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/verify-otp`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`

### 2. User Service
Responsibilities:
- create/list users (phase 1)
- user profile and settings (me endpoints)
- later: KYC hooks and device binding (if needed)

Recommended modules:
- `user`
- `profile`
- `common`

Suggested phase 1 endpoints:
- `POST /api/v1/users`
- `GET /api/v1/users`

### 3. Core Application Service (Resource/Ledger Service)
Responsibilities:
- create/list/update parties (customers/suppliers)
- add/list ledger entries
- compute balances and summaries
- reporting/export
- reminders/notifications

Recommended modules:
- `party`
- `ledger`
- `reporting`
- `reminder`
- `notification`
- `admin`
- `common`

Suggested phase 2 endpoints:
- `POST /api/v1/parties`
- `GET /api/v1/parties`
- `GET /api/v1/parties/{partyId}`
- `POST /api/v1/parties/{partyId}/entries`
- `GET /api/v1/parties/{partyId}/entries`
- `GET /api/v1/reports/summary`

### 3. Mobile UI Application
Responsibilities:
- consume auth and core service APIs
- provide create-user and user-list screens in phase 1
- manage auth state and secure token storage
- provide later payment and wallet flows

Recommended feature groups:
- `auth`
- `users`
- `home`
- `payments`
- `transactions`
- `profile`

## Suggested Backend Structure Per Service
Each backend service should have a similar internal layout:

```text
/src/main/java/com/example/<service-name>
  /config
  /controller
  /service
  /repository
  /entity
  /dto
  /mapper
  /exception
  /security
  /util
/src/main/resources
  application.yml
/src/test
/db/migration
```

## Suggested React Native Mobile Structure
Recommended feature-first structure:

- `src/app`
  - app bootstrap
  - navigation root
  - providers
- `src/features/auth`
  - login
  - signup
  - OTP
  - biometric unlock
- `src/features/onboarding`
  - intro
  - terms
  - KYC steps
- `src/features/home`
  - dashboard
  - balance card
  - quick actions
- `src/features/qr`
  - QR scanner
  - scan permission flow
  - QR payment review
  - QR generation for receive-money
- `src/features/payments`
  - payment confirmation
  - status screen
  - receipt
- `src/features/transactions`
  - transaction list
  - filters
  - transaction details
- `src/features/profile`
  - profile
  - security settings
  - devices
  - limits
- `src/services`
  - api client
  - auth token manager
  - secure storage
  - notification client
- `src/store`
  - global state only where necessary
- `src/components`
  - reusable UI primitives
- `src/theme`
  - typography
  - color tokens
  - spacing
- `src/utils`
  - validators
  - formatters
  - date/currency helpers

## Database Strategy
Use MySQL 8 with explicit schema versioning through Flyway.

Core tables for MVP:

- `users`
  - identity, status, contact information, KYC flags
- `parties`
  - customer/supplier identity and contact info
- `ledger_entries`
  - per-party entries (credit/debit) with notes, timestamps
- `party_balances` (optional optimization)
  - cached running balance per party
- `notifications` (later)
  - reminder delivery state
- `audit_logs`
  - actor, action, entity, before/after summary
- `api_idempotency_keys`
  - request key, operation, response hash, expiry

Key design rules:

- Every money-moving operation must produce immutable ledger entries.
- Business transactions and ledger entries must be linked but stored separately.
- Status fields should use constrained enums or lookup tables.
- Sensitive data must be encrypted or tokenized where appropriate.
- Timestamps should be UTC.
- High-volume tables must be indexed on:
  - user/merchant id
  - transaction reference
  - created_at
  - status
  - provider reference

## Database Design for Scale
To support growth toward 1M+ users:

- Partition `transactions`, `ledger_entries`, and `audit_logs` by month or by time range.
- Use read replicas for reporting and analytics queries.
- Keep OLTP queries narrow and indexed.
- Store raw integration payloads in object storage if DB bloat becomes a problem.
- Archive old audit and reconciliation artifacts.
- Add async reporting pipelines instead of running heavy reports on primary tables.

## Core Functional Flows
### 1. User Registration and Login
1. User enters mobile number/email.
2. User registers or logs in.
3. Profile is created/loaded.

### 2. Party (Customer/Supplier) Management
1. User submits credentials.
2. User creates a party (customer/supplier) with name + phone.
3. User searches parties and opens party details.

### 3. Ledger Entry Flow (Khatabook Core)
1. User opens a party.
2. User adds an entry:
   - Credit: "You gave" (party owes you)
   - Debit: "You got" (party paid you)
3. Backend validates, stores the entry, and updates the party balance.
4. App shows updated running balance and entry list.

### 4. Reports and Reminders (Later)
1. User views summary totals (total receivable/payable).
2. User exports a party ledger as CSV/PDF (later).
3. User triggers reminders (SMS/WhatsApp later).

## API Design Principles
- REST over HTTPS
- Versioned endpoints, for example `/api/v1/...`
- Standard response envelope for errors
- Strong validation with clear field errors
- Idempotency for payment creation and critical write APIs
- Pagination for list endpoints
- Correlation IDs for traceability
- OpenAPI spec maintained with implementation

Recommended endpoint groups:

- Auth
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/verify-otp`
  - `POST /api/v1/auth/login`
  - `POST /api/v1/auth/refresh`
  - `POST /api/v1/auth/logout`
- User
  - `GET /api/v1/users/me`
  - `PATCH /api/v1/users/me`
  - `POST /api/v1/users/me/kyc`
- Wallet
  - `GET /api/v1/wallets/me`
  - `GET /api/v1/wallets/me/balance`
- QR
  - `POST /api/v1/qr/resolve`
  - `POST /api/v1/qr/generate`
- Payments
  - `POST /api/v1/payments`
  - `GET /api/v1/payments/{paymentId}`
- Transactions
  - `GET /api/v1/transactions`
  - `GET /api/v1/transactions/{transactionId}`
- Scheduler/Admin
  - `POST /api/v1/admin/jobs/transactions/sync`
  - `GET /api/v1/admin/reconciliation-runs`

## Security Plan
Security must be built into the first milestone, not added later.

Authentication and authorization:
- JWT access tokens with short TTL
- Refresh token rotation
- Role-based access control for admin and operations
- Device binding for sensitive actions
- Optional biometric unlock on mobile using secure platform storage

Data security:
- TLS everywhere
- Password/PIN hashing with modern algorithms
- Field-level encryption for PII and sensitive identifiers
- Secrets managed in cloud secret manager or vault
- No sensitive payloads in logs

API protection:
- Rate limiting
- WAF/API gateway protection
- Input validation and schema enforcement
- Idempotency keys on critical writes
- Anti-replay and nonce strategy for sensitive operations where needed

Operational security:
- Audit logs for all privileged actions
- Alerting on suspicious login and payment behavior
- Dependency scanning and container image scanning
- Periodic access review

Compliance readiness:
- Data retention policy
- Incident response runbook
- Audit evidence collection
- Regional privacy and payment regulation review before launch

## Reliability and Consistency Plan
- Use transactional boundaries around payment creation and ledger posting.
- Use outbox/event publishing pattern if asynchronous downstream notifications are introduced.
- Distinguish between business transaction state and provider settlement state.
- Build explicit retry policies for external calls.
- Add dead-letter handling for scheduler failures.
- Define reconciliation as a core process, not a support process.

## Caching and Performance
- Redis for:
  - OTP/session support
  - rate limiting
  - idempotency cache
  - frequently read reference data
- Avoid caching mutable balance values without strict invalidation rules.
- Use database indexes and query profiling before adding broad caching.

## Observability
Minimum observability requirements:

- Structured logs with correlation IDs
- Application metrics
  - request latency
  - error rates
  - payment success rate
  - scheduler lag
  - retry counts
- Distributed tracing for payment and sync flows
- Dashboards for:
  - API health
  - database health
  - scheduler health
  - payment funnel
- Alerts for:
  - failed sync jobs
  - elevated payment failure rates
  - auth spikes
  - DB saturation

## Deployment Architecture
Recommended environments:

- `dev`
- `qa`
- `staging`
- `prod`

Deployment components:

- API service containers
- background scheduler worker
- PostgreSQL
- Redis
- API gateway / ingress
- centralized logging and metrics
- secrets manager
- object storage

Deployment approach:

- Containerized builds
- Immutable artifacts
- Rolling or blue/green deployments for backend
- Mobile app distributed through internal QA channels, then store release tracks
- Environment-specific configuration through secrets and config maps

## CI/CD Plan
For backend:
- lint/static analysis
- unit tests
- integration tests
- security scans
- build Docker image
- publish artifact
- deploy to lower environments
- gated promotion to production

For mobile:
- lint
- type checks
- unit/component tests
- build Android/iOS artifacts
- distribute QA builds
- manual approval for production release

Recommended pipeline stages:
1. Validate
2. Test
3. Scan
4. Build
5. Package
6. Deploy
7. Smoke test
8. Promote

## Testing Strategy
### Backend
- Unit tests for domain logic
- Integration tests for repositories and API endpoints
- Contract tests for external integrations
- Scheduler tests with retry and dedupe cases
- Load tests for payment and transaction listing APIs

### Mobile
- Unit tests for utilities and hooks
- Component tests for critical screens
- E2E tests for login, QR payment, and transaction history
- Device testing across low/mid/high-end devices

### System
- End-to-end staging tests against realistic test providers
- Failure injection for external provider latency and downtime
- Reconciliation test scenarios

## Team Plan
Minimum cross-functional team:

- 1 product manager
- 1 solution architect or senior backend lead
- 2 to 3 backend engineers
- 1 to 2 mobile engineers
- 1 QA automation engineer
- 1 DevOps/platform engineer
- 1 security/compliance stakeholder shared across teams

## Delivery Phases
### Phase 0: Discovery and Architecture
Duration: 1 to 2 weeks

Deliverables:
- finalized product scope
- architecture decision records
- environment plan
- integration dependency list
- compliance/risk checklist
- initial data model draft

Exit criteria:
- scope frozen for MVP
- architecture approved
- external dependencies identified

### Phase 1: Basic User Management Flow
Duration: 1 to 2 weeks

Deliverables:
- core application service bootstrap
- mobile UI application bootstrap
- DB migrations setup
- basic user table and migration
- user entity, repository, service, and controller
- `POST /api/v1/users` endpoint to create a user
- `GET /api/v1/users` endpoint to list users
- request validation and standard API error responses
- basic logging and global exception handling
- UI screen to create a user
- UI screen to list users
- API integration between UI and backend
- basic success/error handling in UI

Exit criteria:
- backend can create users in the database
- UI can create a user through the API
- UI can fetch and display the user list
- validation errors are shown correctly
- the full flow works in local/dev environment

Status:
- Completed in this repository (backend + mobile + device install).

Production-readiness note:
- Phase 1 proves the vertical slice. Production readiness requires Phase 3 items (auth-hardening, core auth, CI/CD, ops).

Suggested scope details:
- Backend:
  - fields for v1 user record: `id`, `fullName`, `email`, `phoneNumber`, `status`, `createdAt`
  - pagination can be optional in phase 1, but the API shape should be pagination-ready
  - use DTOs instead of exposing entities directly
- UI:
  - create-user form
  - users list screen
  - loading, empty, success, and error states
  - simple navigation between create and list screens
- Out of scope for phase 1:
  - dedicated authorization service
  - login
  - OTP
  - wallet
  - QR payments
  - scheduler
  - advanced roles and permissions

### Phase 2: Ledger MVP (Khatabook Core)
Status: Completed (2026-04-07)

Duration: 1 to 2 weeks

Deliverables:
- parties CRUD APIs (create/list/get/update)
- ledger entries APIs (add/list)
- balance computation rules (running balance per party)
- mobile UI: party list, party details, add-entry screen
- search/filter for parties and entries
- minimal export (CSV download endpoint) optional

Exit criteria:
- end-to-end ledger flow works on device:
  - create party
  - add credit/debit entries
  - see correct balance and history

### Phase 3: Auth + App Hardening
Status: Completed (2026-04-07)

Duration: 2 to 3 weeks

Deliverables:
- finalize auth flows (register/login/refresh/logout)
- secure core APIs with auth tokens
- rate limiting for auth endpoints
- audit logs for ledger operations
- CI/CD baseline + Docker-based local dev

Exit criteria:
- authenticated ledger operations only
- stable local/dev deployment with repeatable DB migrations

### Phase 4: Reporting and Reminders
Status: Completed (2026-04-07)

Duration: 2 to 3 weeks

Deliverables:
- reporting endpoints (totals, aging buckets simple)
- export (CSV first, PDF later)
- reminders skeleton (store reminder requests and status)
- mobile screens for summary + reminder trigger

Exit criteria:
- reports match ledger balances
- export works for a party ledger

### Phase 5: Offline-First Ledger and Sync
Status: In progress

Duration: 2 to 4 weeks

Deliverables:
- device-local ledger snapshot/storage for parties and entries
- offline create/edit/delete flows on the device
- background sync and retries when online
- server backup/restore after sync
- conflict resolution rules

Exit criteria:
- app can show and edit ledger data offline
- local changes sync safely when connectivity returns
- server remains source of truth after sync

Phase 5 notes:
- Current repo support now includes:
  - auth tokens, UI theme, cached reads, and a ledger write queue stored locally
  - a device-local ledger snapshot for parties and entries
  - background sync that replays queued offline changes and refreshes local backup
  - reminder sync and richer conflict UX are still being tightened

### Phase 7: Security Hardening and Production Readiness
Status: Documented checklist (2026-04-07)

Duration: 2 weeks

Deliverables:
- rate limiting
- secrets hardening
- encryption review
- penetration test fixes
- alerting dashboards
- backup and restore validation
- incident runbooks

Exit criteria:
- production readiness checklist passed
- operational handoff completed

### Phase 8: Pilot Launch
Status: Documented checklist (2026-04-07)

Duration: 1 to 2 weeks

Deliverables:
- limited user rollout
- live monitoring
- support process
- rollback plan

Exit criteria:
- launch KPIs stable
- critical issues resolved or accepted

## Prioritized MVP Backlog
Must-have:
- create user API
- list users API
- create user screen
- user list screen
- user registration and login
- wallet creation
- balance view
- QR scan and pay
- transaction history
- scheduler sync
- notifications
- audit logs
- CI/CD
- observability

Should-have:
- biometric unlock
- merchant QR generation
- admin job dashboard
- reconciliation export

Later:
- rewards/cashback
- referrals
- advanced fraud rules
- merchant settlement dashboards
- multi-currency

## Risk Register
### Technical Risks
- External provider instability
- Incorrect money movement logic
- Poor idempotency design causing duplicate payments
- Scheduler duplication or missed records
- Query degradation as transaction volume grows

Mitigations:
- provider sandbox testing
- ledger-first design
- mandatory idempotency keys
- reconciliation workflow
- load testing and partition-ready schema

### Product and Delivery Risks
- Scope creep
- Compliance surprises late in delivery
- Mobile store release delays
- Ambiguous ownership between payment and reconciliation flows

Mitigations:
- tight MVP definition
- early compliance review
- pilot rollout
- explicit service ownership matrix

## Definition of Done
A feature is complete only if:

- business rules are implemented
- API/mobile behavior is tested
- logs and metrics are added
- failure states are handled
- security implications are reviewed
- documentation is updated
- rollout and rollback considerations are defined if production-impacting

## Recommended Repository Strategy
Two reasonable options:

1. Monorepo
   - backend
   - mobile
   - infra
   - docs
   - shared API contracts

2. Multi-repo
   - backend repo
   - mobile repo
   - infra repo
   - docs/contracts repo

Recommendation:
- Use monorepo for a small team moving quickly.
- Use multi-repo only if organizational boundaries already require it.

## Suggested Folder Layout
```text
/docs
  /architecture
  /api
  /runbooks
/authorization-service
  /src
  /db
  /docker
/user-service
  /src
  /db
  /docker
/core-application-service
  /src
  /db
  /docker
/mobileappnative
  /src
  /android
  /ios
/mobile-app
  (legacy / older scaffold; not the primary app)
/infra
  /terraform or /k8s
/.github or /.gitlab
```

## Immediate Execution Plan

Note: Phases 2-4 are completed in this repo as of 2026-04-07. Phase 5 is now the next active track: offline-first ledger storage on device, sync, and online backup. Phases 7/8 remain baseline hardening and pilot checklist work.

## Immediate Execution Plan
Next (Phase 5: Offline-First Ledger):
- harden the local ledger snapshot layer into a proper embedded DB if needed
- add richer sync/conflict indicators in the UI
- back up and reconcile local changes when online
- make reminder sync/offline behavior consistent

Next (Phase 3/7 hardening):
- token reuse detection, login rate limiting, and logout-all in `authorization-service`
- `GET/PATCH /api/v1/users/me` in `user-service`
- CI pipeline + container builds + basic observability (metrics, logs, tracing IDs)

## AI-Assisted Development Guidance
AI can accelerate scaffolding, DTOs, controllers, test templates, and documentation. It should not be trusted blindly for:

- ledger logic
- payment state transitions
- security configuration
- migration scripts affecting money data
- concurrency-sensitive code

Recommended use:
- generate initial project structure
- generate OpenAPI stubs
- generate boilerplate tests
- generate React Native screen scaffolding
- generate documentation drafts

Mandatory human review areas:
- transactions and ledger consistency
- auth and secrets handling
- database indexing and migration safety
- external provider integration contracts

## Next Documents to Create
After this plan, the highest-value follow-up documents are:

1. Detailed database schema
2. OpenAPI contract
3. Backend module-by-module technical design
4. Mobile navigation and screen map
5. Infra and deployment runbook
6. Security threat model
7. Reconciliation design document

## Final Recommendation
Build the MVP as a modular monolith first, with clean domain boundaries and strong financial consistency rules. Do not start with microservices unless real scale, independent deployment pressure, or team structure justifies the added complexity.

The most important engineering decisions are:

- ledger-first transaction design
- strict idempotency for payment operations
- explicit reconciliation workflow
- production-grade observability from day one
- security and auditability embedded into every module

If needed, this plan can be expanded next into:

- a 1M+ user database schema
- a production-grade Spring Boot project structure
- a React Native starter architecture
- a complete OpenAPI contract
- a single mega-prompt for AI-assisted code generation









### UI Theme Picker (New)
- Add a theme color selector on Login/Register.
- Persist selection in AsyncStorage.
- Apply selected accent color to header gradients and primary CTA buttons.
