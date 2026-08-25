# YAADT --- Yet Another Agentic Dev Team

**A self-hosted, containerised software-development team built around
local LLMs, OpenCode, Beads and semantic code search.**

YAADT provides a repeatable development environment in which specialised
AI workers can turn an idea into a specification, architecture,
implementation tasks, code, tests and reviews --- while keeping the
durable state of the work outside the LLM conversation.

> **Core principle:** agents are disposable; project state is durable.

The development tools and third-party packages run inside Docker rather
than being installed directly on the host. The image build also performs
dependency security checks so that package problems can stop the build
before the development environment is used.

## Why?

Agentic development stacks increasingly ask developers to install npm
packages, Python packages, CLIs, MCP servers and scripts from multiple
projects.

YAADT takes a different approach:

-   isolate the agent toolchain in Docker;
-   use a local or self-hosted LLM rather than requiring a hosted coding
    model;
-   use **Beads** as the durable work and coordination layer;
-   use **OpenCode** as the coding-agent runtime;
-   use **CocoIndex Code** for semantic code discovery;
-   use normal **Git/Gitea/GitHub** repositories as the source of truth
    for code;
-   give each agent a narrowly defined engineering role;
-   security-check package dependencies while building the containers.

The host machine therefore stays relatively clean while projects remain
ordinary Git repositories.

## Architecture

``` mermaid
flowchart TB
    U[Developer / Product Idea]

    subgraph Docker["Docker development environment"]
        SW[Specification Writer]
        SA[Solution Architect]
        TL[Tech Lead]

        BE[Java / Backend Developer]
        FE[Frontend Developer]
        DB[Database Developer]
        DO[DevOps Engineer]

        QA[QA / Test Engineer]
        CR[Code Reviewer]
        SR[Security Reviewer]
        DW[Documentation Writer]

        OC[OpenCode]
        BD[(Beads)]
        CI[CocoIndex Code]
    end

    LLM[Local / Self-hosted LLM]
    GIT[Git Repository]
    REMOTE[Gitea / GitHub]
    UI[Beads UI]

    U --> SW
    SW --> SA
    SA --> TL

    TL --> BD
    BD --> BE
    BD --> FE
    BD --> DB
    BD --> DO

    BE --> QA
    FE --> QA
    DB --> QA
    DO --> QA

    QA --> CR
    CR --> SR
    SR --> DW

    SW --- OC
    SA --- OC
    TL --- OC
    BE --- OC
    FE --- OC
    DB --- OC
    DO --- OC
    QA --- OC
    CR --- OC
    SR --- OC
    DW --- OC

    OC <--> LLM
    OC <--> CI
    CI --> GIT
    BD <--> UI

    BE --> GIT
    FE --> GIT
    DB --> GIT
    DO --> GIT
    GIT <--> REMOTE
```

The components deliberately have different responsibilities:

  -----------------------------------------------------------------------
  Component                           Responsibility
  ----------------------------------- -----------------------------------
  **Beads**                           Tasks, dependencies, worker
                                      routing, discoveries, hand-offs and
                                      durable project knowledge

  **OpenCode**                        Agent execution, tool use and
                                      interaction with the LLM

  **Local LLM**                       Reasoning, planning, coding and
                                      review

  **CocoIndex Code**                  Semantic/AST-aware discovery of
                                      relevant source code

  **Git**                             Source code and change history

  **Gitea/GitHub**                    Remote repository and collaboration

  **Beads UI**                        Browser-based visibility of Beads
                                      work

  **Docker**                          Isolation and reproducibility of
                                      the agent development environment
  -----------------------------------------------------------------------

CocoIndex is intentionally **not** a task database, and the LLM
conversation is intentionally **not** the project memory.

## Agent team

### Planning and design

  -------------------------------------------------------------------------
  Command                 Role                    Typical output
  ----------------------- ----------------------- -------------------------
  `spec-writer`           Product/specification   `docs/specs/...`
                          writer                  

  `solution-architect`    Solution architect      `docs/architecture/...`

  `tech-lead`             Technical lead /        Beads epic, tasks and
                          planner                 dependencies
  -------------------------------------------------------------------------

### Implementation

  -----------------------------------------------------------------------
  Command                 Primary Beads label     Responsibility
  ----------------------- ----------------------- -----------------------
  `java-dev`              `worker:java`           Java/Spring/backend
                                                  implementation

  `frontend-dev`          `worker:frontend`       React/Vite/frontend
                                                  implementation

  `database-dev`          `worker:database`       Schema, migrations and
                                                  persistence

  `devops`                `worker:devops`         Docker, CI/CD and
                                                  runtime infrastructure
  -----------------------------------------------------------------------

### Quality and hand-off

  ----------------------------------------------------------------------------
  Command                 Primary Beads label     Responsibility
  ----------------------- ----------------------- ----------------------------
  `qa-test`               `worker:qa`             Acceptance, integration and
                                                  regression testing

  `code-reviewer`         `worker:review`         Independent code review

  `security-reviewer`     `worker:security`       Application and supply-chain
                                                  security review

  `docs-writer`           `worker:docs`           User/developer/operational
                                                  documentation
  ----------------------------------------------------------------------------

Workers are intentionally small and replaceable. A coding worker
normally claims **one ready Bead**, completes it, records a durable
hand-off and exits.

## Durable workflow

``` mermaid
flowchart LR
    IDEA[Idea] --> SPEC[Specification]
    SPEC --> ARCH[Architecture]
    ARCH --> PLAN[Beads work graph]
    PLAN --> DEV[Implementation worker]
    DEV --> TEST[QA]
    TEST --> REVIEW[Code review]
    REVIEW --> SEC[Security review]
    SEC --> DOCS[Documentation]
    DOCS --> DONE[Complete]

    DEV -. discovered work .-> PLAN
    TEST -. defect .-> PLAN
    REVIEW -. follow-up .-> PLAN
    SEC -. security issue .-> PLAN
```

Beads is used throughout the lifecycle for:

-   issue/task state;
-   dependencies and blockers;
-   worker routing labels;
-   lifecycle/stage labels;
-   comments used as agent hand-offs;
-   discovered follow-up work;
-   reusable project knowledge via `bd remember`;
-   synchronisation via `bd dolt push` when a remote is configured.

This means a worker does not need the previous worker's chat history to
continue the job.

## Beads routing

Tasks are routed with labels.

### Worker labels

``` text
worker:spec
worker:architect
worker:tech-lead
worker:java
worker:frontend
worker:database
worker:devops
worker:qa
worker:review
worker:security
worker:docs
```

### Lifecycle labels

``` text
stage:spec
stage:architecture
stage:planning
stage:implementation
stage:testing
stage:review
stage:security-review
stage:documentation
```

### Area labels

``` text
area:backend
area:frontend
area:database
area:infra
area:test
area:security
area:docs
```

Implementation workers inspect `bd ready --json` and select work
carrying their `worker:*` label. A specific Bead can also be supplied
explicitly.

``` bash
java-dev bd-a1b2
qa-test bd-a1b2
code-reviewer bd-a1b2
```

## Quick start

### 1. Clone YAADT

``` bash
git clone https://github.com/ianleggett/agentic-dev-team.git
cd agentic-dev-team
```

### 2. Configure the environment

The Compose file supports host UID/GID mapping and Git/Gitea settings.
Create an `.env` file appropriate to your environment, for example:

``` dotenv
USER_UID=1000
USER_GID=1000

GIT_AUTHOR_NAME=Your Name
GIT_AUTHOR_EMAIL=you@example.com

GITEA_URL=https://gitea.example.internal

NPM_AUDIT_LEVEL=high
```

Do **not** commit API keys, SSH private keys or other credentials.

The current Compose configuration mounts the host SSH directory
read-only into the devbox. For shared or production-like environments, a
dedicated development key is preferable.

### 3. Build

``` bash
docker compose build --no-cache --progress=plain
```

A dependency security failure is intended to stop the affected image
build.

### 4. Start

``` bash
docker compose up -d
```

Check:

``` bash
docker compose ps
```

The Beads UI is exposed on:

``` text
http://localhost:3007
```

### 5. Enter the devbox

``` bash
docker compose exec devbox bash
```

Your host workspace is mounted at:

``` text
/workspace
```

Clone or open the project you want the team to work on:

``` bash
cd /workspace
git clone git@gitea.example.internal:team/my-project.git
cd my-project
```

### 6. Initialise the team

``` bash
team-init
```

If CocoIndex Code is installed in the image, initialise semantic
indexing for the repository:

``` bash
cocoindex-project-init
```

Then inspect the available work:

``` bash
team-status
```

## LLM deployment options

YAADT does **not** require the LLM to run on the same machine as the devbox.

OpenCode is the boundary between the agent team and the model provider. As long as the devbox can reach a supported provider/API, the model can run:

```text
┌───────────────────────────────┐
│ YAADT host                    │
│                               │
│ Docker                        │
│ ┌───────────────────────────┐ │
│ │ devbox                    │ │
│ │                           │ │
│ │ agents -> OpenCode        │ │
│ │            │              │ │
│ │ Beads      │ CocoIndex    │ │
│ └────────────┼──────────────┘ │
└──────────────┼────────────────┘
               │ HTTPS / HTTP
               ▼
        ┌───────────────┐
        │ LLM endpoint  │
        └───────────────┘
```

That endpoint might be:

- a model server running on the same workstation;
- a GPU server elsewhere on the local network;
- a dedicated on-premises inference server;
- a VM or GPU instance in a private cloud/VPC;
- a hosted OpenAI-compatible inference endpoint;
- a directly supported OpenCode model provider.

This separation is intentional. **YAADT provides the development-team runtime; it does not prescribe where inference must run.**

### Why run inference externally?

Keeping inference separate from the devbox has several advantages:

- the development host does not need a large GPU;
- one inference server can support several development environments;
- the model can be upgraded independently of YAADT;
- large models can run on dedicated GPU hardware;
- the devbox remains comparatively small;
- inference infrastructure can be scaled independently;
- organisations can keep both code and inference inside their own network if required.

A common arrangement is:

```text
Developer workstation
└── YAADT Docker environment
      ├── OpenCode
      ├── Beads
      ├── CocoIndex
      └── source repository
             │
             │ LAN / VPN / HTTPS
             ▼
Dedicated GPU server
└── vLLM
      └── coding model
```

Multiple YAADT instances can also share the same inference service:

```text
YAADT developer A ─┐
YAADT developer B ─┼──► LLM gateway / vLLM ──► GPU(s)
YAADT developer C ─┘
```

### What an external LLM needs

For the simplest integration, expose an **OpenAI-compatible API** reachable from the devbox.

Typical endpoints are:

```text
GET  /v1/models
POST /v1/chat/completions
```

Depending on the OpenCode/provider configuration, other APIs such as the Responses API may also be usable.

For agentic coding, the selected model should ideally support:

- reliable tool/function calling;
- long context;
- code generation and editing;
- structured JSON/tool arguments;
- instruction following;
- sufficient output length.

Tool-calling quality matters more here than it does for a normal chat interface because the model needs to invoke shell, file, Beads and CocoIndex tools correctly.

### Network connectivity

The important requirement is that the **devbox container**, rather than only the Docker host, can reach the inference endpoint.

Enter the devbox:

```bash
docker compose exec devbox bash
```

Then test the external server:

```bash
curl https://llm.example.internal/v1/models
```

or for a LAN server:

```bash
curl http://192.168.1.50:8000/v1/models
```

If this fails, resolve Docker networking, routing, DNS or firewall access before configuring OpenCode.

For a model server running on the Docker host itself, Docker's host gateway may be useful:

```text
host.docker.internal
```

The Compose configuration can map this with:

```yaml
extra_hosts:
  - "host.docker.internal:host-gateway"
```

### Configure OpenCode for an external OpenAI-compatible LLM

OpenCode supports custom providers and custom base URLs, so the external endpoint is configured in OpenCode rather than in the individual agent scripts.

A global configuration can be placed at:

```text
/home/developer/.config/opencode/opencode.json
```

Example:

```json
{
  "$schema": "https://opencode.ai/config.json",
  "provider": {
    "external": {
      "npm": "@ai-sdk/openai-compatible",
      "name": "External Coding LLM",
      "options": {
        "baseURL": "https://llm.example.internal/v1",
        "apiKey": "{env:LLM_API_KEY}"
      },
      "models": {
        "qwen-coder": {
          "name": "Qwen Coder",
          "limit": {
            "context": 131072,
            "output": 16384
          }
        }
      }
    }
  },
  "model": "external/qwen-coder"
}
```

The model ID must match an ID returned by:

```bash
curl https://llm.example.internal/v1/models
```

Test OpenCode after configuration:

```bash
opencode run "Reply with exactly: EXTERNAL LLM WORKING"
```

Then test tool use:

```bash
opencode run "Run pwd using the shell tool and report the directory."
```

The second test is important: a model can successfully answer ordinary prompts while still being unsuitable for an agentic workflow because tool calling is incorrectly configured.

### Example: remote vLLM server

One option is to run vLLM on a separate GPU machine and expose its OpenAI-compatible server.

Conceptually:

```text
GPU server
    │
    ├── vLLM
    │    └── coding model
    │
    └── :8000/v1
           ▲
           │
        LAN/VPN
           │
YAADT ─ OpenCode
```

The exact vLLM arguments depend on the model. For models that support automatic tool use, configure the appropriate tool-call parser for that model.

From the YAADT devbox:

```bash
curl http://gpu-server:8000/v1/models
```

and configure:

```json
"options": {
  "baseURL": "http://gpu-server:8000/v1",
  "apiKey": "{env:LLM_API_KEY}"
}
```

### Hosted model providers

OpenCode also supports numerous hosted providers directly. In that case YAADT does not need to know anything about the provider: the agents simply invoke OpenCode, and OpenCode handles the configured model connection.

This makes the agent team portable:

```text
             ┌── local vLLM
             │
YAADT/OpenCode├── remote vLLM
             │
             ├── private inference service
             │
             └── hosted model provider
```

Changing provider should not require changing `java-dev`, `tech-lead`, `qa-test`, or the other agent scripts.

### Credentials

Do not put API keys directly in `opencode.json` or commit them to Git.

Prefer environment substitution:

```json
"apiKey": "{env:LLM_API_KEY}"
```

and supply the value to the devbox through your environment, Compose secrets, or another appropriate secret-management mechanism.

For example:

```yaml
services:
  devbox:
    environment:
      LLM_API_KEY: ${LLM_API_KEY}
```

For a shared or remotely reachable inference service, use authentication and transport security appropriate to the network. Do not assume that an inference server's built-in API-key support alone provides a complete network-security boundary.

### Reverse proxies and TLS

For an inference endpoint outside a trusted local network, a useful production-style topology is:

```text
YAADT
  │
  │ HTTPS
  ▼
Reverse proxy / gateway
  ├── TLS
  ├── authentication
  ├── access control
  ├── request limits
  └── logging
       │
       ▼
     vLLM
```

The inference service itself can then remain on a private interface.

### Code privacy

An important consequence of using an external model is that **source-code context selected by the agents may be sent to that inference endpoint**.

Before using a third-party hosted provider, understand:

- where prompts/code are processed;
- whether requests are retained;
- whether data is used for training;
- applicable organisational or customer restrictions;
- whether credentials or sensitive files could enter model context.

Running the model on another machine that you control can retain the benefits of separate inference hardware while keeping model traffic within your own network.

### Local versus external inference

| Deployment | Advantages | Trade-offs |
| --- | --- | --- |
| Same workstation | Simple, private, no network dependency | Requires sufficient local GPU/RAM |
| Separate LAN GPU server | Powerful hardware, private, reusable | Requires networking and server administration |
| Private cloud/VPC | Scalable and centrally managed | Infrastructure cost and complexity |
| Hosted provider | Easiest access to large models | Cost, external dependency and data-governance considerations |

YAADT is deliberately neutral between these choices.

The only architectural contract the agent team cares about is:

```text
Agent -> OpenCode -> configured model provider
```

Everything behind that boundary can be changed independently.


## CocoIndex Code

CocoIndex Code provides semantic code search so agents can locate
relevant implementation without repeatedly loading large parts of a
repository into context.

The intended division is:

``` text
conceptual question     -> CocoIndex
exact symbol/string     -> ripgrep / grep
actual implementation   -> read the source
task/project state      -> Beads
```

Configure the OpenCode MCP integration with:

``` bash
configure-cocoindex-opencode
```

For a repository:

``` bash
cd /workspace/my-project
cocoindex-project-init
```

Example direct search:

``` bash
ccc search "where is authentication enforced?"
```

Agents are instructed to use semantic search as a navigation aid, then
inspect the actual source before modifying it.

## Example feature lifecycle

Start with an idea:

``` bash
mkdir -p docs/ideas

cat > docs/ideas/customer-management.md <<'EOF'
Add customer management.

Users must be able to create, update, search and deactivate customers.
The backend is Java/Spring Boot/PostgreSQL.
The frontend is React/Vite.
EOF
```

Create the specification:

``` bash
spec-writer docs/ideas/customer-management.md
```

Create the architecture:

``` bash
solution-architect docs/specs/customer-management.md
```

Turn it into executable work:

``` bash
tech-lead docs/architecture/customer-management.md
```

Inspect queues:

``` bash
team-status
```

Run workers:

``` bash
database-dev
java-dev
frontend-dev
```

Then quality workers:

``` bash
qa-test
code-reviewer
security-reviewer
docs-writer
```

Or target one task explicitly:

``` bash
java-dev bd-a1b2
```

Re-running a worker lets it take the next matching ready task.

## Useful commands

``` bash
team-init
team-status

spec-writer <input>
solution-architect <spec>
tech-lead <architecture-or-spec>

java-dev [bead]
frontend-dev [bead]
database-dev [bead]
devops [bead]

qa-test [bead]
code-reviewer [bead]
security-reviewer [bead]
docs-writer [bead]

team-run-one <worker>

beads-handoff \
  <bead-id> \
  <worker-label> \
  <stage-label> \
  "handoff comment"
```

Normal Beads commands remain available:

``` bash
bd prime
bd ready --json
bd show <id> --json
bd update <id> --claim --json
```

## Repository layout

``` text
agentic-dev-team/
├── beads-ui/
│   └── Dockerfile
├── devbox/
│   ├── Dockerfile
│   ├── bin/
│   │   ├── spec-writer
│   │   ├── solution-architect
│   │   ├── tech-lead
│   │   ├── java-dev
│   │   ├── frontend-dev
│   │   ├── database-dev
│   │   ├── devops
│   │   ├── qa-test
│   │   ├── code-reviewer
│   │   ├── security-reviewer
│   │   ├── docs-writer
│   │   └── ...
│   ├── team/
│   │   ├── _COMMON.md
│   │   └── role definitions...
│   └── templates/
├── workspace/              # local bind-mounted project area
├── docker-compose.yml
├── EXAMPLE-WORKFLOW.md
└── README.md
```

Project-specific files stay in the project repository:

``` text
my-project/
├── .beads/
├── docs/
│   ├── ideas/
│   ├── specs/
│   └── architecture/
├── AGENTS.md
├── opencode.json           # optional project override
└── source...
```

Generic agent policies belong in the devbox image; application-specific
knowledge belongs with the application.

## Security model

YAADT does not claim that package auditing makes arbitrary dependencies
safe. Its goal is to reduce risk and make the development toolchain
easier to inspect and replace.

The approach is:

1.  isolate development agents and their dependencies in containers;
2.  avoid installing the agent stack directly onto the host;
3.  install npm dependencies with lifecycle scripts disabled initially
    where practical;
4.  run vulnerability auditing before enabling/rebuilding packages;
5.  perform npm signature/provenance checks where supported;
6.  audit Python runtime dependencies for CocoIndex Code;
7.  fail builds when the configured security gate fails;
8.  mount credentials only when required;
9.  run the normal devbox as the unprivileged `developer` user.

Important limitations:

-   a clean CVE scan is **not** proof that a package is trustworthy;
-   scanners cannot detect every malicious package or supply-chain
    compromise;
-   mounting SSH credentials gives the container access to those
    credentials;
-   giving agents access to source repositories means they can modify
    that source;
-   local LLMs vary substantially in tool-use reliability.

Treat the devbox as a development environment with meaningful
privileges, not as a hostile-code sandbox.

## Development philosophy

### Agents are disposable

A worker can crash, lose context or be replaced by another model.

Nothing important should depend on its conversation history.

### State is durable

Work state is written to Beads. Code is written to Git. Specifications
and architecture are files in the repository.

### Workers are specialised

A Java developer should not redesign product requirements while
implementing a small backend task. A reviewer should not silently turn
into a feature developer.

### Handoffs are explicit

The next worker receives a Beads task, labels, dependencies, acceptance
criteria and comments --- not an assumption that it knows what happened
in another chat.

### Local-first does not mean model-specific

The architecture is intended to work with different OpenAI-compatible
local/self-hosted models. The LLM is a replaceable execution component.

## Current limitations

YAADT is still an experimental development environment rather than a
fully autonomous engineering platform.

In particular:

-   workers are primarily launched explicitly;
-   parallel workers need additional branch/worktree coordination;
-   automatic pull-request creation and merge policy are not yet the
    core workflow;
-   model quality and tool-calling support affect reliability;
-   Beads/CocoIndex/OpenCode are active upstream projects and their
    interfaces can change.

Human review remains appropriate before merging or deploying generated
changes.

## Roadmap

Useful next steps include:

-   [ ] dispatcher/orchestrator for automatically launching ready
    workers;
-   [ ] one Git branch/worktree per Bead;
-   [ ] safe parallel worker execution;
-   [ ] automatic Gitea/GitHub pull-request creation;
-   [ ] developer ↔ reviewer feedback loops;
-   [ ] worker concurrency limits;
-   [ ] model selection by role;
-   [ ] task duration/token/LLM metrics;
-   [ ] richer Beads dashboard integration;
-   [ ] CI execution of agent-generated changes;
-   [ ] stronger sandboxing of individual implementation workers;
-   [ ] reproducible/pinned dependency manifests and automated update
    PRs;
-   [ ] end-to-end example project demonstrating idea → merged PR.

## Status

This project is experimental and evolving. It is intended for people
exploring local/self-hosted agentic software development who want more
durable coordination and isolation than a collection of prompts and
host-installed scripts.

Contributions, experiments and issue reports are welcome.

## Upstream projects

YAADT combines ideas and tooling from several independent open-source
projects:

-   [Beads](https://github.com/steveyegge/beads) / the Beads ecosystem
    --- durable agent-oriented work tracking
-   [beads-ui](https://github.com/mantoni/beads-ui) --- web UI for Beads
-   [OpenCode](https://github.com/anomalyco/opencode) --- coding-agent
    runtime
-   [CocoIndex Code](https://github.com/cocoindex-io/cocoindex-code) ---
    semantic code intelligence

Please refer to each upstream project for its own documentation, licence
and security information.

## Licence

See [LICENSE](LICENSE).
