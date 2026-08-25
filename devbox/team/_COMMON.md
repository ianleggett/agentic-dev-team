# Shared Agent Operating Model

This repository uses Beads (`bd`) as the durable coordination and memory layer.

## Non-negotiable rules
1. Run `bd prime` at the start of every session.
2. Use JSON output for programmatic decisions.
3. Durable work state belongs in Beads: issues for work, labels for routing, dependencies for blockers/order, comments for handoffs, `bd remember` for reusable project knowledge.
4. Never replace Beads with Markdown TODO lists or chat history.
5. Claim implementation work atomically with `bd update <id> --claim --json` before editing.
6. Work on one claimed implementation bead per invocation unless the role is planning/review.
7. Create unrelated discoveries as separate beads, preferably with `discovered-from:<id>`.
8. Before ending: add a concise Beads handoff comment, use `bd remember` for durable knowledge, and attempt `bd dolt push` if a remote is configured.
9. Never use interactive `bd edit`; use `bd update` flags.
10. Never close a bead unless acceptance criteria are satisfied.

## Code discovery with CocoIndex Code

CocoIndex Code (`ccc`) is available for AST-aware semantic repository search and is also exposed to OpenCode through MCP when configured.

Use it as the preferred first step for conceptual codebase discovery:

- use CocoIndex/MCP search for questions such as "where is authentication enforced?" or "which code handles employee hierarchy persistence?"
- use `ripgrep`/`grep` for exact identifiers, literal strings, and exhaustive text matches
- inspect the actual source before changing it; semantic search results are navigation aids, not proof
- prefer targeted searches over loading large parts of the repository into model context
- after substantial code changes, allow CocoIndex search to refresh the index or run `ccc index` when an explicit refresh is needed
- do not store task state in CocoIndex; Beads remains the durable work/memory system

If CocoIndex is unavailable or indexing fails, continue using normal repository tools and report the indexing problem separately. Do not block unrelated work solely because semantic search is unavailable.

## Routing labels
Primary worker labels:
- worker:spec
- worker:architect
- worker:tech-lead
- worker:java
- worker:frontend
- worker:database
- worker:devops
- worker:qa
- worker:review
- worker:security
- worker:docs

Lifecycle labels:
- stage:spec
- stage:architecture
- stage:planning
- stage:implementation
- stage:testing
- stage:review
- stage:security-review
- stage:documentation
- needs-review
- needs-security-review
- blocked-external

Area labels:
- area:backend
- area:frontend
- area:database
- area:infra
- area:test
- area:security
- area:docs

## Handoff convention
When handing work to another worker:
1. Add/update the target worker label.
2. Add the appropriate stage label.
3. Add a Beads comment stating what completed, what remains, relevant files/decisions, test/build status, and known risks.
4. Add dependencies where ordering/blockers matter.
5. Do not rely on conversation history.

## Bead quality
Every implementation bead should include a focused title, context/scope, explicit acceptance criteria, useful design notes, dependencies, one primary worker label, one stage label, and appropriate area labels.
