#!/bin/sh
# no arguments opens the desktop window; any arguments go to the command line app
cd "$(dirname "$0")" || exit 1
JAR=target/auto-grading-system-1.0-SNAPSHOT.jar
if [ ! -f "$JAR" ]; then
    echo "$JAR not found. Run sh compile.sh first." >&2
    exit 1
fi
if [ "$#" -eq 0 ]; then
    echo "Opening the grading window; keep this terminal open while it runs."
    exec java -cp "$JAR" autograder.gui.GuiLauncher
fi
exec java -jar "$JAR" "$@"
