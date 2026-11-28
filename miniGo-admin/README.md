# miniGo-admin

`miniGo-admin` is the administrator authentication microservice for miniGo.

## Stack

- Java 21 and Spring Boot 4
- DDD-style domain, application, adapter, and bootstrap modules
- MyBatis-Plus with the independent `admin_user` table
- Redis-backed refresh-token rotation
- Nacos service discovery and configuration

## API

- `POST /api/admin/auth/login`
- `POST /api/admin/auth/refresh`
- `POST /api/admin/auth/logout`
- `GET /api/admin/auth/me`

The development account is `admin` / `123456`.

## Run Locally

```powershell
mvn clean package
java -jar miniGo-admin-bootstrap/target/miniGo-admin-bootstrap-1.0-SNAPSHOT.jar
```

Required environment variables:

- `MINIGO_DB_PASSWORD`
- `MINIGO_JWT_SECRET`

When the service runs in Docker, it registers its container address
automatically. When it runs directly on the host, set
`MINIGO_ADMIN_DISCOVERY_IP` to an address reachable from the gateway container.

The service starts on port `8082` and registers in Nacos as `mini-go-admin`.

## Run In Docker

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\deploy.ps1
```

The container is attached to `minigo-app-net` and exposes port `8082` only
inside that network. Do not publish it on the host. Access administrator APIs
through the gateway at `http://127.0.0.1:9000/api/admin/**`.
