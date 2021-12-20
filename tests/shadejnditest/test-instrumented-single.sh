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

if [ ! -f "elastic-apm-agent-1.28.0.jar" ]; then
  wget https://github.com/elastic/apm-agent-java/releases/download/v1.28.0/elastic-apm-java-aws-lambda-layer-1.28.0.zip
  unzip elastic-apm-java-aws-lambda-layer-1.28.0.zip
fi

./gradlew "-Plog4j=${LOG4J_VERSION}" shadowJar testJar

docker run -it -v "${SCRIPTDIR}/:/jnditest:ro" -w "/jnditest" "${DOCKER_IMAGE}" \
  java "-javaagent:build/libs/jnditest-all.jar${AGENT_ARGS}" -javaagent:elastic-apm-agent-1.28.0.jar -jar build/libs/jnditest-tests.jar
#  java -javaagent:elastic-apm-agent-1.28.0.jar "-javaagent:build/libs/jnditest-all.jar${AGENT_ARGS}" -jar build/libs/jnditest-tests.jar

