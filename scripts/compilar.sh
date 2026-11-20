#!/usr/bin/env bash
# Compila o projeto (exige JDK 17 ou superior). Gera as classes em out/classes.
set -e
cd "$(dirname "$0")/.."
rm -rf out/classes && mkdir -p out/classes
javac -encoding UTF-8 --release 17 -d out/classes -cp lib/h2-2.2.224.jar $(find src/main/java -name "*.java")
cp -r src/main/resources/* out/classes/
echo "Compilado em out/classes"
