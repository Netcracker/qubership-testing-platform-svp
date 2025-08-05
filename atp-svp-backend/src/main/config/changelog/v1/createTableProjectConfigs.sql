-- liquibase formatted sql

-- changeset atp-svp-dev-v1:Create_table_project_configs
CREATE TABLE IF NOT EXISTS public.project_configs
(
    project_id uuid NOT NULL,
    project_name character varying(255) NOT NULL,
    default_lc_range integer NOT NULL,
    full_info_pot boolean NOT NULL,
    pages_source_type character varying(20) NOT NULL,
    git_url character varying(500),
    path_folder_local_project character varying(1000),
    last_update_user_name character varying(255),
    last_update_date_time timestamp without time zone NOT NULL,
    CONSTRAINT project_configs_pkey PRIMARY KEY (project_id)
);

-- changeset atp-svp-dev-v1:Create_idx_pages_source_type_for_table_project_configs
CREATE INDEX IF NOT EXISTS idx_pages_source_type
    ON public.project_configs USING btree (pages_source_type);
