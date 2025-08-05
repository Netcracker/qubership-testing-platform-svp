-- liquibase formatted sql

-- changeset atp-svp-dev-v3:Create_table__pot_sessions
CREATE TABLE IF NOT EXISTS public.pot_sessions (
                                     session_id uuid not null,
                                     execution_configuration jsonb,
                                     execution_variables jsonb,
                                     already_validated boolean,
                                     session_pages_loading_already_started boolean,
                                     key_parameter jsonb,
                                     started timestamp
);

DO '
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''pot_sessions_pkey'') THEN
    ALTER TABLE ONLY public.pot_sessions
        ADD CONSTRAINT pot_sessions_pkey PRIMARY KEY (session_id);
    END IF;
END';

-- changeset atp-svp-dev-v3:Create_table__pot_session_page
CREATE TABLE IF NOT EXISTS public.pot_session_page (
                                         id uuid not null,
                                         already_validated boolean,
                                         tabs_loading_already_started boolean,
                                         name varchar(1000),
                                         validation_status integer,
                                         page_config_id uuid,
                                         pot_session_id uuid
);

DO '
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''pot_session_page_pkey'') THEN
    ALTER TABLE ONLY public.pot_session_page
        ADD CONSTRAINT pot_session_page_pkey PRIMARY KEY (id);
    END IF;
END';

DO '
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''fk_page_configuration_id'') THEN
    ALTER TABLE ONLY public.pot_session_page
        ADD CONSTRAINT fk_page_configuration_id FOREIGN KEY (page_config_id)
            REFERENCES public.page_configs (page_id) ON DELETE CASCADE;
    END IF;
END';

DO '
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''fk_pot_session_id'') THEN
    ALTER TABLE ONLY public.pot_session_page
        ADD CONSTRAINT fk_pot_session_id FOREIGN KEY (pot_session_id)
            REFERENCES public.pot_sessions (session_id) ON DELETE CASCADE;
    END IF;
END';

-- changeset atp-svp-dev-v3:Create_table__pot_session_tab
CREATE TABLE IF NOT EXISTS public.pot_session_tab (
                                        id uuid not null,
                                        already_validated boolean,
                                        synchronous_loading boolean,
                                        name varchar(1000),
                                        validation_status integer,
                                        pot_session_page_id uuid
);

DO '
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''pot_session_tab_pkey'') THEN
    ALTER TABLE ONLY public.pot_session_tab
        ADD CONSTRAINT pot_session_tab_pkey PRIMARY KEY (id);
    END IF;
END';

DO '
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''fk_pot_session_page_id'') THEN
    ALTER TABLE ONLY public.pot_session_tab
        ADD CONSTRAINT fk_pot_session_page_id FOREIGN KEY (pot_session_page_id)
            REFERENCES public.pot_session_page (id) ON DELETE CASCADE;
    END IF;
END';

-- changeset atp-svp-dev-v3:Create_table__pot_session_parameter
CREATE TABLE IF NOT EXISTS public.pot_session_parameter (
                                              parameter_id uuid not null,
                                              ar_values jsonb,
                                              er jsonb,
                                              group_name varchar(1000),
                                              synchronous_loading boolean not null,
                                              page varchar(1000),
                                              tab varchar(1000),
                                              validation_info jsonb,
                                              sut_parameter_id uuid,
                                              pot_session_id uuid,
                                              pot_session_tab_id uuid
);

DO '
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''pot_session_parameter_pkey'') THEN
    ALTER TABLE ONLY public.pot_session_parameter
        ADD CONSTRAINT pot_session_parameter_pkey PRIMARY KEY (parameter_id);
    END IF;
END';

DO '
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''fk_sut_parameter_id'') THEN
    ALTER TABLE ONLY public.pot_session_parameter
        ADD CONSTRAINT fk_sut_parameter_id FOREIGN KEY (sut_parameter_id)
            REFERENCES public.sut_parameter_config (parameter_id) ON DELETE CASCADE;
    END IF;
END';

DO '
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''fk_pot_session_id'') THEN
    ALTER TABLE ONLY public.pot_session_parameter
        ADD CONSTRAINT fk_pot_session_id FOREIGN KEY (pot_session_id)
            REFERENCES public.pot_sessions (session_id) ON DELETE CASCADE;
    END IF;
END';

DO '
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''fk_pot_session_tab_id'') THEN
    ALTER TABLE ONLY public.pot_session_parameter
        ADD CONSTRAINT fk_pot_session_tab_id FOREIGN KEY (pot_session_tab_id)
            REFERENCES public.pot_session_tab (id) ON DELETE CASCADE;
    END IF;
END';
