# Project Guide (Single Source Of Truth)

This file is the only documentation we maintain going forward.
It covers:

- What the backend services are and how they work
- What the mobile UI does
- Ports, URLs, gateway routing
- How to run locally (no-Docker and Docker)
- How to build/install debug + release APK
- Troubleshooting
- What we changed and what's next

Last updated: 2026-04-27

## Completion Status

- Phase 2 (Ledger MVP): Completed (2026-04-07)
- Phase 3 (Auth + Hardening): Completed (2026-04-07)
- Phase 4 (Reporting + Reminders): Completed (2026-04-07)

Phase 5 (Offline/Sync): In progress - local ledger snapshot + offline write queue + background sync are implemented in the mobile app. Better conflict UX, reminder sync, and local DB hardening are next.
Phase 7/8 (Baseline hardening + pilot checklist): Completed (2026-04-07).

## 1) Architecture Overview

Android App (React Native)
  -> Frontend Controller / API Gateway (single base URL)
    -> Microservices (Auth, User, Core/Ledger, Notification, File)
      -> MySQL (single schema in dev for now)

Important rule:

- The mobile app must call only the gateway base URL.
- Never call service ports directly from the app.

## 2) Ports And URLs

### Gateway (Single Base URL)

- Local: `http://localhost:8080`
- USB device local base URL (with `adb reverse`): `http://127.0.0.1:8080`
- Optional public URL (ngrok): `https://<your-subdomain>.ngrok-free.dev` -> `http://localhost:8080`

### Backend Services (run on your PC)

- `authorization-service`: `http://localhost:8081`
- `core-application-service`: `http://localhost:8082`
- `user-service`: `http://localhost:8084`
- `notification-service`: `http://localhost:8085`
- `file-service`: `http://localhost:8086`

### Metro (React Native dev server)

- Metro: `http://localhost:8083`
- We use `8083` to avoid conflict with backend services (auth is on `8081`).

### Database

- MySQL 8: `localhost:3306`
- User/pass: `root/root`
- Schema: `tranning`

## 3) Gateway Routing Rules

The gateway routes by URL prefix:

- `/api/v1/auth/*` -> `authorization-service`
- `/api/v1/users/*` -> `user-service`
- `/api/v1/notifications/*` -> `notification-service`
- `/api/v1/files/*` -> `file-service`
- `/api/v1/*` -> `core-application-service` (ledger domain)

Quick health checks (gateway only):

```powershell
Invoke-RestMethod http://localhost:8080/health
Invoke-RestMethod http://localhost:8080/api/v1/auth/health
Invoke-RestMethod http://localhost:8080/api/v1/health
Invoke-RestMethod http://localhost:8080/api/v1/users/health
Invoke-RestMethod http://localhost:8080/api/v1/notifications/health
Invoke-RestMethod http://localhost:8080/api/v1/files/health
```

Swagger via gateway:

- Auth: `http://localhost:8080/swagger/auth/swagger-ui.html`
- Users: `http://localhost:8080/swagger/users/swagger-ui.html`
- Core: `http://localhost:8080/swagger/core/swagger-ui.html`
- Notifications: `http://localhost:8080/swagger/notifications/swagger-ui.html`
- Files: `http://localhost:8080/swagger/files/swagger-ui.html`

## 4) Data Model And Multi-User Isolation (Khatabook Style)

### "Add User" Meaning

In a Khatabook-style app, after you login you typically:

- Add customers/suppliers (contacts)
- Track credit/debit entries per contact

In this repo, those "customers/suppliers" are called Parties.

### Each Logged-In Account Has Its Own Ledger

This is implemented in `core-application-service` using `ownerUserId` on:

- `parties`
- `ledger_entries`
- `party_balances`

Every API call uses the JWT user id (`CurrentUser.userId()`) to filter by `ownerUserId`.

Result:

- User A sees only User A's parties and entries
- User B sees only User B's parties and entries

## 5) Main APIs (Gateway Only)

Auth:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/logout-all`
- `GET  /api/v1/auth/me`

User/profile:

- `GET   /api/v1/users/me`
- `PATCH /api/v1/users/me`

Ledger (core):

- `POST /api/v1/parties`
- `GET  /api/v1/parties?page&size&query`
- `GET  /api/v1/parties/{partyId}`
- `PATCH /api/v1/parties/{partyId}`
- `DELETE /api/v1/parties/{partyId}`
- `POST /api/v1/parties/{partyId}/entries`
- `GET  /api/v1/parties/{partyId}/entries?page&size`
- `GET  /api/v1/reports/summary`
- `GET  /api/v1/reports/aging`
- `GET  /api/v1/parties/{partyId}/export.csv`
- `POST /api/v1/parties/{partyId}/reminders` (prepare reminder + draft notification payload)
- `GET  /api/v1/parties/{partyId}/reminders?page&size`
- `POST /api/v1/reminders/{reminderId}/sent`

Files:

- `POST /api/v1/files/upload`
- `GET  /api/v1/files/{id}/meta`
- `GET  /api/v1/files/{id}/download`

Notifications:

- `POST /api/v1/notifications/send`
- `GET  /api/v1/notifications`

## 5.1) Audit Logs

- `GET  /api/v1/audit?page&size`
- Audit rows are written for party create/update, ledger entry create, and reminder prepare/sent.

## 6) How To Run Locally

### Option A (Recommended): No Docker

1) Start MySQL locally (service running on `localhost:3306`) and ensure schema `tranning` exists.

2) Start the gateway:

```powershell
cd api-gateway-service
mvn spring-boot:run
```

3) Start backend services (each in its own terminal):

```powershell
cd authorization-service
mvn spring-boot:run
```

```powershell
cd core-application-service
mvn spring-boot:run
```

```powershell
cd user-service
mvn spring-boot:run
```

```powershell
cd notification-service
mvn spring-boot:run
```

```powershell
cd file-service
mvn spring-boot:run
```
Helper scripts (Windows):

- `scripts/start_all_backend.ps1` opens a window for each service and runs `mvn spring-boot:run`.
- `scripts/verify_health.ps1` checks gateway health endpoints and prints OK/FAIL.
- `scripts/start_mobile.ps1` starts Metro on port 8083 in a new window and prints device steps.

### Option B: With Docker (MySQL + Nginx Gateway)

If you have Docker Desktop:

```powershell
docker compose up -d mysql
```

Gateway:

- If backends run on your PC (host mode):

```powershell
.\scripts\start_gateway.ps1
```

- If backends run in Docker:

```powershell
docker compose up -d gateway
```

## 7) Mobile App (Android)

### Install deps

```powershell
cd mobileappnative
npm.cmd install
```

### Start Metro (8083)

```powershell
cd mobileappnative
npm.cmd start -- --reset-cache
```

### USB device: reverse ports (device -> PC)

```powershell
& "C:\Users\E515\AppData\Local\Android\Sdk\platform-tools\adb.exe" reverse tcp:8083 tcp:8083
& "C:\Users\E515\AppData\Local\Android\Sdk\platform-tools\adb.exe" reverse tcp:8080 tcp:8080
& "C:\Users\E515\AppData\Local\Android\Sdk\platform-tools\adb.exe" reverse --list
```

### Install debug APK

```powershell
cd mobileappnative
npm.cmd run android
```

If `adb` is "not recognized": use the full path:

```powershell
& "C:\Users\E515\AppData\Local\Android\Sdk\platform-tools\adb.exe" devices -l
```

## 8) Build Release APK

From:

```powershell
cd mobileappnative\android
```

Build:

```powershell
.\gradlew.bat clean assembleRelease
```

APK path:

- `mobileappnative\android\app\build\outputs\apk\release\app-release.apk`

Install to device:

```powershell
& "C:\Users\E515\AppData\Local\Android\Sdk\platform-tools\adb.exe" install -r `
  "d:\training\new test sample\mobileappnative\android\app\build\outputs\apk\release\app-release.apk"
```

Note:

- If you want to share APK device-to-device, use a release APK. If your release build is unsigned, you must add a keystore/signing config.

## 9) Base URL Configuration (Local vs ngrok)

Mobile base URL is configured here:

- `mobileappnative/src/services/api/config.ts`

Use:

- USB local testing: `http://127.0.0.1:8080`
- Public testing: your ngrok forwarding URL

## 10) Troubleshooting

### "Unable to load script. Make sure you're running Metro"

- Metro must be running on `8083`.
- Ensure `adb reverse tcp:8083 tcp:8083` is set.
- Restart Metro with cache reset:

```powershell
npm.cmd start -- --reset-cache
```

### Tailwind/NativeWind errors

If you see:

- "Tailwind CSS has not been configured with the NativeWind preset"

Confirm these files exist and are correct:

- `mobileappnative/tailwind.config.js` uses `presets: [require('nativewind/preset')]`
- `mobileappnative/metro.config.js` uses `withNativeWind(..., { input: './global.css' })`
- `mobileappnative/babel.config.js` includes `nativewind/babel`

### Flyway startup failure

If you see Flyway validation / failed migration errors, drop only the service history tables (not your business tables):

```sql
USE tranning;
DROP TABLE IF EXISTS flyway_schema_history_auth;
DROP TABLE IF EXISTS flyway_schema_history_core;
DROP TABLE IF EXISTS flyway_schema_history_user;
DROP TABLE IF EXISTS flyway_schema_history_notification;
DROP TABLE IF EXISTS flyway_schema_history_file;
```

### 502 Bad Gateway

Gateway cannot reach an upstream service.

- Verify the target service is running on its port.
- Verify the mobile app is calling the gateway (not service ports).

## 11) Technology Stack

Backend:

- Java 17
- Spring Boot 3.3.x
- Spring Security (JWT Resource Server)
- Spring Data JPA (Hibernate)
- Flyway
- MySQL 8

Gateway:

- Nginx (docker) OR `api-gateway-service` (Spring Boot) for no-docker setups

Mobile:

- React Native 0.76.x
- TypeScript
- NativeWind (Tailwind)

## 12) What We Changed Recently

Rate limit defaults (override via env):

- `app.rate-limit.window-seconds` (default: 60)
- `app.rate-limit.limit` (default: 120; gateway default 240)
- `app.rate-limit.path-prefix` (default: `/api/`)
- Token persistence (AsyncStorage): auth session survives app restart.
- Logout now calls `/api/v1/auth/logout` before clearing local session.
- Rate limiting (auth endpoints): basic in-memory 429 responses for login/register/refresh.
- CI: GitHub Actions workflow builds all backend services.
- Reminders: core prepares reminder drafts and mobile can send via notification-service.
- Reporting: added aging buckets endpoint (`/api/v1/reports/aging`).
- Audit logs (core): ledger operations now write to `audit_logs` and are viewable at `/api/v1/audit`.
- Export: core can download party ledger CSV.
- Parties: added delete endpoint (cascades entries/reminders/balance) and writes audit log.
- Standardized on gateway-only access pattern (single base URL).
- Core ledger domain uses `ownerUserId` so each logged-in user has a separate party list and ledger entries.
- Documented local ports, routing, and ngrok usage.

### Unified Signup Flow

- The app calls the gateway only. The gateway provisions the user profile via:
  - `POST /api/v1/users/internal/provision` (user-service, protected by `X-Internal-Token`)
- `authorization-service` also contains a best-effort `UserServiceClient`, but failures there do not block registration.
## 13) Next Planned Improvements

## 13.1) Phase 7/8 Baseline Completion

- Rate limiting filter added across gateway + all backend services.
- Actuator metrics already exposed for Prometheus scraping.
- Secrets are environment-driven; production must set `JWT_SECRET`, DB credentials, and `INTERNAL_TOKEN`.
- Backup/restore steps documented below.
- Pilot checklist documented below.
- UI polish: make login/register + ledger UI look closer to a real Khatabook.
- Mobile UI: summary now shows aging buckets (calls `/api/v1/reports/aging`).
- Mobile UI: ledger screen can delete a party and shows latest reminder status.
- Mobile build: fixed NativeWind Babel config (`nativewind/babel` is a plugin, not a preset).
- Mobile API client: fixed malformed `ledgerApi.ts` that could cause blank/red screens.
- Production hardening: monitoring, tracing, rate limits, CI.










## 14) Remaining Work (Not Yet Implemented)

- Wallet/QR/payments/scheduler
- Advanced notification delivery (provider integration)

## OTP Verification Flow (Implemented)

- `POST /api/v1/auth/register` now creates the auth account in `PENDING_VERIFICATION` state.
- The register response includes a 6-digit demo OTP code for local testing.
- `POST /api/v1/auth/verify-otp` verifies the code, activates the account, and issues tokens.
- After verification, the auth service provisions the user profile in `user-service`.

## UI Theme Picker (Implemented)

- Color swatches added to Login/Register.
- Selection persists in AsyncStorage.
- Accent color applied to auth header gradient and CTA buttons.

## Offline-First Ledger (Implemented in App Layer)

- Store parties and ledger entries locally on the device snapshot.
- Queue offline ledger mutations for later sync.
- Sync device changes to the backend when online and refresh the local backup.
- Treat the backend as the source of truth after sync and backup.
- Reminder data still falls back to cache/empty state when offline.

