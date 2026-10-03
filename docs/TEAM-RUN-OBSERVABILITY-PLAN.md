# Team Run Observability Plan

## Goal

Keep Beads as the durable task graph while adding reliable execution
ownership, crash recovery, and visibility into `team-autorun` and OpenCode
runs.

The design must work with the current shared workspace model:

```text
/workspace/.beads       shared Beads database
/workspace/<project>    Git repositories
```

The plan deliberately does not add a second task database. Beads remains the
source of truth for task status, assignment, dependencies, comments, and
handoffs. Run records describe execution attempts and are a separate audit
stream.

## Decisions

1. Beads remains the task system.
2. A claim is valid only while its execution lease is live.
3. Run records are append-only and keyed by a globally unique `run_id`.
4. A worker must heartbeat independently of model output. A quiet model is not
   necessarily a dead worker.
5. Requeue is conservative: an expired lease is marked abandoned and made
   claimable again; it is not silently closed.
6. The first implementation is local-first and uses files plus the `bd` CLI.
   It must not read `.beads/issues.jsonl` directly.
7. The UI is read-only for run-control in the first release. Requeue and
   cancellation remain CLI operations until authorization and race handling
   are proven.
8. OpenCode and LLM providers remain replaceable. The run layer observes the
   worker process and structured OpenCode events without coupling task state to
   a particular model provider.

## Proposed Runtime Model

```text
team-autorun
  -> worker wrapper
       -> create run record
       -> claim bead
       -> start heartbeat
       -> opencode run --format json
       -> append events and refresh lease
       -> finalize run and handoff
```

Each run has at least:

```text
run_id
worker_id
role
bead_id
project
pid
container_id or host identity
started_at
last_heartbeat_at
lease_expires_at
finished_at
state: starting | claimed | running | succeeded | failed | abandoned | requeued
exit_code
opencode_session_id (optional)
event_log_path
```

Recommended paths, configurable through environment variables:

```text
/workspace/.team-runs/<run_id>.json
/workspace/.team-runs/events/<run_id>.jsonl
```

These files are operational state, not task state. They should be excluded
from Git by default and retained according to a configurable policy.

## Lease Semantics

- Claim and run creation must be correlated by `run_id`.
- The lease owner must be identifiable and different concurrent workers must
  not be able to renew each other's leases.
- Heartbeats should be emitted every 15-30 seconds by a supervisor process or
  background loop.
- The default lease should be longer than the heartbeat interval, for example
  90 seconds, with a configurable grace period for slow storage or a paused
  container.
- A run is stale only when `lease_expires_at` is in the past and the owner
  cannot be verified as alive.
- Requeue must record the reason, prior assignee, old `run_id`, and detection
  time in the Beads audit trail.
- Cleanup must be idempotent. A late worker must not be able to overwrite a
  newer claim or close a bead it no longer owns.

The exact `bd` commands for clearing an assignee or returning a bead to ready
must be verified against the pinned Beads version before implementation. Do
not infer mutation syntax from another Beads release.

## Task Breakdown

The following tasks are intended to be created in the shared Beads database by
the tech lead. Each task includes its suggested routing labels and dependency
IDs. The IDs are stable plan IDs, not generated Beads IDs.

### E1. Runtime Contract

#### TR-001: Define run and lease contract

Labels: `worker:tech-lead`, `stage:planning`, `area:infra`

Dependencies: none

Acceptance criteria:

- Document the run state machine and lease transitions.
- Define the JSON schema for run records and event records.
- Define owner identity, heartbeat interval, lease duration, retention, and
  stale detection rules.
- Define what is durable in Beads versus the run event store.
- Verify all required `bd` mutation commands against the pinned CLI.

#### TR-002: Define failure and recovery matrix

Labels: `worker:tech-lead`, `stage:planning`, `area:infra`

Dependencies: TR-001

Acceptance criteria:

- Cover worker crash before claim, after claim, during OpenCode execution,
  after bead close, and during cleanup.
- Cover container restart, host restart, duplicate autorun processes, clock
  skew, and a late heartbeat.
- Specify the expected Beads state, run state, and audit comment for each
  case.

### E2. Run Records and Worker Wrapper

#### TR-003: Implement run-record writer

Labels: `worker:devops`, `stage:implementation`, `area:infra`

Dependencies: TR-001

Acceptance criteria:

- Create unique run records atomically.
- Append structured JSONL events without corrupting concurrent records.
- Redact credentials, API keys, and sensitive environment values.
- Support disabled/offline operation without blocking the worker.
- Add unit tests for creation, append, finalization, and malformed records.

#### TR-004: Add lease and heartbeat supervisor

Labels: `worker:devops`, `stage:implementation`, `area:infra`

Dependencies: TR-003

Acceptance criteria:

- Start a heartbeat after a successful claim.
- Refresh the lease independently of OpenCode output.
- Stop and finalize the heartbeat on success, failure, signal, and timeout.
- Ensure only the matching `run_id` can renew or finalize a run.
- Test SIGTERM, SIGINT, parent death, and abrupt process termination.

#### TR-005: Integrate `_beads-worker` with run lifecycle

Labels: `worker:devops`, `stage:implementation`, `area:infra`

Dependencies: TR-002, TR-004

Acceptance criteria:

- Create the run before claiming a bead.
- Pass run identity into the worker prompt and log context.
- Record claim success, claim conflict, OpenCode start, exit status, and
  cleanup events.
- Preserve existing one-bead-per-worker behavior.
- Never leave a claimed bead without a recorded terminal or recoverable run.

#### TR-006: Capture OpenCode JSON events

Labels: `worker:devops`, `stage:implementation`, `area:infra`

Dependencies: TR-005

Acceptance criteria:

- Invoke the supported `opencode run --format json` mode.
- Store event metadata and bounded output in the run event stream.
- Capture the OpenCode session ID when available.
- Apply configurable limits for event size, transcript retention, and secret
  redaction.
- Continue to finalize the Beads task if event capture is unavailable.

### E3. Recovery and Control

#### TR-007: Implement stale-run detector

Labels: `worker:devops`, `stage:implementation`, `area:infra`

Dependencies: TR-004, TR-005

Acceptance criteria:

- List runs whose leases have expired.
- Distinguish an expired lease from an actively verified process where
  possible.
- Mark stale runs abandoned exactly once.
- Emit a durable Beads comment containing run ID, worker, bead, timestamps,
  and recovery reason.
- Never automatically close a stale bead.

#### TR-008: Implement safe stale requeue

Labels: `worker:devops`, `stage:implementation`, `area:infra`

Dependencies: TR-007

Acceptance criteria:

- Requeue only when the bead still belongs to the expired run/owner.
- Refuse the mutation when a newer worker has claimed the bead.
- Preserve prior run history and make the next attempt distinguishable.
- Support dry-run and explicit bead/run selection.
- Add race tests for stale detection versus a new claim.

#### TR-009: Harden `team-autorun`

Labels: `worker:devops`, `stage:implementation`, `area:infra`

Dependencies: TR-005, TR-008

Acceptance criteria:

- Assign a dispatcher run ID to each autorun invocation and round.
- Record worker launch, skip, failure, and duration events.
- Do not hide worker failures from the run records even if bounded rounds
  continue.
- Add configurable concurrency only after claim/recovery tests pass.
- Preserve current `--max`, `--watch`, and `--sleep` behavior.

### E4. Verification and Documentation

#### TR-010: Add integration test harness

Labels: `worker:qa`, `stage:testing`, `area:test`

Dependencies: TR-005, TR-008, TR-009

Acceptance criteria:

- Run against a temporary Beads database and test repository.
- Verify successful, failed, killed, abandoned, and requeued runs.
- Verify duplicate workers cannot take the same bead.
- Verify records survive a worker process restart.
- Verify secrets are absent from persisted events.

#### TR-011: Document operations and recovery

Labels: `worker:docs`, `stage:documentation`, `area:docs`

Dependencies: TR-010

Acceptance criteria:

- Document run states, retention, lease tuning, and recovery commands.
- Document how to inspect one bead, one run, and one event stream.
- Document limitations around clock skew, host identity, and shared storage.
- Update README limitations and roadmap.

### E5. Runs/Agents UI

#### TR-012: Choose UI integration boundary

Labels: `worker:architect`, `stage:architecture`, `area:frontend`

Dependencies: TR-001, TR-003

Acceptance criteria:

- Compare extending upstream `beads-ui`, maintaining a small companion UI,
  and adopting BeadBoard components.
- Prefer an upstreamable adapter over a permanent private fork.
- Define the API or file-watch contract for run records.
- Define read-only security behavior and data exposure limits.

#### TR-013: Add Runs and Agents backend

Labels: `worker:fullstack`, `stage:implementation`, `area:frontend`

Dependencies: TR-012, TR-003

Acceptance criteria:

- Expose active, completed, failed, and abandoned runs.
- Expose agent identity, bead, project, role, lease expiry, heartbeat age,
  duration, and terminal status.
- Provide live updates through file watching, SSE, or a similarly bounded
  mechanism.
- Keep Beads issue data accessed through `bd` or an explicitly compatible
  adapter.
- Add tests for malformed, missing, and stale run records.

#### TR-014: Add run detail and audit timeline

Labels: `worker:fullstack`, `stage:implementation`, `area:frontend`

Dependencies: TR-006, TR-013

Acceptance criteria:

- Show the run event timeline with bounded output.
- Add an Agent Output/Logs panel with live-follow and historical replay modes.
- Preserve event types such as assistant output, tool calls, command output,
  warnings, and errors rather than flattening everything into plain text.
- Support pause, resume, search, and download of a redacted run log.
- Link to the Beads issue and OpenCode session where available.
- Clearly distinguish live, stale, failed, abandoned, and successful runs.
- Do not render secrets or unrestricted raw environment data.
- Work on the existing desktop and mobile viewport ranges.

#### TR-015: Add UI verification and upstream contribution package

Labels: `worker:qa`, `stage:testing`, `area:frontend`

Dependencies: TR-010, TR-014

Acceptance criteria:

- Add browser tests for active, stale, completed, and failed runs.
- Verify the existing Beads board remains unchanged and functional.
- Add a Docker smoke test for the UI with a shared workspace.
- Produce an upstream issue/PR-quality proposal with screenshots, schema,
  license, and installation notes.

### E6. Optional Historical Analytics

#### TR-016: Evaluate Thread integration

Labels: `worker:tech-lead`, `stage:planning`, `area:infra`

Dependencies: TR-011, TR-015

Acceptance criteria:

- Determine whether Thread's Beads history metrics add value beyond run
  records and `bd history`.
- Define a non-blocking report/export integration if useful.
- Do not make Thread a runtime dependency for task execution.

## Milestones

### M1: Safe Claims

Tasks: TR-001 through TR-005

Outcome: every worker claim has a run identity, lease, heartbeat, and
recoverable terminal state.

### M2: Recovery

Tasks: TR-006 through TR-011

Outcome: killed or abandoned workers no longer create permanently stale work,
and the behavior is covered by integration tests and operator documentation.

### M3: Live Operations View

Tasks: TR-012 through TR-015

Outcome: humans can see current agents and historical runs without attaching to
the devbox shell.

### M4: Analytics Decision

Task: TR-016

Outcome: historical quality metrics are either integrated deliberately or
rejected as unnecessary scope.

## Open-Source Suitability

This is a good open-source candidate, especially as a small companion project
or an upstream feature set for `beads-ui`, provided the scope stays generic.

### Strong open-source boundary

- `team-runner`: lease, heartbeat, run-record, and stale-recovery CLI/library.
- A documented JSON schema for run records and events.
- A `beads-ui` adapter or optional Runs/Agents panel.
- Pluggable event sinks, with local files as the default.
- Provider-neutral process lifecycle hooks.
- Docker Compose example and failure-injection tests.

### Keep project-specific

- YAADT role names and routing labels.
- YAADT's exact worker prompts.
- Local infrastructure paths and credentials.
- Organization-specific retention and access policy.
- Any hosted telemetry or model-provider integration.

### Licensing and governance requirements

- Confirm compatibility with Beads and `beads-ui` licenses before copying code.
- Prefer adapters and contributions over copying a UI wholesale.
- Do not make external telemetry mandatory.
- Redact prompts, source code, credentials, and environment values by default.
- Provide a local-only mode that works without an account or network.
- Pin or vendor tested CLI/API versions and document compatibility.

### Recommended project shape

```text
team-runner/                 generic OSS runtime package
  schema/                    run and event schemas
  bin/                       lease, heartbeat, stale-requeue commands
  adapters/beads/            bd CLI adapter
  adapters/opencode/         structured event adapter
  docs/                      recovery and integration contracts

beads-ui integration         upstream PR or optional companion package

YAADT                         thin configuration and role-specific wrapper
```

The first contribution should be the runtime contract and tests, not a large
UI fork. That makes the project useful to other Beads-based teams even if
their agent runtime is Claude Code, Codex, OpenCode, Pi, or a custom worker.

## Materializing the Tasks

When working inside a configured `/workspace` deployment, create an epic for
this plan and create the tasks with the labels and dependencies above. The
project should receive its normal `project:<name>` label. A typical sequence
is:

```bash
cd /workspace/<project>
team-init
tech-lead docs/architecture/team-run-observability.md
team-status
team-autorun --max 10
```

The repository copy of this plan is the reviewable specification. Generated
Beads IDs and current status belong in the runtime Beads database, not in this
file.
