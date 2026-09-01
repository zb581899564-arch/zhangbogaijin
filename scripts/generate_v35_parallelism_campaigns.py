#!/usr/bin/env python3
"""Generate the four diagnostic-only V35 parallelism manifests.

No algorithm configuration is generated here.  The immutable values name the
already deployed DOE-1 preflight entry point and are copied into each RunKey.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

from v35_campaign_runner import canonical_json, sha256_text


FREEZE_HASH = "d85a07381da31dffb403a5fa08caaa3093f3d4d4ff2d10fb81c4a85b3081c34f"
DIAGNOSTIC_CONFIG_HASH = "ea1eb9fb09529b325aa122790fd67ce733c80f451f099d6a0c18a732af7fb92d"
REMOTE_ROOT = "/home/inspur/aicomp/zhangbo-v35-doe1-heldout-20260822"


def manifest_for(level: int, host: str, revision: str) -> dict:
    runs = []
    for lane in range(1, level + 1):
        runs.append({
            "algorithm": "V35_DOE1_PREFLIGHT_A4_POOL_OFF",
            "configHash": DIAGNOSTIC_CONFIG_HASH,
            "instance": "20_5_4_1",
            "seed": 20260901,
            "budget": 2000,
            "safetyClass": "short_benchmark",
            "benchmarkId": "V35-FINAL-PARALLELISM-20260822",
            "isolationId": "parallelism-{}-L{}-lane-{:02d}".format(revision, level, lane),
            "maxAttempts": 2,
            "command": [
                "bash", "scripts/v35_parallelism_benchmark_ssh.sh",
                "--host", host,
                "--remote-root", REMOTE_ROOT,
                "--run-key", "{run_key}",
                "--level", str(level),
                "--lane", str(lane),
                "--cpu", str(lane - 1),
            ],
        })
    return {
        "schemaVersion": 1,
        "campaignId": "V35-FINAL-PARALLELISM-20260822-{}-L{}".format(revision, level),
        "maxParallel": level,
        "maxDiagnosticBudget": 2000,
        "frozenBoundaryHash": FREEZE_HASH,
        "freezeEvidence": "docs/evidence/V35-FINAL-FREEZE/FREEZE_MANIFEST.json",
        "runs": runs,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path,
                        default=Path("docs/evidence/V35-FINAL-FREEZE/campaigns"))
    parser.add_argument("--host", default="aic-inspur-home")
    parser.add_argument("--revision", default="r1", help="new evidence revision after an infrastructure failure")
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    for level in (4, 8, 12, 16):
        manifest = manifest_for(level, args.host, args.revision)
        path = args.output / "parallelism-level-{}-{}.json".format(level, args.revision)
        path.write_text(json.dumps(manifest, ensure_ascii=False, sort_keys=True, indent=2) + "\n", encoding="utf-8")
        print("{}\t{}".format(sha256_text(canonical_json(manifest)), path.as_posix()))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
