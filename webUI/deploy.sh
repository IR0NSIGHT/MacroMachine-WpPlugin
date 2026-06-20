#!/usr/bin/env bash
set -e

rm -rf ./dist
npm run build
rm -rf ../src/main/resources/static/*
cp -r dist/* ../src/main/resources/static/
