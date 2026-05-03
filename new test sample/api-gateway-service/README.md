# api-gateway-service (No Docker)

This service replaces the nginx gateway container when you want to run everything on your PC.

It provides a **single entry URL** on port `8080` and routes to local services:

- `/api/v1/auth/**` -> `http://localhost:8081`
- `/api/v1/users/**` -> `http://localhost:8084`
- `/api/v1/notifications/**` -> `http://localhost:8085`
- `/api/v1/files/**` -> `http://localhost:8086`
- `/api/v1/**` -> `http://localhost:8082` (core catch-all)

Swagger passthrough:

- `/swagger/auth/**` -> auth service
- `/swagger/users/**` -> user service
- `/swagger/core/**` -> core service
- `/swagger/notifications/**` -> notification service
- `/swagger/files/**` -> file service

## Run

```powershell
cd api-gateway-service
mvn spring-boot:run
```

Health:

```powershell
Invoke-RestMethod http://localhost:8080/health
```

## Config

You can override upstreams using environment variables:

- `AUTH_UPSTREAM` (example: `http://localhost:8081`)
- `CORE_UPSTREAM`
- `USER_UPSTREAM`
- `NOTIFICATION_UPSTREAM`
- `FILE_UPSTREAM`

