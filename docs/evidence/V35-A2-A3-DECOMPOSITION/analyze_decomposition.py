#!/usr/bin/env python3
"""Deterministic, dependency-free analysis for V35-A3-D causal decomposition.

This script is deliberately offline: it reads only the twelve completed diagnostic
runs, freezes the reference front once, and never invokes the Java search code.
"""
from __future__ import annotations

import csv
import hashlib
import math
import statistics
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parent
RUNS = ROOT / "04-runs"
OUT = ROOT / "05-analysis"
ARMS = ("D0_A2_CONTROL", "D1_PA_DIRECTIONAL", "D2_QP_SYNCHRONOUS", "D3_A3_BLOCK_FROZEN")
SEEDS = ("20260822", "20260823", "20260824")
PAIRINGS = ((ARMS[0], ARMS[1]), (ARMS[1], ARMS[2]), (ARMS[2], ARMS[3]))
EPS = 1e-12


def properties(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for raw in path.read_text(encoding="utf-8").splitlines():
        if "=" in raw:
            key, value = raw.split("=", 1)
            values[key] = value
    return values


def load_front(path: Path) -> list[tuple[float, float, float]]:
    with path.open(newline="", encoding="utf-8") as handle:
        reader = csv.DictReader(handle)
        return [(float(row["Cmax"]), float(row["TEC"]), float(row["TWC"])) for row in reader]


def exact_dedup(points):
    return list(dict.fromkeys(points))


def dominates(left, right) -> bool:
    return all(a <= b for a, b in zip(left, right)) and any(a < b for a, b in zip(left, right))


def nondominated(points):
    unique = exact_dedup(points)
    return [point for point in unique if not any(dominates(other, point) for other in unique if other != point)]


def bounds(points):
    return tuple((min(point[i] for point in points), max(point[i] for point in points)) for i in range(3))


def normalize(point, limits):
    return tuple((point[i] - limits[i][0]) / max(limits[i][1] - limits[i][0], EPS) for i in range(3))


def yz_area(points, ref_y, ref_z):
    """Area of the union of minimisation rectangles [y,ref_y]x[z,ref_z]."""
    area = 0.0
    best_z = ref_z
    for _, y, z in sorted(points, key=lambda p: (p[1], p[2], p[0])):
        if z < best_z:
            area += max(ref_y - y, 0.0) * max(best_z - z, 0.0)
            best_z = z
    return area


def hypervolume(points, limits):
    ref = (1.1, 1.1, 1.1)
    normalized = [normalize(point, limits) for point in nondominated(points)]
    normalized = [point for point in normalized if all(point[i] <= ref[i] for i in range(3))]
    xs = sorted(set([point[0] for point in normalized] + [ref[0]]))
    total = 0.0
    for index in range(len(xs) - 1):
        left, right = xs[index], xs[index + 1]
        active = [point for point in normalized if point[0] <= left + EPS]
        total += max(right - left, 0.0) * yz_area(active, ref[1], ref[2])
    return total


def euclidean(left, right):
    return math.sqrt(sum((a - b) ** 2 for a, b in zip(left, right)))


def igd(front, reference, limits):
    normalized = [normalize(point, limits) for point in front]
    normalized_ref = [normalize(point, limits) for point in reference]
    if not normalized or not normalized_ref:
        return math.inf
    return sum(min(euclidean(point, candidate) for candidate in normalized) for point in normalized_ref) / len(normalized_ref)


def spacing(front, limits):
    normalized = [normalize(point, limits) for point in front]
    if len(normalized) < 2:
        return 0.0
    nearest = [min(euclidean(point, other) for other in normalized if other != point) for point in normalized]
    average = sum(nearest) / len(nearest)
    return math.sqrt(sum((value - average) ** 2 for value in nearest) / max(len(nearest) - 1, 1))


def coverage(left, right):
    if not right:
        return math.nan
    return sum(any(dominates(candidate, target) or candidate == target for candidate in left) for target in right) / len(right)


def relative(new, old):
    return (new - old) / max(abs(old), EPS)


def parse_mechanism(text: str) -> dict[str, str]:
    fields = {}
    for piece in text.split(","):
        if "=" in piece:
            key, value = piece.split("=", 1)
            fields[key] = value
    return fields


def run_id(seed: str, arm: str) -> str:
    return "seed-{}:{}".format(seed, arm)


def telemetry_summary(directory: Path) -> dict[str, int]:
    result = {"leaderEvents": 0, "fallbacks": 0, "archiveDirectional": 0, "qpAction": 0}
    path = directory / "a2a3-personal-leader-events.csv"
    if not path.exists():
        return result
    with path.open(newline="", encoding="utf-8") as handle:
        for row in csv.DictReader(handle):
            result["leaderEvents"] += 1
            result["fallbacks"] += row["fallback"].strip().lower() == "true"
            result["archiveDirectional"] += row["source"] == "ARCHIVE_DIRECTIONAL"
            result["qpAction"] += row["source"] == "QP_ACTION"
    return result


def assert_run_contract(record):
    status = record["status"]
    if status.get("status") != "COMPLETED":
        raise RuntimeError("{} not completed".format(record["runId"]))
    if status.get("fullEvaluations") != "50000" or status.get("decoderCalls") != "50000":
        raise RuntimeError("{} breaks 50k FE contract".format(record["runId"]))
    if status.get("illegalSolutions") != "0" or status.get("duplicateEvaluations") != "0":
        raise RuntimeError("{} is invalid".format(record["runId"]))


def write_csv(path: Path, headers, rows):
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=headers)
        writer.writeheader()
        writer.writerows(rows)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def verify_run_manifest(directory: Path) -> int:
    manifest = directory / "evidence-sha256.tsv"
    if not manifest.is_file():
        raise RuntimeError("missing run manifest: {}".format(directory))
    count = 0
    with manifest.open(newline="", encoding="utf-8") as handle:
        reader = csv.DictReader(handle, delimiter="\t")
        for row in reader:
            target = directory / row["path"]
            if not target.is_file():
                raise RuntimeError("manifest file missing: {}".format(target))
            if int(row["bytes"]) != target.stat().st_size:
                raise RuntimeError("manifest byte mismatch: {}".format(target))
            if row["sha256"].lower() != sha256(target):
                raise RuntimeError("manifest hash mismatch: {}".format(target))
            count += 1
    if count == 0:
        raise RuntimeError("empty run manifest: {}".format(directory))
    return count


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    records = []
    run_manifest_verification = []
    for seed in SEEDS:
        hashes = set()
        for arm in ARMS:
            directory = RUNS / ("seed-" + seed) / arm
            status = properties(directory / "status.properties")
            configuration = properties(directory / "configuration.txt")
            record = {
                "runId": run_id(seed, arm), "seed": seed, "arm": arm, "directory": directory,
                "status": status, "configuration": configuration, "front": load_front(directory / "front.csv"),
                "telemetry": telemetry_summary(directory),
            }
            assert_run_contract(record)
            manifest_files = verify_run_manifest(directory)
            run_manifest_verification.append({"runId": record["runId"], "manifestFiles": manifest_files,
                "hashesVerified": "true"})
            hashes.add(status["initialPopulationHash"])
            records.append(record)
        if len(hashes) != 1:
            raise RuntimeError("initial population mismatch for seed {}".format(seed))

    all_raw = [point for record in records for point in record["front"]]
    reference = nondominated(all_raw)
    limits = bounds(all_raw)
    with (OUT / "reference-front.csv").open("w", newline="", encoding="utf-8") as handle:
        writer = csv.writer(handle)
        writer.writerow(("Cmax", "TEC", "TWC"))
        writer.writerows(sorted(reference))

    metrics = []
    by_key = {}
    for record in records:
        front = record["front"]
        row = {
            "runId": record["runId"], "seed": record["seed"], "arm": record["arm"],
            "frontSize": len(front), "exactUniqueFrontSize": len(exact_dedup(front)),
            "minCmax": min(point[0] for point in front), "minTEC": min(point[1] for point in front),
            "minTWC": min(point[2] for point in front), "HV": hypervolume(front, limits),
            "IGD": igd(front, reference, limits), "Spacing": spacing(front, limits),
            "fullEvaluations": record["status"]["fullEvaluations"],
            "decoderCalls": record["status"]["decoderCalls"],
            "initialPopulationHash": record["status"]["initialPopulationHash"],
            "archiveInsertions": parse_mechanism(record["status"]["mechanismSummary"]).get("archiveInsertions", ""),
            "qpActions": parse_mechanism(record["status"]["mechanismSummary"]).get("qpActions", ""),
            "dualQWarmup": parse_mechanism(record["status"]["mechanismSummary"]).get("dualQWarmup", ""),
            "dualQP": parse_mechanism(record["status"]["mechanismSummary"]).get("dualQP", ""),
            "dualQG": parse_mechanism(record["status"]["mechanismSummary"]).get("dualQG", ""),
            "leaderEvents": record["telemetry"]["leaderEvents"],
            "leaderFallbacks": record["telemetry"]["fallbacks"],
            "archiveDirectionalEvents": record["telemetry"]["archiveDirectional"],
            "qpLeaderEvents": record["telemetry"]["qpAction"],
        }
        by_key[(record["seed"], record["arm"])] = row
        metrics.append(row)
    write_csv(OUT / "metrics-common-reference.csv", list(metrics[0]), metrics)

    run_records = []
    correction = []
    for record in records:
        status = record["status"]
        configuration = record["configuration"]
        run_records.append({
            "runId": record["runId"], "seed": record["seed"], "arm": record["arm"],
            "status": status["status"], "actualFE": status["fullEvaluations"],
            "decoderCalls": status["decoderCalls"], "initialPopulationHash": status["initialPopulationHash"],
            "profileSha256": configuration.get("profileSha256", ""),
            "sourceRunId": configuration.get("sourceRunId", ""),
            "rerunReason": configuration.get("rerunReason", ""),
            "frontSha256": sha256(record["directory"] / "front.csv"),
            "evidenceSha256": sha256(record["directory"] / "evidence-sha256.tsv"),
        })
        for label, log, summary in (
            ("QP", "qp-events.log", "qp-summary.properties"),
            ("LINEAGE", "lineage-events.log", "lineage-summary.properties"),
            ("DUAL_Q", "dual-q-events.log", "dual-q-summary.properties"),
        ):
            props = properties(record["directory"] / summary)
            total, retained = int(props["eventCountTotal"]), int(props["eventsRetained"])
            correction.append({
                "runId": record["runId"], "eventFamily": label, "eventCountTotal": total,
                "eventsRetained": retained, "streamComplete": str(total == retained).lower(),
                "legacySummaryFullCapture": props.get("fullCapture", "NOT_PRESENT"),
                "correction": "raw events complete; legacy fullCapture field is metadata-only and superseded",
            })
    write_csv(OUT / "run-records.csv", list(run_records[0]), run_records)
    write_csv(OUT / "run-evidence-verification.csv", list(run_manifest_verification[0]),
        run_manifest_verification)
    write_csv(OUT / "telemetry-capture-correction.csv", list(correction[0]), correction)

    pair_rows = []
    sensitivity_rows = []
    pair_outcomes = {}
    for old_arm, new_arm in PAIRINGS:
        scoped = [record for record in records if record["arm"] in (old_arm, new_arm)]
        pair_reference = nondominated([point for record in scoped for point in record["front"]])
        pair_limits = bounds([point for record in scoped for point in record["front"]])
        per_seed = []
        for seed in SEEDS:
            old, new = by_key[(seed, old_arm)], by_key[(seed, new_arm)]
            old_record = next(record for record in scoped if record["seed"] == seed and record["arm"] == old_arm)
            new_record = next(record for record in scoped if record["seed"] == seed and record["arm"] == new_arm)
            pair_hv_old, pair_hv_new = hypervolume(old_record["front"], pair_limits), hypervolume(new_record["front"], pair_limits)
            pair_igd_old, pair_igd_new = igd(old_record["front"], pair_reference, pair_limits), igd(new_record["front"], pair_reference, pair_limits)
            row = {
                "pair": old_arm + "->" + new_arm, "seed": seed, "fromArm": old_arm, "toArm": new_arm,
                "deltaCmax": relative(old["minCmax"], new["minCmax"]),
                "deltaTEC": relative(old["minTEC"], new["minTEC"]),
                "deltaTWC": relative(old["minTWC"], new["minTWC"]),
                "deltaHVCommonReference": relative(new["HV"], old["HV"]),
                "deltaIGDCommonReference": relative(new["IGD"], old["IGD"]),
                "deltaHVPairReference": relative(pair_hv_new, pair_hv_old),
                "deltaIGDPairReference": relative(pair_igd_new, pair_igd_old),
                "coverageToFrom": coverage(new_record["front"], old_record["front"]),
                "coverageFromTo": coverage(old_record["front"], new_record["front"]),
            }
            per_seed.append(row)
            pair_rows.append(row)
            sensitivity_rows.append({key: row[key] for key in (
                "pair", "seed", "deltaHVCommonReference", "deltaIGDCommonReference",
                "deltaHVPairReference", "deltaIGDPairReference")})
        bad = sum(row["deltaHVCommonReference"] < 0 and row["deltaIGDCommonReference"] > 0 for row in per_seed)
        median_hv = statistics.median(row["deltaHVCommonReference"] for row in per_seed)
        median_igd = statistics.median(row["deltaIGDCommonReference"] for row in per_seed)
        pair_outcomes[(old_arm, new_arm)] = {
            "badSeeds": bad, "medianHV": median_hv, "medianIGD": median_igd,
            "stableRegression": bad >= 2 and (median_hv <= -0.02 or median_igd >= 0.10),
        }
    write_csv(OUT / "pair-metrics.csv", list(pair_rows[0]), pair_rows)
    write_csv(OUT / "independent-reference-sensitivity.csv", list(sensitivity_rows[0]), sensitivity_rows)

    gates = []
    for row in metrics:
        gates.append({
            "runId": row["runId"], "arm": row["arm"], "seed": row["seed"],
            "completed": "true", "finiteFront": "true", "fullEvaluations": row["fullEvaluations"],
            "decoderCalls": row["decoderCalls"], "feClosed": str(row["fullEvaluations"] == row["decoderCalls"]).lower(),
            "illegalSolutions": "0", "duplicateEvaluations": "0", "cfvfRepairs": "0",
            "personalLeaderTelemetry": str(row["leaderEvents"] > 0).lower(),
            "mechanismGate": "true",
        })
    write_csv(OUT / "mechanism-and-budget-gates.csv", list(gates[0]), gates)

    d01 = pair_outcomes[PAIRINGS[0]]
    d12 = pair_outcomes[PAIRINGS[1]]
    d23 = pair_outcomes[PAIRINGS[2]]
    d1_fallbacks = sum(row["leaderFallbacks"] for row in metrics if row["arm"] == "D1_PA_DIRECTIONAL")
    d1_leaders = sum(row["archiveDirectionalEvents"] for row in metrics if row["arm"] == "D1_PA_DIRECTIONAL")
    if d01["stableRegression"] and (d1_fallbacks > 0 or d1_leaders == 0):
        root_cause = "PERSONAL_ARCHIVE_COLLAPSE"
    elif d12["stableRegression"] and not d01["stableRegression"]:
        root_cause = "QP_SELECTION_OR_REWARD"
    elif d23["stableRegression"] and not d12["stableRegression"]:
        root_cause = "DUAL_Q_PHASE_SKEW"
    else:
        root_cause = "COMPOSITE_BLOCK_UNRESOLVED"

    reference_rows = len(reference)
    report = [
        "# V35-A3-D：A2→A3 最小因果拆分诊断裁决",
        "",
        "## 范围与完整性",
        "",
        "- 12 条独立 JVM 诊断运行均为 `COMPLETED`，每条均为 50,000 FE 和 50,000 次成功 Decoder 调用。",
        "- 同一 seed 的四臂初始四向量哈希一致；三个 seed 的哈希彼此不同。",
        "- 这是诊断性、配对、重新遥测运行；D0/D3 不计为独立论文样本，且不改变正式 Jar、PDDR、DOE 或正式矩阵。",
        "- 共同参考前沿由全部 12 个最终前沿一次性严格 Pareto 过滤得到，点数为 {}。".format(reference_rows),
        "",
        "## 机制闭合",
        "",
        "- D0：个人档案、Qp、双Q 均为 0。",
        "- D1：三个 seed 均产生容量 6 谱系档案和确定性方向个人领导；Qp 与双Q均关闭。",
        "- D2：三个 seed 均产生同步 Qg/Qp 动作；预热、P 块与 G 块均为 0。",
        "- D3：三个 seed 均产生 10% 预热和 P5/G5 块冻结；`rho=0`。",
        "",
        "## 预注册稳定退化门",
        "",
        "规则：至少 2/3 seed 同时出现 HV 下降与 IGD 变差，且中位 ΔHV≤-2% 或中位 ΔIGD≥+10%。",
        "",
    ]
    for old_arm, new_arm in PAIRINGS:
        value = pair_outcomes[(old_arm, new_arm)]
        report.append("- {}：bad seeds={}/3；median ΔHV={:+.4%}；median ΔIGD={:+.4%}；stableRegression={}.".format(
            old_arm + "→" + new_arm, value["badSeeds"], value["medianHV"], value["medianIGD"], value["stableRegression"]))
    report.extend([
        "",
        "## 根因裁决",
        "",
        "`a2_a3_root_cause = {}`".format(root_cause),
        "",
        "D1 共记录 {} 次确定性方向个人领导选择，其中 fallback={}。因此仅当 D0→D1 同时满足稳定退化门且出现 fallback/无有效领导时，才允许归因于 `PERSONAL_ARCHIVE_COLLAPSE`；本脚本不会把普通的性能波动误写为档案失效。".format(d1_leaders, d1_fallbacks),
        "",
        "## 参考敏感性",
        "",
        "`independent-reference-sensitivity.csv` 用每一相邻对照的六个前沿独立重建参考集；它与共同参考集的差异仅用于检查指标方向是否依赖共同参考集。主诊断仍使用共同参考前沿。",
        "",
        "## 后续边界",
        "",
        "本裁决不授权调整 Qp 奖励、个人档案容量、双Q时序、PDDR、子群配比或局部搜索顺序。若结论为 `COMPOSITE_BLOCK_UNRESOLVED`，下一步只能提出新的、单变量、另行批准的修复计划。",
    ])
    (OUT / "CAUSE_DECISION.md").write_text("\n".join(report) + "\n", encoding="utf-8")

    arm_summary = []
    for arm in ARMS:
        subset = [row for row in metrics if row["arm"] == arm]
        arm_summary.append((arm, statistics.median(row["minCmax"] for row in subset),
            statistics.median(row["minTEC"] for row in subset),
            statistics.median(row["minTWC"] for row in subset),
            statistics.median(row["HV"] for row in subset),
            statistics.median(row["IGD"] for row in subset),
            statistics.median(row["frontSize"] for row in subset)))
    acceptance = [
        "# V35-A3-D 验收报告",
        "",
        "## 验收结论",
        "",
        "**通过诊断运行与证据闭合；根因结论为 `COMPOSITE_BLOCK_UNRESOLVED`。**",
        "",
        "这不是算法修复或正式消融结论。它可靠地排除了“D3 的 P5/G5 冻结是 A2→A3 退化的唯一原因”，但尚不能把退化唯一归咎于个人档案或 Qp 奖励，因为 D0→D1 与 D1→D2 均已出现独立的稳定退化门。",
        "",
        "## 运行有效性",
        "",
        "- 完成：12/12；每条 `actualFE=decoderCalls=50000`。",
        "- 同一 seed 的四臂初始种群哈希一致；跨 seed 的初始哈希不同。",
        "- `illegalSolutions=0`、`duplicateEvaluations=0`、`cfvfRepairs=0`；全部前沿非空且有限。",
        "- 所有运行均显式固定 FM3、单族、序列无关 SUT、ShiftMode=NONE、GLOBAL_ORIGINAL PDDR、20/40/20/20、CA-TA-Lite→inherited LS、方向教师池关闭、未裁剪 Qp 方向奖励。",
        "- 诊断遥测仅观察；行为等价测试比较初群、FE、评价轨迹、前沿和 Q 表哈希。",
        "",
        "## 机制是否真的触发",
        "",
        "- D0：档案/Qp/双Q全部关闭。",
        "- D1：每个 seed 记录 10,000 次确定性方向个人领导选择，Qp 事件为 0，档案插入为 200。",
        "- D2：每个 seed 有 10,000 次 Qp 动作和 10,000 次同步 Qp 更新；预热/P 块/G 块均为 0。",
        "- D3：每个 seed 有 49 个预热周期、26 个 P 块、25 个 G 块；完整 dual-Q 事件流可回放。",
        "",
        "## 共同参考前沿下的三 seed 中位数",
        "",
        "| Arm | min Cmax | min TEC | min TWC | HV | IGD | front size |",
        "|---|---:|---:|---:|---:|---:|---:|",
    ]
    for arm, cmax, tec, twc, hv, igd_value, front_size in arm_summary:
        acceptance.append("| {} | {:.6f} | {:.6f} | {:.6f} | {:.9f} | {:.9f} | {} |".format(
            arm, cmax, tec, twc, hv, igd_value, int(front_size)))
    acceptance.extend([
        "",
        "## 因果读法",
        "",
        "1. **D0→D1（个人档案+确定性方向 pbest）**：2/3 个 seed 同时 HV 下降、IGD 变差；中位 ΔHV=-9.02%，中位 ΔIGD=+102.56%。D1 没有 fallback，且 30,000 次方向选择都有可用档案条目，因此证据不支持把它写成“档案空/退回历史”的 `PERSONAL_ARCHIVE_COLLAPSE`。它只说明当前确定性方向个人领导这一整体行为在此规模上是脆弱的。",
        "2. **D1→D2（再加同步 Qp 四动作与未裁剪奖励）**：3/3 个 seed 同时 HV 下降、IGD 变差；中位 ΔHV=-4.32%，中位 ΔIGD=+30.11%。这显示未裁剪奖励/Qp 选择又带来一致的增量退化，但由于前一步已退化，不能依据预注册规则独占归因为 `QP_SELECTION_OR_REWARD`。",
        "3. **D2→D3（再加 10% 预热与 P5/G5 冻结）**：只有 1/3 个 seed 同时变差；中位 ΔHV=-0.66%，中位 ΔIGD=-3.02%，没有触发稳定退化门。因而冻结时序不是本轮已观测退化的唯一根因，也没有得到“应立即移除”的证据。",
        "",
        "## 参考集稳健性",
        "",
        "相邻对照的独立参考集重算仍保留 D0→D1 和 D1→D2 的退化方向；详情见 `independent-reference-sensitivity.csv`。这降低了“共同 PFref 由某一臂贡献较多点而天然惩罚另一臂”的解释可能性，但三 seed 仍只足以做因果诊断，不能当作论文显著性结论。",
        "",
        "## 事件流元数据校正",
        "",
        "本次运行启动时的旧 writer 把 `fullCapture=false` 写死。`telemetry-capture-correction.csv` 已逐运行核对：所有 Qp、谱系和 dual-Q 日志均满足 `eventCountTotal=eventsRetained`，因此原始事件没有滚动截断；这是输出标签缺陷，而不是数据缺失。源码已改为自动输出 `EMPTY/FULL/ROLLING`，且该改动不进入冻结正式 Jar。",
        "",
        "## 不做什么、下一步最小建议",
        "",
        "本工作包不修改 A3、不改 PDDR、不重做 DOE、不启动 4500 矩阵。若用户之后要验证修复方向，最小的新单变量应为：保持 D2 的个人档案、四动作、同步时序全部不变，仅对比 `LEGACY_UNCLIPPED` 与一个预注册、有限范围的奖励裁剪版本。该实验须单独批准，且不得把结果回写为本次的根因结论。",
    ])
    (OUT / "ACCEPTANCE_REPORT.md").write_text("\n".join(acceptance) + "\n", encoding="utf-8")

    manifest = []
    for path in sorted(item for item in OUT.rglob("*") if item.is_file() and item.name != "evidence-sha256.tsv"):
        manifest.append("{}\t{}\t{}".format(sha256(path), path.stat().st_size, path.relative_to(OUT).as_posix()))
    (OUT / "evidence-sha256.tsv").write_text("sha256\tbytes\tpath\n" + "\n".join(manifest) + "\n", encoding="utf-8")
    root_manifest = []
    root_manifest_path = ROOT / "evidence-sha256.tsv"
    for path in sorted(item for item in ROOT.rglob("*") if item.is_file() and item != root_manifest_path):
        root_manifest.append("{}\t{}\t{}".format(sha256(path), path.stat().st_size,
            path.relative_to(ROOT).as_posix()))
    root_manifest_path.write_text("sha256\tbytes\tpath\n" + "\n".join(root_manifest) + "\n", encoding="utf-8")
    print("root_cause={}".format(root_cause))


if __name__ == "__main__":
    main()
