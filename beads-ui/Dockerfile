# =============================================================================
# beads-ui
#
# Source:
#   https://github.com/mantoni/beads-ui
#
# Security:
#   - Reproducible npm install via package-lock.json
#   - npm lifecycle scripts disabled during initial install
#   - vulnerable transitive dependencies updated within declared
#     semver ranges (no overrides, no major bumps) before auditing
#   - npm vulnerability audit
#   - npm signature/provenance verification
#   - lifecycle scripts logged before execution
#   - upstream tests/type-check/lint run during build
#   - build fails on security/test failures
#
# Runtime:
#   - Unprivileged developer user
#   - Beads (bd) CLI installed
#   - Shared projects under /workspace
#   - UI exposed on port 3007
# =============================================================================


# =============================================================================
# BUILDER
# =============================================================================

FROM node:22-bookworm-slim AS builder

ARG DEBIAN_FRONTEND=noninteractive

# Security threshold:
#
#   low
#   moderate
#   high
#   critical
#
# Default: fail on HIGH or CRITICAL vulnerabilities.
ARG NPM_AUDIT_LEVEL=high

ARG BEADS_UI_REPOSITORY=https://github.com/mantoni/beads-ui.git
ARG BEADS_UI_REF=main


# -----------------------------------------------------------------------------
# Build dependencies
# -----------------------------------------------------------------------------

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        git \
        jq \
        build-essential \
    && rm -rf /var/lib/apt/lists/*


# -----------------------------------------------------------------------------
# Pin the npm version
#
# node:22 ships npm 10.x, whose arborist cannot update scoped
# transitive dependencies in the beads-ui tree (crashes with
# "Cannot read properties of null (reading 'edgesOut')").
#
# The pinned npm 11 line performs the scoped update correctly.
# -----------------------------------------------------------------------------

ARG NPM_VERSION=11.20.0

RUN echo "============================================================" && \
    echo " Installing npm ${NPM_VERSION}" && \
    echo "============================================================" && \
    npm install -g "npm@${NPM_VERSION}" && \
    npm --version


# -----------------------------------------------------------------------------
# Verify build environment
# -----------------------------------------------------------------------------

RUN echo "============================================================" && \
    echo " Build environment" && \
    echo "============================================================" && \
    node --version && \
    npm --version && \
    git --version


# -----------------------------------------------------------------------------
# Fetch beads-ui
#
# BEADS_UI_REF can be:
#
#   main
#   tag
#   commit SHA
#
# Once everything is working I recommend pinning this to a commit SHA.
# -----------------------------------------------------------------------------

WORKDIR /opt

RUN git clone "${BEADS_UI_REPOSITORY}" beads-ui && \
    cd beads-ui && \
    git checkout "${BEADS_UI_REF}" && \
    echo "============================================================" && \
    echo " beads-ui source revision" && \
    echo "============================================================" && \
    git rev-parse HEAD


WORKDIR /opt/beads-ui


# -----------------------------------------------------------------------------
# Security reports
# -----------------------------------------------------------------------------

RUN mkdir -p /opt/beads-ui/security-reports

# These are not application source: security reports and Beads test metadata
# are generated during the image build. CHANGES.md is upstream release text
# and is not formatted by the current upstream validation suite.
RUN printf 'security-reports/\n.beads/\nCHANGES.md\n' > /opt/beads-ui/.prettierignore


# -----------------------------------------------------------------------------
# Install npm dependencies
#
# IMPORTANT:
#
# Lifecycle scripts are initially disabled.
#
# This prevents a dependency from executing:
#
#   preinstall
#   install
#   postinstall
#
# before we have performed our security checks.
# -----------------------------------------------------------------------------

RUN echo "============================================================" && \
    echo " Installing npm dependencies" && \
    echo " Lifecycle scripts: DISABLED" && \
    echo "============================================================" && \
    npm ci --ignore-scripts


# -----------------------------------------------------------------------------
# Repair vulnerable transitive dependencies
#
# The upstream package-lock.json pins transitive dependencies for which
# security fixes have been published. All fixes fall WITHIN the semver
# ranges already declared by their parent packages, so a scoped
# "npm update" resolves them without:
#
#   - changing any direct dependency version
#   - introducing new packages
#   - using "overrides"
#
# Only the vulnerable packages are touched. Every change is logged to
# security-reports before the audit gate runs.
#
# Known advisories (beads-ui main @ b1d519a, beads-ui 0.12.6):
#
#   HIGH     brace-expansion  GHSA-q2hr-2g5m-vwhr (<1.1.21, <2.1.7)
#   HIGH     brace-expansion  GHSA-qhr7-859c-m2p7 (<1.1.20, <2.1.6)
#   HIGH     brace-expansion  GHSA-6j4f-fj2g-mc7p (<1.1.19, <2.1.5)
#   HIGH     js-yaml          GHSA-2883-xcg3-v3hh (<4.3.2)
#   MODERATE @humanfs/node    GHSA-p498-v437-472g (<0.16.8)
#   MODERATE qs               GHSA-x5fp-wj9c-mxmx (<=6.15.3)
#   MODERATE qs               GHSA-4mjr-xmp4-gh2g (<6.16.0)
#   MODERATE vitest /
#            @vitest/mocker   GHSA-82fw-gwwq-j7x9 (<4.1.11)
#
# Remove this step once upstream beads-ui ships a lockfile that already
# contains the fixed versions.
# -----------------------------------------------------------------------------

RUN set -eu; \
    echo "============================================================"; \
    echo " Updating vulnerable transitive dependencies"; \
    echo " (scoped, within declared semver ranges)"; \
    echo "============================================================"; \
    npm ls brace-expansion js-yaml qs @humanfs/node vitest \
        > /opt/beads-ui/security-reports/dep-versions-before.log 2>&1 || true; \
    npm update brace-expansion js-yaml qs @humanfs/node vitest \
        --ignore-scripts --no-audit --no-fund; \
    npm ls brace-expansion js-yaml qs @humanfs/node vitest \
        > /opt/beads-ui/security-reports/dep-versions-after.log 2>&1 || true; \
    echo ""; \
    echo "Version changes:"; \
    diff -u \
        /opt/beads-ui/security-reports/dep-versions-before.log \
        /opt/beads-ui/security-reports/dep-versions-after.log || true; \
    echo "============================================================"; \
    echo " Dependency repair complete"; \
    echo "============================================================"


# -----------------------------------------------------------------------------
# NPM vulnerability audit
#
# The complete JSON report is retained.
#
# npm audit returns non-zero when vulnerabilities exist at or above
# NPM_AUDIT_LEVEL.
# -----------------------------------------------------------------------------

RUN set +e; \
    echo "============================================================"; \
    echo " Running npm vulnerability audit"; \
    echo " Failure threshold: ${NPM_AUDIT_LEVEL}"; \
    echo "============================================================"; \
    npm audit \
        --audit-level="${NPM_AUDIT_LEVEL}" \
        --json \
        > /opt/beads-ui/security-reports/npm-audit.json; \
    AUDIT_EXIT=$?; \
    echo ""; \
    echo "NPM audit summary:"; \
    jq '.metadata.vulnerabilities // .' \
        /opt/beads-ui/security-reports/npm-audit.json \
        || cat /opt/beads-ui/security-reports/npm-audit.json; \
    echo ""; \
    if [ "${AUDIT_EXIT}" -ne 0 ]; then \
        echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"; \
        echo " SECURITY AUDIT FAILED"; \
        echo " npm audit exit code: ${AUDIT_EXIT}"; \
        echo " threshold: ${NPM_AUDIT_LEVEL}"; \
        echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"; \
        echo ""; \
        echo "Vulnerable packages:"; \
        jq -r \
            '.vulnerabilities // {} | to_entries[] | "\(.key): severity=\(.value.severity), direct=\(.value.isDirect), fixAvailable=\(.value.fixAvailable)"' \
            /opt/beads-ui/security-reports/npm-audit.json \
            || true; \
        echo ""; \
        echo "Docker image build STOPPED."; \
        exit "${AUDIT_EXIT}"; \
    fi; \
    echo "============================================================"; \
    echo " npm vulnerability audit PASSED"; \
    echo "============================================================"


# -----------------------------------------------------------------------------
# Verify npm package signatures / provenance
# -----------------------------------------------------------------------------

RUN set +e; \
    echo "============================================================"; \
    echo " Checking npm package signatures / provenance"; \
    echo "============================================================"; \
    npm audit signatures \
        > /opt/beads-ui/security-reports/npm-signatures.log 2>&1; \
    SIGNATURE_EXIT=$?; \
    cat /opt/beads-ui/security-reports/npm-signatures.log; \
    if [ "${SIGNATURE_EXIT}" -ne 0 ]; then \
        echo ""; \
        echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"; \
        echo " NPM SIGNATURE CHECK FAILED"; \
        echo " exit code: ${SIGNATURE_EXIT}"; \
        echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"; \
        echo ""; \
        echo "Docker image build STOPPED."; \
        exit "${SIGNATURE_EXIT}"; \
    fi; \
    echo "============================================================"; \
    echo " npm signature check PASSED"; \
    echo "============================================================"


# -----------------------------------------------------------------------------
# Report dependency lifecycle scripts
#
# Presence of a lifecycle script does NOT automatically fail the build.
#
# Legitimate packages such as esbuild and native modules commonly use them.
#
# We record them before allowing any to execute.
# -----------------------------------------------------------------------------

RUN set -eu; \
    echo "============================================================"; \
    echo " Packages containing lifecycle scripts"; \
    echo "============================================================"; \
    REPORT="/opt/beads-ui/security-reports/npm-lifecycle-scripts.log"; \
    : > "${REPORT}"; \
    find node_modules -name package.json -type f -print0 | \
        xargs -0 -r jq -r \
        'select(.scripts.preinstall or .scripts.install or .scripts.postinstall) | "\(.name // "unknown")@\(.version // "unknown") -> \(.scripts)"' \
        | tee "${REPORT}"; \
    echo "============================================================"; \
    echo " Lifecycle script report complete"; \
    echo "============================================================"


# -----------------------------------------------------------------------------
# Dependencies have passed security checks.
#
# Allow required package lifecycle scripts to execute.
# -----------------------------------------------------------------------------

RUN echo "============================================================" && \
    echo " Running approved dependency lifecycle scripts" && \
    echo "============================================================" && \
    npm rebuild


# -----------------------------------------------------------------------------
# Run upstream validation
#
# beads-ui currently provides an "all" script which performs its validation
# suite.
#
# Any failure stops the Docker build.
# -----------------------------------------------------------------------------

RUN echo "============================================================" && \
    echo " Running beads-ui validation suite" && \
    echo "============================================================" && \
    npm run all


# -----------------------------------------------------------------------------
# Build application
# -----------------------------------------------------------------------------

RUN echo "============================================================" && \
    echo " Building beads-ui" && \
    echo "============================================================" && \
    npm run build


# =============================================================================
# RUNTIME
# =============================================================================

FROM node:22-bookworm-slim AS runtime

ARG DEBIAN_FRONTEND=noninteractive

ARG USER_UID=1000
ARG USER_GID=1000


# -----------------------------------------------------------------------------
# Runtime dependencies
# -----------------------------------------------------------------------------

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        git \
        openssh-client \
    && rm -rf /var/lib/apt/lists/*


# -----------------------------------------------------------------------------
# Install Beads CLI
# -----------------------------------------------------------------------------

RUN echo "============================================================" && \
    echo " Installing Beads CLI" && \
    echo "============================================================" && \
    curl -fsSL \
        https://raw.githubusercontent.com/gastownhall/beads/main/scripts/install.sh \
        | bash


# -----------------------------------------------------------------------------
# Locate and verify bd.
#
# Current installer normally installs:
#
#   /usr/local/bin/bd
#
# Avoid attempting to symlink the file to itself.
# -----------------------------------------------------------------------------

RUN set -eu; \
    BD_PATH="$(command -v bd)"; \
    echo "bd installed at: ${BD_PATH}"; \
    if [ "${BD_PATH}" != "/usr/local/bin/bd" ]; then \
        ln -sf "${BD_PATH}" /usr/local/bin/bd; \
    fi; \
    echo "============================================================"; \
    echo " Beads version"; \
    echo "============================================================"; \
    /usr/local/bin/bd version


# -----------------------------------------------------------------------------
# Create unprivileged developer account.
#
# node:22-bookworm-slim already contains a "node" user with UID 1000.
# Therefore we cannot blindly useradd UID 1000.
#
# Reuse/rename an existing UID where necessary.
# -----------------------------------------------------------------------------

RUN set -eux; \
    EXISTING_USER="$(getent passwd "${USER_UID}" | cut -d: -f1 || true)"; \
    EXISTING_GROUP="$(getent group "${USER_GID}" | cut -d: -f1 || true)"; \
    \
    if [ -n "${EXISTING_USER}" ]; then \
        echo "UID ${USER_UID} already belongs to ${EXISTING_USER}"; \
        if [ "${EXISTING_USER}" != "developer" ]; then \
            usermod --login developer "${EXISTING_USER}"; \
            usermod --home /home/developer --move-home developer; \
        fi; \
    else \
        if [ -z "${EXISTING_GROUP}" ]; then \
            groupadd --gid "${USER_GID}" developer; \
            EXISTING_GROUP=developer; \
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
    mkdir -p \
        /workspace \
        /tmp/beads-ui \
        /home/developer; \
    chown -R "${USER_UID}:${USER_GID}" \
        /workspace \
        /tmp/beads-ui \
        /home/developer


# -----------------------------------------------------------------------------
# Copy application
# -----------------------------------------------------------------------------

WORKDIR /opt/beads-ui

COPY --from=builder \
    --chown=${USER_UID}:${USER_GID} \
    /opt/beads-ui \
    /opt/beads-ui


# -----------------------------------------------------------------------------
# Runtime environment
# -----------------------------------------------------------------------------

ENV HOME=/home/developer
ENV BD_BIN=/usr/local/bin/bd
ENV BDUI_RUNTIME_DIR=/tmp/beads-ui
ENV HOST=0.0.0.0
ENV PORT=3007

# -----------------------------------------------------------------------------
# Drop root privileges
# -----------------------------------------------------------------------------

USER developer

# -----------------------------------------------------------------------------
# Shared repository workspace
# -----------------------------------------------------------------------------

WORKDIR /workspace

# -----------------------------------------------------------------------------
# beads-ui
# -----------------------------------------------------------------------------

EXPOSE 3007

CMD ["node", "/opt/beads-ui/server/index.js", "--host", "0.0.0.0", "--port", "3007"]
