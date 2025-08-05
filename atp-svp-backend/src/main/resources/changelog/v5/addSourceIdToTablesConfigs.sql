-- liquibase formatted sql

-- changeset atp-svp-dev-v5:add_source_id_to_config_tables
ALTER TABLE IF EXISTS folders ADD IF NOT EXISTS source_id uuid NULL;

ALTER TABLE IF EXISTS page_configs ADD IF NOT EXISTS source_id uuid NULL;

ALTER TABLE IF EXISTS common_parameters ADD IF NOT EXISTS source_id uuid NULL;

ALTER TABLE IF EXISTS key_parameters ADD IF NOT EXISTS source_id uuid NULL;
