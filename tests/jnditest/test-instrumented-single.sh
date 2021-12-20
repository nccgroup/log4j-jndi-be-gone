#!/bin/sh

SCRIPT="$0"
cd `dirname "${SCRIPT}"`
SCRIPT=`basename "${SCRIPT}"`

while [ -L "${SCRIPT}" ]
do
  SCRIPT=`readlink "${SCRIPT}"`
  cd `dirname "${SCRIPT}"`
  SCRIPT=`basename "${SCRIPT}"`
done
SCRIPTDIR=`pwd -P`
cd "${SCRIPTDIR}"

set -e

if [ "$#" -lt "1" ]; then
  echo "usage: ${0} <log4j-version> [docker-image]" >&2
  exit 1
fi

DOCKER_IMAGE="openjdk:17-jdk"

if [ $# -gt 1 ]; then
  DOCKER_IMAGE="${2}"
fi

LOG4J_VERSION="${1}"

AGENT_ARGS=""
if [ $# -gt 2 ]; then
  AGENT_ARGS="=${3}"
fi

LOG4J_REPKG=""
if [ $# -gt 3 ]; then
  LOG4J_REPKG="${4}"
fi


cd ../../
./gradlew
cd "${SCRIPTDIR}"

if [ $# -gt 3 ]; then
  #./gradlew "-Plog4j=${LOG4J_VERSION}" "-Plog4jpkg=${LOG4J_REPKG}" shadowJar testJar
  ./gradlew "-Plog4j=${LOG4J_VERSION}" shadowJar testJar
else
  ./gradlew "-Plog4j=${LOG4J_VERSION}" shadowJar testJar
fi

docker run -it -v "${SCRIPTDIR}/build:/build:ro" "${DOCKER_IMAGE}" \
  java "-javaagent:build/libs/jnditest-all.jar${AGENT_ARGS}" -jar build/libs/jnditest-tests.jar
