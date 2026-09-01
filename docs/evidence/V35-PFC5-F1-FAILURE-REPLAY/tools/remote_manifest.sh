#!/usr/bin/env bash
# Emit the remote file manifest (path / bytes / sha256) for the F1 deployment.
set -euo pipefail
R=/home/inspur/aicomp/zhangbo-v35-pfc5-f1-20260829
cd "$R"
while IFS= read -r -d '' f; do
  rel="${f#./}"
  size=$(stat -c '%s' "$f")
  hash=$(sha256sum "$f" | cut -d' ' -f1)
  printf '%s\t%s\t%s\n' "$rel" "$size" "$hash"
done < <(find . -type f -print0 | sort -z)
