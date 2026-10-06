# 🥫 Skafferi

> **Skafferi** *(Norwegian)*: The pantry / larder. Derived from the verb *å skaffe* (to procure, obtain, and supply).

**Skafferi** is a modern, lightweight, self-hosted pantry and household inventory management application packaged into a **single Docker container**.

It bridges the gap between your recipes (**Mealie**), your mobile grocery list (**Bring!**), and your shopping trips (**Kroger & Barcode Scanning**).

---

## ✨ Features

* 📱 **Mealie-Inspired Design:** Clean responsive UI with a rich **Purple/Slate** theme for maximum contrast against fresh, expiring, and expired item status indicators.
* 🧾 **Pluggable Receipt Ingestion (Strategy Pattern):** Paste text/HTML or upload receipt files from **Kroger** (or other grocers) into an interactive review checklist before stocking the pantry.
* 📷 **Camera Barcode Scanning:** Instant in-browser mobile camera barcode lookup powered by **Open Food Facts** and the **Kroger Catalog API**.
* 🍳 **Mealie Integration:** Receives `meal_cooked` webhooks from Mealie to automatically deduct recipe ingredients from your pantry using **FIFO** (First-In, First-Out) expiration tracking.
* 🛒 **Bring! Two-Way Sync:** Automatically pushes items to your shared Bring! mobile shopping list whenever pantry stock drops below your configured minimum threshold.
* 📦 **Single Docker Container:** Built with **Java 21 (Spring Boot 3)** and **Vue 3 (Vite + Tailwind CSS)** with zero-configuration embedded **SQLite**.

---

## 🚀 Quick Start with Docker

### Using Docker Compose (Recommended)

1. Clone or copy the project:
   ```bash
   git clone https://github.com/your-repo/skafferi.git
   cd skafferi
   ```

2. Start the container:
   ```bash
   docker compose up -d --build
   ```

3. Open your browser at **`http://localhost:8080`**.

Data is persisted inside the `skafferi_data` volume at `/data/skafferi.db`.

---

## 🛠️ Architecture

```
                               ┌────────────────────────┐
                               │  Kroger Digital / OCR  │
                               └───────────┬────────────┘
                                           │
                                           ▼
┌──────────────────┐           ┌────────────────────────┐           ┌──────────────────┐
│      Mealie      │ ────────► │        Skafferi        │ ────────► │      Bring!      │
│ (Meal Webhooks)  │           │   (Spring Boot + Vue)  │           │  (Shopping List) │
└──────────────────┘           └────────────────────────┘           └──────────────────┘
```

---

## 🔌 Integrations Setup

### 1. Bring! Shopping List
* Open **Skafferi > Settings / Integrations**.
* Enter your Bring! account email and password, then click **Test Connection**.
* Enable *"Automatic background sync on low stock"* to automatically add depleted items.

### 2. Mealie Webhooks
* In Mealie, go to **Settings > Webhooks**.
* Create a webhook with target: `http://<skafferi-ip>:8080/api/webhooks/mealie`.
* Select the **Meal Cooked / Meal Plan** event.

---

## 💻 Local Development

### Backend (Spring Boot 3 / Java 21)
```bash
cd backend
mvn spring-boot:run
```

### Frontend (Vue 3 / Vite)
```bash
cd frontend
npm install
npm run dev
```
Open **`http://localhost:5173`** with Vite hot module reloading proxied to Spring Boot on `8080`.
