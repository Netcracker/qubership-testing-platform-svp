-- liquibase formatted sql

-- changeset atp-svp-dev-v3:Create_table__ShedLock
CREATE TABLE IF NOT EXISTS shedlock(name VARCHAR(512) NOT NULL, lock_until TIMESTAMP NOT NULL,
                      locked_at TIMESTAMP NOT NULL, locked_by VARCHAR(255) NOT NULL, PRIMARY KEY (name));
