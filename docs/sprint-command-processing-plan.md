# Durable Sprint Command Processing Plan

## Goal

Make sprint commands durable, idempotent, and Temporal-backed. The HTTP API
must acknowledge a command without applying a state transition locally. The
sprint workflow owns command ordering and state transitions; the controller
database stores the durable command inbox, audit projection, and retryable
delivery state.

## Invariants

- `(sprint_id, idempotency_key)` identifies one logical command.
- Repeating a command returns the original command identity and does not create
  another transition, plan version, or audit event.
- A command is not considered applied until the workflow validates it and the
  application activity commits the transition and audit event together.
- A command cannot be lost between the HTTP transaction and Temporal signal.
- Temporal workflow code remains deterministic and does not access JPA.
- Commands sent before a sprint workflow starts use `signalWithStart`.
- Failed signal delivery is retried from a durable outbox; it is not reported
  as successful application.

## Data Flow

```text
HTTP command
  -> command inbox + outbox row (one DB transaction)
  -> 202 command reference
  -> outbox dispatcher / signalWithStart
  -> sprint workflow receives command ID
  -> apply-command activity locks inbox row and validates state/version
  -> state transition + audit + inbox result (one DB transaction)
  -> workflow continues or waits for the next command
```

## Implementation Steps

1. Replace the current final-state idempotency record with a command inbox
   containing the full request, actor, status, result, and unique key.
2. Add an outbox record and dispatcher with bounded retry/backoff and visible
   failure state.
3. Add a single typed Temporal command signal for all sprint commands,
   including replan, and use `signalWithStart` for not-yet-started workflows.
4. Move state-machine validation, plan-version validation, plan replacement,
   and audit creation into a transactional application activity.
5. Return command references from REST and expose command status for polling;
   retain sprint read models for current state.
6. Add H2/unit tests for duplicate keys, key reuse conflicts, invalid
   transitions, stale plans, replan audit/versioning, outbox retries, and
   Temporal signal delivery.
7. Run Maven verification and a Compose-backed command round trip before
   closing the implementation bead.

## Compatibility

The existing `GLOBAL_SERIAL` execution default and no-push checkpoint policy
remain unchanged. Existing project-manager approval is migrated to the same
command path rather than retaining a separate direct mutation path.
