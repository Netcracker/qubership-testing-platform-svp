{{/* Helper functions, do NOT modify */}}
{{- define "env.default" -}}
{{- $ctx := get . "ctx" -}}
{{- $def := get . "def" | default $ctx.Values.SERVICE_NAME -}}
{{- $pre := get . "pre" | default (eq $ctx.Values.PAAS_PLATFORM "COMPOSE" | ternary "" $ctx.Release.Namespace) -}}
{{- get . "val" | default ((empty $pre | ternary $def (print $pre "_" (trimPrefix "atp-" $def))) | nospace | replace "-" "_") -}}
{{- end -}}

{{- define "env.factor" -}}
{{- $ctx := get . "ctx" -}}
{{- get . "def" | default (eq $ctx.Values.PAAS_PLATFORM "COMPOSE" | ternary "1" (default "3" $ctx.Values.KAFKA_REPLICATION_FACTOR)) -}}
{{- end -}}

{{- define "env.compose" }}
{{- range $key, $val := merge (include "env.lines" . | fromYaml) (include "env.secrets" . | fromYaml) }}
{{ printf "- %s=%s" $key $val }}
{{- end }}
{{- end }}

{{- define "env.cloud" }}
{{- range $key, $val := (include "env.lines" . | fromYaml) }}
- name: {{ $key }}
  value: "{{ $val }}"
{{- end }}
{{- $keys := (include "env.secrets" . | fromYaml | keys | uniq | sortAlpha) }}
{{- if eq (default "" .Values.ENCRYPT) "secrets" }}
{{- $keys = concat $keys (list "ATP_CRYPTO_KEY" "ATP_CRYPTO_PRIVATE_KEY") }}
{{- end }}
{{- range $keys }}
- name: {{ . }}
  valueFrom:
    secretKeyRef:
      name: {{ $.Values.SERVICE_NAME }}-secrets
      key: {{ . }}
{{- end }}
{{- end }}

{{- define "securityContext.pod" -}}
runAsNonRoot: true
seccompProfile:
  type: "RuntimeDefault"
{{- with .Values.podSecurityContext }}
{{ toYaml . }}
{{- end -}}
{{- end -}}

{{- define "securityContext.container" -}}
allowPrivilegeEscalation: false
capabilities:
  drop: ["ALL"]
{{- with .Values.containerSecurityContext }}
{{ toYaml . }}
{{- end -}}
{{- end -}}
{{/* Helper functions end */}}

{{/* Environment variables to be used AS IS */}}
{{- define "env.lines" }}
ATP_BVT_URL: "{{ .Values.ATP_BVT_URL }}"
ATP_HTTP_LOGGING: "{{ .Values.ATP_HTTP_LOGGING }}"
ATP_HTTP_LOGGING_HEADERS: "{{ .Values.ATP_HTTP_LOGGING_HEADERS }}"
ATP_HTTP_LOGGING_HEADERS_IGNORE: "{{ .Values.ATP_HTTP_LOGGING_HEADERS_IGNORE }}"
ATP_HTTP_LOGGING_URI_IGNORE: "{{ .Values.ATP_HTTP_LOGGING_URI_IGNORE }}"
ATP_INTEGRATION_ENABLED: "{{ .Values.ATP_INTEGRATION_ENABLED }}"
ATP_INTERNAL_GATEWAY_ENABLED: "{{ .Values.ATP_INTERNAL_GATEWAY_ENABLED }}"
ATP_SERVICE_PATH: "{{ .Values.ATP_SERVICE_PATH }}"
ATP_SERVICE_PUBLIC: "{{ .Values.ATP_SERVICE_PUBLIC }}"
AUDIT_LOGGING_ENABLE: "{{ .Values.AUDIT_LOGGING_ENABLE }}"
AUDIT_LOGGING_TOPIC_NAME: "{{ include "env.default" (dict "ctx" . "val" .Values.AUDIT_LOGGING_TOPIC_NAME "def" "audit_logging_topic") }}"
AUDIT_LOGGING_TOPIC_PARTITIONS: "{{ .Values.AUDIT_LOGGING_TOPIC_PARTITIONS }}"
AUDIT_LOGGING_TOPIC_REPLICAS: "{{ include "env.factor" (dict "ctx" . "def" .Values.AUDIT_LOGGING_TOPIC_REPLICAS) }}"
CATALOGUE_URL: "{{ .Values.CATALOGUE_URL }}"
CONSUL_ENABLED: "{{ .Values.CONSUL_ENABLED }}"
CONSUL_PORT: "{{ .Values.CONSUL_PORT }}"
CONSUL_PREFIX: "{{ .Values.CONSUL_PREFIX }}"
CONSUL_TOKEN: "{{ .Values.CONSUL_TOKEN }}"
CONSUL_URL: "{{ .Values.CONSUL_URL }}"
CONTENT_SECURITY_POLICY: "{{ .Values.CONTENT_SECURITY_POLICY }}"
DEFERRED_SEARCH_RESULTS_LIFESPAN_SEC: "{{ .Values.DEFERRED_SEARCH_RESULTS_LIFESPAN_SEC }}"
EI_CLEAN_JOB_WORKDIR: "{{ .Values.EI_CLEAN_JOB_WORKDIR }}"
EI_CLEAN_JOB_FILE_DELETE_AFTER_MS: "{{ .Values.EI_CLEAN_JOB_FILE_DELETE_AFTER_MS }}"
EI_CLEAN_JOB_ENABLED: "{{ .Values.EI_CLEAN_JOB_ENABLED }}"
EI_CLEAN_SCHEDULED_JOB_PERIOD_MS: "{{ .Values.EI_CLEAN_SCHEDULED_JOB_PERIOD_MS }}"
EI_GRIDFS_DB: "{{ include "env.default" (dict "ctx" . "val" .Values.EI_GRIDFS_DB "def" "atp-ei-gridfs") }}"
EUREKA_CLIENT_ENABLED: "{{ .Values.EUREKA_CLIENT_ENABLED }}"
FEIGN_ATP_BULKVALIDATOR_NAME: "{{ .Values.FEIGN_ATP_BULKVALIDATOR_NAME }}"
FEIGN_ATP_BULKVALIDATOR_ROUTE: "{{ .Values.FEIGN_ATP_BULKVALIDATOR_ROUTE }}"
FEIGN_ATP_BULKVALIDATOR_URL: "{{ .Values.FEIGN_ATP_BULKVALIDATOR_URL }}"
FEIGN_ATP_EI_NAME: "{{ .Values.FEIGN_ATP_EI_NAME }}"
FEIGN_ATP_EI_ROUTE: "{{ .Values.FEIGN_ATP_EI_ROUTE }}"
FEIGN_ATP_EI_URL: "{{ .Values.FEIGN_ATP_EI_URL }}"
FEIGN_ATP_ENVIRONMENTS_NAME: "{{ .Values.FEIGN_ATP_ENVIRONMENTS_NAME }}"
FEIGN_ATP_ENVIRONMENTS_ROUTE: "{{ .Values.FEIGN_ATP_ENVIRONMENTS_ROUTE }}"
FEIGN_ATP_ENVIRONMENTS_URL: "{{ .Values.FEIGN_ATP_ENVIRONMENTS_URL }}"
FEIGN_ATP_INTERNAL_GATEWAY_NAME: "{{ .Values.FEIGN_ATP_INTERNAL_GATEWAY_NAME }}"
FEIGN_ATP_LOGCOLLECTOR_NAME: "{{ .Values.FEIGN_ATP_LOGCOLLECTOR_NAME }}"
FEIGN_ATP_LOGCOLLECTOR_ROUTE: "{{ .Values.FEIGN_ATP_LOGCOLLECTOR_ROUTE }}"
FEIGN_ATP_LOGCOLLECTOR_URL: "{{ .Values.FEIGN_ATP_LOGCOLLECTOR_URL }}"
FEIGN_ATP_USERS_NAME: "{{ .Values.FEIGN_ATP_USERS_NAME }}"
FEIGN_ATP_USERS_ROUTE: "{{ .Values.FEIGN_ATP_USERS_ROUTE }}"
FEIGN_ATP_USERS_URL: "{{ .Values.FEIGN_ATP_USERS_URL }}"
FEIGN_CONNECT_TIMEOUT: "{{ .Values.FEIGN_CONNECT_TIMEOUT }}"
FEIGN_READ_TIMEOUT: "{{ .Values.FEIGN_READ_TIMEOUT }}"
GRAYLOG_HOST: "{{ .Values.GRAYLOG_HOST }}"
GRAYLOG_ON: "{{ .Values.GRAYLOG_ON }}"
GRAYLOG_PORT: "{{ .Values.GRAYLOG_PORT }}"
GRIDFS_DB_ADDR: "{{ .Values.GRIDFS_DB_ADDR }}"
GRIDFS_DB_PORT: "{{ .Values.GRIDFS_DB_PORT }}"
HEAP_XMS: "{{ .Values.HEAP_XMS }}"
HEAP_XMX: "{{ .Values.HEAP_XMX }}"
HIKARI_MAX_POOL_SIZE: '{{ .Values.HIKARI_MAX_POOL_SIZE }}'
HIKARI_MIN_POOL_SIZE: '{{ .Values.HIKARI_MIN_POOL_SIZE }}'
JAVA_OPTIONS: "-Dcom.sun.management.jmxremote={{ .Values.JMX_ENABLE }} -Dcom.sun.management.jmxremote.port={{ .Values.JMX_PORT }} -Dcom.sun.management.jmxremote.rmi.port={{ .Values.JMX_RMI_PORT }} -Djava.rmi.server.hostname=127.0.0.1 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false {{ .Values.ADDITIONAL_JAVA_OPTIONS }}"
KAFKA_CLIENT_ID: "atp-svp-{{ .Release.Namespace }}"
KAFKA_ENABLE: "{{ .Values.KAFKA_ENABLE }}"
KAFKA_GROUP_ID: "atp-svp-{{ .Release.Namespace }}"
KAFKA_LOGCOLLECTOR_EVENT_ENABLE: "{{ .Values.KAFKA_LOGCOLLECTOR_EVENT_ENABLE }}"
KAFKA_PROJECT_EVENT_ENABLE: "{{ .Values.KAFKA_PROJECT_EVENT_ENABLE }}"
KAFKA_REPORTING_SERVERS: '{{ .Values.KAFKA_REPORTING_SERVERS }}'
KAFKA_SERVERS: "{{ .Values.KAFKA_SERVERS }}"
KAFKA_SERVICE_ENTITIES_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_SERVICE_ENTITIES_TOPIC "def" "service entities") }}"
KAFKA_SERVICE_ENTITIES_TOPIC_PARTITIONS: "{{ .Values.KAFKA_SERVICE_ENTITIES_TOPIC_PARTITIONS }}"
KAFKA_SERVICE_ENTITIES_TOPIC_REPLICATION_FACTOR: "{{ include "env.factor" (dict "ctx" . "def" .Values.KAFKA_SERVICE_ENTITIES_TOPIC_REPLICATION_FACTOR) }}"
KAFKA_SVP_EVENT_ENABLE: "{{ .Values.KAFKA_SVP_EVENT_ENABLE }}"
KAFKA_SVP_GET_INFO_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_SVP_GET_INFO_TOPIC "def" "svp_get_info_notification_topic") }}"
KAFKA_SVP_GET_INFO_TOPIC_PARTITIONS: "{{ .Values.KAFKA_SVP_GET_INFO_TOPIC_PARTITIONS }}"
KAFKA_SVP_GET_INFO_TOPIC_REPLICATION: "{{ include "env.factor" (dict "ctx" . "def" .Values.KAFKA_SVP_GET_INFO_TOPIC_REPLICATION) }}"
KAFKA_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_TOPIC "def" "catalog_notification_topic") }}"
KAFKA_TOPIC_END_LOGCOLLECTOR: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_TOPIC_END_LOGCOLLECTOR "def" "atp_lc_end_execution") }}"
KEYCLOAK_AUTH_URL: "{{ .Values.KEYCLOAK_AUTH_URL }}"
KEYCLOAK_ENABLED: "{{ .Values.KEYCLOAK_ENABLED }}"
KEYCLOAK_REALM: "{{ .Values.KEYCLOAK_REALM }}"
LIQUIBASE_MIGRATION_ENABLE: "{{ .Values.LIQUIBASE_MIGRATION_ENABLE }}"
LOCALE_RESOLVER: "{{ .Values.LOCALE_RESOLVER }}"
LOG_LEVEL: "{{ .Values.LOG_LEVEL }}"
MAX_RAM: "{{ .Values.MAX_RAM }}"
MICROSERVICE_NAME: "{{ .Values.SERVICE_NAME }}"
OPENSHIFT_HOST: "{{ default .Values.CLOUD_PUBLIC_HOST .Values.OPENSHIFT_HOST }}"
OPENSHIFT_PROJECT: "{{ .Release.Namespace }}"
PG_DB_ADDR: "{{ .Values.PG_DB_ADDR }}"
PG_DB_PORT: "{{ .Values.PG_DB_PORT }}"
PROFILER_ENABLED: "{{ .Values.PROFILER_ENABLED }}"
PROJECTS_CONFIG: "{{ .Values.PROJECTS_CONFIG }}"
PROJECTS_CONFIG_NAME: "{{ .Values.PROJECTS_CONFIG_NAME }}"
PROJECTS_CONFIG_PATH: "{{ .Values.PROJECTS_CONFIG_PATH }}"
PROJECT_INFO_ENDPOINT: "{{ .Values.PROJECT_INFO_ENDPOINT }}"
REMOTE_DUMP_HOST: "{{ .Values.REMOTE_DUMP_HOST }}"
REMOTE_DUMP_PORT: "{{ .Values.REMOTE_DUMP_PORT }}"
REST_TIMEOUT_SEC: "{{ .Values.REST_TIMEOUT_SEC }}"
SERVICE_ENTITIES_MIGRATION_ENABLED: "{{ .Values.SERVICE_ENTITIES_MIGRATION_ENABLED }}"
SERVICE_REGISTRY_URL: "{{ .Values.SERVICE_REGISTRY_URL }}"
SESSION_LIFESPAN: "{{ .Values.SESSION_LIFESPAN }}"
SPRING_PROFILES: "{{ .Values.SPRING_PROFILES }}"
SVP_DB: "{{ include "env.default" (dict "ctx" . "val" .Values.SVP_DB) }}"
SVP_GETTING_INFO_THREAD_POOL_CORE_SIZE: "{{ .Values.SVP_GETTING_INFO_THREAD_POOL_CORE_SIZE }}"
SVP_GETTING_INFO_THREAD_POOL_MAX_SIZE: "{{ .Values.SVP_GETTING_INFO_THREAD_POOL_MAX_SIZE }}"
SVP_GETTING_INFO_THREAD_POOL_QUEUE_CAPACITY: "{{ .Values.SVP_GETTING_INFO_THREAD_POOL_QUEUE_CAPACITY }}"
SVP_VALIDATION_THREAD_POOL_CORE_SIZE: "{{ .Values.SVP_VALIDATION_THREAD_POOL_CORE_SIZE }}"
SVP_VALIDATION_THREAD_POOL_MAX_SIZE: "{{ .Values.SVP_VALIDATION_THREAD_POOL_MAX_SIZE }}"
SVP_VALIDATION_THREAD_POOL_QUEUE_CAPACITY: "{{ .Values.SVP_VALIDATION_THREAD_POOL_QUEUE_CAPACITY }}"
SWAGGER_ENABLED: "{{ .Values.SWAGGER_ENABLED }}"
UNDERTOW_THREADS_IO: '{{ .Values.UNDERTOW_THREADS_IO }}'
UNDERTOW_THREADS_WORKER: '{{ .Values.UNDERTOW_THREADS_WORKER }}'
USE_RELEASE_PROJECTS: "{{ .Values.USE_RELEASE_PROJECTS }}"
VAULT_ENABLE: "{{ .Values.VAULT_ENABLE }}"
VAULT_NAMESPACE: "{{ .Values.VAULT_NAMESPACE }}"
VAULT_ROLE_ID: "{{ .Values.VAULT_ROLE_ID }}"
VAULT_URI: "{{ .Values.VAULT_URI }}"
WEBSOCKET_BUFFER_SIZE_LIMIT_MB: "{{ .Values.WEBSOCKET_BUFFER_SIZE_LIMIT_MB }}"
WEBSOCKET_TIMOUT_LIMIT_SEC: "{{ .Values.WEBSOCKET_TIMOUT_LIMIT_SEC }}"
ZIPKIN_ENABLE: "{{ .Values.ZIPKIN_ENABLE }}"
ZIPKIN_PROBABILITY: "{{ .Values.ZIPKIN_PROBABILITY }}"
ZIPKIN_URL: "{{ .Values.ZIPKIN_URL }}"
{{- end }}

{{/* Sensitive data to be converted into secrets whenever possible */}}
{{- define "env.secrets" }}
EI_GRIDFS_PASSWORD: "{{ include "env.default" (dict "ctx" . "val" .Values.EI_GRIDFS_PASSWORD "def" "atp-ei-gridfs") }}"
EI_GRIDFS_USER: "{{ include "env.default" (dict "ctx" . "val" .Values.EI_GRIDFS_USER "def" "atp-ei-gridfs") }}"
SVP_DB_PASSWORD: "{{ include "env.default" (dict "ctx" . "val" .Values.SVP_DB_PASSWORD "def" .Values.SERVICE_NAME ) }}"
SVP_DB_USER: "{{ include "env.default" (dict "ctx" . "val" .Values.SVP_DB_USER "def" .Values.SERVICE_NAME ) }}"
KEYCLOAK_CLIENT_NAME: "{{ default "atp-svp" .Values.KEYCLOAK_CLIENT_NAME }}"
KEYCLOAK_SECRET: "{{ default "" .Values.KEYCLOAK_SECRET }}"
VAULT_SECRET_ID: "{{ default "" .Values.VAULT_SECRET_ID }}"
{{- end }}

{{- define "env.deploy" }}
SERVICE_NAME: "{{ .Values.SERVICE_NAME }}"
pg_pass: "{{ .Values.pg_pass }}"
pg_user: "{{ .Values.pg_user }}"
{{- end }}
