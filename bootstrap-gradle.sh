#!/usr/bin/env bash
set -euo pipefail

if ! command -v gradle >/dev/null 2>&1; then
  echo "Install Gradle 9.6 or open the project in Android Studio first."
  exit 1
fi

gradle wrapper --gradle-version 9.6.0
chmod +x gradlew
