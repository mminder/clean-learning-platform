# Tasks

A task is one implementation step of an [increment spec](../specs/README.md). It's small enough for one agent session and one pull request, and it can be checked on its own.

The format borrows from Matt Pocock's [skills](https://github.com/mattpocock/skills) (`prd-to-plan`, `prd-to-issues`, `tdd`: vertical slices, AFK/HITL, "blocked by", test-first) and from [OpenSpec](https://github.com/Fission-AI/OpenSpec) (a technical plan next to checkable tasks).

## Layout

```
_docs/tasks/
├─ README.md                  this file
└─ NN-<increment>/            one folder per increment spec
   ├─ README.md               technical plan: task graph, durable decisions, names, shapes
   └─ NN-<task>.md            one file per task
```

## Task header

| Field | Values |
|---|---|
| Type | **AFK**: an agent can finish it alone. **HITL**: a human has to act; the task says exactly what they do. |
| Blocked by | Tasks that must be **Done** first. |
| Status | **Todo**, **In progress** or **Done**. It lives only in the task file. |

A task is **ready** when every task it's blocked by is **Done**.

## Working on a task

1. Take the task you were given, or the first ready one. Set it to **In progress** on your branch.
2. Read the task, the increment's technical plan (its `README.md`), the spec, the linked use cases, and the architecture doc for each side you touch.
3. Work test-first in small vertical steps: one failing test for one behavior, make it pass, refactor. Don't write all the tests up front.
4. Stay inside the task. If something is missing, add it under **Notes** in the task and tell the user; don't widen the task.
5. When every acceptance criterion holds and the definition of done is met, tick the criteria, set the task to **Done**, commit, and open the pull request (if you were asked to).

## Definition of done

Every task, on top of its own acceptance criteria:

- All checks that exist so far pass: `./mvnw verify` in `backend/`, and `npm run lint`, `npm run format:check`, `npm test`, `npm run build` and `npm run check:api` in `frontend/`.
- Each new route has a happy-path integration test and a row in the role matrix.
- `_docs/` matches the code: a changed behavior updates its use case, and a changed decision updates [decisions.md](../decisions.md).
- New or changed commands are in the **Commands** section of [AGENTS.md](../../AGENTS.md).
- Each TODO left in the code names a task or the future list.

## Planning the tasks

- **Vertical slices.** Each task delivers a thin path through every layer it touches and can be demonstrated or tested on its own. Pure scaffolding is the exception, and it stays minimal.
- **Durable decisions go in the technical plan**: routes, schema, response shapes, class names and test setup, everything other tasks rely on. Task files link to the plan instead of repeating it.
- **Glossary names**, as AGENTS.md requires.
- **Checkable acceptance criteria.** Each one can be checked by running a command, a test or a click.
- **Parallel where possible.** The task graph shows which tasks can run at the same time.
- Mark tasks AFK unless a human really has to act.

## Template

```markdown
# NN Title

| | |
|---|---|
| Increment | [NN name](../../specs/NN-name.md) |
| Type | AFK |
| Blocked by | [NN](NN-name.md) |
| Status | Todo |

## Goal
What works after this task that didn't before. One or two sentences.

## What to build
The behavior end to end, then the technical notes needed to get it right.
Link to the technical plan for names and shapes.

## Acceptance criteria
- [ ] Checkable statement

## Tests
The tests this task adds, by kind (domain, REST, authorization, frontend).

## Out of scope
What this task leaves for others.

## Notes
Found while working; empty at the start.
```

## Index

| Increment | Technical plan |
|---|---|
| 01 Shop tracer bullet | [01-shop-tracer-bullet/](01-shop-tracer-bullet/README.md) |
