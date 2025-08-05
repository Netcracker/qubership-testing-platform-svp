-- liquibase formatted sql

-- changeset atp-svp-dev-v5:add_cascade_remove_for_pot_session
ALTER TABLE public.pot_session_parameter DROP CONSTRAINT IF EXISTS fk_sut_parameter_id;
ALTER TABLE public.pot_session_parameter
    ADD CONSTRAINT fk_sut_parameter_id FOREIGN KEY (sut_parameter_id)
        REFERENCES public.sut_parameter_config(parameter_id) ON DELETE CASCADE;

ALTER TABLE public.pot_session_page DROP CONSTRAINT IF EXISTS fk_page_configuration_id;
ALTER TABLE public.pot_session_page
    ADD CONSTRAINT fk_page_configuration_id FOREIGN KEY (page_config_id)
        REFERENCES public.page_configs(page_id) ON DELETE CASCADE;
