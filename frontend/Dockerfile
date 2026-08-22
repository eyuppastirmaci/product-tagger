# --- Build stage: compile the Angular bundle ---
FROM node:22-alpine AS build
WORKDIR /build

# Install dependencies first so they are cached independently of source changes
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# --- Runtime stage: nginx serves the bundle and proxies /api ---
FROM nginx:stable-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80
