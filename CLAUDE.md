# SeatFlow Project Development Guidelines

## 1. Project Overview

**Your Role:** You are 'Claude Code', an AI Senior Developer. Your mission is to implement the backend and frontend for the SeatFlow project, strictly adhering to the rules outlined in this document.

**Project Goal:** To develop 'SeatFlow', a real-time, high-concurrency seat reservation system. The core features are robust concurrency control using Redis distributed locks and database constraints, and real-time seat status visualization using WebSockets and Redis Pub/Sub.

**Core Tech Stack:**
*   **Backend:** Kotlin 1.9+, Spring Boot 3.x (WebFlux), R2DBC, PostgreSQL
*   **Concurrency & Messaging:** Redis (using the Lettuce client)
*   **Frontend:** SvelteKit, TypeScript, Socket.IO-client
*   **Infra:** Docker Compose

## 2. Global Rules

### 2.1. Language & Formatting
*   **Language:** All code, comments, and commit messages MUST be written in **English**.
*   **Code Formatting:**
    *   **Kotlin:** Strictly adhere to the default rules of `ktlint`.
    *   **Svelte/TypeScript:** Strictly adhere to the default rules of `Prettier`.
*   **Naming Conventions:**
    *   **Kotlin:** Use `PascalCase` for classes and `camelCase` for functions/variables.
    *   **TypeScript:** Use `PascalCase` for types/interfaces and `camelCase` for functions/variables.
    *   **API DTOs:** Use `camelCase` for field names, which will be automatically handled by JSON serialization/deserialization.

### 2.2. Commit Messages
Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification.
*   `feat:`: A new feature
*   `fix:`: A bug fix
*   `docs:`: Documentation only changes
*   `style:`: Changes that do not affect the meaning of the code (white-space, formatting, etc.)
*   `refactor:`: A code change that neither fixes a bug nor adds a feature
*   `test:`: Adding missing tests or correcting existing tests
*   `chore:`: Changes to the build process or auxiliary tools

**Example:** `feat(reservation): implement seat hold logic with redis lock`

## 3. Backend Architecture (Kotlin/Spring WebFlux)

### 3.1. Package Structure (Layered Architecture)
Code should be organized by domain, with each domain containing layers such as `controller`, `service`, `repository`, `dto`, and `entity`.

```
com.seatflow
├── common/             // Common exceptions, base entities, etc.
├── config/             // Configuration classes for Redis, R2DBC, WebSockets, etc.
├── domain/
│   ├── reservation/
│   │   ├── controller/   // API Endpoints (uses DTOs)
│   │   ├── service/      // Business Logic (uses domain entities)
│   │   ├── repository/   // R2DBC interfaces
│   │   ├── dto/          // Data Transfer Objects for Requests/Responses
│   │   └── entity/       // Entities mapped to database tables
│   └── show/
│       └── ...
└── infrastructure/
    ├── lock/           // Redis distributed lock implementation
    └── messaging/      // Redis Pub/Sub publisher/subscriber implementation
```

### 3.2. Reactive Programming
*   **All I/O operations MUST be non-blocking.** As we are using Spring WebFlux and R2DBC, all service and controller methods must return `Mono<T>` or `Flux<T>`.
*   **NEVER use `block()`.** The use of blocking operators like `block()` or `blockFirst()` is forbidden in all business logic outside of test code.

### 3.3. Concurrency & Locking
*   **Lock Key Format:** Strictly use the format `lock:seat:{showId}:{seatId}`.
*   **Lock Implementation:** Use Redis's `SET key value NX EX ttl` command. The `value` must store a **Fencing Token**.
    *   **Fencing Token:** A unique value to ensure lock ownership. Generate it using `UUID.randomUUID().toString()` or a more sophisticated timestamp-based token.
*   **Lock Release:** Use the `DEL` command, but it is strongly recommended to use a Lua script that checks the Fencing Token to ensure you are only deleting a lock that you acquired.
*   **DB Constraint:** The `UNIQUE(schedule_id, seat_id)` constraint on the `seat_inventory` table is the final line of defense for concurrency control. It guarantees data integrity even if the lock logic fails.

### 3.4. Messaging
*   **Message Channel:** Use channel names with the format `seats:{showId}`.
*   **Message Format:** When a seat's status changes, publish a JSON string in the following format:
    ```json
    {
      "seatId": "C5",
      "status": "HOLD", // Can be HOLD, CONFIRMED, AVAILABLE
      "userId": "user-a-session-id", // Actor identifier (for demo purposes)
      "holdExpiresAt": "2025-11-05T21:45:00Z" // Included only for HOLD status
    }
    ```
*   **Expiration Handling:** Subscribe to Redis **Keyspace Notifications** (`__keyevent@<db>__:expired`) to detect expired lock keys. When a key expires, change the corresponding seat's status to `AVAILABLE` and publish the change using the message format above.

### 3.5. API Design
*   **Idempotency:** The `POST /reservations/hold` and `POST /reservations/{id}/confirm` APIs must accept an `Idempotency-Key` (UUID) in the header. Store this key in Redis for a short duration to prevent duplicate processing of the same request.
*   **Response Format:** All API responses should have a consistent JSON structure.
    ```json
    {
      "success": true,
      "data": { ... }, // Data on success
      "error": null    // Error details on failure
    }
    ```

## 4. Frontend Architecture (SvelteKit)

### 4.1. Directory Structure
*   `src/routes`: Page components.
*   `src/lib/components`: Reusable UI components (e.g., `SeatMap.svelte`, `Seat.svelte`).
*   `src/lib/stores`: Svelte stores for global state management (e.g., `seatStore.ts`).
*   `src/lib/api`: Backend API client logic (with types).
*   `src/lib/websocket`: WebSocket connection and event handling logic.

### 4.2. State Management
*   All state for the seat map must be managed in `src/lib/stores/seatStore.ts`.
*   The store should be a `writable` store containing the array of seats, currently selected seats, show information, etc.
*   When a seat status change event is received from the WebSocket, this store must be updated. The UI will update automatically due to Svelte's reactivity.

### 4.3. Real-time Communication
*   On page load, connect to the backend WebSocket using `socket.io-client`.
*   Upon receiving a seat status change message from the backend, update the `seatStore`.
*   The `Seat.svelte` component must dynamically change its `class` (for color/animation) and display a countdown badge based on state changes in the store.

### 4.4. Demo UI Implementation
*   **Screen Layout:** The main page will have a two-column layout.
*   **Left Panel (Scenario Panel):** This will display descriptions of the demo scenario and buttons to execute them (e.g., "1. User A clicks seat C5 (Request Hold)").
*   **Right Panel (Visualization Panel):** This will display the `SeatMap.svelte` component and a real-time event log. When a user clicks a button in the left panel, the corresponding API request should be triggered, and the result must be visually reflected in this panel immediately.

## 5. Development Order

**You MUST generate the code following this sequence.**

1.  **Phase 1: Backend - Data Model & Basic API**
    1.  Set up dependencies in `build.gradle.kts` (WebFlux, R2DBC, Lettuce, Jackson Kotlin Module).
    2.  Write the PostgreSQL table DDL in `schema.sql`.
    3.  Define R2DBC-compatible data classes (`entity`) and repository interfaces.
    4.  Implement the `GET /shows/{id}/seats` API (initially, query the DB directly).
2.  **Phase 2: Frontend - Basic UI Setup**
    1.  Initialize the SvelteKit project and set up the basic two-column layout.
    2.  Implement the static UI for `SeatMap.svelte` and `Seat.svelte` components.
    3.  Call the API from Phase 1 to render the seat map.
3.  **Phase 3: Backend - Core Concurrency Logic**
    1.  Implement `RedisLockManager` (using SETNX, Fencing Tokens, and Lua scripts).
    2.  Implement the `POST /reservations/hold` API (acquire distributed lock -> update DB).
    3.  Implement the `POST /reservations/{id}/confirm` API.
    4.  Add the `Idempotency-Key` handling logic.
4.  **Phase 4: Real-time Integration (Backend + Frontend)**
    1.  **Backend:** Configure WebSockets and Redis Pub/Sub. Add logic to publish messages on seat status changes.
    2.  **Backend:** Implement the Redis Keyspace Notifications listener to handle hold expirations.
    3.  **Frontend:** Implement the WebSocket client and integrate it with the `seatStore`.
5.  **Phase 5: Infrastructure & Finalization**
    1.  Write the `docker-compose.yml` file (for backend, frontend, postgres, redis).
    2.  Write `Dockerfile`s for the backend and frontend services.
    3.  Document the demo scenarios and setup instructions in `README.md`.

---
Please begin the development of the SeatFlow project following these guidelines. When I request code generation for each phase, you will provide consistent code according to the rules in this document.