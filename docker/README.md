# PropMange Production Stack: Logto + Caddy + Cloudflare Tunnel

Architecture:

- **Frontend**: Hosted on **Cloudflare Pages** (external to this stack). Authenticates via PKCE directly with Logto.
- **Backend API**: Spring Boot + Java 25, running in Docker. Validates JWTs via Logto's JWKS.
- **Gateway**: **Caddy** — reverse proxy for the API and Logto. Auth is enforced by Spring Boot, not Caddy.
- **Auth**: **Logto** (OIDC Provider) — issues JWTs, supports local accounts and social sign-in.
- **Database**: **PostgreSQL 18** — one instance holding the `propmanager` and `logto` databases.
- **Connectivity**: **Cloudflare Tunnel** (`cloudflared`) — exposes the stack without opening firewall ports.

## Quick Start

### 1. Create `.env` from `.env.example`

Required values:

- `UI_DOMAIN` — your Cloudflare Pages hostname (e.g. `propmanager.pages.dev`)
- `API_DOMAIN` — your API subdomain (e.g. `api.propmanager.com`)
- `AUTH_DOMAIN` — your Logto subdomain (e.g. `auth.propmanager.com`)
- `TUNNEL_TOKEN` — from Cloudflare Zero Trust > Networks > Tunnels
- `POSTGRES_PASSWORD` — generate: `openssl rand -base64 36 | tr -d '\n'`
- `MAIL_PASSWORD` — SMTP password (Resend API key by default)

Optional: `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_FROM`, `DB_CLEAR_SCHEDULE` (cron, default Sundays 03:00 UTC).

`AUTH_ISSUER_URI`, `AUTH_LOGOUT_URL`, `CORS_ALLOWED_ORIGINS` and `APP_BASE_URL` are derived in `docker-compose.yml` from the domain variables — do not set them in `.env`.

### 2. Configure Cloudflare Tunnel

In the Cloudflare Zero Trust dashboard, add **two Public Hostnames** pointing to `http://caddy:80`:

1. `API_DOMAIN` → `http://caddy:80`
2. `AUTH_DOMAIN` → `http://caddy:80`

The `cloudflared` container reaches Caddy via the `public` Docker network.

### 3. Start the stack

```bash
docker compose up -d
docker compose logs -f logto   # first start seeds the Logto DB
```

### 4. Configure Logto

The admin console is bound to the host's `127.0.0.1:3002`. From your machine, open a tunnel and browse to `http://localhost:3002`, then create the admin account on first visit:

```bash
ssh -L 3002:localhost:3002 <host>
```

1. **API resource** — _API resources > Create_
   - Identifier: e.g. `https://<API_DOMAIN>` (this becomes `VITE_OIDC_RESOURCE`)
   - Add a permission named `access` (the frontend requests the `<resource>/access` scope)
2. **Application** — _Applications > Create > Single page app_
   - Redirect URI: `https://<UI_DOMAIN>/auth/callback` and `https://<UI_DOMAIN>/auth/silent-callback`
   - Post sign-out redirect URI: `https://<UI_DOMAIN>`
   - CORS allowed origin: `https://<UI_DOMAIN>`
   - Copy the **App ID** (this becomes `VITE_OIDC_CLIENT_ID`)

### 5. Configure the frontend (Cloudflare Pages env vars)

```
VITE_OIDC_AUTHORITY=https://<AUTH_DOMAIN>/oidc
VITE_OIDC_CLIENT_ID=<App ID>
VITE_OIDC_REDIRECT_URI=https://<UI_DOMAIN>/auth/callback
VITE_OIDC_RESOURCE=<API resource identifier>
```

The backend needs no Logto-specific config: `AUTH_ISSUER_URI` is `https://<AUTH_DOMAIN>/oidc`, and `JWKS_URI` points at the internal `http://logto:3001/oidc/jwks` to avoid a round-trip through the tunnel.

Logto tokens carry no `groups` claim, so the backend builds authorities from the database (see `JwtConfig` / `JwtAccessHydrationFilter`).

## Image Versions

Images use floating tags (`latest`, `alpine`, `25-jre-alpine`) — `docker compose pull && docker compose up -d --build` picks up updates.

**Exception: Postgres is pinned to a major version** (`postgres:18-alpine`). A new major cannot read the previous major's data directory, so once there is live data, bumping it needs a `pg_dumpall` / restore.

## Networking & Security

- **Host ports**:
  - `:80` — Caddy (`http://localhost/api/...` works locally without the tunnel)
  - `127.0.0.1:3002` — Logto admin console (host-local only; not exposed via Caddy or the tunnel — use an SSH tunnel)
- **External access**: Only `cloudflared` reaches Caddy, via the `public` Docker network. Postgres is not published.
- **CORS**: Handled entirely by Spring Boot (`CorsConfig`). Do not add CORS headers in Caddy.
- **JWT validation**: Spring Boot validates `Authorization: Bearer` tokens against Logto's JWKS and the issuer `https://<AUTH_DOMAIN>/oidc`. Logto's `ENDPOINT` must match the public URL so the `iss` claim lines up.

## File Structure

- `docker-compose.yml` — full stack definition
- `docker/caddy/Caddyfile` — Caddy routing
- `docker/caddy/Dockerfile` — unused; the compose file pulls `caddy:alpine` directly
- `docker/init-db/01-create-databases.sh` — creates the `propmanager` and `logto` Postgres users/DBs
- `docker/db-scheduler/` — cron container that truncates app tables on `DB_CLEAR_SCHEDULE`
