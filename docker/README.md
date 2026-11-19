# PropMange Deployment

The prod stack is defined in the **homelab repo**, not here. bee-hive clones this repo (to `~/apps/prop-manager`). The homelab's `deploy.sh` pulls it, builds the image from the `Dockerfile` (tagged `prop-mange-api:<commit>` and `:current`) and restarts the stack. This file describes what that compose has to provide.

## Architecture

- **Frontend**: Cloudflare Pages builds `prop-manager-ui`. It authenticates with Logto directly (PKCE).
- **API**: this repo's `Dockerfile` (Spring Boot, `SPRING_PROFILES_ACTIVE=prod`).
- **Edge** (homelab): cloudflared and Caddy on the `public` network. Caddy also joins `prop-mange` to reach the API, which is not on `public`.
- **Auth** (homelab): the shared Logto. The API reaches it for JWKS over the `auth` network.
- **Database** (homelab): a shared PostgreSQL 18 instance on the `db` network.

## API container environment

| Variable               | Value                                                     |
| ---------------------- | --------------------------------------------------------- |
| `POSTGRES_HOST`        | Shared Postgres container name                            |
| `POSTGRES_DB`          | `propmange`                                               |
| `POSTGRES_USER`        | Dedicated prop-mange role (not the superuser)             |
| `POSTGRES_PASSWORD`    | That role's password                                      |
| `AUTH_ISSUER_URI`      | `https://auth.akandiah.ca/oidc` (must match the `iss` claim) |
| `JWKS_URI`             | `http://logto:3001/oidc/jwks` (over the `auth` network) |
| `AUTH_AUDIENCE`        | Prod Logto API resource identifier (= `VITE_OIDC_RESOURCE`) |
| `CORS_ALLOWED_ORIGINS` | `https://<UI_DOMAIN>`                                     |
| `APP_BASE_URL`         | `https://<UI_DOMAIN>`                                     |
| `AUTH_LOGOUT_URL`      | `https://auth.akandiah.ca/oidc/session/end`               |
| `MAIL_PASSWORD`        | SMTP password (Resend API key by default)                 |
| `MAIL_FROM`            | Sender address on a domain verified in Resend             |

Optional, with defaults: `MAIL_HOST` (`smtp.resend.com`), `MAIL_PORT` (`587`), `MAIL_USERNAME` (`resend`), `NOTIFICATION_EMAIL_ENABLED` (`true`), `INVITE_EXPIRY_HOURS` (`72`), `INVITE_RESEND_COOLDOWN_MINUTES` (`15`), `INVITE_MAX_EMAIL_RETRIES` (`3`), `INVITE_EMAIL_RETRY_INTERVAL_MINUTES` (`15`), `CACHE_PERMISSIONS_MAX_SIZE` (`10000`), `CACHE_PERMISSIONS_TTL_MINUTES` (`60`).

The app refuses to start without `AUTH_AUDIENCE`. The root filesystem can be read-only if `/tmp` is a tmpfs.

Liquibase creates the schema on first start (`ddl-auto: validate`). The DB role needs to own the `propmange` database.

Healthcheck: `wget -qO- http://localhost:8080/actuator/health/liveness || exit 1`.

## Edge routing (`API_DOMAIN` → `app:8080`)

- Forward only `/api/*` and `/actuator/health*`. Return 404 for everything else, including Swagger, `/v3/api-docs` and the rest of `/actuator`.
- TLS ends at Cloudflare. Caddy listens on `http://` and passes `X-Forwarded-Proto https`.
- Set `X-Forwarded-For` to `{http.request.header.Cf-Connecting-IP}`. Prod trusts forwarded headers from private-range proxies, so this is how per-IP rate limiting and audit logs get the real client IP.
- Spring Boot handles CORS. Don't add CORS headers in Caddy.

## Logto (shared tenant)

Prod has its own SPA app and its own API resource on the shared tenant. The API resource identifier must equal `AUTH_AUDIENCE` and `VITE_OIDC_RESOURCE`, and it needs an `access` permission because the frontend requests the `<resource>/access` scope. The tenant-wide custom JWT script adds `email`, `email_verified`, `name` and `roles`. `/api/me` rejects tokens that don't carry a verified email.

## Files here

- `init-db/01-create-databases.sh`: creates the `propmange` user and DB for the devcontainer Postgres.
- `clear-app-db.sh`: truncates the dev-oidc app tables.
