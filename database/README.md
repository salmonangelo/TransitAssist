# Database notes

* Database name: **transitassist_db** (MySQL 8.x)
* The application creates the database (`createDatabaseIfNotExist=true`) and the tables
  (`spring.jpa.hibernate.ddl-auto=update`) by itself. Demo data is inserted on the first start.
* `schema.sql` is a reference copy of the table design. Running it manually is optional.

## Useful queries for the viva

```sql
USE transitassist_db;

SELECT * FROM users;
SELECT * FROM helpers;
SELECT * FROM helper_skills;
SELECT * FROM assistance_requests;

-- requests with user and helper names
SELECT r.id, u.name AS user, h.name AS helper, r.trip_date, r.trip_time, r.status
FROM assistance_requests r
JOIN users u ON u.id = r.user_id
LEFT JOIN helpers h ON h.id = r.helper_id
ORDER BY r.trip_date, r.trip_time;

-- workload of every helper (non-cancelled requests)
SELECT h.name, r.trip_date, COUNT(*) AS requests
FROM helpers h JOIN assistance_requests r ON r.helper_id = h.id
WHERE r.status IN ('ASSIGNED','COMPLETED')
GROUP BY h.name, r.trip_date;
```

## Reset the demo data

```sql
DROP DATABASE transitassist_db;
```
Restart the application: the database, tables and demo data are created again.
