#!/bin/sh
# grades the 100-student demo cohort, generating it first when it is missing
# sh demo.sh [workers]            grade build/demo-cohort with demo.properties
# sh demo.sh generate [count] [seed]  rebuild the cohort (default 94 synthetic plus the 6 real)
cd "$(dirname "$0")" || exit 1
JAR=target/auto-grading-system-1.0-SNAPSHOT.jar
if [ ! -f "$JAR" ]; then
    echo "$JAR not found. Run sh compile.sh first." >&2
    exit 1
fi
if [ "$1" = "generate" ]; then
    shift
    exec java -cp "$JAR" autograder.demo.DemoCohortGenerator "$@"
fi
if [ "$#" -gt 1 ]; then
    echo "usage: sh demo.sh [workers] | sh demo.sh generate [count] [seed]" >&2
    exit 1
fi
if [ ! -d build/demo-cohort/submissions ]; then
    java -cp "$JAR" autograder.demo.DemoCohortGenerator || exit 1
fi
if [ "$#" -eq 1 ]; then
    exec java -jar "$JAR" --workers "$1" demo.properties
fi
exec java -jar "$JAR" demo.properties
