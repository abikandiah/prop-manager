# PropMange Production Stack: Authentik + Caddy + Cloudflare Tunnel

Architecture:
- **Frontend**: Hosted on **Cloudflare Pages** (external to this stack). Authenticates via PKCE directly with Authentik.
- **Backend API**: Spring Boot + Java 21, running in Docker. Validates JWTs via Authentik JWKS.
- **Gateway**: **Caddy** (Reverse Proxy + Forward Auth) — routes API traffic, optionally pre-validates tokens via Authentik outpost.
- **Auth**: **Authentik** (OIDC Provider) — issues JWTs, supports local accounts, Google SSO, and more.
- **Connectivity**: **Cloudflare Tunnel** (`cloudflared`) — exposes the stack without opening firewall ports.

## Quick Start

### 1. Create `.env` from `.env.example`

Fill in all required values:
- `UI_DOMAIN` — your Cloudflare Pages hostname (e.g. `propmanager.pages.dev`)
- `API_DOMAIN` — your API subdomain (e.g. `api.propmanager.com`)
- `AUTH_DOMAIN` — your Authentik subdomain (e.g. `auth.propmanager.com`)
- `TUNNEL_TOKEN` — from Cloudflare Zero Trust > Networks > Tunnels
- `POSTGRES_PASSWORD` — generate: `openssl rand -base64 36 | tr -d '\n'`
- `AUTHENTIK_SECRET_KEY` — generate: `openssl rand -base64 60 | tr -d '\n'`

`CORS_ALLOWED_ORIGIN`, `AUTH_ISSUER_URI`, `AUTH_LOGOUT_URL`, and `AUTHENTIK_HOST` are derived automatically in `docker-compose.yml` from these three domain variables — do not set them in `.env`.

### 2. Configure Cloudflare Tunnel

In the Cloudflare Zero Trust dashboard, add **two Public Hostnames** pointing to `http://caddy:80`:
1. `API_DOMAIN` (e.g. `api.propmanager.com`) → `http://caddy:80`
2. `AUTH_DOMAIN` (e.g. `auth.propmanager.com`) → `http://caddy:80`

The `cloudflared` container connects to Caddy via the `public` Docker network.

### 3. Start the stack

```bash
docker compose up -d
```

Wait for Authentik to initialize (check with `docker compose logs -f authentik`).

### 4. Initial Authentik setup

Access the Authentik setup wizard:
```
https://<AUTH_DOMAIN>/if/flow/initial-setup/
```
Set the admin password.

### 5. Create an OAuth2 Provider and Application

In **Authentik Admin UI** (https://`<AUTH_DOMAIN>`/if/admin/):

1. Go to **Applications > Providers > Create** → choose **OAuth2/OpenID Provider**
   - Name: `prop-manager`
   - Client type: `Public` (PKCE, no client secret)
   - Client ID: copy this — needed by the frontend
   - Redirect URIs: `https://propmanager.pages.dev/auth/callback` (your Pages URL)
   - Signing Key: select the default RSA key
   - Scopes: `openid`, `email`, `profile`, `groups` (or `offline_access` if using refresh tokens)
   - Subject mode: `Based on the hashed User ID` (recommended)
   - Click **Save**

2. Go to **Applications > Applications > Create**
   - Name: `PropMange API`
   - Slug: `prop-manager`
   - Provider: select the provider you just created
   - Click **Save**

3. Note the **Issuer URL** from the provider settings — it should be `https://<AUTH_DOMAIN>/application/o/prop-manager/`, which is automatically derived in `docker-compose.yml` from `AUTH_DOMAIN`.

### 6. Restart the app

The `JWKS_URI` in `docker-compose.yml` is hardcoded to the internal Authentik URL (`http://authentik:9000/...`) — Spring Boot uses this path for key fetching to avoid a round-trip through the tunnel.

Restart the app:
```bash
docker compose up -d app
```

### 7. (Optional) Add Google SSO

In Authentik Admin:
1. Go to **Directory > Federation & Social login > Create** → choose **Google OAuth Source**
2. Fill in your Google OAuth client ID and secret (from Google Cloud Console)
3. Users can now sign in with Google. Authentik brokers the identity and issues its own JWT — the backend sees only Authentik tokens regardless of login method.

## Networking & Security

- **No open ports on host**: Only `cloudflared` connects to Caddy via the `public` Docker network. Ports 80/443 are not bound to the host.
- **CORS**: Handled entirely by the Spring Boot app (`CorsConfig`). Do not add CORS headers in Caddy.
- **JWT validation**: Spring Boot validates `Authorization: Bearer` tokens against Authentik's JWKS. Caddy's forward auth adds a gateway-level pre-check (defence in depth).
- **AUTHENTIK_HOST**: Derived from `AUTH_DOMAIN` so JWTs contain the correct external issuer URL.

## Authentik: Local Accounts vs Google OIDC

Both work simultaneously — Authentik is an identity broker:
- **Local accounts**: username/password managed in Authentik (good for admins and service accounts)
- **Google OIDC**: users sign in with Google; Authentik creates/links a local user record and issues its own JWT

From the Spring Boot API's perspective, all tokens are Authentik JWTs — it doesn't matter which login method was used.

## File Structure

- `docker-compose.yml` — full stack definition
- `docker/caddy/Caddyfile` — Caddy routing and forward auth config
- `docker/caddy/Dockerfile` — (unused; image is pulled directly)
- `docker/init-db/01-create-databases.sh` — creates `propmanager` and `authentik` Postgres users/DBs
