# Increment specs

We build the platform in increments. An increment spec says **what** one increment delivers and **why**, and how we know it's done. Its [tasks](../tasks/README.md) say **how**.

The format borrows from [OpenSpec](https://github.com/Fission-AI/OpenSpec) (requirements with WHEN/THEN scenarios) and from Matt Pocock's [skills](https://github.com/mattpocock/skills): `write-a-prd`, `prd-to-plan` and `grill-me` (user stories, durable decisions up front, tracer-bullet slices, and resolving open questions before work starts).

## How specs relate to the other docs

| Doc | Answers | Changes when |
|---|---|---|
| [Use cases](../use-cases/README.md) | What the finished system does | Behavior changes |
| Increment spec (this folder) | Which part of which use cases we build now, why, and how we check it | Never after it's done; it's a record |
| [Tasks](../tasks/README.md) | The technical plan, cut into tasks of one agent session each | The increment is planned or a task is done |
| [decisions.md](../decisions.md) | Why the system is built the way it is | A decision is made or changed |

A spec doesn't restate business rules. It links to the use case and says which steps and rules are in this increment. If planning an increment changes a rule or a decision, the use case or [decisions.md](../decisions.md) changes in the same commit.

## Lifecycle

```
Draft ──► Ready ──► In progress ──► Done
```

- **Draft:** being written. Open questions are listed in the spec.
- **Ready:** every open question is answered and the tasks exist.
- **In progress:** the first task has started.
- **Done:** the increment has been closed (see below).

## Planning an increment

1. Pick the use cases and read them, the [architecture](../architecture/overview.md) and the [decisions](../decisions.md).
2. Grill the plan: list every open question, give each a recommended answer, and let the product owner decide. Don't ask what the docs or the code already answer.
3. Write the spec from the template below.
4. Write the technical plan and the tasks ([tasks/README.md](../tasks/README.md#planning-the-tasks)).
5. Update the use cases and decisions that the answers changed. Set the spec to **Ready**.

## Closing an increment

1. Run the spec's demo.
2. Check every scenario. Each one is covered by an automated test or was shown in the demo.
3. Move anything left over into the next spec or the [future list](../use-cases/README.md#future-not-planned).
4. Set the spec to **Done** and update the index below.

## Template

```markdown
# NN Title

| | |
|---|---|
| Status | Draft |
| Use cases | UC-xx (which steps) |
| Tasks | [tasks/NN-name](../tasks/NN-name/README.md) |

## Why
The problem, and why this increment comes now.

## What changes
Bullets a stakeholder understands.

## User stories
1. As a <actor>, I want <capability>, so that <benefit>.

## Requirements
### Requirement: <name>
What the system does, linking the use case for the rules.

#### Scenario: <name>
- **GIVEN** <state>
- **WHEN** <action>
- **THEN** <observable outcome>

## Decisions
Answers to the open questions, with the reason. Durable ones also go into decisions.md or the architecture docs.

## Out of scope
What a reader might expect here but won't get, and where it lands instead.

## Demo
Commands and clicks to see the increment working.

## Risks
What could go wrong, and how the plan addresses it.
```

## Index

| # | Spec | Status |
|---|---|---|
| 01 | [Shop tracer bullet](01-shop-tracer-bullet.md) | Done |
| 02 | [Manage consultation offerings](02-manage-consultation-offerings.md) | Done |
| 03 | [Buy a consultation package](03-buy-consultation-package.md) | Done |
| 04 | [Book a consultation appointment](04-book-consultation-appointment.md) | Ready |
