#!/bin/sh
# builds the runnable jar with the maven wrapper; only a jdk 17+ is required
cd "$(dirname "$0")" || exit 1
echo "Building... the first run downloads Maven and the dependencies (needs internet)"
# sh keeps this working when an unzip tool drops the executable bit
if ! sh ./mvnw -DskipTests package; then
    echo "Build failed. Check that javac -version and JAVA_HOME point to a JDK 17 or newer." >&2
    exit 1
fi
echo "Built target/auto-grading-system-1.0-SNAPSHOT.jar"
