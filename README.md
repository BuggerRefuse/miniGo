# miniGo

`miniGo` uses a monorepo layout. All microservices live in the same repository
and share the same environment branches.

## Modules

- `miniGo-client`: user account and authentication service.
- `miniGo-admin`: administrator authentication service.
- `miniGo-gateway`: API gateway, routing, JWT validation, and access logging.

## Branch Model

- `main`: stable releases.
- `dev`: shared integration branch for all services.
- Feature branches: created from `dev` for a change, regardless of which
  service is affected. They are merged back into `dev` and are not kept as
  long-lived module branches.

## Build

Build all services from the repository root:

```powershell
mvn clean test
```

Build one service from its module directory:

```powershell
cd miniGo-client
mvn clean test
```

```powershell
cd miniGo-admin
mvn clean test
```

```powershell
cd miniGo-gateway
mvn clean test
```

## Local Deployment

The client, admin, and gateway keep their own `compose.yaml` and deployment
scripts inside their module directories. All services use the shared Docker
network `minigo-app-net` and discover each other through Nacos.
