# Skafferi

Skafferi is a self-hosted pantry and household food inventory management application packaged as a single Docker container with an embedded SQLite database.

---

## Features

- **Inventory Tracking:** Track food items with quantities, units of measurement, categories, custom notes, and package sizes.
- **Barcode Scanning:** Scan product barcodes using a device camera or manual input, with automatic product lookup via Open Food Facts.
- **Produce & Non-Barcoded Items:** Track fresh produce and perishable goods with optional purchase and use-by dates.
- **Storage Zones:** Organize inventory across default locations (Pantry, Refrigerator, Freezer) and user-defined custom zones. Move items between zones as needed.
- **Expiration Tracking:** Monitor shelf-life with visual urgency indicators, countdown days, and filters for expiring and expired products.
- **Freshness & Expiration Notifications:** In-app notification center and periodic alerts for expiring items and perishable refrigerator goods.
- **Multi-User Profiles:** Manage household user profiles with configurable avatars and roles.
- **Single Container Deployment:** Packaged as a single lightweight container image running Spring Boot and a Vue frontend with zero external database dependencies.

---

## Quick Start

### Docker Compose

```yaml
services:
  skafferi:
    image: ghcr.io/bjostad/skafferi:latest
    container_name: skafferi
    restart: unless-stopped
    ports:
      - "8080:8080"
    volumes:
      - ./data:/data
    environment:
      - PORT=8080
      - DATA_DIR=/data
      - DATABASE_PATH=/data/skafferi.db
```

Start the application:

```bash
docker compose up -d
```

### Docker CLI

```bash
docker run -d \
  --name skafferi \
  --restart unless-stopped \
  -p 8080:8080 \
  -v $(pwd)/data:/data \
  ghcr.io/bjostad/skafferi:latest
```

Access the application in your browser at `http://localhost:8080`.

---

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `PORT` | `8080` | Web server listening port |
| `DATA_DIR` | `./data` | Directory where application data is stored |
| `DATABASE_PATH` | `/data/skafferi.db` | Path to the SQLite database file |

---

## Development

### Prerequisites
- Java 21 JDK
- Node.js 20+
- Maven 3.9+

### Backend
```bash
cd backend
mvn spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm run dev
```
The frontend dev server runs on `http://localhost:5173` and proxies API requests to the backend.
