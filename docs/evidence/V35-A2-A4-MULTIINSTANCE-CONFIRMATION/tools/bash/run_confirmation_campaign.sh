#!/usr/bin/env bash
# Fail closed: only schedule the remaining 58 confirmation runs after the first A2/A4 pair passes.
set -euo pipefail

if [[ "$#" != 2 ]]; then
  echo "usage: $0 <confirmation-root> <stage2-root>" >&2
  exit 64
fi

ROOT="$(realpath "$1")"
STAGE2="$(realpath "$2")"
PREPARE="$ROOT/tools/bash/prepare_confirmation.sh"
MASTER="$ROOT/tools/python/confirmation_master.py"

if [[ ! -f "$ROOT/status/preparation.properties" ]]; then
  "$PREPARE" "$ROOT" "$STAGE2"
else
  grep -qx 'status=PREPARED' "$ROOT/status/preparation.properties"
  [[ -f "$ROOT/manifests/first.json" && -f "$ROOT/manifests/remainder.json" ]] || {
    echo "incomplete prepared state" >&2; exit 65;
  }
fi
python3 "$MASTER" --manifest "$ROOT/manifests/first.json" \
  --progress "$ROOT/status/first-progress.csv" --groups-per-wave 1
touch "$ROOT/status/first-pair-accepted.marker"
python3 "$MASTER" --manifest "$ROOT/manifests/remainder.json" \
  --progress "$ROOT/status/remainder-progress.csv" --groups-per-wave 8
touch "$ROOT/status/campaign-completed.marker"
echo "CONFIRMATION_CAMPAIGN_COMPLETED runs=60"
