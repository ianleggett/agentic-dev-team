# Example workflow

```bash
cd /workspace/org-chart
team-init

# Idea -> durable specification
spec-writer docs/ideas/new-feature.md

# Specification -> architecture
solution-architect docs/specs/new-feature.md

# Architecture/spec -> routed Beads graph
tech-lead docs/architecture/new-feature.md

# View ready queues
team-status

# One-shot workers; rerun to take the next routed ready bead
fullstack-dev
database-dev
devops
qa-test
code-reviewer
security-reviewer
docs-writer

# Or dispatch the ready queues automatically (bounded rounds; --watch for continuous)
team-autorun --max 10
```

Workers are intentionally one-shot so each run has bounded context. Durable state and handoffs remain in Beads.
