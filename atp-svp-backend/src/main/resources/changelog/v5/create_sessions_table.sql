-- liquibase formatted sql

-- changeset atp-svp-dev-v5:Create_sessions_tables
CREATE TABLE IF NOT EXISTS sessions (
                            id uuid NOT NULL,
                            pod_name varchar NOT NULL,
                            CONSTRAINT sessions_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS sessions_id_idx ON public.sessions USING btree (id, pod_name);
CREATE INDEX IF NOT EXISTS sessions_pod_name_idx ON public.sessions USING btree (pod_name);
