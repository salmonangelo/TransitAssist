-- Reference schema for TransitAssist (MySQL 8).
-- You do NOT need to run this: Hibernate creates the tables automatically (spring.jpa.hibernate.ddl-auto=update).
-- It is provided so the design can be explained/inspected in the viva.

CREATE DATABASE IF NOT EXISTS transitassist_db CHARACTER SET utf8mb4;
USE transitassist_db;

CREATE TABLE IF NOT EXISTS users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    phone         VARCHAR(20),
    password_hash VARCHAR(100),
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    active        BIT          NOT NULL DEFAULT 1,
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS helpers (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL,
    email      VARCHAR(150) NOT NULL,
    phone      VARCHAR(20),
    available  BIT          NOT NULL DEFAULT 1,
    active     BIT          NOT NULL DEFAULT 1,
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_helpers_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS helper_skills (
    helper_id BIGINT      NOT NULL,
    skill     VARCHAR(30) NOT NULL,
    CONSTRAINT fk_helper_skills_helper FOREIGN KEY (helper_id) REFERENCES helpers (id)
);

CREATE TABLE IF NOT EXISTS assistance_requests (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    helper_id        BIGINT       NULL,
    pickup_point     VARCHAR(150) NOT NULL,
    destination      VARCHAR(150) NOT NULL,
    trip_date        DATE         NOT NULL,
    trip_time        TIME         NOT NULL,
    assistance_type  VARCHAR(30)  NOT NULL,
    additional_notes VARCHAR(500),
    status           VARCHAR(20)  NOT NULL DEFAULT 'REQUESTED',
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ar_user   FOREIGN KEY (user_id)   REFERENCES users (id),
    CONSTRAINT fk_ar_helper FOREIGN KEY (helper_id) REFERENCES helpers (id),
    INDEX idx_ar_user (user_id),
    INDEX idx_ar_helper (helper_id),
    INDEX idx_ar_trip_date (trip_date),
    INDEX idx_ar_status (status)
);
