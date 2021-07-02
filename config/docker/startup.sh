#!/bin/sh

set -e
set -x

JAVA_OPTS=${JAVA_OPTS:-"-Djava.lib.path=/opt/TDengine-client-2.1.3.2/driver"}

SENTINEL_OPTS=${SENTINEL_OPTS:-"-Dproject.name=sentinel-dashboard -Dcsp.sentinel.log.output.type=console"}

exex java ${JAVA_OPTS} ${SENTINEL_OPTS} -jar /opt/sentinel-dashboard.jar
