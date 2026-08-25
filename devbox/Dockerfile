FROM ubuntu:24.04

ARG DEBIAN_FRONTEND=noninteractive
ARG USER_UID=1000
ARG USER_GID=1000
ARG NPM_AUDIT_LEVEL=high
ARG COCOINDEX_CODE_VERSION=0.2.39

ENV DEV_UID=${USER_UID}
ENV DEV_GID=${USER_GID}

# -----------------------------------------------------------------------------
# Base development tools
# -----------------------------------------------------------------------------

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        bash \
        build-essential \
        ca-certificates \
        curl \
        wget \
        git \
        git-lfs \
        jq \
        unzip \
        zip \
        sqlite3 \
        openssh-client \
        python3 \
        python3-pip \
        python3-venv \
        openjdk-21-jdk \
        maven \
        gradle \
        sudo \
        less \
        vim \
        nano \
        ripgrep \
        gosu \
        && rm -rf /var/lib/apt/lists/*

# -----------------------------------------------------------------------------
# Node.js 22
# -----------------------------------------------------------------------------

RUN curl -fsSL https://deb.nodesource.com/setup_22.x | bash - && \
    apt-get update && \
    apt-get install -y --no-install-recommends nodejs && \
    rm -rf /var/lib/apt/lists/*

RUN node --version && \
    npm --version && \
    java -version && \
    mvn -version && \
    git --version

# -----------------------------------------------------------------------------
# Create/reuse developer account matching host UID/GID
# -----------------------------------------------------------------------------

RUN set -eux; \
    EXISTING_USER="$(getent passwd "${USER_UID}" | cut -d: -f1 || true)"; \
    EXISTING_GROUP="$(getent group "${USER_GID}" | cut -d: -f1 || true)"; \
    \
    if [ -n "${EXISTING_USER}" ]; then \
        if [ "${EXISTING_USER}" != "developer" ]; then \
            usermod --login developer "${EXISTING_USER}"; \
            usermod --home /home/developer --move-home developer; \
        fi; \
    else \
        if [ -z "${EXISTING_GROUP}" ]; then \
            groupadd --gid "${USER_GID}" developer; \
        fi; \
        useradd \
            --create-home \
            --shell /bin/bash \
            --uid "${USER_UID}" \
            --gid "${USER_GID}" \
            developer; \
    fi; \
    \
    usermod --shell /bin/bash developer; \
    echo "developer ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/developer; \
    chmod 0440 /etc/sudoers.d/developer; \
    mkdir -p /workspace /home/developer/.local/bin; \
    chown -R "${USER_UID}:${USER_GID}" /workspace /home/developer

# -----------------------------------------------------------------------------
# Beads
# -----------------------------------------------------------------------------

USER developer

ENV HOME=/home/developer
ENV PATH="/home/developer/.local/bin:${PATH}"

RUN curl -fsSL \
      https://raw.githubusercontent.com/gastownhall/beads/main/scripts/install.sh \
      | bash && \
    command -v bd && \
    bd version

RUN git config --global init.defaultBranch main && \
    git config --global safe.directory '*'

# -----------------------------------------------------------------------------
# OpenCode - security gated
# -----------------------------------------------------------------------------

USER root

WORKDIR /opt/opencode

RUN npm init -y && \
    npm install --ignore-scripts --package-lock-only opencode-ai && \
    npm ci --ignore-scripts

RUN mkdir -p /opt/opencode/security-reports

RUN set +e; \
    npm audit \
        --audit-level="${NPM_AUDIT_LEVEL}" \
        --json \
        > /opt/opencode/security-reports/npm-audit.json; \
    AUDIT_EXIT=$?; \
    jq '.metadata.vulnerabilities // .' \
        /opt/opencode/security-reports/npm-audit.json \
        || cat /opt/opencode/security-reports/npm-audit.json; \
    if [ "${AUDIT_EXIT}" -ne 0 ]; then \
        echo "OpenCode npm dependency audit FAILED"; \
        exit "${AUDIT_EXIT}"; \
    fi

RUN set +e; \
    npm audit signatures \
        > /opt/opencode/security-reports/npm-signatures.log 2>&1; \
    SIGNATURE_EXIT=$?; \
    cat /opt/opencode/security-reports/npm-signatures.log; \
    if [ "${SIGNATURE_EXIT}" -ne 0 ]; then \
        echo "OpenCode npm signature/provenance check FAILED"; \
        exit "${SIGNATURE_EXIT}"; \
    fi

RUN set -eu; \
    REPORT="/opt/opencode/security-reports/npm-lifecycle-scripts.log"; \
    : > "${REPORT}"; \
    find node_modules -name package.json -type f -print0 | \
        xargs -0 -r jq -r \
        'select(.scripts.preinstall or .scripts.install or .scripts.postinstall) | "\(.name // "unknown")@\(.version // "unknown") -> \(.scripts)"' \
        | tee "${REPORT}"

RUN npm rebuild && \
    chown -R "${USER_UID}:${USER_GID}" /opt/opencode

# -----------------------------------------------------------------------------
# CocoIndex Code FULL - pinned and security gated
# -----------------------------------------------------------------------------

RUN python3 -m venv /opt/cocoindex-code && \
    /opt/cocoindex-code/bin/python -m pip install --upgrade pip setuptools wheel && \
    /opt/cocoindex-code/bin/pip install \
        "cocoindex-code[full]==${COCOINDEX_CODE_VERSION}"

RUN python3 -m venv /opt/python-security-tools && \
    /opt/python-security-tools/bin/python -m pip install --upgrade pip && \
    /opt/python-security-tools/bin/pip install pip-audit

RUN mkdir -p /opt/cocoindex-code/security-reports && \
    set +e; \
    /opt/python-security-tools/bin/pip-audit \
        --path /opt/cocoindex-code/lib/python3.12/site-packages \
        --format json \
        --output /opt/cocoindex-code/security-reports/pip-audit.json; \
    AUDIT_EXIT=$?; \
    if [ -s /opt/cocoindex-code/security-reports/pip-audit.json ]; then \
        jq '.' /opt/cocoindex-code/security-reports/pip-audit.json \
            || cat /opt/cocoindex-code/security-reports/pip-audit.json; \
    fi; \
    if [ "${AUDIT_EXIT}" -ne 0 ]; then \
        echo "CocoIndex Code Python dependency audit FAILED"; \
        exit "${AUDIT_EXIT}"; \
    fi

RUN /opt/cocoindex-code/bin/ccc --help >/dev/null && \
    chown -R "${USER_UID}:${USER_GID}" /opt/cocoindex-code

# -----------------------------------------------------------------------------
# Agent team
# -----------------------------------------------------------------------------

COPY team/ /opt/agent-team/
COPY templates/ /opt/agent-team/templates/

COPY bin/_beads-worker /usr/local/libexec/_beads-worker
COPY bin/_planning-worker /usr/local/libexec/_planning-worker

COPY bin/spec-writer /usr/local/bin/spec-writer
COPY bin/solution-architect /usr/local/bin/solution-architect
COPY bin/tech-lead /usr/local/bin/tech-lead
COPY bin/java-dev /usr/local/bin/java-dev
COPY bin/frontend-dev /usr/local/bin/frontend-dev
COPY bin/database-dev /usr/local/bin/database-dev
COPY bin/devops /usr/local/bin/devops
COPY bin/qa-test /usr/local/bin/qa-test
COPY bin/code-reviewer /usr/local/bin/code-reviewer
COPY bin/security-reviewer /usr/local/bin/security-reviewer
COPY bin/docs-writer /usr/local/bin/docs-writer

COPY bin/team-status /usr/local/bin/team-status
COPY bin/team-init /usr/local/bin/team-init
COPY bin/team-run-one /usr/local/bin/team-run-one
COPY bin/beads-handoff /usr/local/bin/beads-handoff
COPY bin/configure-cocoindex-opencode /usr/local/bin/configure-cocoindex-opencode
COPY bin/cocoindex-project-init /usr/local/bin/cocoindex-project-init

COPY bin/project-add /usr/local/bin/project-add
COPY bin/project-list /usr/local/bin/project-list

RUN chmod 0755 \
      /usr/local/libexec/_beads-worker \
      /usr/local/libexec/_planning-worker \
      /usr/local/bin/spec-writer \
      /usr/local/bin/solution-architect \
      /usr/local/bin/tech-lead \
      /usr/local/bin/java-dev \
      /usr/local/bin/frontend-dev \
      /usr/local/bin/database-dev \
      /usr/local/bin/devops \
      /usr/local/bin/qa-test \
      /usr/local/bin/code-reviewer \
      /usr/local/bin/security-reviewer \
      /usr/local/bin/docs-writer \
      /usr/local/bin/team-status \
      /usr/local/bin/team-init \
      /usr/local/bin/team-run-one \
      /usr/local/bin/beads-handoff \
      /usr/local/bin/project-add \
      /usr/local/bin/project-list \
      /usr/local/bin/configure-cocoindex-opencode \
      /usr/local/bin/cocoindex-project-init && \
    chmod 0644 \
      /opt/agent-team/*.md \
      /opt/agent-team/templates/*

# -----------------------------------------------------------------------------
# Writable OpenCode / CocoIndex state
# -----------------------------------------------------------------------------

RUN mkdir -p \
      /home/developer/.local/share/opencode/log \
      /home/developer/.local/state/opencode \
      /home/developer/.config/opencode \
      /home/developer/.cache/opencode \
      /home/developer/.cache/cocoindex \
      /home/developer/.local/share/cocoindex && \
    chown -R "${USER_UID}:${USER_GID}" \
      /home/developer/.local \
      /home/developer/.config \
      /home/developer/.cache

ENV PATH="/opt/cocoindex-code/bin:/opt/opencode/node_modules/.bin:/home/developer/.local/bin:${PATH}"

# -----------------------------------------------------------------------------
# Runtime entrypoint
#
# /workspace is a bind mount, so ownership set during docker build is hidden
# when the container starts. This entrypoint repairs the bind-mounted workspace
# ownership at runtime, then drops privileges to `developer`.
#
# Set FIX_WORKSPACE_PERMISSIONS=false if you intentionally do not want this.
# -----------------------------------------------------------------------------

RUN cat > /usr/local/bin/devbox-entrypoint <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

DEV_UID="${DEV_UID:-1000}"
DEV_GID="${DEV_GID:-1000}"
FIX_WORKSPACE_PERMISSIONS="${FIX_WORKSPACE_PERMISSIONS:-true}"

echo "devbox startup:"
echo "  developer UID:GID = ${DEV_UID}:${DEV_GID}"
echo "  workspace         = /workspace"

# Ensure writable application state exists every time the container starts.
mkdir -p \
  /home/developer/.local/share/opencode/log \
  /home/developer/.local/state/opencode \
  /home/developer/.config/opencode \
  /home/developer/.cache/opencode \
  /home/developer/.cache/cocoindex \
  /home/developer/.local/share/cocoindex

chown -R "${DEV_UID}:${DEV_GID}" \
  /home/developer/.local \
  /home/developer/.config \
  /home/developer/.cache

if [[ "${FIX_WORKSPACE_PERMISSIONS}" == "true" ]] && [[ -d /workspace ]]; then
  echo "Checking bind-mounted workspace ownership..."

  # A bind mount replaces the image's /workspace, so repair ownership here.
  # Only change entries that do not already match the developer UID/GID.
  find /workspace \
    \( ! -uid "${DEV_UID}" -o ! -gid "${DEV_GID}" \) \
    -exec chown "${DEV_UID}:${DEV_GID}" {} + 2>/dev/null || true

  # The root directory itself must also be writable.
  chown "${DEV_UID}:${DEV_GID}" /workspace 2>/dev/null || true
fi

if [[ -d /workspace ]] && ! gosu developer test -w /workspace; then
  echo "ERROR: /workspace is still not writable by developer." >&2
  echo "Current permissions:" >&2
  ls -ldn /workspace >&2 || true
  exit 1
fi

echo "Workspace permissions OK."

exec gosu developer "$@"
EOF

RUN chmod 0755 /usr/local/bin/devbox-entrypoint

# -----------------------------------------------------------------------------
# Runtime
#
# IMPORTANT: container deliberately starts as root so the entrypoint can repair
# bind-mount ownership. The application command itself is then executed as the
# unprivileged developer user using gosu.
# -----------------------------------------------------------------------------

USER root

WORKDIR /workspace

ENV FIX_WORKSPACE_PERMISSIONS=true

ENTRYPOINT ["/usr/local/bin/devbox-entrypoint"]

CMD ["sleep", "infinity"]
