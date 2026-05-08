#!/bin/sh
echo "precommit hook - backend"
if ! git diff --cached --name-only | grep -q '^src/main/java/'; then
    echo "No Java files changed, skipping"
    exit 0
fi

mvn clean test