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
./gradlew shadowJar testJar

docker run -it -v "${SCRIPTDIR}/build:/build:ro" openjdk:17-jdk \
  java -javaagent:build/libs/jnditest-all.jar -jar build/libs/jnditest-tests.jar
