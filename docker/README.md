# PropMange Deployment

The prod stack is defined in the **homelab repo**, not here. bee-hive pulls this repo and builds the API image from `prop-manager/Dockerfile`. This file describes what that compose has to provide.

## Architecture

- **Frontend**: Cloudflare Pages builds `prop-manager-ui`. It authenticates with Logto directly (PKCE).
- **API**: this repo's `Dockerfile` (Spring Boot, `SPRING_PROFILES_ACTIVE=prod`).
- **Edge** (homelab): a shared compose with cloudflared, Caddy and Logto on a public network. The API container joins that network.
- **Database** (homelab): a shared PostgreSQL instance on a separate db network. The API container joins that network as well.

## API container environment

| Variable               | Value                                                     |
| ---------------------- | --------------------------------------------------------- |
| `POSTGRES_HOST`        | Shared Postgres container name                            |
| `POSTGRES_DB`          | `propmanager`                                             |
| `POSTGRES_USER`        | Dedicated prop-mange role (not the superuser)             |
| `POSTGRES_PASSWORD`    | That role's password                                      |
| `AUTH_ISSUER_URI`      | `https://auth.akandiah.ca/oidc` (must match the `iss` claim) |
| `JWKS_URI`             | `http://<logto-container>:3001/oidc/jwks` (internal)      |
| `AUTH_AUDIENCE`        | Prod Logto API resource identifier (= `VITE_OIDC_RESOURCE`) |
| `CORS_ALLOWED_ORIGINS` | `https://<UI_DOMAIN>`                                     |
| `APP_BASE_URL`         | `https://<UI_DOMAIN>`                                     |
| `AUTH_LOGOUT_URL`      | `https://auth.akandiah.ca/oidc/session/end`               |
| `MAIL_PASSWORD`        | SMTP password (Resend API key by default)                 |

Optional: `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_FROM`. The app refuses to start without `AUTH_AUDIENCE`.

Liquibase creates the schema on first start (`ddl-auto: validate`). The DB role needs to own the `propmanager` database.

Healthcheck: `wget -qO- http://localhost:8080/actuator/health/liveness || exit 1`.

## Edge routing (`API_DOMAIN` → `app:8080`)

- Forward only `/api/*` and `/actuator/health*`. Return 404 for everything else, including Swagger, `/v3/api-docs` and the rest of `/actuator`.
- TLS ends at Cloudflare. Caddy listens on `http://` and passes `X-Forwarded-Proto https`.
- Spring Boot handles CORS. Don't add CORS headers in Caddy.

## Logto (shared tenant)

Prod has its own SPA app and its own API resource on the shared tenant. The API resource identifier must equal `AUTH_AUDIENCE` and `VITE_OIDC_RESOURCE`, and it needs an `access` permission because the frontend requests the `<resource>/access` scope. The tenant-wide custom JWT script adds `email`, `email_verified`, `name` and `roles`. `/api/me` rejects tokens that don't carry a verified email.

## Files here

- `init-db/01-create-databases.sh`: creates the `propmanager` user and DB for the devcontainer Postgres.
- `clear-app-db.sh`: truncates the dev-oidc app tables.
