#!/bin/bash
# SessionStart hook: prepares Claude Code cloud sessions (task 01, cloud environment).
# Locally it does nothing.
set -euo pipefail

if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

# Docker for Testcontainers. The image has dockerd but no running daemon.
if ! docker info >/dev/null 2>&1; then
  nohup setsid dockerd >/var/log/dockerd.log 2>&1 < /dev/null &
  for _ in $(seq 1 30); do
    docker info >/dev/null 2>&1 && break
    sleep 1
  done
  if ! docker info >/dev/null 2>&1; then
    echo "dockerd did not start, see /var/log/dockerd.log" >&2
    exit 1
  fi
fi

# Java 25, installed by the environment's setup script. The image sets JAVA_HOME to 21.
java25=/usr/lib/jvm/java-25-openjdk-amd64
if [ -d "$java25" ] && [ -n "${CLAUDE_ENV_FILE:-}" ]; then
  echo "export JAVA_HOME=$java25" >> "$CLAUDE_ENV_FILE"
fi
