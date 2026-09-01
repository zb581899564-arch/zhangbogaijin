#!/usr/bin/env python3
"""Verify that the new Q0 instrumentation preserves existing D1/D2 behavior.

The 2k preflight intentionally reaches only the frozen initial population under
phase-consistent termination.  It still proves that new configuration plumbing
does not alter input handling, evaluation traces, fronts or Q-table identity.
Wall-clock timing is explicitly excluded from this behavior contract.
"""
from __future__ import annotations

import csv
import hashlib
from pathlib import Path


ROOT = Path(__file__).resolve().parent
OLD = ROOT.parent / "V35-A2-A3-DECOMPOSITION" / "03-preflight"
NEW = ROOT / "02-compatibility-preflight"
OUT = NEW
ARMS = ("D1_PA_DIRECTIONAL", "D2_QP_SYNCHRONOUS")
STATUS_KEYS = (
    "status", "mode", "fullEvaluations", "decoderCalls", "illegalSolutions",
    "duplicateEvaluations", "initialPopulationHash", "evaluationTraceHash", "stopReason",
)
FILES = ("front.csv", "a2a3-personal-leader-events.csv", "qp-events.log",
         "dual-q-events.log", "profile.sha256")


def sha(path: Path) -> str:
    value = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            value.update(block)
    return value.hexdigest()


def props(path: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if "=" in line:
            key, value = line.split("=", 1)
            result[key] = value
    return result


def main() -> None:
    rows = []
    all_pass = True
    for arm in ARMS:
        old = OLD / ("seed-20260822-" + arm)
        new = NEW / ("seed-20260822-" + arm)
        old_status, new_status = props(old / "status.properties"), props(new / "status.properties")
        for key in STATUS_KEYS:
            equal = old_status.get(key) == new_status.get(key)
            rows.append({"arm": arm, "kind": "status", "field": key,
                         "old": old_status.get(key, ""), "new": new_status.get(key, ""),
                         "equal": str(equal).lower()})
            all_pass = all_pass and equal
        for filename in FILES:
            old_hash, new_hash = sha(old / filename), sha(new / filename)
            equal = old_hash == new_hash
            rows.append({"arm": arm, "kind": "file_sha256", "field": filename,
                         "old": old_hash, "new": new_hash, "equal": str(equal).lower()})
            all_pass = all_pass and equal
        old_qp, new_qp = props(old / "qp-summary.properties"), props(new / "qp-summary.properties")
        for key in ("tableHash", "KEEP.count", "DIRECTIONAL.count", "EPSILON.count", "COMPLEMENTARY.count"):
            equal = old_qp.get(key) == new_qp.get(key)
            rows.append({"arm": arm, "kind": "qp_summary", "field": key,
                         "old": old_qp.get(key, ""), "new": new_qp.get(key, ""),
                         "equal": str(equal).lower()})
            all_pass = all_pass and equal

    with (OUT / "compatibility-checks.csv").open("w", newline="", encoding="utf-8") as stream:
        writer = csv.DictWriter(stream, fieldnames=("arm", "kind", "field", "old", "new", "equal"))
        writer.writeheader(); writer.writerows(rows)
    (OUT / "COMPATIBILITY_REPORT.md").write_text(
        "# D1/D2 2k 行为兼容预检\n\n"
        "结果：**{}**。\n\n".format("通过" if all_pass else "失败")
        + "比较的是冻结D1/D2预检与新增Q0遥测代码下的重放。比较范围为输入哈希、FE、评价轨迹、"
        "最终前沿、个人领导/Qp/双Q事件文件、profile哈希和Qp表/动作计数。纳秒计时不是决策或"
        "行为哈希，因此不纳入逐字节相等门。\n\n"
        + "2k按当前phase-consistent预算语义在初始100 FE后停止；这不是50k正式诊断的预算契约。"
        "逐字段明细见`compatibility-checks.csv`。\n",
        encoding="utf-8")
    if not all_pass:
        raise SystemExit("compatibility contract failed")
    print("COMPATIBILITY_PASSED")


if __name__ == "__main__":
    main()
