#!/bin/bash

JAR=runtime/standalone/target/manager-runtime-standalone-2019.1-SNAPSHOT-selfcontained.jar
_HOME=${HOME}/.otc-home

clear

set -x

mkdir -p ${_HOME}

sudo java \
	-Dmanager.home=${_HOME} \
	-Dlogging.file=${_HOME}/manager.log \
	-jar ${JAR} \
	2>&1 | tee launch-standalone.log;
