# TransitAssist — Accessible Transit Assistance Booking

A Spring Boot + MySQL backend (with a small HTML/CSS/JS dashboard) that lets persons with disabilities request
transit assistance (wheelchair, escort, mobility support) **ahead of a trip**, and lets staff assign helpers and
track each request until it is completed or cancelled.

## 1. Problem statement
Persons with disabilities on campus often need assistance such as wheelchair assistance or escort assistance to use
shuttle transit, but currently they must request it informally at the last minute.

## 2. Objective
Build a backend system that allows users to request assistance before a trip and allows staff/helpers to assign and
fulfil those requests — while guaranteeing (in the service layer) that helpers are never double-booked and that
request status changes follow valid rules.

## 3. Features
* User, Helper and Assistance-Request CRUD (delete = soft deactivate for users/helpers, cancel for requests)
* Request assistance with pickup, destination, date, time, type, optional notes
* Assign an available, active helper — overlapping bookings are rejected (HTTP 409)
* Status tracking: `REQUESTED → ASSIGNED → COMPLETED`, or `→ CANCELLED`
* Helper workload for a selected day
* Bean Validation + clean JSON errors (`GlobalExceptionHandler`)
* Demo data loaded automatically, Swagger UI, Postman collection, JUnit 5 tests

## 4. Technology stack
Java 21 · Spring Boot 3.3.5 · Spring Web · Spring Data JPA / Hibernate · Jakarta Validation · Jackson · Maven ·
MySQL 8 + MySQL Connector/J · springdoc-openapi (Swagger UI) · JUnit 5 + Spring Boot Test (H2 in-memory DB for tests only) ·
HTML/CSS/vanilla JS frontend.

## 5. Architecture
`Controller → Service → Repository → MySQL`. Business rules live only in the services.
Diagrams: [`docs/architecture.md`](docs/architecture.md), [`docs/class-diagram.md`](docs/class-diagram.md),
[`docs/database-schema.md`](docs/database-schema.md).

## 6. Database design
Tables: `users`, `helpers`, `helper_skills`, `assistance_requests` (PKs are `BIGINT AUTO_INCREMENT`, FKs, indexes on
`user_id`, `helper_id`, `trip_date`, `status`). Reference DDL: [`database/schema.sql`](database/schema.sql).

## 7. Entity relationships
* `User 1 ── * AssistanceRequest` (a request belongs to exactly one user)
* `Helper 1 ── * AssistanceRequest` (a request has 0 or 1 helper)
* `Helper 1 ── * helper_skills` (multiple skills per helper)

## 8. Business rules (all enforced in `AssistanceRequestService`)
| # | Rule | Result |
|---|------|--------|
| 1 | A helper cannot have two overlapping requests (slot = 60 min, configurable) | **409** "Helper is already assigned to another request at the requested time." |
| 2 | COMPLETED request cannot be cancelled | **400** "Completed requests cannot be cancelled." |
| 3 | COMPLETED request cannot be assigned/reassigned | **400** |
| 4 | CANCELLED request cannot be completed | **400** |
| 5 | COMPLETED request cannot be modified (helper, date, time, type) | **400** |
| 6 | CANCELLED request cannot be assigned | **400** |
| 7 | Only `active` **and** `available` helpers can be assigned | **400** |
| 8 | Trip date/time required and must not be in the past | **400** |
| – | Valid transitions only: REQUESTED→ASSIGNED/CANCELLED, ASSIGNED→COMPLETED/CANCELLED | **400** otherwise |

Design notes you can mention in the viva:
* "Active" requests for the overlap check are those in status `ASSIGNED` or `COMPLETED`; cancelled requests free the slot.
* The helper row is locked (`SELECT … FOR UPDATE`, `@Lock(PESSIMISTIC_WRITE)`) while assigning, so two simultaneous
  assignments cannot both pass the overlap check.
* A completed request cannot be *completed* directly from `REQUESTED` — a helper must be assigned first.
* An `ASSIGNED` request may be re-assigned to a different helper (it stays `ASSIGNED`); the new helper is checked for overlaps too.

## 9. API list
| Method | URL | Description |
|--------|-----|-------------|
| POST | `/api/users` | Create user (201) |
| GET | `/api/users`, `/api/users/{id}` | List / get |
| PUT | `/api/users/{id}` | Update |
| DELETE | `/api/users/{id}` | Deactivate (204) |
| POST | `/api/helpers` | Create helper (201) |
| GET | `/api/helpers`, `/api/helpers/{id}` | List / get |
| PUT | `/api/helpers/{id}` | Update |
| DELETE | `/api/helpers/{id}` | Deactivate (204) |
| GET | `/api/helpers/{id}/workload?date=YYYY-MM-DD` | Workload for a day |
| POST | `/api/assistance-requests` | Create request (201) |
| GET | `/api/assistance-requests[?status=&userId=&date=]` | List (optional filters) |
| GET | `/api/assistance-requests/{id}` | Get |
| PUT | `/api/assistance-requests/{id}` | Partial update (only REQUESTED / ASSIGNED) |
| DELETE | `/api/assistance-requests/{id}` | Cancel |
| POST | `/api/assistance-requests/{id}/assign/{helperId}` | Assign helper |
| POST | `/api/assistance-requests/{id}/assign` | Assign helper, body `{"helperId": 1}` |
| POST | `/api/assistance-requests/{id}/complete` | Mark completed |
| POST | `/api/assistance-requests/{id}/cancel` | Cancel |

## 10. Project structure
```
transit-assist/
├── pom.xml
├── README.md
├── postman/            TransitAssist-API.postman_collection.json, TransitAssist.postman_environment.json
├── docs/               class-diagram.md, architecture.md, database-schema.md (Mermaid)
├── database/           README.md, schema.sql
└── src/
    ├── main/java/com/transitassist/
    │   ├── TransitAssistApplication.java
    │   ├── controller/  service/  repository/  entity/  dto/  exception/  config/  enums/  util/
    ├── main/resources/  application.properties, application-example.properties, static/{index.html,style.css,app.js}
    └── test/            JUnit 5 tests (+ application-test.properties using H2)
```

## 11. MySQL setup
1. Install MySQL 8 and make sure the server is running (port 3306).
2. Nothing else is required — the app creates the database `transitassist_db` and its tables itself.
3. Tell the app your MySQL login, **either**:
   * edit `src/main/resources/application.properties` (`spring.datasource.username` / `spring.datasource.password`), **or**
   * set environment variables `DB_USERNAME` and `DB_PASSWORD` (recommended; no password is stored in the project).
   * Windows PowerShell: `$env:DB_PASSWORD="your_password"` · cmd: `set DB_PASSWORD=your_password` · Linux/macOS: `export DB_PASSWORD=your_password`
4. Optional manual check: `mysql -u root -p -e "SHOW DATABASES;"`

## 12. IntelliJ setup
1. Extract the ZIP. **File → Open** → select the `transit-assist` folder (the one containing `pom.xml`).
2. Trust the project; IntelliJ imports it as a Maven project and downloads dependencies (internet needed the first time).
3. **File → Project Structure → Project SDK = JDK 21**, language level 21.
4. To pass the DB password: **Run → Edit Configurations → TransitAssistApplication → Environment variables** →
   `DB_USERNAME=root;DB_PASSWORD=your_password`.

## 13. Maven commands
```bash
mvn clean test       # run the tests (uses in-memory H2, MySQL not needed)
mvn clean package    # build target/transit-assist-1.0.0.jar
mvn spring-boot:run  # start the application (MySQL must be running)
java -jar target/transit-assist-1.0.0.jar
```
No Maven wrapper is included — use IntelliJ's bundled Maven or an installed Maven 3.8+.

## 14. How to run
1. Start MySQL. 2. Run `TransitAssistApplication` in IntelliJ (or `mvn spring-boot:run`).
3. Open <http://localhost:8080/> (dashboard) and <http://localhost:8080/swagger-ui.html> (API docs).

## 15. Demo data (loaded on first start)
| Type | Data |
|------|------|
| Users | 1 Arun Kumar · 2 Priya Sharma · 3 Demo Student · 4 Campus Staff (STAFF) |
| Helpers | 1 Ravi Kumar (WHEELCHAIR) · 2 Meena Devi (ESCORT) · 3 Demo Helper (WHEELCHAIR, ESCORT) |
| Requests | #1 COMPLETED (yesterday, Ravi) · #2 ASSIGNED (tomorrow 10:00, Ravi) · #3 ASSIGNED (tomorrow 10:00, Meena) · #4 REQUESTED (tomorrow 10:30) · #5 REQUESTED · #6 CANCELLED · #7 REQUESTED |

Ready-made viva shortcuts: assign **Ravi (1)** to **#4** → 409 (overlaps #2) · cancel **#1** → 400 · assign **#1** to anyone → 400 ·
assign **#6** → 400 · complete **#6** → 400 · cancel **#7** → OK.

## 16. Recommended demo sequence (viva)
1. Start MySQL. 2. Start Spring Boot. 3. Open the dashboard at `http://localhost:8080/`.
4. **Users** tab: view / add a user. 5. **Helpers** tab: view / add a helper.
6. **New Request** tab: create a request for tomorrow at 09:00 (WHEELCHAIR).
7. **Requests** tab: choose *Demo Helper* in the new row → **Assign** (status → ASSIGNED).
8. View the assigned request in the table. 9. **Workload** tab: *Demo Helper*, tomorrow → the request is listed.
10. Click **Complete** on that request (status → COMPLETED).
11. Create another request for tomorrow at **09:30**.
12. Try to **Assign** *Demo Helper* to it. 13. The server rejects it: **409 – Helper is already assigned to another request at the requested time.**
14. Click **Cancel** on the COMPLETED request. 15. Rejected: **400 – Completed requests cannot be cancelled.**
16. Click **Cancel** on a REQUESTED request → succeeds (status CANCELLED).

## 17. How to test with Postman
1. Import `postman/TransitAssist-API.postman_collection.json` (and optionally `TransitAssist.postman_environment.json`, then select it).
   `baseUrl = http://localhost:8080`.
2. Start the app once so the demo data exists (IDs in the collection variables match it).
3. Run the folders **in this order** (or use *Runner*): `1. Users` → `2. Helpers` → `3. Assistance Requests` → `4. Negative Tests`.
   Every request has a Postman test that checks the expected HTTP status; folder 3 stores `requestId` / `cancelId`, and folder 4 stores `overlapId`.
   A random future trip date is generated automatically so re-running never collides with old data.

## 18. Sample requests / responses
Create request:
```http
POST /api/assistance-requests
{
  "userId": 1,
  "pickupPoint": "Main Gate",
  "destination": "Library",
  "tripDate": "2030-01-15",
  "tripTime": "10:00",
  "assistanceType": "WHEELCHAIR",
  "additionalNotes": "Needs ramp"
}
```
`201 Created`
```json
{
  "id": 8, "userId": 1, "userName": "Arun Kumar", "helperId": null, "helperName": null,
  "pickupPoint": "Main Gate", "destination": "Library", "tripDate": "2030-01-15", "tripTime": "10:00:00",
  "assistanceType": "WHEELCHAIR", "additionalNotes": "Needs ramp", "status": "REQUESTED",
  "createdAt": "2030-01-01T09:00:00", "updatedAt": "2030-01-01T09:00:00"
}
```
Helper workload — `GET /api/helpers/1/workload?date=2030-01-15`:
```json
{ "helperId": 1, "helperName": "Ravi Kumar", "date": "2030-01-15", "totalRequests": 1,
  "assignedCount": 1, "completedCount": 0, "requests": [ { "id": 8, "status": "ASSIGNED", "...": "..." } ] }
```

## 19. Expected error cases
`409 Conflict` — overlapping helper booking
```json
{ "timestamp": "2030-01-01T09:05:00", "status": 409, "error": "Conflict",
  "message": "Helper is already assigned to another request at the requested time.",
  "path": "/api/assistance-requests/4/assign/1" }
```
`400 Bad Request` — validation
```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "Validation failed",
  "path": "/api/assistance-requests",
  "errors": { "pickupPoint": "Pickup point is required", "tripDate": "Trip date must be today or later" } }
```
Other: `400` "Completed requests cannot be cancelled." · `404` "Assistance request not found with id 999999" ·
`503` database not reachable · `500` generic message (no stack trace is ever returned).

## 20. Frontend usage
Open <http://localhost:8080/>. Tabs: **Dashboard** (counts) · **Requests** (table with status badges; per-row helper drop-down and
Assign / Complete / Cancel buttons — all buttons are always enabled so you can demonstrate that the *server* rejects invalid actions) ·
**New Request** · **Helpers** · **Workload** · **Users**. Green banner = success, red banner = error.

## 21. Swagger usage
<http://localhost:8080/swagger-ui.html> → expand an endpoint → *Try it out* → *Execute*. Raw OpenAPI JSON: `/v3/api-docs`.

## 22. Troubleshooting
| Problem | Fix |
|---------|-----|
| `Access denied for user 'root'@'localhost'` | Wrong MySQL password — set `DB_PASSWORD` (section 11). |
| `Communications link failure` / `Connection refused` | MySQL is not running or not on port 3306. |
| `Unable to load class 'com.mysql.cj.jdbc.Driver'` | Reload Maven project (IntelliJ: Maven tab → Reload). |
| `Port 8080 was already in use` | Stop the other program or add `server.port=8081` to `application.properties`. |
| `release version 21 not supported` | Use JDK 21 (Project Structure → SDK). |
| Dates rejected (400) | Use `YYYY-MM-DD` and `HH:mm`; trip must not be in the past. |
| Want fresh demo data | `DROP DATABASE transitassist_db;` and restart the app. |
| Tests fail to start | Tests use H2, not MySQL; run `mvn clean test` with internet access on the first run to download dependencies. |
