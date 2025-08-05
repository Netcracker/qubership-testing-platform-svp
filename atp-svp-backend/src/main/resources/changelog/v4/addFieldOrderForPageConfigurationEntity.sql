-- liquibase formatted sql

-- changeset atp-svp-dev-v4:addFieldOrderForPageConfigurationEntity
ALTER TABLE IF EXISTS page_configs
    ADD COLUMN IF NOT EXISTS "order" INTEGER DEFAULT (null);
