#!/usr/bin/env bash
set -e

echo "Build and Start MacroMachine Backend Server"

mvn compile dependency:copy-dependencies
java -cp "target/classes;target/dependency/*" org.ironsight.wpplugin.macromachine.WebUIServer