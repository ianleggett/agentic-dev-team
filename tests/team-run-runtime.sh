#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_BIN="$ROOT/devbox/bin/_team-run"
STORE="$(mktemp -d)"
trap 'rm -rf "$STORE"' EXIT

run() { TEAM_RUNS_DIR="$STORE" bash "$RUN_BIN" "$@"; }
fail() { printf 'FAIL: %s\n' "$*" >&2; exit 1; }
assert_eq() { [[ "$1" == "$2" ]] || fail "expected [$2], got [$1]"; }

run create run-test frontend bd-test demo worker-test >/dev/null
run event run-test opencode 'authorization: Bearer super-secret' >/dev/null
run heartbeat run-test >/dev/null

EVENT="$(<"$STORE/events/run-test.jsonl")"
[[ "$EVENT" == *'[REDACTED]'* ]] || fail 'event secret was not redacted'
[[ "$EVENT" != *'super-secret'* ]] || fail 'event secret leaked'

STATE="$(run list | jq -r '.[0].state')"
assert_eq "$STATE" running
COUNT="$(run list | jq -r '.[0].event_count')"
assert_eq "$COUNT" 1

run finish run-test succeeded 0 >/dev/null
STATE="$(run list | jq -r '.[0].state')"
assert_eq "$STATE" succeeded
[[ -z "$(run stale)" ]] || fail 'finished run was reported stale'

run create stale-test frontend bd-stale demo worker-test >/dev/null
TEAM_LEASE_SECONDS=-1 TEAM_RUNS_DIR="$STORE" bash "$RUN_BIN" heartbeat stale-test >/dev/null
[[ -n "$(run stale)" ]] || fail 'expired run was not reported stale'

printf 'team-run-runtime: ok\n'
