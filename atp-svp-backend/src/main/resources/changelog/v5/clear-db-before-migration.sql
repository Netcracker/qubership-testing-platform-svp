-- liquibase formatted sql

-- changeset atp-svp-dev-v5:Truncate_potSession_tables
TRUNCATE TABLE pot_session_parameter, pot_session_tab, pot_session_page, pot_sessions;

-- changeset atp-svp-dev-v5:Remove_cascade_delete_from_potSession_tables
ALTER TABLE public.pot_session_parameter DROP CONSTRAINT IF EXISTS fk_sut_parameter_id;
ALTER TABLE public.pot_session_parameter
    ADD CONSTRAINT fk_sut_parameter_id FOREIGN KEY (sut_parameter_id)
        REFERENCES public.sut_parameter_config(parameter_id);

ALTER TABLE public.pot_session_page DROP CONSTRAINT IF EXISTS fk_page_configuration_id;
ALTER TABLE public.pot_session_page
    ADD CONSTRAINT fk_page_configuration_id FOREIGN KEY (page_config_id)
        REFERENCES public.page_configs(page_id);

-- changeset atp-svp-dev-v5:Remove_configs_before_migrates
DELETE FROM folders WHERE project_id IN (
    SELECT project_id FROM project_configs WHERE pages_source_type = 'GIT'
);
