# Shared Agent Operating Model

YAADT uses one Beads database at `/workspace/.beads` across repositories below `/workspace`.

## Project routing

Every project has exactly one label `project:<name>`, mapping directly to `/workspace/<name>`.
Every executable task must have exactly one `project:*` label and one primary `worker:*` label.
Do not initialise separate `.beads` databases inside child repositories.

Use `BEADS_DIR=/workspace/.beads` for all Beads commands.

Workers use Beads for durable tasks, dependencies, labels, handoffs and `bd remember`.
CocoIndex is for semantic code discovery; Git remains the source-code/history system.

## Worker labels
`worker:spec`, `worker:architect`, `worker:tech-lead`, `worker:java`,
`worker:frontend`, `worker:database`, `worker:devops`, `worker:qa`,
`worker:review`, `worker:security`, `worker:docs`.

## Stages
`stage:spec`, `stage:architecture`, `stage:planning`, `stage:implementation`,
`stage:testing`, `stage:review`, `stage:security-review`, `stage:documentation`.

Before editing implementation work, claim the bead atomically. Before ending, leave a
durable Beads comment. Never close a bead until its acceptance criteria are satisfied.
