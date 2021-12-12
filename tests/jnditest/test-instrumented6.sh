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

cd ../../
./gradlew
cd "${SCRIPTDIR}"
./gradlew -Pjava6=true shadowJar testJar

docker run --read-only -it -v "${SCRIPTDIR}/build:/build:ro" openjdk:6-jdk \
  java -javaagent:build/libs/jnditest-all.jar -jar build/libs/jnditest-tests.jar
