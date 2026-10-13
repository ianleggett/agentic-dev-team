# YAADT Controller v1 Specification

This document is normative for the v1 implementation. The words MUST, MUST
NOT, SHOULD, and MAY describe implementation requirements.

## 1. Purpose

Replace the file-based Team Runs UI and ad-hoc worker dispatch with a
Spring Boot controller and React console that can plan, execute, pause,
resume, and monitor durable agent workflows.

The controller will manage projects, agent profiles, project directives,
sprints, human approvals, agent execution, CI results, previews, and audit
history. Beads remains the authoritative engineering task system.

The first implementation runs without Kubernetes. Kubernetes execution is a
future executor option, not a requirement for controller v1.

The v1 implementation starts with the controller foundation already defined in
`controller/`: Spring Boot, JPA, PostgreSQL, Flyway, project CRUD, and health
endpoints. Temporal, the manager workflow, and agent execution are the next
vertical slices.

## 2. Confirmed v1 Decisions

- Beads is the authoritative task graph and engineering work record.
- Git is the authoritative source for code and checkpoint history.
- Temporal is the authoritative runtime for workflow execution and recovery.
- JPA/PostgreSQL stores controller configuration, leases, locks, projections,
  references, and audit records; it does not mirror active Temporal state.
- A project contains one or more user-defined Git repositories.
- A project has one mutually exclusive project-manager workflow.
- Project managers make sequential planning turns. v1 uses one global planning
  turn at a time.
- v1 starts with serial agent execution while the lock model is validated.
- Once lock correctness is proven, unrelated project sprints may execute
  concurrently, subject to capacity and resource locks.
- Same-repository parallel editing is not enabled in v1 without separate
  worktrees and an explicit merge step.
- Agent execution initially runs in the local Devbox.
- Humans control approval, pause, resume, retry, skip, cancel, and replan.
- An active agent is not force-stopped by a normal pause request.

## 3. Goals

- Provide a human-facing workflow console.
- Allow a project to contain multiple user-defined Git repositories.
- Allow each agent role to use a configurable model and execution profile.
- Allow a project manager to decide which Beads are relevant to its project
  goals and directives.
- Generate durable sprints containing sequential and parallel steps.
- Prevent concurrent managers from controlling the same project.
- Prevent conflicting agents from modifying the same repository or workspace.
- Resume workflows after controller, worker, or host restart.
- Support human approval, pause, resume, retry, skip, cancel, and replan.
- Preserve checkpoint commits and Git history for recovery.
- Display agent output, step state, Beads state, CI state, and artifacts.
- Allow unrelated project sprints to execute concurrently once locking is
  proven safe.

## 4. Non-goals for v1

- Kubernetes-based agent execution.
- Automatic production deployment.
- Manual editing of generated sprint plans.
- Replacing Beads as the engineering task system.
- Replacing Git as the source of truth for code.
- Allowing an agent to access Docker or Kubernetes credentials.
- Fully autonomous interruption of an active agent step.
- Same-repository parallel worktrees and automatic merge resolution.
- Concurrent project-manager planning turns.
- Enabling concurrent execution before lock validation passes.

## 5. Terminology

### Controller

The Spring Boot API, Temporal integration, project configuration service,
execution coordinator, and React web application.

### Project

A user-defined product or application containing one or more Git repositories,
shared directives, Beads project identity, agent permissions, and runtime
configuration.

### Project manager

An agent role responsible for inspecting a project, selecting relevant Beads,
creating sprint plans, evaluating new work, and replanning. It does not
implement application code.

### Sprint

A durable Temporal workflow representing a bounded set of work toward a clear
objective. A sprint contains an immutable plan version and one or more steps.

### Step

One executable unit in a sprint. A step may invoke an agent, run CI, run QA,
request human approval, or perform another explicitly supported action.

### Run

One attempt to execute one step. A step may have multiple runs after retries.

### Agent profile

A versioned configuration containing the model, provider reference, prompt,
permissions, tools, token/step limits, and execution policy for an agent role.

## 6. System Architecture

```text
React controller UI
        |
        v
Spring Boot controller API
        |
        +-- JPA/PostgreSQL
        |     projects and repositories
        |     agent profiles
        |     directives
        |     users and audit records
        |     UI projections and references
        |
        +-- Temporal client
        |     starts workflows
        |     sends signals
        |     queries workflow state
        |
        +-- Beads adapter
        |     reads and writes through bd or a compatible API
        |
        +-- Agent executor
              launches existing Devbox/OpenCode workers

Temporal server
        |
        +-- manager workflows
        +-- sprint workflows
        +-- step retries, timers, signals, and recovery

PostgreSQL server
        |
        +-- yaadt_controller database
        +-- temporal database
```

The controller service, Temporal worker, and agent executor are logically
separate. They may share a container during the first local implementation,
but their interfaces must remain separate so the executor can later move to
another process, host, or Kubernetes Job.

The existing Devbox remains the initial agent execution environment because it
contains OpenCode, Beads, Java, Node, project tools, and semantic search.

## 7. Persistence Ownership

### Beads owns

- Engineering task status.
- Task dependencies.
- Project and worker labels.
- Claims and assignments.
- Comments and handoffs.
- Discovered work and follow-up tasks.

The controller must use `bd` or an explicitly compatible Beads adapter. It must
not read Beads internal files directly or create a competing task database.

### Temporal owns

- Workflow execution state.
- Sprint plan execution.
- Step state and attempts.
- Timers and retry schedules.
- Pause, resume, cancel, and replan signals.
- Workflow recovery after process failure.
- Durable workflow history.

### JPA/PostgreSQL owns

- Project definitions.
- Repository definitions.
- Project directives and directive revisions.
- Agent profiles and profile revisions.
- Controller settings.
- User identities and authorization metadata.
- Project-manager assignments.
- Manager leases and resource locks.
- Workflow indexes and UI read projections.
- CI, preview, artifact, and Git commit references.
- Human action audit records.

JPA must not duplicate Temporal as the source of truth for active workflow
execution. JPA may store projections and references needed by the UI.

## 8. Docker Compose Services

The controller Compose configuration should provide these logical services:

```text
postgres
temporal
temporal-ui
controller-api
controller-worker
agent-executor
```

The PostgreSQL service should create separate databases:

```text
yaadt_controller
temporal
```

The Beads database remains separate and is accessed through the configured
workspace or Beads service. It must not be merged into
`yaadt_controller`.

The controller must read credentials from environment variables or mounted
secrets. Database passwords and model provider keys must not be stored in Git
or included in agent prompts.

## 9. Project Model

A project is CRUD-managed by the controller and contains:

- Stable project ID and display name.
- Beads project label.
- One or more repositories.
- Default branch per repository.
- Workspace path or workspace provider.
- Repository role: backend, frontend, database, infrastructure, tests, docs,
  or custom.
- Build, test, and validation commands.
- Preview and CI configuration.
- Allowed agent roles.
- Active project directives.
- Project-manager assignment.
- Project-specific resource limits.

Each repository record should include:

```text
repository_id
project_id
name
remote_url
default_branch
role
relative_workspace_path
build_commands
test_commands
health_check
active
```

The project may also contain a checked-in manifest such as `yaadt.yaml`.
Controller settings provide environment-specific overrides, while the
manifest describes repository and component topology that should travel with
the code.

## 10. Agent Profiles

Agent profiles are versioned JPA records. A profile contains:

```text
profile_id
name
agent_role
model_identifier
provider_identifier
prompt_reference
tool_policy
permission_policy
max_steps
timeout_policy
environment_policy
active
version
```

Provider credentials are referenced by secret name. They are never stored in
the profile content or sent to the LLM as prompt text.

A running step records the exact profile version used. Changes to an agent
profile affect future runs, not an active run.

The controller UI must allow users to configure profiles, but must not expose
secret values after they are stored.

## 11. Project Directives

Project managers use user-defined directives to focus planning. Examples:

- Build only the MVP scope.
- Prioritise authentication and onboarding.
- Defer reporting and administration features.
- Do not change the public API without approval.

Directives are versioned records:

```text
directive_id
project_id
version
content
active
created_by
created_at
```

A sprint records the directive version used to create its plan. Changing a
directive affects future planning. It does not mutate an executing sprint
unless the project manager explicitly creates a replan.

## 12. Project-Manager Workflow

Each configured project manager runs as a durable Temporal manager workflow.
The manager loop is continuous:

1. Acquire the project-manager lease.
2. Read project repositories, documentation, directives, and relevant Beads.
3. Detect newly created or changed Beads.
4. Select work relevant to the project goals and directives.
5. Group related work into functional strands.
6. Identify dependencies and resource conflicts.
7. Create a sprint plan when work is ready.
8. Request human approval when the project setting requires it.
9. Start and monitor the sprint.
10. Evaluate new Beads and step outcomes.
11. Replan when the current sprint is affected by new information.
12. Release the project-manager lease at a safe boundary.
13. Wait for a timer or relevant Beads event before the next turn.

If no relevant work exists and no sprint is created, the manager waits rather
than repeatedly polling. A new related Bead or scheduled wake-up starts the
next planning turn.

The project manager may create new Beads when work is missing, but every new
Bead must have the required project, worker, stage, and area labels.

## 13. Manager Exclusivity

Only one project manager may actively control a project at a time. This must
be enforced transactionally, not only by prompt instruction.

The controller should maintain a lease containing:

```text
project_id
manager_id
temporal_workflow_id
acquired_at
heartbeat_at
expires_at
```

A second manager for the same project must be rejected or remain waiting.
Expired leases may be recovered after verifying that the owning workflow is no
longer active.

Managers for different projects may exist concurrently, but their planning
turns are serialized globally in v1. A planning-turn lease is separate from a
project-manager lease. The planning-turn lease prevents two managers from
mutating or interpreting the Beads graph at the same time.

The planning-turn lease contains:

```text
lease_id
manager_id
project_id
acquired_at
heartbeat_at
expires_at
```

After lock validation, planning turns remain sequential while unrelated
project sprints are allowed to execute concurrently.

## 14. Sprint Plans

A project manager generates a sprint plan containing:

```text
sprint_id
project_id
objective
reasoning
directive_version
plan_version
steps
dependencies
resource_locks
approval_policy
failure_policy
created_at
created_by
```

Each step contains:

```text
step_id
bead_ids
agent_role
agent_profile_version
repository_ids
depends_on
resource_locks
success_criteria
failure_policy
```

Plans are immutable after execution begins. A replan creates a new plan
version linked to the previous version.

The project manager decides which steps are required. The controller validates
the resulting plan before submitting it to Temporal.

Plan validation must reject:

- Unknown agent roles.
- Missing project labels.
- Missing Beads IDs.
- Cyclic dependencies.
- Duplicate ownership of the same Bead.
- Conflicting resource locks for parallel steps.
- Disallowed production operations.
- Invalid repository references.

## 15. Parallelism and Resource Locks

Parallel steps are allowed when the locking model proves that they cannot
clash.

Initial lock types include:

```text
project:<id>
repository:<id>
workspace:<id>
bead:<id>
agent:<role-or-capacity-key>
environment:<id>
```

Two steps may run in parallel only if their lock sets do not conflict.

Different repositories may be edited concurrently. The same repository must
be serialized unless the controller provisions separate Git worktrees and an
explicit merge step.

Controller settings define upper limits:

```text
max_concurrent_agent_runs
max_concurrent_runs_per_project
max_concurrent_runs_per_repository
```

The v1 execution mode is `GLOBAL_SERIAL`: only one agent run may be active at
a time. The controller MUST keep this as the default until lock validation is
complete.

The controller MAY enable `LOCKED_CONCURRENT` only after the lock test suite
proves all of the following:

- Conflicting repository, workspace, Bead, environment, and capacity locks
  cannot be acquired together.
- Lease expiry cannot allow two owners to continue writing concurrently.
- A late result cannot release or overwrite a newer owner's lock.
- Controller and executor restarts preserve lock ownership semantics.
- Unrelated projects can run concurrently without shared-workspace writes.

`LOCKED_CONCURRENT` permits unrelated project sprints to execute concurrently.
It does not permit concurrent planning turns, same-repository edits, or
parallel steps with intersecting lock sets.

## 16. Agent Execution

Temporal activities invoke an agent executor with:

```text
project_id
sprint_id
step_id
bead_ids
repository_ids
agent_role
agent_profile_version
workspace_id
timeout
```

The executor:

1. Creates or opens the sprint workspace.
2. Checks out the required repositories and branches.
3. Exports `BEADS_DIR` and `BEADS_ACTOR`.
4. Invokes the existing agent wrapper and OpenCode runtime.
5. Captures structured output and stderr.
6. Maintains a heartbeat and run lease.
7. Records commit SHA values for every changed repository.
8. Pushes only when the sprint execution policy permits it.
9. Adds a direct Beads handoff comment.
10. Returns status, commits, CI references, preview references, and artifacts.

The agent must not receive Docker or Kubernetes credentials.

## 17. Workspace and Commit Policy

Each sprint receives a workspace namespace:

```text
/workspace/runs/<sprint-id>/<repository-name>
```

Sequential steps in the same sprint may reuse the workspace and checkpoint
each successful step with a commit.

Agents should not wait until the entire sprint goal is complete before
committing. Checkpoint commits are required for recovery and auditability.

Parallel steps that touch the same repository require separate worktrees and a
merge step. Until that exists, the controller must serialize those steps.

A sprint involving multiple repositories is complete only when all required
repository validations pass. The controller stores one commit SHA per
repository.

## 18. State Machines and Human Controls

Sprint states:

```text
DRAFT
PLANNED
WAITING_FOR_APPROVAL
READY
RUNNING
PAUSED
FAILED
COMPLETED
CANCELLED
```

Step states:

```text
PENDING
READY
RUNNING
SUCCEEDED
FAILED
BLOCKED
WAITING_FOR_HUMAN
SKIPPED
CANCELLED
```

### Project-manager state machine

The manager states are `DISABLED`, `WAITING_FOR_LEASE`, `PLANNING`,
`WAITING_FOR_APPROVAL`, `WAITING_FOR_SPRINT`, `REPLANNING`,
`WAITING_FOR_EVENT`, and `FAILED`.

```text
DISABLED -> WAITING_FOR_LEASE
WAITING_FOR_LEASE -> PLANNING
PLANNING -> WAITING_FOR_APPROVAL
PLANNING -> WAITING_FOR_SPRINT
PLANNING -> WAITING_FOR_EVENT
WAITING_FOR_APPROVAL -> WAITING_FOR_SPRINT
WAITING_FOR_APPROVAL -> PLANNING
WAITING_FOR_SPRINT -> PLANNING
WAITING_FOR_SPRINT -> REPLANNING
REPLANNING -> WAITING_FOR_APPROVAL
REPLANNING -> WAITING_FOR_SPRINT
REPLANNING -> WAITING_FOR_EVENT
WAITING_FOR_EVENT -> WAITING_FOR_LEASE
ANY_ACTIVE_STATE -> FAILED
FAILED -> WAITING_FOR_LEASE
ANY_STATE -> DISABLED
```

`PLANNING` and `REPLANNING` require both the project lease and the global
planning-turn lease. The manager releases the planning-turn lease before
waiting for a sprint, human action, timer, or Beads event. The project lease
may remain held while the manager monitors its active sprint, but it must be
renewed and recoverable.

### Sprint state machine

```text
DRAFT -> PLANNED
PLANNED -> WAITING_FOR_APPROVAL
PLANNED -> READY
WAITING_FOR_APPROVAL -> READY
WAITING_FOR_APPROVAL -> DRAFT
READY -> RUNNING
RUNNING -> PAUSED
RUNNING -> FAILED
RUNNING -> COMPLETED
RUNNING -> CANCELLED
PAUSED -> READY
PAUSED -> CANCELLED
FAILED -> READY
FAILED -> CANCELLED
```

`DRAFT` is controller-created state only. A plan is immutable after entering
`READY`. Rejecting an approval returns the sprint to `DRAFT` and requires a
new plan version. A retry of a failed step returns the sprint to `READY` and
creates a new step attempt. Replanning never mutates the current plan; it
creates a new sprint plan version linked to the prior version.

`COMPLETED` and `CANCELLED` are terminal. A failed sprint is resumable only
when its failure policy permits retry or a human explicitly requests retry.
`RUNNING -> COMPLETED` is valid only when every required step is `SUCCEEDED`
or explicitly accepted as `SKIPPED` by the completion policy.

### Step state machine

```text
PENDING -> READY
PENDING -> BLOCKED
READY -> RUNNING
READY -> CANCELLED
RUNNING -> SUCCEEDED
RUNNING -> FAILED
RUNNING -> WAITING_FOR_HUMAN
RUNNING -> CANCELLED
WAITING_FOR_HUMAN -> READY
WAITING_FOR_HUMAN -> FAILED
WAITING_FOR_HUMAN -> CANCELLED
FAILED -> READY
FAILED -> SKIPPED
BLOCKED -> READY
BLOCKED -> SKIPPED
```

Dependencies determine whether a step is `READY`. A step cannot become
`READY` while a dependency is `PENDING`, `READY`, `RUNNING`, or `BLOCKED`.
`SKIPPED`, `SUCCEEDED`, and `CANCELLED` are terminal step states. Skipping
requires a human reason and may only happen if the remaining plan still has a
valid completion policy.

### Agent-run state machine

Each execution attempt has its own run record:

```text
CREATED -> STARTING
STARTING -> RUNNING
STARTING -> FAILED
RUNNING -> SUCCEEDED
RUNNING -> FAILED
RUNNING -> TIMED_OUT
RUNNING -> CANCEL_REQUESTED
CANCEL_REQUESTED -> CANCELLED
```

`SUCCEEDED`, `FAILED`, `TIMED_OUT`, and `CANCELLED` are terminal. A retry
creates a new run and never reopens a terminal run. The executor callback must
include the run ID and idempotency key; callbacks for stale runs are recorded
but cannot change step or sprint state.

### Approval state machine

```text
NOT_REQUIRED -> APPROVED
PENDING -> APPROVED
PENDING -> REJECTED
REJECTED -> PENDING
```

Approval records include the user, plan version, decision time, and reason.
Only `APPROVED` can release a sprint into `READY`. A rejected plan must be
revised before it can be submitted again.

The UI must support:

- Approve sprint.
- Reject sprint.
- Pause sprint.
- Resume sprint.
- Retry a failed step.
- Skip a step with a required reason.
- Cancel a sprint.
- Request replan.
- Inspect agent output and events.
- Inspect Beads and handoff comments.
- Inspect commit, CI, preview, screenshot, and trace references.

Pausing stops scheduling new steps. An active agent is allowed to finish by
default. Force-stop is a separate operation and must be explicitly confirmed.

Every human action records the user, timestamp, reason, previous state, and
new state.

## 19. New Beads During Execution

The project manager evaluates new or changed Beads during each manager turn
and at safe sprint boundaries.

- Unrelated Beads remain in the backlog.
- Relevant but non-blocking Beads may be added to a future sprint.
- Sprint-impacting Beads trigger a replan decision.
- An active agent is not interrupted by default.
- Affected sprints pause after the current step and create a new plan version.

The manager must record why a Bead was included, deferred, or excluded.

## 20. Failure and Recovery

Temporal handles workflow retries, timers, and recovery. The controller must
still protect external side effects.

Required rules:

- Every activity has an idempotency key based on sprint ID and step ID.
- Every retry has a separate attempt record.
- A late agent result must not overwrite a newer attempt.
- A failed step pauses or retries according to its policy.
- A manager restart must not create a second active project manager.
- A controller restart must resume Temporal workflows.
- A paused sprint may resume from the last completed step.
- Checkpoint commits and workspace state must be discoverable.
- Beads handoffs must include sprint ID, step ID, run ID, commit SHA, and
  validation results.

## 21. Controller API and React UI

The API should expose resources for:

```text
projects
repositories
agent-profiles
directives
project-managers
sprints
sprint-steps
agent-runs
approvals
artifacts
audit-events
```

The UI should provide:

- Project and repository CRUD.
- Agent profile settings.
- Directive history.
- Project-manager status.
- Sprint graph and timeline.
- Step state and dependencies.
- Live agent output.
- Beads links and comments.
- CI and preview links.
- Approval and control actions.
- Audit history.

The existing Team Runs UI is replaced only after the controller can display
agent runs, step attempts, logs, leases, and historical artifacts.

## 22. Authentication and Authorization

Use Spring Security with OIDC through Gitea, Keycloak, or another configured
identity provider. The controller must be designed for authenticated users
from the first implementation.

At minimum, define these capabilities:

```text
view_project
edit_project
manage_agent_profiles
approve_sprint
control_sprint
manage_directives
manage_secrets
```

Provider credentials, Git credentials, and deployment credentials must be
stored as secret references and never returned by the API.

## 23. Initial Implementation Plan

The concrete service topology, relational model, and first planning-to-recovery
vertical slice are specified in `docs/v1-controller-work-plan.md`.

### Phase 1: Controller foundation

- Create Spring Boot controller service.
- Create React application shell.
- Add PostgreSQL Compose service.
- Add separate controller database.
- Add project and repository CRUD.
- Add JPA migrations.
- Add authentication and audit identity.

### Phase 2: Temporal integration

- Add Temporal Compose services and separate database.
- Add controller Temporal client.
- Add Temporal worker process.
- Implement project-manager workflow.
- Implement project-manager lease.
- Implement manager wake-up and idle waiting.

### Phase 3: Beads integration

- Implement Beads CLI/API adapter.
- Read project-related Beads.
- Create labelled Beads.
- Add comments and handoffs.
- Detect newly created relevant Beads.
- Preserve Beads as task authority.

### Phase 4: Sprint planning and execution

- Define sprint plan schema.
- Validate DAGs and resource locks.
- Implement human approval.
- Implement local Devbox executor.
- Implement checkpoint commits.
- Implement sequential step execution.
- Implement retry, skip, cancel, pause, and resume.

### Phase 5: Parallel execution

- Add global and project concurrency settings.
- Implement repository and workspace locks.
- Add safe parallel steps for unrelated repositories.
- Prove lock and late-result behaviour.
- Enable concurrent sprints for unrelated projects.

### Phase 6: Controller UI replacement

- Add sprint graph and control views.
- Add live step and agent output.
- Add Beads, Git, CI, and artifact links.
- Add manager and directive settings.
- Retire `team-runs-ui` after feature parity.

## 24. v1 Acceptance Criteria

v1 is complete when:

- A user can create a project containing multiple repositories.
- A user can configure directives and agent profiles.
- Only one project manager can control a project at a time.
- A project manager can create a sprint from relevant Beads.
- The controller rejects invalid or conflicting plans.
- A user can approve, pause, resume, retry, skip, cancel, and replan.
- Temporal resumes workflows after controller restart.
- Sequential agent steps reuse a sprint workspace safely.
- Successful agent steps create checkpoint commits.
- Failed and retried steps are distinguishable.
- New Beads can be deferred or cause a replan.
- Beads remains the authoritative task system.
- The UI shows current and historical workflow state.
- Human actions and agent attempts are auditable.
- No agent receives Docker or Kubernetes credentials.
- Planning turns are serialized globally in v1.
- The default execution mode is `GLOBAL_SERIAL`.
- Unrelated project sprints can execute concurrently only after resource-lock
  tests pass.
