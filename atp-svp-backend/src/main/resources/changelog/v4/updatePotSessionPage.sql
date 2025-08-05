-- liquibase formatted sql

-- changeset atp-svp-dev-v4:updatePotSessionPage
ALTER TABLE ONLY public.pot_session_page
    ADD COLUMN IF NOT EXISTS started TIMESTAMP NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS project_id UUID DEFAULT NULL;
