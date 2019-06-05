#!/bin/bash

PROJECTDIR=netbeans-project
SOOTLIB=${PROJECTDIR}/lib/sootclasses-trunk-jar-with-dependencies.jar


function build {
	mkdir -p build
	javac -d build -cp $SOOTLIB ${PROJECTDIR}/src/lac/jinn/*.java ${PROJECTDIR}/src/lac/jinn/*/*.java
}


function run {
	USER_PATH=$1
	USER_CLASS=$2
	java -cp ${PROJECTDIR}/lib/sootclasses-trunk-jar-with-dependencies.jar:build lac.jinn.ProfilerPassDriver -w -cp build:/home/juniocezar/Apps/java8/jre/lib/rt.jar:samples:${USER_PATH} $USER_CLASS
}


option=$1

if [ "$option" == "build" ]; then
	build
elif [ "$option" == "run" ]; then
	run $2 $3
elif [ "$option" == "clear" ]; then
	rm -r sootOutput build
else
	echo "Wrong syntax. Use commands build or run"
fi