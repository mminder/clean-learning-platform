# 01 Cloud environment

| | |
|---|---|
| Increment | [01 Shop tracer bullet](../../specs/01-shop-tracer-bullet.md) |
| Type | HITL |
| Blocked by | – |
| Status | In progress |

## Goal

A fresh Claude Code cloud session of this repository can run the backend build with Java 25 and Testcontainers, and the frontend checks, without manual setup.

## What we know

Checked in a cloud session on 2026-11-07:

| Finding | Consequence |
|---|---|
| The image has JDK 21, and `JAVA_HOME` is set to `/usr/lib/jvm/java-21-openjdk-amd64`. | Install JDK 25 and point `JAVA_HOME` at it. Otherwise the Maven wrapper keeps using 21. |
| After `apt-get update`, Ubuntu's own repositories offer `openjdk-25-jdk-headless` (25.0.4). The network policy blocks Adoptium and GitHub release downloads. | Install it with apt. |
| The Docker CLI and `dockerd` are installed, but no daemon runs. Started as root with `dockerd &`, it works, and `postgres:18-alpine` pulls from Docker Hub. | Start the daemon in every session. A setup script can't keep a daemon running. |
| Maven Central and the npm registry are reachable. Node 22 and npm 10 are installed. | Nothing to do. |

## What to build

A human does this with Claude in a cloud session. Claude proposes each change; the human approves it.

1. **Setup script** of this repository's cloud environment (an environment setting, not a file in the repo):

   ```bash
   apt-get update && apt-get install -y openjdk-25-jdk-headless
   ```

2. **SessionStart hook**, committed under `.claude/`. It runs only in cloud sessions (when `CLAUDE_CODE_REMOTE` is set) and:
   - starts `dockerd` in the background if `docker info` fails, then waits until Docker answers;
   - writes `JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64` to `$CLAUDE_ENV_FILE` if that directory exists, so every later command uses Java 25.

   Locally the hook does nothing. Claude's `session-start-hook` skill covers how to write it.

3. **AGENTS.md:** one line saying that cloud sessions get Java 25 and Docker from the setup script and the hook.

## Acceptance criteria

- [x] The cloud environment used for this repository installs JDK 25 in its setup script.
- [ ] In a fresh cloud session, `java -version` and `mvn -v` report Java 25.
- [ ] In a fresh cloud session, `docker run --rm postgres:18-alpine postgres --version` works without starting anything by hand.
- [x] Without `CLAUDE_CODE_REMOTE`, the hook exits without doing anything.
- [x] AGENTS.md mentions the hook.

## Tests

None. The acceptance criteria are checked by hand in a fresh session.

## Out of scope

- The Maven project ([02](02-backend-skeleton.md)) and CI ([05](05-ci.md)).

## Notes

- 2026-11-07: Created the personal cloud environment **clean-learning-platform** (repository `mminder/clean-learning-platform`, same network as Default: package registries and MCP servers, no extra hosts) with the setup script above. The Default environment stays unchanged, because it serves every repository.
- Checked in the session that created it: the setup script installs 25.0.4, and `update-alternatives` makes `/usr/bin/java` Java 25, but `mvn` follows `JAVA_HOME`, so the hook is still needed. With the hook's env file sourced, `java -version` and `mvn -v` report 25. The hook starts `dockerd` in about a second, and `postgres:18-alpine` runs with a reachable mapped port. Run without `CLAUDE_CODE_REMOTE`, the hook writes nothing and starts nothing.
- Still open: the two "fresh cloud session" criteria. Start a new session of this repository on the **clean-learning-platform** environment (not Default), check `java -version`, `mvn -v` and `docker run --rm postgres:18-alpine postgres --version`, then tick them and set the task to **Done**. The hook only runs from a branch that contains it, so merge this change first.
