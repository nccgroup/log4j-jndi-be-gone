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

if [ "$#" != "1" ]; then
  echo "usage: ${0} <docker-image>" >&2
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

for v in "2.0" "2.0.1" "2.0.2" "2.1" "2.2" "2.3" "2.4" "2.4.1" "2.5" "2.6" "2.6.1" "2.6.2" "2.7" \
         "2.8" "2.8.1" "2.8.2" "2.9.0" "2.9.1" "2.10.0" "2.11.0" "2.11.1" "2.11.2" "2.12.0" "2.12.1" "2.12.2" \
         "2.13.0" "2.13.1" "2.13.2" "2.13.3" "2.14.0" "2.14.1" "2.15.0" "2.16.0" "2.17.0" ; do
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


