# Duck Shop — Server-Side Kotlin

A small full-stack demo used to teach server-side development with Kotlin: a rubber-duck shop with a
REST API and a (fixed-design) React frontend. The single artifact serves both the API and the UI.

## Layout

```
server/       Spring Boot 3 / Kotlin / JVM 21 — REST API at /api/ducks (the main app)
ktor-server/  Ktor 3 / Kotlin / JVM 21 — a subset of /api/ducks, for the Spring-vs-Ktor comparison
frontend/     React + TypeScript (design is fixed; only the API layer changes)
```

## Stack & features

- **Spring Boot 3.3** · Kotlin 2.0 · JVM 21
- **REST** resource `/api/ducks` (GET/POST/PUT/PATCH/DELETE), server-side state
- **JPA** persistence — `Duck` ↔ `Accessory` (`@ManyToMany`), file-based **H2** (survives restart)
- **Spring Security** — GET public, mutations authenticated, `PUT` = ADMIN (HTTP Basic, dev users)
- **Tests** — unit, web (MockMvc + security), Testcontainers PostgreSQL
- **Actuator** + Prometheus metrics; optional GraalVM native build

## Run

```bash
cd server
./gradlew bootRun                 # dev (also builds the frontend into the app)
# or
./gradlew bootJar && java -jar build/libs/duck-shop-server-0.0.1-SNAPSHOT.jar
```

Open <http://localhost:8080> for the UI · `GET /api/ducks` for the API · `/actuator/health` for status.

Dev credentials (HTTP Basic): `admin/admin` (ADMIN), `user/user` (USER).

## Ktor (comparison — Module 9)

A lightweight port of a **subset** of `/api/ducks` (`GET` / `GET /state` / `PUT` / `POST`), used to
contrast Spring with Ktor on four axes (routing, serialization, DB, auth). API-only, separate port,
its own H2 file — runs side by side with the Spring app.

```bash
cd ktor-server
./gradlew run                     # starts on http://localhost:8081
```

- Routing DSL · **kotlinx.serialization** · **Exposed** (file H2, durable) · **Ktor Authentication** (Basic).
- Authorization is a manual ADMIN check (Ktor has no built-in `hasRole`): `PUT` = ADMIN, `POST` = any user, `GET` public.
- Same dev credentials as Spring. `PATCH`/`DELETE` and the `Duck ↔ Accessory` relationship are intentionally not ported.
