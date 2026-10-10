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

## Integrations

Skafferi includes native integrations with popular home-lab, grocery, and recipe tools. Integrations can be configured directly in **Settings > Integrations & APIs**.

### 1. Mealie (Recipe & Meal Planning Webhooks)

Skafferi can automatically deduct cooked ingredients from your inventory via Mealie's webhook system.

#### Setup in Mealie:
1. In Mealie, navigate to **Settings > Webhooks > Add Webhook**.
2. Set the **URL** to:
   ```
   http://<skafferi-host>:8080/api/webhooks/mealie
   ```
   *(or `https://your-skafferi-domain.com/api/webhooks/mealie` if using a reverse proxy)*
3. Select the event trigger: **Meal Cooked** / **Meal Plan Executed**.
4. Save the webhook. When a recipe is marked cooked in Mealie, Skafferi matches the recipe ingredients to items in your pantry and deducts stock using FIFO (First-In, First-Out) expiration order.

#### ⚠️ Reverse Proxy & Authentik Access Considerations:
If Skafferi is protected behind an identity-aware reverse proxy (such as **Authentik Forward Auth / Proxy Outpost**, **Authelia**, **Cloudflare Access**, or basic auth):
- **Webhooks will fail with HTTP 302 or 401/403** because Mealie sends automated server-to-server POST requests and cannot complete interactive browser login redirects.
- **Recommended Solutions:**
  - **Internal Docker Network (Best Practice):** If Mealie and Skafferi run on the same Docker host or Docker network, point Mealie directly to Skafferi's internal container name or IP without passing through the external proxy:
    ```
    http://skafferi:8080/api/webhooks/mealie
    ```
  - **Proxy Bypass / Unauthenticated Route:** In your reverse proxy (Nginx, Caddy, Traefik, or Authentik Proxy Outpost), add an explicit bypass or unauthenticated policy rule for the webhook path:
    ```
    /api/webhooks/mealie
    ```
    Ensure POST requests to this specific path are allowed without triggering an authentication challenge.

---

### 2. Bring! (Shared Shopping List Sync)

Skafferi connects to your Bring! account to synchronize low-stock items directly to your mobile shopping list.

#### Setup:
1. In Skafferi, navigate to **Settings > Integrations & APIs > Bring! Shopping List**.
2. Enter your Bring! account **Email** and **Password**.
   > *Note for Google Sign-in users:* If you normally log in to Bring! via Google on your phone, visit [web.getbring.com](https://web.getbring.com) and click **"Forgot Password?"** with your Gmail address to establish a dedicated password for API access. This will not disrupt Google Sign-in on your phone.
3. Click **Test Connection**.
4. Once verified, choose your target Bring! shopping list and optionally enable **"Automatic background sync on low stock"**.

---

### 3. Digital Receipts & Grocery Importers

Quickly stock items in bulk from digital receipts or grocery order confirmations without manual data entry.

#### Setup & Usage:
1. In the top navigation bar, click the **Receipt** button (or navigate to `/shopping`).
2. Paste the text from your grocery order confirmation (e.g. Kroger "My Purchases", email confirmation, or digital receipt text).
3. Skafferi parses the line items, quantities, and prices into an interactive checklist.
4. Review the items, map them to pantry categories/storage zones, and click **Commit to Pantry** to add them to your inventory in a single batch.

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

---

## License

This project is licensed under the [GNU Affero General Public License v3.0 (AGPL-3.0)](LICENSE).

