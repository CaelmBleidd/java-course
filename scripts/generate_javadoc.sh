#!/usr/bin/env bash
LINK=https://docs.oracle.com/en/java/javase/11/docs/api/;
PACKAGE=javaifmo.rain.menshutin.implementor
CLASSPATH=artifacts/info.kgeorgiy.java.advanced.implementor.jar:lib:out/production/java-advanced-2019/ru/ifmo/rain/menshutin/implementor
SOURCEPATH=java:modules

rm -rf javadoc6
mkdir javadoc6
javadoc -html5 -link ${LINK} -private -author -version -sourcepath ${SOURCEPATH} -classpath ${CLASSPATH} -d javadoc6 java/ru/ifmo/rain/menshutin/implementor/Implementor.java