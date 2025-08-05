-- liquibase formatted sql

-- changeset atp-svp-dev-v3:Create_table__folders
CREATE TABLE IF NOT EXISTS public.folders (
                                folder_id uuid NOT NULL,
                                name character varying(1000),
                                project_id uuid NOT NULL,
                                last_update_date_time timestamp without time zone NOT NULL
);

DO '
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''folder_name_and_project_id_constraint'') THEN
    ALTER TABLE ONLY public.folders
        ADD CONSTRAINT folder_name_and_project_id_constraint UNIQUE (name, project_id);
    END IF;
END ';

DO '
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''folders_pkey'') THEN
    ALTER TABLE ONLY public.folders
        ADD CONSTRAINT folders_pkey PRIMARY KEY (folder_id);
    END IF;
END ';

DO '
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''fk_project_id__project'') THEN
    ALTER TABLE ONLY public.folders
        ADD CONSTRAINT fk_project_id__project FOREIGN KEY (project_id)
            REFERENCES public.project_configs (project_id) ON DELETE CASCADE;
    END IF;
END ';

-- changeset atp-svp-dev-v3:Create_table__key_parameters
CREATE TABLE IF NOT EXISTS public.key_parameters (
                                       key_parameter_id uuid NOT NULL,
                                       name character varying(1000),
                                       folder_id uuid NOT NULL,
                                       last_update_date_time timestamp without time zone NOT NULL,
                                       key_order integer NOT NULL
);

DO '
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''key_name_and_folder_id_constraint'') THEN
    ALTER TABLE ONLY public.key_parameters
        ADD CONSTRAINT key_name_and_folder_id_constraint UNIQUE (name, folder_id);
    END IF;
END ';

DO '
BEGIN
IF NOT EXISTS (SELECT 1
FROM pg_constraint
WHERE conname = ''key_parameters_pkey'') THEN
    ALTER TABLE ONLY public.key_parameters
        ADD CONSTRAINT key_parameters_pkey PRIMARY KEY (key_parameter_id);
    END IF;
END ';

DO '
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''fk_key_parameters__folder'') THEN
    ALTER TABLE ONLY public.key_parameters
        ADD CONSTRAINT fk_key_parameters__folder FOREIGN KEY (folder_id)
            REFERENCES public.folders (folder_id) ON DELETE CASCADE;
    END IF;
END';

-- changeset atp-svp-dev-v3:Create_table__page_configs
CREATE TABLE IF NOT EXISTS public.page_configs (
                                     page_id uuid NOT NULL,
                                     name character varying(1000),
                                     folder_id uuid NOT NULL,
                                     synchronous_loading boolean NOT NULL,
                                     last_update_date_time timestamp without time zone NOT NULL
);

DO '
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''page_configs_pkey'') THEN
    ALTER TABLE ONLY public.page_configs
        ADD CONSTRAINT page_configs_pkey PRIMARY KEY (page_id);
    END IF;
END';

DO '
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''page_name_and_folder_id_constraint'') THEN
    ALTER TABLE ONLY public.page_configs
        ADD CONSTRAINT page_name_and_folder_id_constraint UNIQUE (name, folder_id);
    END IF;
END';

DO '
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''fk_page_configs__folder'') THEN
    ALTER TABLE ONLY public.page_configs
        ADD CONSTRAINT fk_page_configs__folder FOREIGN KEY (folder_id)
            REFERENCES public.folders (folder_id) ON DELETE CASCADE;
    END IF;
END';

-- changeset atp-svp-dev-v3:Create_table__tab_configs
CREATE TABLE IF NOT EXISTS public.tab_configs (
                                    tab_id uuid NOT NULL,
                                    name character varying(1000),
                                    synchronous_loading boolean NOT NULL,
                                    page_id uuid,
                                    last_update_date_time timestamp without time zone NOT NULL,
                                    tab_order integer NOT NULL
);

DO '
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''tab_configs_pkey'') THEN
    ALTER TABLE ONLY public.tab_configs
        ADD CONSTRAINT tab_configs_pkey PRIMARY KEY (tab_id);
    END IF;
END';

DO '
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''tab_name_and_page_id_constraint'') THEN
    ALTER TABLE ONLY public.tab_configs
        ADD CONSTRAINT tab_name_and_page_id_constraint UNIQUE (name, page_id);
    END IF;
END';

DO '
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''fk_tab_configs__page'') THEN
    ALTER TABLE ONLY public.tab_configs
        ADD CONSTRAINT fk_tab_configs__page FOREIGN KEY (page_id)
            REFERENCES public.page_configs (page_id) ON DELETE CASCADE;
    END IF;
END';

-- changeset atp-svp-dev-v3:Create_table__group_configs
CREATE TABLE IF NOT EXISTS public.group_configs (
                                      group_id uuid NOT NULL,
                                      name character varying(1000),
                                      hidden boolean NOT NULL,
                                      synchronous_loading boolean NOT NULL,
                                      tab_id uuid,
                                      last_update_date_time timestamp without time zone NOT NULL,
                                      group_order integer NOT NULL
);

DO '
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''group_configs_pkey'') THEN
    ALTER TABLE ONLY public.group_configs
        ADD CONSTRAINT group_configs_pkey PRIMARY KEY (group_id);
    END IF;
END';

DO '
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''group_name_and_tab_id_constraint'') THEN
    ALTER TABLE ONLY public.group_configs
        ADD CONSTRAINT group_name_and_tab_id_constraint UNIQUE (name, tab_id);
    END IF;
END';

DO '
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''fk_group_configs__tab'') THEN
    ALTER TABLE ONLY public.group_configs
        ADD CONSTRAINT fk_group_configs__tab FOREIGN KEY (tab_id)
            REFERENCES public.tab_configs (tab_id) ON DELETE CASCADE;
    END IF;
END';

-- changeset atp-svp-dev-v3:Create_table__sut_parameter_config
CREATE TABLE IF NOT EXISTS public.sut_parameter_config (
                                             parameter_id uuid NOT NULL,
                                             name character varying(1000),
                                             additional_sources jsonb,
                                             component character varying(255),
                                             display_type character varying(100),
                                             er_config jsonb,
                                             preconfigured boolean DEFAULT false NOT NULL,
                                             source jsonb,
                                             group_id uuid,
                                             last_update_date_time timestamp without time zone NOT NULL,
                                             parameter_order integer NOT NULL
);

DO '
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''sut_parameter_config_pkey'') THEN
    ALTER TABLE ONLY public.sut_parameter_config
        ADD CONSTRAINT sut_parameter_config_pkey PRIMARY KEY (parameter_id);
    END IF;
END';

DO '
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''fk_sut_parameter__group'') THEN
    ALTER TABLE ONLY public.sut_parameter_config
        ADD CONSTRAINT fk_sut_parameter__group FOREIGN KEY (group_id)
            REFERENCES public.group_configs (group_id) ON DELETE CASCADE;
    END IF;
END';

-- changeset atp-svp-dev-v3:Create_table__common_parameters
CREATE TABLE IF NOT EXISTS public.common_parameters (
                                          parameter_id uuid NOT NULL,
                                          folder_id uuid NOT NULL
);

DO '
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''common_parameters_pkey'') THEN
    ALTER TABLE ONLY public.common_parameters
        ADD CONSTRAINT common_parameters_pkey PRIMARY KEY (parameter_id);
    END IF;
END';

DO '
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''fk_common_params__folder'') THEN
    ALTER TABLE ONLY public.common_parameters
        ADD CONSTRAINT fk_common_params__folder FOREIGN KEY (folder_id)
            REFERENCES public.folders (folder_id) ON DELETE CASCADE;
    END IF;
END ';

DO '
BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''fk_common_params__parameter'') THEN
    ALTER TABLE ONLY public.common_parameters
        ADD CONSTRAINT fk_common_params__parameter FOREIGN KEY (parameter_id)
            REFERENCES public.sut_parameter_config (parameter_id) ON DELETE CASCADE;
    END IF;
END ';
