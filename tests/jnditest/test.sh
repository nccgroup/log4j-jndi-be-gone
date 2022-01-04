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

image="openjdk:6-jdk"
echo "${image}"
echo "============"
./test-instrumented-multi.sh "${image}" "2.0" "2.0.1" "2.0.2" "2.1" "2.2" "2.3"

for image in "openjdk:8-jdk" "ibm-semeru-runtimes:open-8-jdk" \
             "openjdk:11-jdk" "ibm-semeru-runtimes:open-11-jdk" \
             "eclipse-temurin:17-jdk" "ibm-semeru-runtimes:open-17-jdk"; do
  echo "${image}"
  echo "============"
  ./test-instrumented-all.sh "${image}"
  echo ""
done
