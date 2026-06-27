#!/usr/bin/env bash
set -e

echo "Starting MacroMachine Backend Server"

java -Djava.awt.headless=true -cp "target/classes:target/dependency/*" org.ironsight.wpplugin.macromachine.WebUIServer
