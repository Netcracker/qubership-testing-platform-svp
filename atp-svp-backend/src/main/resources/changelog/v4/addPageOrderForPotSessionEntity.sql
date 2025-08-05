-- liquibase formatted sql

-- changeset atp-svp-dev-v4:addPageOrderForPotSessionEntity
ALTER TABLE IF EXISTS pot_sessions
    ADD COLUMN IF NOT EXISTS page_order text;
