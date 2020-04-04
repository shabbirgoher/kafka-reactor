#!/bin/sh -e

echo "Build application"
./gradlew --stacktrace clean build

echo "Create version file"
git rev-parse --short HEAD > version.txt
