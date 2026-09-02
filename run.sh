#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="$PROJECT_ROOT/config.properties"

property() {
    awk -F= -v key="$1" '$1 == key {sub(/^[^=]*=/, ""); print; exit}' "$CONFIG_FILE"
}

JAVA_COMMAND="$(property java.runtime.command)"
CLASSES_SETTING="$(property application.classes.directory)"
MAIN_CLASS="$(property application.main.class)"

if [[ -z "$JAVA_COMMAND" || -z "$CLASSES_SETTING" || -z "$MAIN_CLASS" ]]; then
    echo "Missing runtime setting in config.properties" >&2
    exit 1
fi

CLASSES_DIRECTORY="$PROJECT_ROOT/$CLASSES_SETTING"
MAIN_CLASS_FILE="$CLASSES_DIRECTORY/${MAIN_CLASS//.//}.class"
if [[ ! -f "$MAIN_CLASS_FILE" ]]; then
    echo "Application is not compiled. Run ./compile.sh first." >&2
    exit 1
fi

cd "$PROJECT_ROOT"
if [[ $# -eq 0 ]]; then
    exec "$JAVA_COMMAND" -cp "$CLASSES_DIRECTORY" "$MAIN_CLASS" --config "$CONFIG_FILE"
fi
exec "$JAVA_COMMAND" -cp "$CLASSES_DIRECTORY" "$MAIN_CLASS" "$@"
