#!/bin/bash

if [ "$1" == "compile" ];
then 
	cd manager/console

	mvn install -DskipTests

	cd -
fi


rm manager/console/target/jnlp/launch.jnlp
cp launch.jnlp manager/console/target/jnlp

javaws -J-Djava.net.useSystemProxy=false -J-Xdebug -J-Xnoagent -J-Xrunjdwp:server=y,transport=dt_socket,address=4000,suspend=n launch.jnlp
