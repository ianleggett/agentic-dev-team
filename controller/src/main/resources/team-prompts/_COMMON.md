# Shared Agent Operating Model

YAADT uses one Beads database at `/workspace/.beads` across repositories below `/workspace`.

Every executable task must have exactly one `project:*` label and one primary `worker:*` label. Use `BEADS_DIR=/workspace/.beads` for all Beads commands. Workers use Beads for durable tasks, dependencies, labels, handoffs and `bd remember`. Git remains the source-code/history system.
