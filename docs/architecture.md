# Architecture

TransitAssist is a classic layered Spring Boot application.

```mermaid
flowchart TD
    Browser["Browser (index.html + app.js)<br/>or Postman / Swagger UI"] -->|HTTP + JSON| C
    subgraph SpringBoot["Spring Boot application"]
        C["Controllers<br/>UserController, HelperController, AssistanceRequestController<br/>(HTTP only, @Valid on DTOs)"]
        S["Services (@Service @Transactional)<br/>UserService, HelperService, AssistanceRequestService<br/>ALL business rules live here"]
        R["Repositories (Spring Data JPA)<br/>UserRepository, HelperRepository, AssistanceRequestRepository"]
        E["Entities (JPA / Hibernate)<br/>User, Helper, AssistanceRequest"]
        X["GlobalExceptionHandler (@RestControllerAdvice)<br/>JSON error responses"]
        C --> S --> R --> E
        C -.exceptions.-> X
        S -.exceptions.-> X
    end
    E -->|JDBC| DB[("MySQL: transitassist_db")]
```

| Layer | Package | Responsibility |
|-------|---------|----------------|
| Controller | `controller` | Map URLs to service calls, validate DTOs, choose HTTP status |
| Service | `service` | Business rules, state transitions, transactions |
| Repository | `repository` | Database access through `JpaRepository` |
| Entity | `entity` | Tables and relationships |
| DTO | `dto` | Request / response objects (entities are never exposed) |
| Exception | `exception` | `ResourceNotFoundException`, `BusinessRuleException`, `GlobalExceptionHandler` |
| Config | `config` | `DataInitializer` (demo data), `OpenApiConfig` (Swagger) |

## Assign-helper sequence

```mermaid
sequenceDiagram
    participant UI as Client
    participant C as AssistanceRequestController
    participant S as AssistanceRequestService
    participant DB as MySQL
    UI->>C: POST /api/assistance-requests/{id}/assign/{helperId}
    C->>S: assign(id, helperId)
    S->>DB: load request
    S->>S: reject if COMPLETED or CANCELLED (400)
    S->>DB: load helper FOR UPDATE (row lock)
    S->>S: reject if helper inactive / unavailable (400)
    S->>DB: load helper's ASSIGNED + COMPLETED requests
    S->>S: overlap check on date + time (409 on conflict)
    S->>DB: save request (status = ASSIGNED, helper set)
    S-->>C: AssistanceRequestResponse
    C-->>UI: 200 OK + JSON
```

## Overlap rule

Every request occupies a slot of `transitassist.assistance-duration-minutes` (default **60**) starting at its trip
time. Two slots overlap when `startA < endB AND startB < endA`. So 10:00 and 10:30 overlap, 10:00 and 11:00 do not.
