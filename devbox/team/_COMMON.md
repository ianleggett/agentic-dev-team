# Shared Agent Operating Model

YAADT uses one Beads database at `/workspace/.beads` across repositories below `/workspace`.

## Project routing

Every project has exactly one label `project:<name>`, mapping directly to `/workspace/<name>`.
Every executable task must have exactly one `project:*` label and one primary `worker:*` label.
Do not initialise separate `.beads` databases inside child repositories.

Use `BEADS_DIR=/workspace/.beads` for all Beads commands.

Workers use Beads for durable tasks, dependencies, labels, handoffs and `bd remember`.

## Code discovery

For conceptual code questions ("where is X", "how does Y work"), use CocoIndex
Code semantic search first. Use ripgrep for exact symbol/string matches. Always
inspect the actual source before editing. Git remains the source-code/history system.

## Worker labels
`worker:spec`, `worker:architect`, `worker:tech-lead`, `worker:fullstack`,
`worker:database`, `worker:devops`, `worker:qa`,
`worker:review`, `worker:security`, `worker:docs`.

## Stages
`stage:spec`, `stage:architecture`, `stage:planning`, `stage:implementation`,
`stage:testing`, `stage:review`, `stage:security-review`, `stage:documentation`.

Before editing implementation work, claim the bead atomically. Before ending, leave a
durable Beads comment. Never close a bead until its acceptance criteria are satisfied.

For handoffs, run `bd comment` as a direct command. Avoid chaining it with output
filters or cleanup commands so the Beads write receives a separate permission check
and is not lost when a later shell operation is rejected.
