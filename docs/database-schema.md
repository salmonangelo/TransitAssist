# Database Schema

Database: `transitassist_db` (MySQL). Tables are created automatically by Hibernate (`ddl-auto=update`).
A reference SQL script is in [`database/schema.sql`](../database/schema.sql).

```mermaid
erDiagram
    USERS ||--o{ ASSISTANCE_REQUESTS : "creates"
    HELPERS |o--o{ ASSISTANCE_REQUESTS : "fulfils"
    HELPERS ||--o{ HELPER_SKILLS : "has"

    USERS {
        BIGINT id PK
        VARCHAR name
        VARCHAR email UK
        VARCHAR phone
        VARCHAR password_hash
        VARCHAR role
        BIT active
        DATETIME created_at
    }
    HELPERS {
        BIGINT id PK
        VARCHAR name
        VARCHAR email UK
        VARCHAR phone
        BIT available
        BIT active
        DATETIME created_at
    }
    HELPER_SKILLS {
        BIGINT helper_id FK
        VARCHAR skill
    }
    ASSISTANCE_REQUESTS {
        BIGINT id PK
        BIGINT user_id FK
        BIGINT helper_id FK "nullable"
        VARCHAR pickup_point
        VARCHAR destination
        DATE trip_date
        TIME trip_time
        VARCHAR assistance_type
        VARCHAR additional_notes
        VARCHAR status
        DATETIME created_at
        DATETIME updated_at
    }
```

## Indexes on `assistance_requests`
`idx_ar_user (user_id)`, `idx_ar_helper (helper_id)`, `idx_ar_trip_date (trip_date)`, `idx_ar_status (status)`.

## Relationships
* **users 1 — * assistance_requests** (`assistance_requests.user_id`, NOT NULL)
* **helpers 1 — * assistance_requests** (`assistance_requests.helper_id`, NULL until assigned)
* **helpers 1 — * helper_skills** (`@ElementCollection`, one row per skill)

Enums (`role`, `assistance_type`, `status`, `skill`) are stored as text with `@Enumerated(EnumType.STRING)`.
