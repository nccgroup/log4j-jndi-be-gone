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
./gradlew
./gradlew shadowJar

for v in ${@:2} ; do
  cd "${SCRIPTDIR}"
  ./gradlew "-Plog4j=${v}" testJar 1>/dev/null 2>/dev/null
  docker run -it -v "${SCRIPTDIR}/build:/build:ro" "${DOCKER_IMAGE}" \
    java -javaagent:build/libs/jnditest-all.jar -jar build/libs/jnditest-tests.jar \
  | grep -q 'OK (1 test)'
  if [ "$?" = "0" ]; then
    echo "Version: ${v} - Pass"
  else
    echo "Version: ${v} - Fail"
  fi
done


