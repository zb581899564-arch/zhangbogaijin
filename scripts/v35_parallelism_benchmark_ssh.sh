#!/usr/bin/env bash
# Local wrapper used by v35_campaign_runner.py.  It transports only the
# diagnostic launcher over stdin; it does not copy or mutate algorithm source.
set -euo pipefail

HOST=""
REMOTE_ROOT=""
RUN_KEY=""
LEVEL=""
LANE=""
CPU=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --host) HOST="$2"; shift 2;;
    --remote-root) REMOTE_ROOT="$2"; shift 2;;
    --run-key) RUN_KEY="$2"; shift 2;;
    --level) LEVEL="$2"; shift 2;;
    --lane) LANE="$2"; shift 2;;
    --cpu) CPU="$2"; shift 2;;
    *) echo "unknown argument: $1" >&2; exit 64;;
  esac
done
[[ -n "$HOST" && -n "$REMOTE_ROOT" && -n "$RUN_KEY" && -n "$LEVEL" && -n "$LANE" && -n "$CPU" ]] || {
  echo "host, remote-root, run-key, level, lane, and cpu are required" >&2; exit 64;
}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# The existing SSH path accepts remote commands but rejects a streamed stdin
# program.  Pass the reviewed launcher as a quoted `bash -lc` argument rather
# than opening a second control connection or copying a file to the host.
REMOTE_PROGRAM="$(< "$SCRIPT_DIR/v35_parallelism_benchmark_remote.sh")"
quote() { printf '%q' "$1"; }
REMOTE_COMMAND="bash -lc $(quote "$REMOTE_PROGRAM") -- $(quote "$REMOTE_ROOT") $(quote "$RUN_KEY") $(quote "$LEVEL") $(quote "$LANE") $(quote "$CPU")"
if [[ -n "${V35_SSH_BIN:-}" ]]; then
  SSH_BIN="$V35_SSH_BIN"
elif [[ -x /mnt/c/Windows/System32/OpenSSH/ssh.exe ]]; then
  # The workspace runs Bash through WSL but the accepted host alias and its
  # Tailscale route are owned by the Windows OpenSSH configuration.
  SSH_BIN=/mnt/c/Windows/System32/OpenSSH/ssh.exe
else
  SSH_BIN=ssh
fi
set +e
"$SSH_BIN" -o BatchMode=yes -o ConnectTimeout=15 "$HOST" "$REMOTE_COMMAND"
REMOTE_EXIT=$?
set -e
if [[ "$REMOTE_EXIT" -eq 0 ]]; then
  exit 0
fi

# This host can close concurrent SSH sessions after the remote child has
# already finished.  Treat that as a transport ambiguity, never as a blind
# success: a fresh, serial read must prove the exact RunKey's durable result.
VERIFY_PATH="$REMOTE_ROOT/parallelism-benchmark-v35-final-freeze-20260822/runs/$RUN_KEY/benchmark.properties"
for _ in 1 2 3; do
  set +e
  VERIFIED="$("$SSH_BIN" -o BatchMode=yes -o ConnectTimeout=15 "$HOST" \
    "test -f $(quote "$VERIFY_PATH") && awk -F= '\$1==\"runKey\" || \$1==\"exitCode\" || \$1==\"runnerStatus\" || \$1==\"fullEvaluations\" {print}' $(quote "$VERIFY_PATH")" 2>/dev/null)"
  VERIFY_EXIT=$?
  set -e
  if [[ "$VERIFY_EXIT" -eq 0 ]] \
      && grep -Fxq "runKey=$RUN_KEY" <<<"$VERIFIED" \
      && grep -Fxq 'exitCode=0' <<<"$VERIFIED" \
      && grep -Fxq 'runnerStatus=COMPLETED' <<<"$VERIFIED" \
      && grep -Fxq 'fullEvaluations=100' <<<"$VERIFIED"; then
    echo "V35_BENCHMARK_TRANSPORT_RECOVERED runKey=$RUN_KEY originalSshExit=$REMOTE_EXIT" >&2
    exit 0
  fi
  sleep 1
done
exit "$REMOTE_EXIT"
