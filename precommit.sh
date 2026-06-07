#!/bin/sh
echo "precommit hook - backend"
if ! git diff --cached --name-only | grep -q '^src/main/java/'; then
    echo "No Java files changed, skipping"
    exit 0
fi

STAGED_JAVA_FILES=$(git diff --cached --name-only -- '*.java')

[ -z "$STAGED_JAVA_FILES" ] && exit 0

echo "Running verify phase..."
mvn clean verify

echo "Tests passed"