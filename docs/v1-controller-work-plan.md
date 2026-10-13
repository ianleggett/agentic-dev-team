# YAADT v1 Controller Work Plan

This document turns `docs/update-changes.md` into the first implementable
vertical slice. It defines the local Compose topology, controller domain
model, service contracts, and the first end-to-end workflow.

## 1. v1 Boundary

The first slice is:

```text
project configuration
  -> project-manager planning turn
  -> sprint plan persisted and validated
  -> human approval
  -> local Devbox agent execution
  -> checkpoint commit
  -> pause/resume
  -> failed step retry
```

The slice uses one project, one manager workflow, one repository, and one
agent step first. The data model MUST support multiple repositories and steps
from the beginning, even though the first acceptance test uses one of each.

The slice does not include:

- Kubernetes execution.
- OIDC provider integration beyond an authenticated-user abstraction.
- Parallel planning turns.
- Concurrent agent execution before lock validation.
- Same-repository worktrees or automatic merges.
- Automatic Git push.
- CI and preview orchestration beyond nullable result references.

## 2. Compose Topology

### 2.1 Services

The initial Compose topology is:

```text
                         +----------------+
                         | controller-ui  |
                         | React/Vite     |
                         +-------+--------+
                                 |
                                 v
+----------------+       +-------+--------+       +------------------+
| temporal-ui    |       | controller-api |------>| controller DB    |
| optional       |       | Spring Boot    |       | PostgreSQL        |
+-------+--------+       +-------+--------+       +------------------+
        |                        |
        v                        v
 +------+-------+         +------+---------+
 | Temporal      |<--------| controller     |
 | server        |          | worker        |
 +------+-------+          +------+---------+
        |                         |
        v                         v
 +------+-------------------------+---------+
 | Temporal PostgreSQL database             |
 +------------------------------------------+

 +----------------+       +----------------+
 | agent-executor |------>| local Devbox   |
 | controller     |       | OpenCode + bd  |
 +----------------+       +----------------+
```

The concrete services are:

| Service | Responsibility | Persistent data | Exposure |
| --- | --- | --- | --- |
| `controller-api` | REST API, validation, commands, projections, Temporal client | none | `8080` |
| `controller-worker` | Temporal workflow/activity workers | none | none |
| `agent-executor` | Launches and monitors local Devbox agent runs | workspace and run artifacts | none |
| `controller-ui` | React/Vite web console | none | `5173` |
| `postgres-controller` | JPA/Flyway database | `yaadt-controller-data` | internal only |
| `postgres-temporal` | Temporal persistence | `temporal-data` | internal only |
| `temporal` | Workflow service | through Temporal database | `7233` internal |
| `temporal-ui` | Optional workflow diagnostics | none | `8233` |
| `devbox` | Existing OpenCode, Beads, Git, build, and test environment | mounted workspace | internal only |

`controller-api`, `controller-worker`, and `agent-executor` are separate
logical processes. They MAY initially share one built image, but each process
MUST have a separate command and health contract so that they can be split
later.

### 2.2 Networks and volumes

Use two Docker networks:

```text
yaadt-control
  controller-api
  controller-worker
  postgres-controller
  temporal
  postgres-temporal
  temporal-ui

yaadt-execution
  controller-worker
  agent-executor
  devbox
```

`controller-api` does not need direct access to the Devbox filesystem. The
worker/executor boundary carries run requests and results. The initial local
implementation may mount the same workspace into `agent-executor` and
`devbox`, but the executor owns workspace creation and cleanup.

Required volumes:

```text
yaadt-controller-data   -> postgres-controller
yaadt-temporal-data     -> postgres-temporal
yaadt-workspace         -> agent-executor and devbox
yaadt-artifacts         -> agent-executor and controller-api read-only
```

The Beads directory is part of the configured workspace and is accessed by
`bd`. It is not mounted into either PostgreSQL service.

### 2.3 Database configuration

The controller database uses:

```text
database: yaadt_controller
user:     yaadt
schema:   public
```

The Temporal database uses:

```text
database: temporal
user:     temporal
schema:   public
```

The controller MUST use Flyway migrations and `ddl-auto=validate`. Temporal
owns its own schema initialization. The controller MUST NOT create Temporal
tables or query Temporal persistence directly.

Required health dependencies:

```text
postgres-controller healthy -> controller-api
postgres-temporal healthy   -> temporal
temporal healthy             -> controller-worker
controller-api healthy      -> controller-ui
```

`devbox` may start independently, but an agent run cannot start until the
executor confirms that the configured workspace and Beads directory are
available.

### 2.4 Service contracts

`controller-api`:

- Exposes REST commands and read models.
- Starts manager workflows.
- Sends approval and control signals.
- Never performs long-running agent work in an HTTP request.
- Returns a command ID or workflow reference for asynchronous operations.

`controller-worker`:

- Registers manager and sprint workflows.
- Registers planning, lock, execution, commit, and result activities.
- Does not serve public HTTP.
- Uses Temporal heartbeats for long-running activities.

`agent-executor`:

- Accepts an idempotent `AgentRunRequest`.
- Creates `/workspace/runs/<sprint-id>/<repository-name>`.
- Invokes the existing Devbox worker/OpenCode wrapper.
- Captures stdout, stderr, structured events, commit SHAs, and validation
  results.
- Returns an `AgentRunResult` exactly once, with duplicate delivery tolerated.

`devbox`:

- Remains the initial execution environment.
- Receives only the credentials and tools allowed by the agent profile.
- MUST NOT receive Docker or Kubernetes credentials.

## 3. Controller Domain Model

The controller database stores configuration, coordination, projections, and
audit records. Temporal stores active workflow history. Beads stores work
items, dependencies, labels, claims, and handoffs.

### 3.1 Entity relationship overview

```text
User
  |
  +--< AuditEvent
  +--< Approval

Project
  +--< Repository
  +--< DirectiveRevision
  +--< AgentProfileBinding
  +--< Sprint
  +--1 ProjectManager
  +--< Lease
  +--< Lock

AgentProfile
  +--< AgentProfileVersion

Sprint
  +--< SprintPlanVersion
  +--< SprintStep
  +--< Approval
  +--< AgentRun
  +--< ArtifactReference
  +--< AuditEvent

SprintStep
  +--< StepDependency
  +--< AgentRun
  +--< Lock
```

### 3.2 Project and repository configuration

`Project`:

```text
id UUID primary key
name varchar(120) not null
beads_project varchar(120) not null
workspace_provider varchar(40) not null
default_branch varchar(120) not null
execution_mode enum(GLOBAL_SERIAL, LOCKED_CONCURRENT)
max_concurrent_agent_runs integer not null
max_concurrent_runs_per_project integer not null
approval_required boolean not null
active boolean not null
created_at timestamptz not null
updated_at timestamptz not null
```

`Repository`:

```text
id UUID primary key
project_id UUID not null references projects
name varchar(120) not null
remote_url varchar(500) not null
default_branch varchar(120) not null
role varchar(40) not null
relative_workspace_path varchar(240) not null
build_commands jsonb not null
test_commands jsonb not null
health_check jsonb
active boolean not null
created_at timestamptz not null
updated_at timestamptz not null
```

Constraints:

- Repository names are unique within a project.
- Workspace paths are unique within a project.
- A project must have at least one active repository before planning.
- Remote URLs are configuration values; credentials are secret references.
- Commands are displayed to users and executed only through an approved
  executor policy.

### 3.3 Agent profiles

`AgentProfile` identifies a role. `AgentProfileVersion` makes execution
reproducible:

```text
AgentProfile
  id UUID primary key
  name varchar(120) unique not null
  role varchar(60) unique not null
  active boolean not null

AgentProfileVersion
  id UUID primary key
  profile_id UUID not null references agent_profiles
  version integer not null
  model_identifier varchar(160) not null
  provider_identifier varchar(120) not null
  prompt_reference varchar(240) not null
  tool_policy jsonb not null
  permission_policy jsonb not null
  environment_policy jsonb not null
  max_steps integer not null
  timeout_seconds integer not null
  created_by UUID not null references users
  created_at timestamptz not null
```

The unique key is `(profile_id, version)`. A step stores the immutable
`agent_profile_version_id` it selected. Provider and Git credentials are
secret names only; secret values are not stored in PostgreSQL or prompt text.

Required initial roles:

```text
project-manager
fullstack-dev
qa
review
security
docs
ui-assessor
```

Only `fullstack-dev` is required for the first end-to-end execution slice.

### 3.4 Directives

`DirectiveRevision`:

```text
id UUID primary key
project_id UUID not null references projects
version integer not null
content text not null
active boolean not null
created_by UUID not null references users
created_at timestamptz not null
```

The unique key is `(project_id, version)`. A sprint plan records the exact
directive revision used. Updating a directive never mutates an executing
sprint.

### 3.5 Project manager assignment

`ProjectManager`:

```text
id UUID primary key
project_id UUID unique not null references projects
agent_profile_version_id UUID not null references agent_profile_versions
temporal_workflow_id varchar(240) unique
state enum(DISABLED, WAITING_FOR_LEASE, PLANNING, WAITING_FOR_APPROVAL,
           WAITING_FOR_SPRINT, REPLANNING, WAITING_FOR_EVENT, FAILED)
updated_at timestamptz not null
```

The unique `project_id` constraint prevents two configured managers. Temporal
workflow identity is deterministic: `project-manager:<project-id>`.

### 3.6 Sprint and plan versions

`Sprint`:

```text
id UUID primary key
project_id UUID not null references projects
objective varchar(500) not null
state enum(DRAFT, PLANNED, WAITING_FOR_APPROVAL, READY, RUNNING, PAUSED,
           FAILED, COMPLETED, CANCELLED) not null
current_plan_version integer not null
temporal_workflow_id varchar(240) unique
created_by UUID not null
created_at timestamptz not null
updated_at timestamptz not null
```

`SprintPlanVersion`:

```text
id UUID primary key
sprint_id UUID not null references sprints
version integer not null
directive_revision_id UUID not null references directive_revisions
reasoning text not null
approval_policy jsonb not null
failure_policy jsonb not null
created_by varchar(80) not null
created_at timestamptz not null
```

The unique key is `(sprint_id, version)`. A plan version is immutable after
the sprint reaches `READY`.

`SprintStep`:

```text
id UUID primary key
plan_version_id UUID not null references sprint_plan_versions
step_key varchar(120) not null
bead_ids jsonb not null
agent_role varchar(60) not null
agent_profile_version_id UUID not null references agent_profile_versions
repository_ids jsonb not null
state enum(PENDING, READY, RUNNING, SUCCEEDED, FAILED, BLOCKED,
           WAITING_FOR_HUMAN, SKIPPED, CANCELLED) not null
success_criteria jsonb not null
failure_policy jsonb not null
created_at timestamptz not null
updated_at timestamptz not null
```

`StepDependency` stores directed edges and has a unique key on
`(step_id, depends_on_step_id)`. The controller rejects self-edges and cycles
before a plan can be approved.

### 3.7 Leases and locks

`Lease` represents ownership with expiry:

```text
id UUID primary key
lease_type enum(PROJECT_MANAGER, PLANNING_TURN, AGENT_RUN)
owner_id varchar(240) not null
project_id UUID references projects
sprint_id UUID references sprints
step_id UUID references sprint_steps
heartbeat_at timestamptz not null
expires_at timestamptz not null
created_at timestamptz not null
```

Lease acquisition MUST be transactional. The owner renews before half the
lease duration has elapsed. Expired lease recovery requires checking the
owning Temporal workflow or agent run before reassignment.

`Lock` represents a resource held by a run or workflow:

```text
id UUID primary key
lock_key varchar(240) not null
lock_mode enum(EXCLUSIVE) not null
owner_type enum(PLANNING_TURN, SPRINT_STEP, AGENT_RUN) not null
owner_id varchar(240) not null
lease_id UUID not null references leases
acquired_at timestamptz not null
released_at timestamptz
```

Active `lock_key` values must be unique. Initial keys are:

```text
project:<project-id>
repository:<repository-id>
workspace:<workspace-id>
bead:<bead-id>
agent:<capacity-key>
environment:<environment-id>
```

In `GLOBAL_SERIAL`, the controller also acquires a global agent capacity lock.
In `LOCKED_CONCURRENT`, it acquires all step locks in sorted key order inside
one transaction to avoid lock-order deadlocks.

### 3.8 Agent attempts

`AgentRun` records one attempt and never gets reopened:

```text
id UUID primary key
step_id UUID not null references sprint_steps
attempt_number integer not null
idempotency_key varchar(240) unique not null
state enum(CREATED, STARTING, RUNNING, SUCCEEDED, FAILED, TIMED_OUT,
           CANCEL_REQUESTED, CANCELLED) not null
executor_id varchar(240)
workspace_id varchar(240) not null
started_at timestamptz
heartbeat_at timestamptz
finished_at timestamptz
error_code varchar(80)
error_message text
created_at timestamptz not null
```

The unique key is `(step_id, attempt_number)`. The idempotency key is derived
from sprint ID, step ID, and attempt number. A callback for a non-current run
is recorded as a late result and cannot change current state.

`AgentRunRepository` stores structured events separately from the run summary:

```text
run_event_id UUID primary key
agent_run_id UUID not null references agent_runs
sequence_number bigint not null
event_type varchar(80) not null
payload jsonb not null
redacted boolean not null
created_at timestamptz not null
```

### 3.9 Approvals

`Approval`:

```text
id UUID primary key
sprint_id UUID not null references sprints
plan_version_id UUID not null references sprint_plan_versions
state enum(NOT_REQUIRED, PENDING, APPROVED, REJECTED) not null
requested_at timestamptz
decided_at timestamptz
decided_by UUID references users
reason text
```

Only an approval for the current plan version can release a sprint to
`READY`. Rejection returns the plan to `DRAFT` and requires a new plan version
or an explicit resubmission policy.

### 3.10 Artifacts and repository results

`ArtifactReference`:

```text
id UUID primary key
agent_run_id UUID references agent_runs
sprint_id UUID not null references sprints
artifact_type varchar(60) not null
uri varchar(1000) not null
metadata jsonb
created_at timestamptz not null
```

Commit results are stored as structured references with repository ID, commit
SHA, branch/worktree, validation status, and whether the commit is a
checkpoint. Git content itself remains in Git, not PostgreSQL.

### 3.11 Audit events

`AuditEvent` is append-only:

```text
id UUID primary key
actor_type enum(USER, PROJECT_MANAGER, AGENT, SYSTEM)
actor_id varchar(240) not null
project_id UUID references projects
sprint_id UUID references sprints
step_id UUID references sprint_steps
event_type varchar(100) not null
from_state varchar(60)
to_state varchar(60)
reason text
payload jsonb not null
created_at timestamptz not null
```

Every state transition, lease acquisition/release, lock acquisition/release,
approval, agent attempt, Beads handoff, checkpoint commit, pause, resume,
retry, skip, cancel, and replan MUST create an audit event. Audit events are
never updated or deleted by the application.

## 4. Beads Integration Contract

The controller uses a `BeadsClient` port. The first adapter invokes `bd` in a
configured project workspace; a future adapter may use a compatible service.

Required port operations:

```text
listRelevantIssues(projectLabel, filters)
getIssue(issueId)
createIssue(issue)
claimIssue(issueId, actor)
addComment(issueId, actor, comment)
releaseClaim(issueId, actor)
```

The adapter MUST:

- Set `BEADS_DIR` to the project Beads directory.
- Set `BEADS_ACTOR` to the stable controller/agent actor identity.
- Use JSON output for machine-readable commands.
- Never read Beads internal database files directly.
- Preserve Beads IDs in sprint steps and agent handoffs.

The controller stores Beads IDs as references, not replicated issue records.
The project manager records inclusion, exclusion, and deferral reasoning in a
planning event and, when appropriate, in a Beads comment.

## 5. First End-to-End Workflow

### 5.1 Start conditions

The test fixture contains:

- One active project.
- One active repository.
- One active `project-manager` profile.
- One active `fullstack-dev` profile.
- One active directive revision.
- One ready Bead labelled for the project and `worker:fullstack`.
- A local Git remote or test repository.
- A human test user.

The controller has `execution_mode=GLOBAL_SERIAL` and
`approval_required=true`.

### 5.2 Manager planning turn

1. `controller-api` starts `project-manager:<project-id>` if absent.
2. The manager workflow acquires the project-manager lease.
3. The manager workflow acquires the global planning-turn lease.
4. A planning activity reads project configuration, directives, repository
   metadata, and relevant Beads through `BeadsClient`.
5. The manager selects the ready Bead and produces a plan with one step.
6. The controller validates the plan DAG, labels, profile, repository, and
   lock set.
7. The controller persists `Sprint`, `SprintPlanVersion`, `SprintStep`, and a
   `PENDING` approval in one transaction.
8. The manager releases the planning-turn lease.
9. The manager waits in `WAITING_FOR_APPROVAL`.

The plan must request:

```text
project:<project-id>
repository:<repository-id>
workspace:<sprint-id>
bead:<bead-id>
agent:fullstack-dev
```

### 5.3 Human approval

1. The UI reads the immutable plan and its reasoning.
2. The user approves the exact plan version.
3. The API verifies the plan is still current and the user can approve.
4. The API writes `Approval=APPROVED`, changes the sprint to `READY`, and
   appends an audit event in one transaction.
5. The API signals the sprint workflow with `approve(planVersion)`.

Approval of an old plan version returns a conflict and creates no state change.

### 5.4 Local agent execution

1. The sprint workflow acquires the configured resource locks in sorted order.
2. It changes the step to `READY`, then creates `AgentRun` attempt 1.
3. It changes the step and run to `RUNNING` and starts an execution activity.
4. The executor prepares `/workspace/runs/<sprint-id>/<repository-name>`.
5. It checks out the configured branch and sets `BEADS_DIR` and
   `BEADS_ACTOR`.
6. It invokes `fullstack-dev` with project, sprint, step, Beads, repository,
   profile, workspace, and success criteria.
7. It streams redacted events and sends Temporal heartbeats.
8. The agent claims the Bead, implements the change, runs configured tests,
   and writes a Beads handoff comment.

The handoff MUST contain:

```text
sprint_id
step_id
agent_run_id
bead_id
status
tests
commit_sha
remaining_work
```

### 5.5 Checkpoint commit

On successful validation:

1. The executor creates a checkpoint commit if the working tree changed.
2. The executor returns repository, commit SHA, branch/worktree, and
   validation results.
3. The controller stores the commit reference and artifact references.
4. The run changes to `SUCCEEDED`.
5. The step changes to `SUCCEEDED`.
6. The sprint changes to `COMPLETED` if all required steps satisfy the
   completion policy; otherwise the next ready step is scheduled.
7. Locks are released transactionally and an audit event is appended.

The first slice does not push automatically. Push behavior is a later
execution policy and must be explicit per project.

### 5.6 Pause and resume

1. A user sends `pause(sprintId)`.
2. The API verifies the sprint is `RUNNING` and sends a Temporal signal.
3. The sprint changes to `PAUSED` at the next safe workflow boundary.
4. No new step is scheduled while paused.
5. An active executor is allowed to finish unless force-stop is explicitly
   added later.
6. A user sends `resume(sprintId)`.
7. The sprint changes to `READY`, preserving completed steps and checkpoint
   commits.
8. The workflow reacquires required locks and continues from the next ready
   step.

Pause and resume are idempotent for the same requested state.

### 5.7 Failure and retry

When the executor fails, times out, or loses its heartbeat:

1. The current run becomes `FAILED` or `TIMED_OUT`.
2. The step becomes `FAILED`.
3. The sprint becomes `FAILED` unless its policy automatically retries.
4. The controller releases expired or explicitly releasable locks.
5. The UI displays the error, events, workspace, and last checkpoint.
6. A user sends `retry(stepId, reason)`.
7. The controller creates `AgentRun` attempt 2 with a new idempotency key.
8. The step becomes `READY` and the sprint becomes `READY`.
9. The sprint reacquires locks and runs attempt 2.
10. A late result from attempt 1 is recorded but cannot alter attempt 2.

Retry preserves the original attempt and audit history. It does not silently
reset the Bead or delete the workspace.

## 6. API Commands for the First Slice

Configuration:

```text
POST   /api/projects
GET    /api/projects/{projectId}
POST   /api/projects/{projectId}/repositories
POST   /api/projects/{projectId}/directives
POST   /api/agent-profiles
POST   /api/projects/{projectId}/manager
```

Planning and control:

```text
GET    /api/projects/{projectId}/sprints
GET    /api/sprints/{sprintId}
POST   /api/sprints/{sprintId}/approve
POST   /api/sprints/{sprintId}/pause
POST   /api/sprints/{sprintId}/resume
POST   /api/steps/{stepId}/retry
GET    /api/runs/{runId}
GET    /api/runs/{runId}/events
GET    /api/sprints/{sprintId}/audit-events
```

Commands return `202 Accepted` when they signal a running Temporal workflow.
State-changing commands include an idempotency key and return the resulting
resource reference. Invalid state transitions return `409 Conflict`.

## 7. Implementation Order

1. Add domain migrations for repositories, profiles, directives, manager
   assignments, plans, steps, leases, locks, runs, approvals, artifacts, and
   audit events.
2. Add JPA entities and repositories with transition validation in services.
3. Add project repository, profile, and directive APIs.
4. Add `BeadsClient` and a fake adapter for tests.
5. Add Temporal Compose services and a worker process.
6. Implement the project-manager workflow and global planning-turn lease.
7. Implement plan validation and approval signaling.
8. Implement `LocalAgentExecutor` behind an interface, with a fake executor
   for deterministic tests.
9. Implement real Devbox execution, heartbeats, redacted events, and Beads
   handoffs.
10. Implement checkpoint commit capture.
11. Implement pause/resume signals.
12. Implement retry with immutable attempt records and stale-result tests.
13. Add React views for plan approval, sprint state, output, and controls.
14. Add end-to-end Compose verification.

## 8. Completion Tests

The first slice is accepted when automated tests prove:

- A manager cannot acquire two project-manager leases for one project.
- Two managers cannot hold the global planning-turn lease together.
- A plan with a cycle or conflicting lock is rejected.
- Approval is bound to the exact plan version.
- An approved plan starts exactly one idempotent first attempt.
- The executor creates the documented workspace and environment.
- Successful execution records a checkpoint commit and Beads handoff.
- Pause prevents a new step from starting.
- Resume continues from the last completed step.
- Retry creates attempt 2 without reopening attempt 1.
- A late attempt-1 callback cannot overwrite attempt-2 state.
- Controller restart resumes the Temporal workflow.
- An expired lease is recoverable only after owner liveness is checked.
- Every transition and human action has an append-only audit event.
- The default execution mode permits only one active agent run.
