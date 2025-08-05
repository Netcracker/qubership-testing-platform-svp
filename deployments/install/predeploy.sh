#!/usr/bin/env sh

if [ ! -f ./atp-common-scripts/openshift/common.sh ]; then
  echo "ERROR: Cannot locate ./atp-common-scripts/openshift/common.sh" && exit 1
fi

. ./atp-common-scripts/openshift/common.sh

_ns="${NAMESPACE}"
SVP_DB="$(env_default "${SVP_DB}" "${SERVICE_NAME}" "${_ns}")"
SVP_DB_USER="$(env_default "${SVP_DB_USER}" "${SERVICE_NAME}" "${_ns}")"
SVP_DB_PASSWORD="$(env_default "${SVP_DB_PASSWORD}" "${SERVICE_NAME}" "${_ns}")"

echo "***** Create postgres database and user"
init_pg "${PG_DB_ADDR}" "${SVP_DB}" "${SVP_DB_USER}" "${SVP_DB_PASSWORD}" "${PG_DB_PORT}" "${pg_user}" "${pg_pass}"

case ${PAAS_PLATFORM:-OPENSHIFT} in
  COMPOSE)
    mkdir -p data -m 777
    ;;
  OPENSHIFT|KUBERNETES)
    ;;
  *)
    echo "ERROR: Unsupported PAAS_PLATFORM '${PAAS_PLATFORM}'. Expected values: COMPOSE, OPENSHIFT, KUBERNETES"
    exit 1
esac

echo "***** Setting up encryption *****"
encrypt "${ENCRYPT}" "${SERVICE_NAME}"
