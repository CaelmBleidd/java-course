#!/usr/bin/env bash
export CLASSPATH=..

rmiregistry &

java -cp out/production/java-advanced-2019/ ru.ifmo.rain.menshutin.rmi.BankTest
