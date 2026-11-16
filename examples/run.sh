#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mvn -q -DskipTests compile
mkdir -p examples/build
mvn -q dependency:build-classpath -Dmdep.outputFile=examples/build/classpath.txt
CP=$(cat examples/build/classpath.txt)
javac -cp "target/classes:$CP" -d examples/build examples/Server.java
exec java -cp "target/classes:examples/build:$CP" Server
