# Docker: Authentik + Caddy + Prop Manager

Stack: **single PostgreSQL** (propmanager + authentik DBs), **Caddy** (reverse proxy + forward auth), **Authentik** (OIDC), **Prop Manager** (API).

## Quick start

**Note:** If you previously had two separate Postgres containers, use a fresh `postgres_data` volume or create the `authentik` user and database manually in your existing Postgres (see `docker/init-db/01-create-databases.sh`).

1. **Create `.env`** from `.env.example` and set:

   - `POSTGRES_PASSWORD` — one password for Postgres (postgres, propmanager, authentik users); e.g. `openssl rand -base64 36 | tr -d '\n'`
   - `AUTHENTIK_SECRET_KEY` — e.g. `openssl rand -base64 60 | tr -d '\n'`

2. **Start stack** (Authentik will not have an OIDC provider yet):

   ```bash
   docker compose up -d
   ```

3. **Initial Authentik setup**

   - Open **http://localhost:9000/if/flow/initial-setup/** (trailing slash) and set the admin password.
   - Create an **Application** (e.g. "Prop Manager").
   - Create a **Provider** → **Proxy Provider**:
     - Name: e.g. `prop-manager`
     - **Forward auth** mode (e.g. "Forward auth (single application)").
     - **External host**: `http://caddy` (or the host Caddy uses to reach Authentik; for same compose, `http://caddy` is fine if outpost is embedded).
   - For the API to validate JWTs, create an **OIDC Provider** (or use the same app with an OIDC provider):
     - In Authentik: **Applications** → your app → **Providers** → add **OIDC Provider**.
     - Note the **Issuer** URL (e.g. `http://authentik:9000/application/o/prop-manager/`).

4. **Set `AUTH_ISSUER_URI`** in `.env` to that Issuer URL (internal hostname `authentik` so the app can fetch JWKS):
   ```env
   AUTH_ISSUER_URI=http://authentik:9000/application/o/prop-manager/
   ```
   Restart the app container: `docker compose up -d app`.

## Ports

| Port | Service                                                |
| ---- | ------------------------------------------------------ |
| 80   | Caddy (main entry; API, Authentik UI, forward auth)    |
| 443  | Caddy (HTTPS if you configure a hostname in Caddyfile) |
| 9000 | Authentik (direct, for initial setup and admin)        |

## Caddyfile

- `docker/caddy/Caddyfile`: routes `/outpost.goauthentik.io`, `/if`, `/application` to Authentik; `/api/*` with forward_auth then to the app; `/actuator`, `/swagger-ui` to the app.
- For production TLS, change the Caddyfile first line to your domain (e.g. `api.example.com`) and use Caddy’s automatic HTTPS.
