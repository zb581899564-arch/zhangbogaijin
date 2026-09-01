#!/usr/bin/env bash
# V35-PFC5-F1: package the raw run output on the训练机 for download.
# Remote originals are NEVER deleted by this script (user requirement, section 十二).
set -euo pipefail
R=/home/inspur/aicomp/zhangbo-v35-pfc5-f1-20260829
RUN_ID=V35PFC5F1-100_5_3_1-20260901-A4
OUT="$R/output/A4"

[ -d "$OUT" ] || { echo "REFUSING: no output dir $OUT"; exit 71; }

mkdir -p "$R/artifacts"
TAR="$R/artifacts/$RUN_ID-raw.tar.gz"
rm -f "$TAR"
tar czf "$TAR" -C "$OUT" .

echo "tarPath=$TAR"
echo "tarBytes=$(stat -c '%s' "$TAR")"
echo "tarSha256=$(sha256sum "$TAR" | cut -d' ' -f1)"
echo "--- remote output listing (provenance preserved, not deleted) ---"
ls -la "$OUT"
echo "--- partial residue check ---"
find "$R" -maxdepth 3 -name ".partial-*" -print | wc -l
echo "--- runner still active? (must be 0 after completion) ---"
pgrep -af "ZhangBoV35FormalAblationArmRunner" | grep -v pgrep | wc -l
