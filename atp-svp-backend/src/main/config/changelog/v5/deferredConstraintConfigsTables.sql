-- liquibase formatted sql

-- changeset atp-svp-dev-v5:Deferred_constraint_for_config_tables
ALTER TABLE public.page_configs DROP CONSTRAINT IF EXISTS page_name_and_folder_id_constraint;
ALTER TABLE public.page_configs
    ADD CONSTRAINT IF NOT EXISTS page_name_and_folder_id_constraint UNIQUE (name, folder_id)
    DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE public.tab_configs DROP CONSTRAINT IF EXISTS tab_name_and_page_id_constraint;
ALTER TABLE public.tab_configs
    ADD CONSTRAINT IF NOT EXISTS tab_name_and_page_id_constraint UNIQUE (name, page_id)
    DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE public.group_configs DROP CONSTRAINT IF EXISTS group_name_and_tab_id_constraint;
ALTER TABLE public.group_configs
    ADD CONSTRAINT IF NOT EXISTS group_name_and_tab_id_constraint UNIQUE (name, tab_id)
    DEFERRABLE INITIALLY DEFERRED;
