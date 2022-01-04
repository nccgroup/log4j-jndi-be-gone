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

if [ "$#" -lt "2" ]; then
  echo "usage: ${0} <docker-image> [log4j-versions...]" >&2
  exit 1
fi

DOCKER_IMAGE="${1}"

cd ../../
./gradlew clean
./gradlew
./gradlew shadowJar

cd "${SCRIPTDIR}"

set +e
if [ "${TRACE}" = "1" ]; then
  set -o xtrace
fi

for v in ${@:2} ; do
  ./gradlew "-Plog4j=${v}" testJar 1>/dev/null 2>/dev/null
  docker run -it -v "${SCRIPTDIR}/build:/build:ro" "${DOCKER_IMAGE}" \
    java -javaagent:build/libs/jnditest-all.jar -jar build/libs/jnditest-tests.jar \
  | grep -q 'OK (1 test)'
  if [ "$?" = "0" ]; then
    echo "Version: ${v} - Pass"
  else
    echo "Version: ${v} - Fail"
  fi

  docker run -it -v "${SCRIPTDIR}/build:/build:ro" "${DOCKER_IMAGE}" \
    java -javaagent:build/libs/jnditest-all.jar=structureMatch=0 -jar build/libs/jnditest-tests.jar \
  | grep -q 'OK (1 test)'
  if [ "$?" = "0" ]; then
    echo "Version: ${v} - Pass (structureMatch=0)"
  else
    echo "Version: ${v} - Fail (structureMatch=0)"
  fi

  docker run -it -v "${SCRIPTDIR}/../..:/log4j-jndi-be-gone:ro" "${DOCKER_IMAGE}" \
    java -javaagent:/log4j-jndi-be-gone/build/libs/log4j-jndi-be-gone-1.1.0-standalone.jar -jar /log4j-jndi-be-gone/tests/jnditest/build/libs/jnditest-tests.jar \
  | grep -q 'OK (1 test)'
  if [ "$?" = "0" ]; then
    echo "Version: ${v} - Pass (standalone)"
  else
    echo "Version: ${v} - Fail (standalone)"
  fi

  docker run -it -v "${SCRIPTDIR}/../..:/log4j-jndi-be-gone:ro" "${DOCKER_IMAGE}" \
    java -javaagent:/log4j-jndi-be-gone/build/libs/log4j-jndi-be-gone-1.1.0-standalone.jar=structureMatch=0 -jar /log4j-jndi-be-gone/tests/jnditest/build/libs/jnditest-tests.jar \
  | grep -q 'OK (1 test)'
  if [ "$?" = "0" ]; then
    echo "Version: ${v} - Pass (standalone, structureMatch=0)"
  else
    echo "Version: ${v} - Fail (standalone, structureMatch=0)"
  fi

done


