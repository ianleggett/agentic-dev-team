# Yaadt
## Yet another Agentic dev team idea!
Why? With so many new concepts like beads, cocoIndex.. the list goes on, you are installing npm and python packages straight from a script somewhere online. You can not be sure of CVEs and other security issues that may exist. So we containerise the dev team in docker containers and pre validate the container build before deploying it. You local machine stays clean from the sprawl.


# Complete Beads + OpenCode Agent Team


This bundle adds a durable multi-role software team to the devbox. Beads is the system of record; OpenCode/local LLM workers are disposable executors.

## Roles

Planning/design:
- `spec-writer [file-or-text]`
- `solution-architect [spec-file]`
- `tech-lead [architecture-or-spec-file]`

Implementation:
- `java-dev [bead-id]`
- `frontend-dev [bead-id]`
- `database-dev [bead-id]`
- `devops [bead-id]`

Quality/handoff:
- `qa-test [bead-id]`
- `code-reviewer [bead-id]`
- `security-reviewer [bead-id]`
- `docs-writer [bead-id]`

Utilities:
- `team-init`
- `team-status`
- `team-run-one <worker>`
- `beads-handoff <bead> <worker:...> <stage:...> [comment]`

## Routing model

Implementation workers call `bd ready --json` and filter by their primary `worker:*` label. Passing a bead ID explicitly bypasses queue selection and targets that bead.

Examples:

```bash
cd /workspace/my-project
team-init

spec-writer docs/ideas/customer-management.md
solution-architect docs/specs/customer-management.md
tech-lead docs/architecture/customer-management.md

team-status
java-dev
frontend-dev
qa-test
code-reviewer
```

Or target a bead directly:

```bash
java-dev bd-a1b2
qa-test bd-c3d4
```

## Durable coordination

The policies require:
- labels for routing/stage
- dependencies for blockers and ordering
- comments for handoffs
- `bd remember` for durable reusable knowledge
- `bd dolt push` at session end when a remote is configured

## Suggested labels

Primary workers:
`worker:spec`, `worker:architect`, `worker:tech-lead`, `worker:java`, `worker:frontend`, `worker:database`, `worker:devops`, `worker:qa`, `worker:review`, `worker:security`, `worker:docs`.

Stages:
`stage:spec`, `stage:architecture`, `stage:planning`, `stage:implementation`, `stage:testing`, `stage:review`, `stage:security-review`, `stage:documentation`.

Areas:
`area:backend`, `area:frontend`, `area:database`, `area:infra`, `area:test`, `area:security`, `area:docs`.

Keep project-specific `opencode.json`, source, specs, architecture docs, `AGENTS.md`, and `.beads/` inside the repository. Generic role policies are baked into the image under `/opt/agent-team`.
