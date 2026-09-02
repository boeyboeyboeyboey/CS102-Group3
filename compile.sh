#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="$PROJECT_ROOT/config.properties"

property() {
    awk -F= -v key="$1" '$1 == key {sub(/^[^=]*=/, ""); print; exit}' "$CONFIG_FILE"
}

JAVAC_COMMAND="$(property java.compiler.command)"
CLASSES_SETTING="$(property application.classes.directory)"

if [[ -z "$JAVAC_COMMAND" || -z "$CLASSES_SETTING" ]]; then
    echo "Missing compiler or classes-directory setting in config.properties" >&2
    exit 1
fi

CLASSES_DIRECTORY="$PROJECT_ROOT/$CLASSES_SETTING"
mkdir -p "$CLASSES_DIRECTORY"
find "$CLASSES_DIRECTORY" -type f -name '*.class' -delete

JAVA_SOURCES=()
while IFS= read -r -d '' SOURCE_FILE; do
    JAVA_SOURCES+=("$SOURCE_FILE")
done < <(find "$PROJECT_ROOT/src" -type f -name '*.java' -print0)
if [[ ${#JAVA_SOURCES[@]} -eq 0 ]]; then
    echo "No Java source files found under src/" >&2
    exit 1
fi

"$JAVAC_COMMAND" -encoding UTF-8 -d "$CLASSES_DIRECTORY" "${JAVA_SOURCES[@]}"
echo "Compiled ${#JAVA_SOURCES[@]} source files into $CLASSES_DIRECTORY"
