# Stage 1: Build the UI
FROM node:20-alpine AS ui-builder

# Install pnpm
RUN corepack enable && corepack prepare pnpm@latest --activate

WORKDIR /app

# Copy package files first for better caching
# Note: These paths are relative to the build context (which should be the workspace root)
COPY prop-manager-ui/package.json prop-manager-ui/pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

# Copy the rest of the UI source
COPY prop-manager-ui/ .

# Build the UI
# We can pass build arguments if needed, e.g., for VITE_ variables
ARG VITE_CONSTRUCTION_DISABLED
ENV VITE_CONSTRUCTION_DISABLED=$VITE_CONSTRUCTION_DISABLED

RUN pnpm build

# Stage 2: Final Caddy image
FROM caddy:2-alpine

# Copy UI build from previous stage
COPY --from=ui-builder /app/dist /usr/share/caddy

# The Caddyfile will be mounted or copied. 
# In the multi-stage build, we copy it so the image is self-contained.
COPY prop-manager/docker/caddy/Caddyfile /etc/caddy/Caddyfile
