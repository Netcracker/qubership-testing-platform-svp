#!/bin/sh

if [ "${ATP_INTERNAL_GATEWAY_ENABLED}" = "true" ]; then
  echo "Internal gateway integration is enabled."
  FEIGN_ATP_USERS_NAME=$FEIGN_ATP_INTERNAL_GATEWAY_NAME
  FEIGN_ATP_ENVIRONMENTS_NAME=$FEIGN_ATP_INTERNAL_GATEWAY_NAME
  FEIGN_ATP_LOGCOLLECTOR_NAME=$FEIGN_ATP_INTERNAL_GATEWAY_NAME
  FEIGN_ATP_BULKVALIDATOR_NAME=$FEIGN_ATP_INTERNAL_GATEWAY_NAME
  FEIGN_ATP_EI_NAME=$FEIGN_ATP_INTERNAL_GATEWAY_NAME
else
  echo "Internal gateway integration is disabled."
  FEIGN_ATP_USERS_ROUTE=
  FEIGN_ATP_ENVIRONMENTS_ROUTE=
  FEIGN_ATP_LOGCOLLECTOR_ROUTE=
  FEIGN_ATP_BULKVALIDATOR_ROUTE=
  FEIGN_ATP_EI_ROUTE=
fi

FUll_PATH_PROJECT_CONFIG_FILE="$PROJECTS_CONFIG_PATH/$PROJECTS_CONFIG_NAME"

if [ "${USE_RELEASE_PROJECTS}" = "false" ]; then
  echo "$PROJECTS_CONFIG">"${FUll_PATH_PROJECT_CONFIG_FILE}"
fi

## all parameter coming from OpenShift
# *** Set JVM options
JAVA_OPTIONS="${JAVA_OPTIONS} -Dspring.devtools.add-properties=false"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dspring.config.location=classpath:application.properties"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dspring.cloud.bootstrap.location=classpath:bootstrap.properties"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dsvp.projects.config.path=$PROJECTS_CONFIG_PATH"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dsvp.projects.config.name=$PROJECTS_CONFIG_NAME"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dlog.graylog.on=$GRAYLOG_ON"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dlog.graylog.host=$GRAYLOG_HOST"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dlog.graylog.port=$GRAYLOG_PORT"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dlog.level=$LOG_LEVEL"
# heap size restrictions
JAVA_OPTIONS="${JAVA_OPTIONS} -Xms${HEAP_XMS:-256m}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Xmx${HEAP_XMX:-1600m}"

# Profiler uses this path
/usr/bin/java -XX:+PrintFlagsFinal -XX:MaxRAM=${MAX_RAM:-2048m} ${JAVA_OPTIONS} -cp "./config/:./lib/*" org.qubership.atp.svp.Main
