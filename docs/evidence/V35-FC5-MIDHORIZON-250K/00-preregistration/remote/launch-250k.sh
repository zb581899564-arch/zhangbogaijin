#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "usage: launch-250k.sh REMOTE_ROOT" >&2
  exit 2
fi

ROOT="$1"
PREREG="$ROOT/00-preregistration"
MANIFEST="$PREREG/PREUPLOAD_SHA256.tsv"
RUNTIME_JAR="$PREREG/runtime/diagnostic-runtime-A0A1E74D.jar"
FORMAL_JAR="$PREREG/runtime/formal-algorithm-8DAD8F40.jar"
LAUNCHER_JAR="$PREREG/tools/V35MidHorizon250kExternalRunner.jar"
RUNNER="$PREREG/remote/run-one-250k.sh"
SESSION="v35-fc5-midhorizon-250k-20260827-r3"

EXPECTED_RUNTIME_SHA="A0A1E74D00403CAC69FBC25B52AEAEB454A6CC2D9FA6BF2A1F6A0D12FFE15FF7"
EXPECTED_FORMAL_SHA="8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9"
EXPECTED_LAUNCHER_SHA="0E13E6DAC59E7593C4B3B55720327CEFC0AF86EF070E3617D04ECBC3AE4A831E"

for required in sha256sum stat java tmux taskset; do
  command -v "$required" >/dev/null 2>&1 || { echo "missing remote command: $required" >&2; exit 5; }
done
[[ -f "$MANIFEST" ]] || { echo "missing preregistration manifest: $MANIFEST" >&2; exit 6; }

HEADER="$(head -n 1 "$MANIFEST")"
HEADER="${HEADER%$'\r'}"
[[ "$HEADER" == $'sha256\tbytes\tpath' ]] || {
  echo "manifest header is not tab-separated" >&2
  exit 7
}

REMOTE_MANIFEST="$ROOT/REMOTE_PACKAGE_SHA256.tsv"
printf 'sha256\tbytes\tpath\n' > "$REMOTE_MANIFEST"
while IFS=$'\t' read -r expected_bytes expected_size relative_path; do
  [[ -z "$expected_bytes" ]] && continue
  [[ "$expected_bytes" == "sha256" ]] && continue
  relative_path="${relative_path%$'\r'}"
  [[ -n "$expected_size" && -n "$relative_path" ]] || {
    echo "malformed manifest row" >&2
    exit 8
  }
  file="$PREREG/$relative_path"
  [[ -f "$file" ]] || { echo "missing manifest file: $relative_path" >&2; exit 9; }
  actual_size="$(stat -c '%s' -- "$file")"
  [[ "$actual_size" == "$expected_size" ]] || {
    echo "size mismatch: $relative_path expected=$expected_size actual=$actual_size" >&2
    exit 10
  }
  actual_sha="$(sha256sum -- "$file" | awk '{print toupper($1)}')"
  [[ "$actual_sha" == "$expected_bytes" ]] || {
    echo "sha256 mismatch: $relative_path expected=$expected_bytes actual=$actual_sha" >&2
    exit 11
  }
  printf '%s\t%s\t%s\n' "$actual_sha" "$actual_size" "$relative_path" >> "$REMOTE_MANIFEST"
done < "$MANIFEST"

actual_runtime_sha="$(sha256sum -- "$RUNTIME_JAR" | awk '{print toupper($1)}')"
actual_formal_sha="$(sha256sum -- "$FORMAL_JAR" | awk '{print toupper($1)}')"
actual_launcher_sha="$(sha256sum -- "$LAUNCHER_JAR" | awk '{print toupper($1)}')"
[[ "$actual_runtime_sha" == "$EXPECTED_RUNTIME_SHA" ]] || { echo "runtime Jar identity mismatch" >&2; exit 12; }
[[ "$actual_formal_sha" == "$EXPECTED_FORMAL_SHA" ]] || { echo "formal Jar identity mismatch" >&2; exit 13; }
[[ "$actual_launcher_sha" == "$EXPECTED_LAUNCHER_SHA" ]] || { echo "launcher identity mismatch" >&2; exit 14; }

[[ ! -e "$ROOT/output/250k" ]] || { echo "refusing to reuse existing output root" >&2; exit 15; }
if tmux has-session -t "$SESSION" 2>/dev/null; then
  echo "refusing to reuse existing tmux session: $SESSION" >&2
  exit 16
fi

mkdir -p "$ROOT/output/250k" "$ROOT/logs" "$ROOT/pids"
printf 'runId\tinstance\tseed\tarm\tcpuset\tlog\toutput\n' > "$ROOT/LAUNCH_TASKS.tsv"

tasks=(
  '100_2_4_1 20260901 A2 100_2_4_1-s20260901-A2 0-1'
  '100_2_4_1 20260901 A4 100_2_4_1-s20260901-A4 2-3'
  '100_2_4_1 20260902 A2 100_2_4_1-s20260902-A2 4-5'
  '100_2_4_1 20260902 A4 100_2_4_1-s20260902-A4 6-7'
  '100_2_4_1 20260903 A2 100_2_4_1-s20260903-A2 8-9'
  '100_2_4_1 20260903 A4 100_2_4_1-s20260903-A4 10-11'
  '100_5_3_1 20260901 A2 100_5_3_1-s20260901-A2 12-13'
  '100_5_3_1 20260901 A4 100_5_3_1-s20260901-A4 14-15'
  '100_5_3_1 20260902 A2 100_5_3_1-s20260902-A2 16-17'
  '100_5_3_1 20260902 A4 100_5_3_1-s20260902-A4 18-19'
  '100_5_3_1 20260903 A2 100_5_3_1-s20260903-A2 20-21'
  '100_5_3_1 20260903 A4 100_5_3_1-s20260903-A4 22-23'
)
[[ "${#tasks[@]}" -eq 12 ]] || { echo "internal task roster error" >&2; exit 17; }

first=1
for task in "${tasks[@]}"; do
  read -r instance seed arm run_id cpuset <<< "$task"
  output="$ROOT/output/250k/$instance/seed-$seed/$arm"
  log="$ROOT/logs/$run_id.log"
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    "$run_id" "$instance" "$seed" "$arm" "$cpuset" "$log" "$output" >> "$ROOT/LAUNCH_TASKS.tsv"
  command="bash '$RUNNER' '$ROOT' '$instance' '$seed' '$arm' '$run_id' '$cpuset' > '$log' 2>&1"
  if [[ "$first" -eq 1 ]]; then
    tmux new-session -d -s "$SESSION" -n "$run_id" "$command"
    first=0
  else
    tmux new-window -d -t "$SESSION" -n "$run_id" "$command"
  fi
done

tmux list-windows -t "$SESSION" -F 'index=#{window_index}\tname=#{window_name}\tactive=#{window_active}' > "$ROOT/LAUNCH_TMUX_WINDOWS.txt"

pid_file_count=0
alive_pid_count=0
runner_pid_count=0
for attempt in $(seq 1 15); do
  pid_file_count=0
  alive_pid_count=0
  runner_pid_count=0
  for task in "${tasks[@]}"; do
    read -r _ _ _ run_id _ <<< "$task"
    pid_file="$ROOT/pids/$run_id.pid"
    if [[ -s "$pid_file" ]]; then
      pid_file_count=$((pid_file_count + 1))
      pid="$(tr -d '[:space:]' < "$pid_file")"
      if [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" 2>/dev/null; then
        alive_pid_count=$((alive_pid_count + 1))
        cmdline="$(tr -d '\0' < "/proc/$pid/cmdline" 2>/dev/null || true)"
        if [[ "$cmdline" == *'v35campaign.V35MidHorizon250kExternalRunner'* ]]; then
          runner_pid_count=$((runner_pid_count + 1))
        fi
      fi
    fi
  done
  if [[ "$pid_file_count" -eq 12 && "$alive_pid_count" -eq 12 && "$runner_pid_count" -eq 12 ]]; then
    break
  fi
  sleep 1
done

tmux list-windows -t "$SESSION" -F 'index=#{window_index}\tname=#{window_name}\tactive=#{window_active}' > "$ROOT/LAUNCH_TMUX_WINDOWS.txt"
pgrep -af 'v35campaign.V35MidHorizon250kExternalRunner' > "$ROOT/LAUNCH_PROCESS_SNAPSHOT.txt" || true

tmux_window_count="$(wc -l < "$ROOT/LAUNCH_TMUX_WINDOWS.txt" | tr -d '[:space:]')"
launch_verified=false
if [[ "$pid_file_count" -eq 12 && "$alive_pid_count" -eq 12 && "$runner_pid_count" -eq 12 && "$tmux_window_count" -eq 12 ]]; then
  launch_verified=true
fi

{
  printf 'campaign=V35-FC5-MIDHORIZON-250K\n'
  printf 'launchedAtUtc=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  printf 'formalAlgorithmJarSha256=%s\n' "$actual_formal_sha"
  printf 'diagnosticRuntimeJarSha256=%s\n' "$actual_runtime_sha"
  printf 'diagnosticLauncherJarSha256=%s\n' "$actual_launcher_sha"
  printf 'PDDR=GLOBAL_ORIGINAL\nmixture=20/40/20/20\nShiftMode=NONE\ndecodeMode=FM3\n'
  printf 'telemetry=ON\ntermination=phase-consistent\nmaxFEs=250000\n'
  printf 'requestedRuns=12\nstartedRuns=%s\n\n' "$pid_file_count"
  printf 'diagnosticToolingValidated=true\n250kReadyForPreregistration=true\n250kApproved=%s\n250kStarted=%s\n' "$launch_verified" "$launch_verified"
  printf 'formalMatrixRunning=false\nFC5=INCONCLUSIVE\n'
  printf 'rawRunOutputsUseUnifiedCataFields=true\npostRunNormalizationRequired=false\n'
  printf 'launchVerified=%s\ntmuxSession=%s\ntmuxWindowCount=%s\npidFileCount=%s\nalivePidCount=%s\nrunnerPidCount=%s\n' \
    "$launch_verified" "$SESSION" "$tmux_window_count" "$pid_file_count" "$alive_pid_count" "$runner_pid_count"
  printf 'resultReadingStarted=false\n'
} > "$ROOT/LAUNCH_STATUS.properties"

printf 'runId\tpid\talive\trunnerCommandMatched\n' > "$ROOT/LAUNCH_PID_STATUS.tsv"
for task in "${tasks[@]}"; do
  read -r _ _ _ run_id _ <<< "$task"
  pid_file="$ROOT/pids/$run_id.pid"
  pid=''
  alive=false
  command_match=false
  if [[ -s "$pid_file" ]]; then
    pid="$(tr -d '[:space:]' < "$pid_file")"
    if [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" 2>/dev/null; then
      alive=true
      cmdline="$(tr -d '\0' < "/proc/$pid/cmdline" 2>/dev/null || true)"
      if [[ "$cmdline" == *'v35campaign.V35MidHorizon250kExternalRunner'* ]]; then command_match=true; fi
    fi
  fi
  printf '%s\t%s\t%s\t%s\n' "$run_id" "$pid" "$alive" "$command_match" >> "$ROOT/LAUNCH_PID_STATUS.tsv"
done

cat "$ROOT/LAUNCH_STATUS.properties"
[[ "$launch_verified" == true ]] || exit 18
