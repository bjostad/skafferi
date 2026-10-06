# ==========================================
# Stage 1: Build Vue 3 Frontend
# ==========================================
FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend

COPY frontend/package*.json ./
RUN npm ci || npm install

COPY frontend/ .
RUN npm run build

# ==========================================
# Stage 2: Build Spring Boot Backend
# ==========================================
FROM maven:3.9-eclipse-temurin-21-alpine AS backend-builder
WORKDIR /app/backend

COPY backend/pom.xml .
RUN mvn dependency:go-offline -B

COPY backend/src ./src

# Copy compiled frontend assets into Spring Boot's static resources
COPY --from=frontend-builder /app/frontend/dist ./src/main/resources/static/

ARG APP_VERSION=""
RUN if [ -n "$APP_VERSION" ]; then \
      mvn clean package -DskipTests -B -Dskafferi.version="$APP_VERSION"; \
    else \
      mvn clean package -DskipTests -B; \
    fi

# ==========================================
# Stage 3: Lightweight Production JRE Runtime
# ==========================================
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="Skafferi Team"
LABEL description="Skafferi - Self-Hosted Pantry & Inventory Manager"

WORKDIR /app

# Create data directory for SQLite database persistence
RUN mkdir -p /data
VOLUME /data

ENV PORT=8080
ENV DATA_DIR=/data
ENV DATABASE_PATH=/data/skafferi.db

COPY --from=backend-builder /app/backend/target/skafferi-backend-*.jar /app/skafferi.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/skafferi.jar"]
