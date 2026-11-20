#!/usr/bin/env bash
# Compila e roda todos os testes automatizados (JUnit 4).
set -e
cd "$(dirname "$0")/.."
./scripts/compilar.sh
rm -rf out/test-classes && mkdir -p out/test-classes
CP="out/classes:lib/h2-2.2.224.jar:out/test-classes:lib-teste/junit-4.13.2.jar:lib-teste/hamcrest-core-1.3.jar"
javac -encoding UTF-8 --release 17 -d out/test-classes -cp "$CP" $(find src/test/java -name "*.java")
CLASSES=$(cd out/test-classes && find . -name "*Test.class" | sed 's|^\./||; s|\.class$||; s|/|.|g' | sort | tr '\n' ' ')
java -Dfile.encoding=UTF-8 -cp "$CP" org.junit.runner.JUnitCore $CLASSES
