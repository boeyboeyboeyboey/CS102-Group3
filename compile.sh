#!/bin/sh
# builds the runnable jar with the maven wrapper; only a jdk 17+ is required
cd "$(dirname "$0")" || exit 1
if ! ./mvnw -q -DskipTests package; then
    echo "Build failed. Check that a JDK 17 or newer is on PATH (javac -version)." >&2
    exit 1
fi
echo "Built target/auto-grading-system-1.0-SNAPSHOT.jar"
