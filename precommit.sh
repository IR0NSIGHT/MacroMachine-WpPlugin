#!/bin/sh
echo "precommit hook - backend"
if ! git diff --cached --name-only | grep -q '^src/main/java/'; then
    echo "No Java files changed, skipping"
    exit 0
fi

STAGED_JAVA_FILES=$(git diff --cached --name-only -- '*.java')

[ -z "$STAGED_JAVA_FILES" ] && exit 0

echo "Checking formatting..."

mvn spotless:check \
  -DspotlessFiles="$(echo "$STAGED_JAVA_FILES" | paste -sd, -)" || {
    echo "❌ Spotless check failed. Run: mvn spotless:apply"
    exit 1
}

echo "Running tests..."
mvn clean test

echo "Tests passed"