#!/bin/sh
set -eu

: "${AUTH_UPSTREAM:=authorization-service:8081}"
: "${CORE_UPSTREAM:=core-application-service:8082}"
: "${USER_UPSTREAM:=user-service:8084}"
: "${NOTIFICATION_UPSTREAM:=notification-service:8085}"
: "${FILE_UPSTREAM:=file-service:8086}"

export AUTH_UPSTREAM CORE_UPSTREAM USER_UPSTREAM NOTIFICATION_UPSTREAM FILE_UPSTREAM

envsubst '${AUTH_UPSTREAM} ${CORE_UPSTREAM} ${USER_UPSTREAM} ${NOTIFICATION_UPSTREAM} ${FILE_UPSTREAM}' < /etc/nginx/nginx.conf.template > /etc/nginx/nginx.conf

exec nginx -g 'daemon off;'
