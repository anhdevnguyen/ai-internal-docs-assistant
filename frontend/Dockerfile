# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM node:22-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --silent

COPY . .
RUN npm run build

# ─── Stage 2: Serve ───────────────────────────────────────────────────────────
# nginx:alpine is ~25MB. No Node.js runtime needed in production.
FROM nginx:alpine AS runtime

# Remove the default nginx welcome page.
RUN rm -rf /usr/share/nginx/html/*

COPY --from=build /app/dist /usr/share/nginx/html

# SPA routing: all routes must fall back to index.html so React Router handles them.
# This nginx.conf is minimal — the outer reverse-proxy nginx handles SSL and rate limiting.
COPY nginx-spa.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]