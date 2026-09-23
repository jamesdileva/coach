#!/usr/bin/env bash
# Release build for Project Coach (Sprint 32).
# Usage: ./release/run.sh
# Optional signing: COACH_KEYSTORE=/path/coach.jks COACH_KEY_ALIAS=coach COACH_KEY_PASS=*** ./release/run.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT/plugin"

if [[ -z "${JAVA_HOME:-}" ]]; then
  if [[ -d "$HOME/tools/jdk/jdk-11.0.32+9" ]]; then
    export JAVA_HOME="$HOME/tools/jdk/jdk-11.0.32+9"
  fi
fi

echo "== Coach release build (JAVA_HOME=${JAVA_HOME:-system}) =="
./gradlew --no-daemon clean buildRelease "$@"

echo
echo "Release package: $ROOT/plugin/build/release/"
ls -la "$ROOT/plugin/build/release/"
