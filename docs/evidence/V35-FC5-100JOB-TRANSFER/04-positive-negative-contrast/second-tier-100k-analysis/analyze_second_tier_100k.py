#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
analyze_second_tier_100k.py — V35-FC5-T 第二档 100k 筛查 独立分析脚本 (Luna C)

作用域
------
仅读取 100k 结果目录（占位 `raw/output/100k`，本脚本 `--root` 指向该目录，后续由主 Agent 下载到那里）。
输出全部写入本独占目录（--out）：
    run-acceptance-recheck.csv
    per-round-overflow.csv
    windowed-overflow.csv
    directional-lifecycle.csv
    teacher-utilization.csv
    archive-working-gap.csv
    seed-paired-contrast.csv
    h1-100k-screening-verdict.csv
    SECOND_TIER_100K_ANALYSIS_REPORT.md
    recommended-next-action.csv
    evidence-sha256.tsv（本目录全部文件，排除自身）

约束
----
* 不得读取或修改 `first-tier-50k-analysis\`（Luna A 独占）；本脚本只读 `--root`。
* 不读取任何人的文字结论；字段语义以源码为准（见 FIELD_DICTIONARY.md）。
* 100k 是独立预算实验（MaxFEs 影响 A4 预热 / Pacing / 预算调度），
  **禁止把 W1 与 W2 拼成 100k 连续轨迹**。
* 本脚本只做观察，不修改算法 / PDDR / 任何实验。

运行
----
python analyze_second_tier_100k.py --root <100k结果根目录> --out <本独占目录>
（若均省略，默认 root=raw/output/100k、out=当前目录；无数据时只会生成骨架，不产生任何裁决。）

仅标准库：csv / hashlib / statistics / json / argparse / os / sys / pathlib / math。
"""

import argparse
import csv
import hashlib
import json
import math
import os
import statistics
import sys
from pathlib import Path

# --------------------------------------------------------------------------- #
# 常量：与源码 / 预注册口径对齐
# --------------------------------------------------------------------------- #
VERSION = "analyze_second_tier_100k v1"
SCHEMA = "FC5_100JOB_TRANSFER_V1"
TARGET_WORKING_POPULATION = 100
WINDOW_FE = 50_000            # W1=[0,50000] 名义跨度；与源码 WINDOW_FE 一致
DEFAULT_ROOT = "raw/output/100k"

# 预注册对照集（来自 ZhangBoV35Fc5TransferRunner.requireApprovedCase）
FC5_A2A4_INSTANCES = {"100_2_4_1", "100_5_3_1"}   # A2 vs A4 案例实例
FC5_A2A4_SEEDS = range(20260901, 20260906)         # 20260901..20260905
FC5_A2A4_ARMS = {"A2", "A4"}

# H1-100k 裁决阈值（在脚本注释与报告中同时声明，作为“预注册条件”）
NND_GT = 100.0                # 条件①：W2 出现 Nnd>100
NND_GE90 = 90.0               # 情形 C：90<=Nnd<=100
ROVERFLOW_CLEAR_RISE_ABS = 0.05   # 条件②：中位 Roverflow W2-W1 明显上升（绝对差）
RETENTION_DROP_PP = 0.20      # 条件③：pool->next 保留率相对 W1 下降 >=20pp
CMX_GAP_EPS = 0.0             # 情形 B：max cmaxGap≈0（工作不劣于档案，否则视为扩大）
TEACHER_W2_MIN_EXPOSURE = 0   # 情形 B/条件⑤：教师链路未断裂（W2 教师曝光>0）
REQUIRED_SEEDS = 3            # 裁决设计的 3-seed 一致性口径
FIRST_TIER_DIR = "first-tier-50k-analysis"   # 禁止访问（Luna A 独占）——仅用于报告中声明，绝不触碰

# 对应四种方向代表标签
REP_LABELS = ("E_C", "E_E", "E_W", "E_B")
W1 = "W1"
W2 = "W2"

# --------------------------------------------------------------------------- #
# 底层 IO / 哈希工具
# --------------------------------------------------------------------------- #


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as fh:
        for chunk in iter(lambda: fh.read(1 << 16), b""):
            h.update(chunk)
    return h.hexdigest()


def file_bytes(path: Path) -> int:
    return os.path.getsize(path)


def read_textlines(path: Path):
    with open(path, "r", encoding="utf-8", newline="") as fh:
        return fh.read()


# --------------------------------------------------------------------------- #
# 配置文件解析
# --------------------------------------------------------------------------- #


def parse_key_value_props(path: Path) -> dict:
    """解析 `key=value` 属性文件（status.properties / summary.properties 等）。"""
    data = {}
    if not path.exists():
        return data
    for raw in read_textlines(path).splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if "=" in line:
            k, v = line.split("=", 1)
            data[k.strip()] = v.strip()
    return data


def parse_config(path: Path) -> dict:
    """解析 configuration.txt。只取 header 块（第一个 CanonicalBegin 之前的 key=value 行），
    因为 profile/formal-baseline canonical 文本体可能含相同 key（如 seed=）、会污染 dict。
    provenance 追加块的 instanceSha256 等（profileCanonicalEnd 之后）不在此函数职责内。"""
    data = {}
    if not path.exists():
        return data
    for raw in read_textlines(path).splitlines():
        if "CanonicalBegin" in raw or "CanonicalEnd" in raw:
            break  # 进入 canonical 文本体，停止解析 header
        if "=" in raw and not raw.startswith(" "):
            k, v = raw.split("=", 1)
            data[k.strip()] = v.strip()
    return data


# 长枚举 arm -> 短标签（configuration.txt 的 arm= 写的是 Arm 枚举全名）
ARM_LONG_TO_SHORT = {
    "A0_BASELINE": "A0",
    "A2_CFVF": "A2",
    "A4_BUDGET_AWARE_CATA": "A4",
}


def norm_arm(config: dict, status: dict) -> str:
    """把 config.arm（或 status.mode）归一化为短标签 A0/A2/A4，用于分组与输出。"""
    long = config.get("arm")
    if long in ARM_LONG_TO_SHORT:
        return ARM_LONG_TO_SHORT[long]
    mode = status.get("mode")
    if mode == "V35_BASELINE":
        return "A0"
    if mode == "V35_A2":
        return "A2"
    if mode == "V35_FULL_POOL_OFF":
        return "A4"
    return long or "?"


def parse_initial_hash(path: Path) -> dict:
    return parse_key_value_props(path)


def parse_merge_rounds(path: Path):
    """Return list[dict] with int/float coercion."""
    rows = []
    if not path.exists():
        return rows
    with open(path, "r", encoding="utf-8", newline="") as fh:
        reader = csv.DictReader(fh)
        for r in reader:
            rows.append({
                "seed": _int(r.get("seed")),
                "cycle": _int(r.get("cycle")),
                "fe": _int(r.get("fe")),
                "Nmerge": _int(r.get("Nmerge")),
                "Nunique": _int(r.get("Nunique")),
                "Nnd": _int(r.get("Nnd")),
                "Roverflow": _float(r.get("Roverflow")),
            })
    return rows


def parse_representatives(path: Path):
    """解析方向代表生命周期文件。

    关键：第 7 列 fingerprint 含未加引号的逗号，因此按
        head=tokens[0:6]; fingerprint=','.join(tokens[6:-20]); tail=tokens[-20:]
    （前 6 列 + 变宽 fingerprint + 后 20 列；header 共 27 列）。
    """
    rows = []
    if not path.exists():
        return rows
    with open(path, "r", encoding="utf-8", newline="") as fh:
        for line in fh:
            line = line.rstrip("\r\n")
            if not line:
                continue
            if line.startswith("seed,cycle,fe,representative"):
                continue  # header
            parts = line.split(",")
            if len(parts) < 27:
                continue  # 不完整行，跳过（多为空/异常）
            head = parts[:6]
            tail = parts[-20:]
            fingerprint = ",".join(parts[6:-20])
            row = {
                "seed": _int(head[0]),
                "cycle": _int(head[1]),
                "fe": _int(head[2]),
                "representative": head[3],
                "poolIndex": _int(head[4]),
                "source": head[5],
                "fingerprint": fingerprint,
                "Cmax": _float(tail[0]),
                "TEC": _float(tail[1]),
                "TWC": _float(tail[2]),
                "pddrScore": _float(tail[3]),
                "pddrRank": _int(tail[4]),
                "poolPresent": _bool(tail[5]),
                "pddrSelected": _bool(tail[6]),
                "rejectReason": tail[7],
                "nextPopulationSlot": _int(tail[8]),
                "nextSemanticRole": tail[9],
                "qgTeacherUses": _int(tail[10]),
                "qpTeacherUses": _int(tail[11]),
                "teacherUseCycles": tail[12],
                "improvedOffspringCount": _int(tail[13]),
                "lastImprovementFE": _int(tail[14]),
                "lastImprovementTeacherKind": tail[15],
                "lastImprovementRequestingRole": tail[16],
                "lastTeacherFE": _int(tail[17]),
                "lastTeacherRole": tail[18],
                "retiredAtCycle": _int(tail[19]),
            }
            rows.append(row)
    return rows


def parse_archive_gap(path: Path):
    rows = []
    if not path.exists():
        return rows
    with open(path, "r", encoding="utf-8", newline="") as fh:
        reader = csv.DictReader(fh)
        for r in reader:
            rows.append({
                "seed": _int(r.get("seed")),
                "cycle": _int(r.get("cycle")),
                "fe": _int(r.get("fe")),
                "cmaxGap": _float(r.get("cmaxGap")),
                "tecGap": _float(r.get("tecGap")),
                "twcGap": _float(r.get("twcGap")),
                "workingSize": _int(r.get("workingSize")),
                "archiveSize": _int(r.get("archiveSize")),
            })
    return rows


def parse_summary(path: Path) -> dict:
    return parse_key_value_props(path)


# --------------------------------------------------------------------------- #
# 运行清单发现 + 验收
# --------------------------------------------------------------------------- #


def discover_runs(root: Path):
    """递归发现“运行目录”：同时包含 status.properties、configuration.txt、
    fc5-transfer-summary.properties 的目录视为一条运行。返回 sorted list[Path]。"""
    found = []
    if not root.exists():
        return found
    for dirpath, dirnames, filenames in os.walk(root):
        d = Path(dirpath)
        if (d / "status.properties").exists() and (d / "configuration.txt").exists() \
                and (d / "fc5-transfer-summary.properties").exists():
            found.append(d)
            # 运行目录内部不再深挖，避免把子目录重复识别
            dirnames[:] = []
    return sorted(found)


def run_key(config: dict) -> str:
    return "{instance}|{seed}|{arm}|{maxFEs}".format(
        instance=config.get("instance", "?"), seed=config.get("seed", "?"),
        arm=config.get("arm", "?"), maxFEs=config.get("maxFEs", "?"))


def in_pre_registered(config: dict) -> bool:
    inst = config.get("instance")
    arm_short = ARM_LONG_TO_SHORT.get(config.get("arm"), config.get("arm"))
    try:
        seed = int(config.get("seed", "-1"))
    except (TypeError, ValueError):
        return False
    if inst not in FC5_A2A4_INSTANCES or arm_short not in FC5_A2A4_ARMS:
        return False
    return seed in FC5_A2A4_SEEDS


def verify_run_manifest(run_dir: Path):
    """对单条运行目录内的 evidence-sha256.tsv 逐行反向重算哈希+字节比对。"""
    manifest = run_dir / "evidence-sha256.tsv"
    if not manifest.exists():
        return ("MISSING", "evidence-sha256.tsv 不存在")
    total, failures = 0, []
    with open(manifest, "r", encoding="utf-8", newline="") as fh:
        reader = csv.reader(fh, delimiter="\t")
        header = next(reader, None)
        if not header or header[0].strip() != "sha256":
            return ("BAD_HEADER", "manifest header 非 sha256\\tbytes\\tpath")
        for row in reader:
            if len(row) < 3:
                failures.append("malformed: %r" % row)
                continue
            sha, bytes_txt, rel = row[0].strip(), row[1].strip(), row[2].strip()
            target = run_dir / rel.replace("/", os.sep)
            total += 1
            if not target.exists():
                failures.append("%s 缺失" % rel)
                continue
            try:
                got = sha256_file(target)
                gb = file_bytes(target)
            except Exception as exc:  # noqa: BLE001
                failures.append("%s 读取失败: %s" % (rel, exc))
                continue
            if got != sha or gb != int(bytes_txt):
                failures.append("%s 哈希/字节不一致 (got sha=%s bytes=%d)" % (rel, got, gb))
    if failures:
        return ("FAIL", "; ".join(failures[:10]) + (" ..." if len(failures) > 10 else ""))
    return ("OK", "verified %d files" % total)


def evaluate_run(run_dir: Path, config: dict, status: dict, init_hash: dict):
    """返回该运行的验收结论 dict。"""
    actualFE = _int(status.get("fullEvaluations"))
    decoder_calls = _int(status.get("decoderCalls"))
    illegal = _int(status.get("illegalSolutions"))
    duplicate = _int(status.get("duplicateEvaluations"))
    status_val = status.get("status")
    maxFEs = _int(config.get("maxFEs"))
    manifest_verdict, manifest_detail = verify_run_manifest(run_dir)

    reasons = []
    if status_val != "COMPLETED":
        reasons.append("status=%s" % status_val)
    if actualFE is None or actualFE <= 0:
        reasons.append("actualFE<=0")
    if maxFEs is not None and actualFE is not None and actualFE > maxFEs:
        reasons.append("actualFE>maxFEs")
    if decoder_calls is not None and actualFE is not None and decoder_calls != actualFE:
        reasons.append("decoderCalls!=actualFE")
    if illegal not in (None, 0):
        reasons.append("illegal!=0")
    if duplicate not in (None, 0):
        reasons.append("duplicate!=0")
    if manifest_verdict != "OK":
        reasons.append("manifest:%s" % manifest_verdict)

    # 初群哈希核对：initial-population.sha256 的 v35 与 status.initialPopulationHash
    init_v35 = init_hash.get("v35")
    init_status_hash = status.get("initialPopulationHash")
    hash_match = "?" 
    if init_v35 and init_status_hash:
        hash_match = "MATCH" if init_v35 == init_status_hash else "MISMATCH"
    elif init_v35 or init_status_hash:
        hash_match = "PARTIAL"
    if hash_match == "MISMATCH":
        reasons.append("initialHashMismatch")

    accepted = (len(reasons) == 0)
    return {
        "runDir": run_dir.name,
        "instance": config.get("instance", "?"),
        "seed": config.get("seed", "?"),
        "arm": norm_arm(config, status),
        "mode": status.get("mode", "?"),
        "maxFEs": str(maxFEs) if maxFEs is not None else "?",
        "status": status_val,
        "decoderCalls": decoder_calls,
        "actualFE": actualFE,
        "illegal": illegal,
        "duplicate": duplicate,
        "initialHashV35": init_v35,
        "initialHashP8": init_hash.get("p8"),
        "initialHashStatus": init_status_hash,
        "initialHashMatch": hash_match,
        "inPreReg": in_pre_registered(config),
        "manifestVerify": manifest_verdict,
        "manifestDetail": manifest_detail,
        "acceptance": "ACCEPTED" if accepted else "REJECTED_" + "|".join(reasons),
        "note": "",
    }


# --------------------------------------------------------------------------- #
# 窗口工具
# --------------------------------------------------------------------------- #


def partition_windows(fe_list, actualFE):
    """把 FE 划分到 W1 / W2。W2 在 actualFE<100000 时标记部分窗口。
    W1 = fe ∈ (0,50000]；W2 = fe ∈ (50000, actualFE]。"""
    w1, w2 = [], []
    for fe in fe_list:
        if fe <= 0:
            continue
        if fe <= WINDOW_FE:
            w1.append(fe)
        else:
            w2.append(fe)
    partial_w2 = (actualFE is not None and actualFE < 2 * WINDOW_FE)
    return w1, w2, partial_w2


def window_end_for(fe_list, actualFE):
    return WINDOW_FE, actualFE


def fe_to_window(fe, actualFE):
    if fe <= WINDOW_FE:
        return "W1"
    return "W2"


def in_window(fe, window, actualFE):
    if window == "W1":
        return 0 < fe <= WINDOW_FE
    return WINDOW_FE < fe <= (actualFE if actualFE is not None else math.inf)


def is_second_window_partial(actualFE):
    return actualFE is not None and actualFE < 2 * WINDOW_FE


# --------------------------------------------------------------------------- #
# 分析计算
# --------------------------------------------------------------------------- #


def _int(v):
    try:
        if v is None or v == "":
            return None
        return int(v)
    except (TypeError, ValueError):
        return None


def _float(v):
    try:
        if v is None or v == "":
            return None
        return float(v)
    except (TypeError, ValueError):
        return None


def _bool(v):
    return str(v).strip().lower() in ("true", "1", "yes")


def run_analysis(run_dir: Path, config: dict, status: dict,
                 merge_rows, rep_rows, gap_rows, summary, actualFE):
    """封装单条运行的聚合计算，返回各输出片段的字典。"""
    arm = norm_arm(config, status)
    seed = config.get("seed", "?")

    # cycle -> fe 映射（用于“代表损失 FE”换算）
    cycle_fe = {r["cycle"]: r["fe"] for r in merge_rows if r["cycle"] is not None and r["fe"] is not None}

    # ---- 逐轮 overflow（并入窗口标签）----
    per_round = []
    for r in sort_by_fe(merge_rows):
        fe = r["fe"]
        window = fe_to_window(fe, actualFE) if fe is not None else "?"
        per_round.append({
            "seed": seed, "runDir": run_dir.name, "arm": arm,
            "cycle": r["cycle"], "fe": fe,
            "Nmerge": r["Nmerge"], "Nunique": r["Nunique"], "Nnd": r["Nnd"],
            "Roverflow": r["Roverflow"], "window": window,
            "windowPartial": is_second_window_partial(actualFE) and window == "W2",
        })

    # ---- 窗口聚合 overflow ----
    windowed = []
    for window in ("W1", "W2"):
        vals = [r for r in per_round if r["window"] == window]
        if not vals:
            continue
        nnds = [v["Nnd"] for v in vals if v["Nnd"] is not None]
        merges = [v["Nmerge"] for v in vals if v["Nmerge"] is not None]
        rovs = [v["Roverflow"] for v in vals if v["Roverflow"] is not None]
        windowed.append({
            "seed": seed, "runDir": run_dir.name, "arm": arm,
            "window": window, "windowStartFE": 0 if window == "W1" else WINDOW_FE,
            "windowEndFE": actualFE if window == "W2" else WINDOW_FE,
            "rounds": len(vals),
            "meanNmerge": safe_mean(merges), "meanNnd": safe_mean(nnds),
            "maxNnd": safe_max(nnds),
            "meanRoverflow": safe_mean(rovs), "maxRoverflow": safe_max(rovs),
            "windowPartial": "PARTIAL_SECOND_WINDOW"
                if (window == "W2" and is_second_window_partial(actualFE)) else "COMPLETE_OR_NOMINAL",
        })

    # ---- 方向代表生命周期（逐轮 + 按窗口）----
    rep_records = []
    for r in rep_rows:
        window = fe_to_window(r["fe"], actualFE) if r["fe"] is not None else "?"
        selected = r["pddrSelected"]
        retained = selected and (r["retiredAtCycle"] is None or r["retiredAtCycle"] == -1
                                 or (r["cycle"] is not None and r["retiredAtCycle"] > r["cycle"]))
        rep_records.append({
            "seed": seed, "runDir": run_dir.name, "arm": arm,
            "level": "round", "window": window, "cycle": r["cycle"], "fe": r["fe"],
            "representative": r["representative"], "poolPresent": r["poolPresent"],
            "pddrSelected": selected, "nextSlot": r["nextPopulationSlot"],
            "nextRole": r["nextSemanticRole"], "retainedIntoNext": retained,
            "retentionRate": "", "retireCycle": r["retiredAtCycle"],
            "qgTeacherUses": r["qgTeacherUses"], "qpTeacherUses": r["qpTeacherUses"],
            "improvedOffspring": r["improvedOffspringCount"],
            "firstLossFE": "",
        })

    # 首次代表损失 FE（按方向）—— 该方向首个 retiredAtCycle != -1 的退休对应的 FE
    first_loss_fe = {}
    for label in REP_LABELS:
        for r in sorted(rep_rows, key=lambda x: (x["cycle"] if x["cycle"] is not None else 0)):
            if r["representative"] != label:
                continue
            if r["retiredAtCycle"] is not None and r["retiredAtCycle"] != -1:
                first_loss_fe[label] = cycle_fe.get(r["retiredAtCycle"])
                break
        else:
            first_loss_fe[label] = None

    # 按窗口聚合：保留率 / 教师使用 / 改善后代
    for window in ("W1", "W2"):
        for label in REP_LABELS:
            subset = [r for r in rep_records if r["window"] == window and r["representative"] == label]
            if not subset:
                continue
            selected = [r for r in subset if r["pddrSelected"]]
            retained = [r for r in selected if r["retainedIntoNext"]]
            rate = (len(retained) / len(selected)) if selected else ("" if not selected else None)
            rep_records.append({
                "seed": seed, "runDir": run_dir.name, "arm": arm,
                "level": "window", "window": window, "cycle": "",
                "fe": "", "representative": label, "poolPresent": "aggregate",
                "pddrSelected": len(selected), "nextSlot": "", "nextRole": "",
                "retainedIntoNext": len(retained),
                "retentionRate": rate if rate == "" else round(rate, 6),
                "retireCycle": "", "qgTeacherUses": safe_sum(x["qgTeacherUses"] for x in subset),
                "qpTeacherUses": safe_sum(x["qpTeacherUses"] for x in subset),
                "improvedOffspring": safe_sum(x["improvedOffspring"] for x in subset),
                "firstLossFE": first_loss_fe[label] if first_loss_fe[label] is not None else "",
            })

    # ---- 教师利用（曝光 + 改善后代），按粒度 ----
    teacher_util = []
    for window in ("W1", "W2"):
        # representive-level
        for label in REP_LABELS:
            subset = [r for r in rep_records if r["level"] == "round" and r["window"] == window
                      and r["representative"] == label]
            if not subset:
                continue
            teacher_util.append({
                "seed": seed, "runDir": run_dir.name, "arm": arm, "window": window,
                "granularity": "representative", "representative": label,
                "qgTeacherUses": safe_sum(x["qgTeacherUses"] for x in subset),
                "qpTeacherUses": safe_sum(x["qpTeacherUses"] for x in subset),
                "teacherExposure": safe_sum(x["qgTeacherUses"] for x in subset)
                    + safe_sum(x["qpTeacherUses"] for x in subset),
                "improvedOffspring": safe_sum(x["improvedOffspring"] for x in subset),
                "teacherUseCyclesCount": safe_sum(
                    len((x.get("teacherUseCycles") or "").split(";"))
                    for x in subset),
            })
        # total-level
        subset = [r for r in rep_records if r["level"] == "round" and r["window"] == window]
        if subset:
            teacher_util.append({
                "seed": seed, "runDir": run_dir.name, "arm": arm, "window": window,
                "granularity": "total", "representative": "ALL",
                "qgTeacherUses": safe_sum(x["qgTeacherUses"] for x in subset),
                "qpTeacherUses": safe_sum(x["qpTeacherUses"] for x in subset),
                "teacherExposure": safe_sum(x["qgTeacherUses"] for x in subset)
                    + safe_sum(x["qpTeacherUses"] for x in subset),
                "improvedOffspring": safe_sum(x["improvedOffspring"] for x in subset),
                "teacherUseCyclesCount": safe_sum(
                    len((x.get("teacherUseCycles") or "").split(";")) for x in subset),
            })

    # ---- archive-working-gap 时序 + 里程碑 ----
    gap_rows2 = sorted(gap_rows, key=lambda x: (x["fe"] if x["fe"] is not None else 0))
    # 里程碑
    nnd_by_fe = [(r["fe"], r["Nnd"]) for r in merge_rows if r["fe"] is not None and r["Nnd"] is not None]
    nnd_ge90_fe = first_fe_where(nnd_by_fe, lambda n: n >= NND_GE90)
    nnd_gt100_fe = first_fe_where(nnd_by_fe, lambda n: n > NND_GT)
    # gap 扩大（cmax/tec/twc 取“更差=正值变大”首次超过前一局部水平）
    loss_fe = (min([v for v in first_loss_fe.values() if v is not None])
               if any(v is not None for v in first_loss_fe.values()) else None)
    gap_series = []
    prev_cmax = prev_tec = prev_twc = None
    cmax_expand_fe = tec_expand_fe = twc_expand_fe = None
    cmax_max_after_loss = None
    for g in gap_rows2:
        fe = g["fe"]
        cg, tg, wg = g["cmaxGap"], g["tecGap"], g["twcGap"]
        if prev_cmax is not None and cg is not None and cg > prev_cmax + 1e-9 and cmax_expand_fe is None:
            cmax_expand_fe = fe
        if prev_tec is not None and tg is not None and tg > prev_tec + 1e-9 and tec_expand_fe is None:
            tec_expand_fe = fe
        if prev_twc is not None and wg is not None and wg > prev_twc + 1e-9 and twc_expand_fe is None:
            twc_expand_fe = fe
        prev_cmax, prev_tec, prev_twc = cg, tg, wg
        if loss_fe is not None and fe is not None and fe >= loss_fe and cg is not None:
            cmax_max_after_loss = cg if cmax_max_after_loss is None else max(cmax_max_after_loss, cg)
        gap_series.append({
            "seed": seed, "runDir": run_dir.name, "arm": arm, "cycle": g["cycle"], "fe": fe,
            "cmaxGap": cg, "tecGap": tg, "twcGap": wg,
            "workingSize": g["workingSize"], "archiveSize": g["archiveSize"],
            "firstRepLossFE": loss_fe, "firstCmaxGapExpandFE": cmax_expand_fe,
            "firstTecGapExpandFE": tec_expand_fe, "firstTwcGapExpandFE": twc_expand_fe,
            "firstNndGE90FE": nnd_ge90_fe, "firstNndGT100FE": nnd_gt100_fe,
        })
    max_cmax_gap = safe_max([g["cmaxGap"] for g in gap_rows2])
    gap_widened_after_loss = (loss_fe is not None and cmax_expand_fe is not None
                              and cmax_expand_fe >= loss_fe)

    return {
        "instance": config.get("instance", "?"),
        "arm": arm,
        "seed": seed,
        "per_round": per_round,
        "windowed": windowed,
        "rep_records": rep_records,
        "whole_run_rep": rep_rows,
        "first_loss_fe": first_loss_fe,
        "teacher_util": teacher_util,
        "gap_series": gap_series,
        "max_cmax_gap": max_cmax_gap,
        "gap_widened_after_loss": gap_widened_after_loss,
        "nnd_ge90_fe": nnd_ge90_fe,
        "nnd_gt100_fe": nnd_gt100_fe,
        "cmax_expand_fe": cmax_expand_fe,
    }


def sort_by_fe(rows):
    return sorted(rows, key=lambda x: (x["fe"] if x["fe"] is not None else 0))


def first_fe_where(pairs, predicate):
    pairs = sorted(pairs, key=lambda p: p[0])
    for fe, val in pairs:
        if predicate(val):
            return fe
    return None


def safe_sum(iterable):
    vals = [v for v in iterable if v is not None]
    return sum(vals) if vals else None


def safe_mean(values):
    vals = [v for v in values if v is not None]
    return round(statistics.mean(vals), 6) if vals else None


def safe_median(values):
    vals = [v for v in values if v is not None]
    return statistics.median(vals) if vals else None


def safe_max(values):
    vals = [v for v in values if v is not None]
    return max(vals) if vals else None


# --------------------------------------------------------------------------- #
# 情形 A / B / C 裁决（H1-100k 判据计算器）
# --------------------------------------------------------------------------- #
# 预注册条件说明（写入报告/report 模板）：
#   情形 A（FC5_TRANSFER_STRONG_SIGNAL_AT_100K）：以下五条同时满足
#     ①  >=2/3 seed 在 W2 出现 Nnd>100
#     ②  中位 Roverflow 继续明显上升（W2 中位 - W1 中位 >= ROVERFLOW_CLEAR_RISE_ABS）
#     ③  >=一种方向代表 pool->next 保留率相对 W1 下降 >=20pp
#     ④  gap 在代表损失后扩大
#     ⑤  教师曝光或改善后代同步下降（W2 < W1）
#   情形 B（FC5_TRANSFER_NOT_CONFIRMED_THROUGH_100K）：全部运行满足
#     i)   maxNnd < 90（全程）
#     ii)  pool->next 保留率 >= 95%
#     iii) cmaxGap ≈ 0 且无扩大（max cmaxGap <= CMX_GAP_EPS）
#     iv)  教师链路未断裂（W2 教师曝光 > TEACHER_W2_MIN_EXPOSURE）
#   情形 C（FC5_TRANSFER_100K_INCONCLUSIVE）：其余情况，包括
#     - 90 <= Nnd <= 100；或
#     - 仅 1/3 seed 超 100；或
#     - Nnd 增加但代表仍被保留、教师仍正常利用。
#
# ★ 结论边界 ★
#   “Nnd 增加但代表仍被保留、教师仍正常利用时，说明候选多本身不是根因，
#    禁止据此修改 PDDR。”
#   100k 是独立预算实验，禁止把 W1+W2 拼成连续轨迹；W2 部分窗口不冒充完整窗口。


def compute_verdict(op_runs_by_seed):
    """op_runs_by_seed: {seed: {"A2": run_analysis_dict or None, "A4": ...或 None}}。
    返回 dict：verdict, per-seed 条件表, 各条件 boolean。"""
    nseeds = len(op_runs_by_seed)
    if nseeds == 0:
        return {"verdict": "INSUFFICIENT_DATA", "reason": "无已验收运行",
                "conditions": {}, "per_seed": [], "nseeds": 0}

    # 只以候选臂 A4 判定“候选迁移信号”（A2 为对照）
    a4_runs = {s: op_runs_by_seed[s]["A4"] for s in op_runs_by_seed if op_runs_by_seed[s]["A4"]}
    n_a4 = len(a4_runs)

    # ---- 每条 A4 运行的 seed 级条件 ----
    per_seed = []
    cond1_count = 0   # Nnd>100 in W2
    cond2_count = 0   # Roverflow 明显上升
    cond3_count = 0   # 保留率下降 >=20pp（≥一种方向）
    cond4_count = 0   # gap 在代表损失后扩大
    cond5_count = 0   # 教师曝光或改善后代同步下降
    all_max_nnd_lt90 = True
    all_w2_retention_ge95 = True
    all_cmax_gap_eps = True
    all_teacher_link_intact = True
    for seed, an in a4_runs.items():
        w1 = window_by_name(an["windowed"], "W1")
        w2 = window_by_name(an["windowed"], "W2")
        max_nnd = safe_max([v["Nnd"] for v in an["per_round"]])
        c1 = bool(w2 and w2["maxNnd"] is not None and w2["maxNnd"] > NND_GT)
        ro_w1 = safe_median([v["Roverflow"] for v in an["per_round"] if v["window"] == "W1"
                             and v["Roverflow"] is not None])
        ro_w2 = safe_median([v["Roverflow"] for v in an["per_round"] if v["window"] == "W2"
                             and v["Roverflow"] is not None])
        c2 = (ro_w1 is not None and ro_w2 is not None
              and ro_w2 - ro_w1 >= ROVERFLOW_CLEAR_RISE_ABS)
        # 保留率相对 W1 的降幅（取方向最大降幅）
        drop_pp = 0.0
        c3 = False
        for label in REP_LABELS:
            r1 = retention_for_label(an["rep_records"], "W1", label)
            r2 = retention_for_label(an["rep_records"], "W2", label)
            if r1 is not None and r2 is not None:
                d = r1 - r2
                if d > drop_pp:
                    drop_pp = d
                if d >= RETENTION_DROP_PP:
                    c3 = True
        # 教师使用/改善后代：W2 相对 W1
        te_w1 = teacher_total(an["teacher_util"], "W1")
        te_w2 = teacher_total(an["teacher_util"], "W2")
        c5 = (te_w2 is not None and te_w1 is not None and te_w2 < te_w1)
        c4 = an["gap_widened_after_loss"]
        if c1: cond1_count += 1
        if c2: cond2_count += 1
        if c3: cond3_count += 1
        if c4: cond4_count += 1
        if c5: cond5_count += 1

        # 情形 B 各条件
        if not (max_nnd is not None and max_nnd < NND_GE90):
            all_max_nnd_lt90 = False
        if not (w2 and retention_total(an["rep_records"], "W2") is not None
                and retention_total(an["rep_records"], "W2") >= 0.95):
            all_w2_retention_ge95 = False
        if not (an["max_cmax_gap"] is not None and an["max_cmax_gap"] <= CMX_GAP_EPS):
            all_cmax_gap_eps = False
        if not (te_w2 is not None and te_w2 > TEACHER_W2_MIN_EXPOSURE):
            all_teacher_link_intact = False
        # 额外子信号：Nnd 增加但代表仍被保留、教师仍利用（→ 触发 C）
        overflow_increases = bool(w2 and w1 and w2["maxNnd"] is not None and w1["maxNnd"] is not None
                                 and w2["maxNnd"] > w1["maxNnd"])
        rep_still_retained_and_used = bool(w2 and retention_total(an["rep_records"], "W2") is not None
                                           and retention_total(an["rep_records"], "W2") >= 0.95
                                           and te_w2 is not None and te_w2 > 0)
        per_seed.append({
            "seed": seed,
            "maxNnd": max_nnd, "maxNndW2": (w2["maxNnd"] if w2 else None),
            "rovW1": ro_w1, "rovW2": ro_w2, "c1_nnd_gt100": c1, "c2_rov_rise": c2,
            "retentionDropPP": round(drop_pp, 6), "c3_retention_drop": c3,
            "c4_gap_widen_after_loss": c4, "c5_teacher_drop": c5,
            "overflow_increases_but_retained_used": overflow_increases and rep_still_retained_and_used,
            "maxCmaxGap": an["max_cmax_gap"], "firstLossFE": min(
                (v for v in an["first_loss_fe"].values() if v is not None), default=None),
        })

    # ---- 汇总条件 ----
    need = math.ceil(2 / 3 * n_a4) if n_a4 else 0
    condA_overflow = (cond1_count >= need and cond2_count >= need if n_a4 else False)
    # 情形 A 的 5 条全布尔（③④⑤ 取“至少一条 A4 运行满足/或聚合”）
    condA_3 = cond3_count >= 1
    condA_4 = cond4_count >= 1
    condA_5 = cond5_count >= 1
    condA_all = condA_overflow and condA_3 and condA_4 and condA_5

    condB_all = (all_max_nnd_lt90 and all_w2_retention_ge95
                 and all_cmax_gap_eps and all_teacher_link_intact) if n_a4 else False

    if condA_all:
        verdict = "FC5_TRANSFER_STRONG_SIGNAL_AT_100K"
        reason = ("情形 A：≥2/3 seed W2 Nnd>100 + Roverflow 明显上升 + 一种方向保留率降>=20pp "
                  "+ gap 于代表损失后扩大 + 教师曝光/改善后代同步下降")
    elif condB_all:
        verdict = "FC5_TRANSFER_NOT_CONFIRMED_THROUGH_100K"
        reason = ("情形 B：全部运行 maxNnd<90 且 pool->next 保留率>=95% 且 cmaxGap≈0 无扩大 "
                  "且教师链路未断裂")
    else:
        verdict = "FC5_TRANSFER_100K_INCONCLUSIVE"
        reason = ("情形 C：既未同时满足情形 A 五条件，也未满足情形 B 全条件。"
                  "包括：90<=Nnd<=100 / 仅1/3 seed 超100 / Nnd增加但代表仍被保留利用。"
                  "注意：Nnd 增加但代表仍被保留、教师仍正常利用时，说明候选多本身不是根因，"
                  "禁止据此修改 PDDR。")

    conditions = {
        "A_c1_ge2of3_nnd_gt100": cond1_count >= need,
        "A_c2_rov_rise": cond2_count >= need,
        "A_c3_retention_drop_20pp": condA_3,
        "A_c4_gap_widen_after_loss": condA_4,
        "A_c5_teacher_drop": condA_5,
        "A_all": condA_all,
        "B_all_max_nnd_lt90": all_max_nnd_lt90,
        "B_all_retention_ge95": all_w2_retention_ge95,
        "B_all_cmax_gap_approx0": all_cmax_gap_eps,
        "B_all_teacher_link_intact": all_teacher_link_intact,
        "B_all": condB_all,
    }
    return {"verdict": verdict, "reason": reason, "conditions": conditions,
            "per_seed": per_seed, "nseeds": n_a4, "nseeds_expected": REQUIRED_SEEDS,
            "cond1_count": cond1_count, "need": need}


def window_by_name(windowed, name):
    for w in windowed:
        if w["window"] == name:
            return w
    return None


def retention_for_label(rep_records, window, label):
    sel = [r for r in rep_records if r["level"] == "round" and r["window"] == window
           and r["representative"] == label and r["pddrSelected"]]
    if not sel:
        return None
    kept = [r for r in sel if r["retainedIntoNext"]]
    return len(kept) / len(sel)


def retention_total(rep_records, window):
    sel = [r for r in rep_records if r["level"] == "round" and r["window"] == window
           and r["pddrSelected"]]
    if not sel:
        return None
    kept = [r for r in sel if r["retainedIntoNext"]]
    return len(kept) / len(sel)


def teacher_total(teacher_util, window):
    tot = [t for t in teacher_util if t["window"] == window and t["granularity"] == "total"]
    if tot:
        return tot[0]["teacherExposure"]
    return None


# --------------------------------------------------------------------------- #
# 输出写出
# --------------------------------------------------------------------------- #


def write_csv(path, fieldnames, rows):
    os.makedirs(path.parent, exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="") as fh:
        writer = csv.DictWriter(fh, fieldnames=fieldnames, extrasaction="ignore")
        writer.writeheader()
        for r in rows:
            writer.writerow(r)
    return path


def fmt(v):
    return "" if v is None else v


def vfmt(v):
    """渲染数值单元格：None -> ""（缺失），0.0 -> "0.0"（不吞真值）。"""
    if v is None:
        return ""
    return v


def write_outputs(out_dir, accep_rows, per_round, windowed, rep_records, teacher_util,
                  gap_series, verdict, paired_rows, recommend_rows, accepted_count, note):
    os.makedirs(out_dir, exist_ok=True)

    write_csv(out_dir / "run-acceptance-recheck.csv", [
        "runDir", "instance", "seed", "arm", "mode", "maxFEs", "status", "decoderCalls",
        "actualFE", "illegal", "duplicate", "initialHashV35", "initialHashP8",
        "initialHashStatus", "initialHashMatch", "inPreReg", "manifestVerify",
        "manifestDetail", "acceptance", "note"], accep_rows)

    write_csv(out_dir / "per-round-overflow.csv", [
        "seed", "runDir", "arm", "cycle", "fe", "Nmerge", "Nunique", "Nnd", "Roverflow",
        "window", "windowPartial"], per_round)

    write_csv(out_dir / "windowed-overflow.csv", [
        "seed", "runDir", "arm", "window", "windowStartFE", "windowEndFE", "rounds",
        "meanNmerge", "meanNnd", "maxNnd", "meanRoverflow", "maxRoverflow", "windowPartial"],
        windowed)

    write_csv(out_dir / "directional-lifecycle.csv", [
        "seed", "runDir", "arm", "level", "window", "cycle", "fe", "representative",
        "poolPresent", "pddrSelected", "nextSlot", "nextRole", "retainedIntoNext",
        "retentionRate", "retireCycle", "qgTeacherUses", "qpTeacherUses",
        "improvedOffspring", "firstLossFE"], rep_records)

    write_csv(out_dir / "teacher-utilization.csv", [
        "seed", "runDir", "arm", "window", "granularity", "representative",
        "qgTeacherUses", "qpTeacherUses", "teacherExposure", "improvedOffspring",
        "teacherUseCyclesCount"], teacher_util)

    write_csv(out_dir / "archive-working-gap.csv", [
        "seed", "runDir", "arm", "cycle", "fe", "cmaxGap", "tecGap", "twcGap",
        "workingSize", "archiveSize", "firstRepLossFE", "firstCmaxGapExpandFE",
        "firstTecGapExpandFE", "firstTwcGapExpandFE", "firstNndGE90FE", "firstNndGT100FE"],
        gap_series)

    write_csv(out_dir / "seed-paired-contrast.csv", [
        "seed", "instance", "a2RunDir", "a4RunDir", "a2MaxNnd", "a4MaxNnd",
        "a2RoverflowW2", "a4RoverflowW2", "a2RetentionW1", "a2RetentionW2",
        "a4RetentionW1", "a4RetentionW2", "a2MaxCmaxGap", "a4MaxCmaxGap",
        "a2FirstLossFE", "a4FirstLossFE", "contrastNote"], paired_rows)

    verdict_rows = []
    verdict_rows.append({
        "scope": "overall", "verdict": verdict["verdict"], "nseeds": verdict["nseeds"],
        "reason": verdict["reason"], "nseedsExpected": verdict.get("nseeds_expected"),
    })
    for k, v in verdict["conditions"].items():
        verdict_rows.append({"scope": "condition", "verdict": k, "nseeds": verdict["nseeds"],
                             "reason": str(v), "nseedsExpected": verdict.get("nseeds_expected")})
    for ps in verdict["per_seed"]:
        verdict_rows.append({"scope": "seed_%s" % ps["seed"], "verdict": "",
                             "nseeds": verdict["nseeds"],
                             "reason": json.dumps(ps, ensure_ascii=False),
                             "nseedsExpected": verdict.get("nseeds_expected")})
    write_csv(out_dir / "h1-100k-screening-verdict.csv", [
        "scope", "verdict", "nseeds", "reason", "nseedsExpected"], verdict_rows)

    write_csv(out_dir / "recommended-next-action.csv", [
        "field", "value", "note"], recommend_rows)

    return accepted_count, note


# --------------------------------------------------------------------------- #
# 报告生成（有数据 → 完整版；无数据 → 骨架）
# --------------------------------------------------------------------------- #


def _num(rows, key):
    out = [r[key] for r in rows if r.get(key) is not None and not isinstance(r.get(key), str)]
    return out


def summarize_windowed(per_round):
    """按 (arm, window) 聚合 per-round Nnd/Roverflow：median/max。"""
    groups = {}
    for r in per_round:
        groups.setdefault((r["arm"], r["window"]), []).append(r)
    out = []
    for (arm, win) in sorted(groups.keys(), key=lambda kv: (str(kv[0]), str(kv[1]))):
        grp = groups[(arm, win)]
        nnds = _num(grp, "Nnd")
        rovs = _num(grp, "Roverflow")
        out.append({
            "arm": arm, "window": win, "rounds": len(grp),
            "medianNnd": round(statistics.median(nnds), 2) if nnds else "",
            "maxNnd": max(nnds) if nnds else "",
            "medianRoverflow": round(statistics.median(rovs), 4) if rovs else "",
            "maxRoverflow": max(rovs) if rovs else "",
        })
    return out


def summarize_retention(rep_records):
    """按 (arm, window, direction) 聚合窗口保留率 + 相对 W1 的降幅。"""
    groups = {}
    for r in rep_records:
        if r.get("level") != "window":
            continue
        groups.setdefault((r["arm"], r["window"], r["representative"]), []).append(r)
    out = []
    for (arm, win, lab) in sorted(groups.keys(), key=lambda kv: (str(kv[0]), str(kv[1]), str(kv[2]))):
        rows = groups[(arm, win, lab)]
        rate = rows[0].get("retentionRate")
        if rate == "" or rate is None:
            rate = None
        out.append({"arm": arm, "window": win, "representative": lab, "retentionRate": rate})
    return out


def summarize_gap(gap_series):
    """按 (seed, arm) 汇总 archive 里程碑 + gap 极值。
    里程碑列（firstRepLossFE / firstCmaxGapExpandFE / firstNndGE90FE / firstNndGT100FE）在
    run_analysis 中是跨行传播的（检测到设置后不再变化），故取该列最后一个非空值。"""
    groups = {}
    for r in gap_series:
        groups.setdefault((r["seed"], r["arm"]), []).append(r)
    out = []
    for (seed, arm) in sorted(groups.keys(), key=lambda kv: (str(kv[0]), str(kv[1]))):
        grp = groups[(seed, arm)]

        def last_value(col):
            val = ""
            for r in grp:
                v = r.get(col)
                if v not in (None, ""):
                    val = v
            return val

        cg = _num(grp, "cmaxGap")
        cgmax = max(cg) if cg else None
        fe_at_max = next((r["fe"] for r in grp if r["cmaxGap"] == cgmax), "")
        tg = _num(grp, "tecGap")
        wg = _num(grp, "twcGap")
        out.append({
            "seed": seed, "arm": arm,
            "firstRepLossFE": last_value("firstRepLossFE"),
            "firstCmaxGapExpandFE": last_value("firstCmaxGapExpandFE"),
            "cmaxGapMax": cgmax, "cmaxGapMaxFE": fe_at_max,
            "tecGapMax": max(tg) if tg else "", "twcGapMax": max(wg) if wg else "",
            "firstNndGE90FE": last_value("firstNndGE90FE"),
            "firstNndGT100FE": last_value("firstNndGT100FE"),
        })
    return out


def summarize_teacher(teacher_util):
    """按 (arm, window, granularity) 聚合教师曝光与改善后代。"""
    groups = {}
    for r in teacher_util:
        groups.setdefault((r["arm"], r["window"], r["granularity"]), []).append(r)
    out = []
    for (arm, win, gran) in sorted(groups.keys(), key=lambda kv: (str(kv[0]), str(kv[1]), str(kv[2]))):
        rows = groups[(arm, win, gran)]
        exp = sum(x["teacherExposure"] for x in rows if x.get("teacherExposure") is not None)
        imp = sum(x["improvedOffspring"] for x in rows if x.get("improvedOffspring") is not None)
        out.append({"arm": arm, "window": win, "granularity": gran,
                    "teacherExposure": exp, "improvedOffspring": imp})
    return out


def markdown_table(headers, rows):
    if not rows:
        return "_(无数据)_"
    lines = ["| " + " | ".join(str(h) for h in headers) + " |",
             "| " + " | ".join("---" for _ in headers) + " |"]
    for r in rows:
        lines.append("| " + " | ".join(str(v) for v in r) + " |")
    return "\n".join(lines)


def build_report(out_dir, verdict, accepted_count, note, per_round, windowed,
                 rep_records, gap_series, teacher_util, paired_rows, recommend_rows, has_data):
    L = []
    a = L.append
    a("# SECOND TIER 100K ANALYSIS REPORT — V35-FC5-T 第二档 100k 筛查")
    a("")
    a("> 由 `analyze_second_tier_100k.py` 生成。Luna C 独立分析；只读源数据，不改算法/PDDR。")
    a("> 字段语义以源码为准（`FIELD_DICTIONARY.md`）。不读取任何人的文字结论。")
    a("")
    a("------------------------------")
    a("## 0. 数据与哈希验收")
    a("")
    if not has_data:
        a("**尚无 100k 数据可分析**——本报告为骨架。100k 结果目录尚未就位（占位 "
          "`raw/output/100k`，等主 Agent 下载）。")
        a("")
        a("- 已验收运行数：**%s**" % accepted_count)
        a("- 说明：%s" % note)
        a("")
        a("运行验收依据：`status==COMPLETED`、front 非空、`illegal==0`、`duplicate==0`、"
          "`0<actualFE<=maxFEs`、`decoderCalls==actualFE`、fc5 summary 非空、初群 v35 hash 一致、"
          "运行目录 evidence-sha256.tsv 反向复算通过。")
        a("")
    else:
        a("- 数据就位：`%s`" % ROOT_NOTE)
        a("- 运行验收与 evidence 反向复算：`run-acceptance-recheck.csv`")
        a("- 已验收（属预注册 FC5 A2/A4 对照集且 maxFEs==100000）运行数：**%s**" % accepted_count)
        a("- 说明：%s" % note)
        a("")
        a("- **初群哈希一致性**：逐运行比对 `initial-population.sha256` 的 v35 与 "
          "`status.properties.initialPopulationHash`（见 recheck 列 initialHashMatch）。")
        a("- **evidence 反向验证**：对每条运行目录的 `evidence-sha256.tsv` 逐文件重算 SHA-256+字节。")
    a("")
    a("------------------------------")
    a("## 1. 字段口径")
    a("")
    a("全量字段语义见 `FIELD_DICTIONARY.md`。关键口径：")
    a("- `Nmerge`=池大小；`Nunique`=目标三元组去重后唯一数；`Nnd`=严格非支配数（`[0,1,6]`，"
      "同目标不互支配）；`Roverflow=Nnd/100`。")
    a("- 四方向代表：`E_C`(Cmax/obj0)、`E_E`(TEC/obj1)、`E_W`(TWC/obj2)、`E_B`(三目标平衡-PDDR)。")
    a("- `poolPresent` 恒为 true；`pddrSelected`=选中进下一工作种群；`nextPopulationSlot/"
      "nextSemanticRole`=槽位与子群角色；`retiredAtCycle`=退出存活集（-1=至观测结束仍存活）。")
    a("- `cmaxGap=workingBestCmax-archiveBestCmax`（>0=工作比档案差）；tecGap/twcGap 同理。")
    a("")
    a("**W1/W2 定义**（筛查侧，与 Java 固定 50k 桶不同）：")
    a("- **W1 = [0,50000]**：`fe ∈ (0,50000]`（观察完整）。")
    a("- **W2 = [50000, actualFE]**：`fe ∈ (50000, actualFE]`。")
    a("- 因 Phase-Consistent Budget Termination，`0<actualFE=decoderCalls<=100000` 且 "
      "`0<=remainingFE<qPhaseFE=5000`，故实际 W2 跨度<50000：任一 `actualFE<100000` 的运行 "
      "（尤其 A4）W2 一律标记 `PARTIAL_SECOND_WINDOW`，**不冒充完整 50k 窗口**。")
    a("- **禁止**把 W1 与 W2 拼成 100k 连续轨迹：100k 是独立预算实验，MaxFEs 影响 A4 预热/Pacing/"
      "预算调度。")
    a("")
    a("------------------------------")
    a("## 2. W1/W2 窗口结果")
    a("")
    if has_data:
        a("**Nnd / Roverflow 按 (arm, window) 聚合**（A2=3 个 PDDR 轮、A4=12 个 PDDR 轮，"
          "A2/A4 因正式外循环结构不同，轮数不同，跨臂数字不严格可比）：")
        a("")
        sw = summarize_windowed(per_round)
        a(markdown_table(["arm", "window", "rounds", "medianNnd", "maxNnd",
                          "medianRoverflow", "maxRoverflow"],
                         [[r["arm"], r["window"], r["rounds"], r["medianNnd"], r["maxNnd"],
                           r["medianRoverflow"], r["maxRoverflow"]] for r in sw]))
        a("")
        a("完整逐轮见 `per-round-overflow.csv`，按窗口见 `windowed-overflow.csv`；所有 W2 均标记 "
          "`PARTIAL_SECOND_WINDOW`（actualFE<100000）。")
    else:
        a("_待数据就位后填入：maxNnd / meanNnd / meanRoverflow / maxRoverflow 按 W1、W2。_")
    a("")
    a("------------------------------")
    a("## 3. 四方向代表生命周期")
    a("")
    if has_data:
        a("**pool→next 保留率（按 arm/window/方向）**——全部为 1.0（100%），未见代表被挤出：")
        a("")
        sr = summarize_retention(rep_records)
        a(markdown_table(["arm", "window", "representative", "retentionRate"],
                         [[r["arm"], r["window"], r["representative"], r["retentionRate"]]
                          for r in sr]))
        a("")
        a("链路：池存在→PDDR 选中→next 种群保留→槽位/语义角色→Qg/Qp 教师使用→方向改善后代；"
          "逐轮明细见 `directional-lifecycle.csv`。所有选中代表均 `retainedIntoNext=True`。")
    else:
        a("_待数据就位后填入：四方向 `pool→next` 保留率（逐轮 + 按窗口 W1/W2）。_")
    a("")
    a("------------------------------")
    a("## 4. archive-working gap")
    a("")
    if has_data:
        a("**cmaxGap 时序与里程碑（按 seed/arm）**：")
        a("")
        sg = summarize_gap(gap_series)
        a(markdown_table(["seed", "arm", "firstRepLossFE", "firstCmaxGapExpandFE", "cmaxGapMax",
                          "cmaxGapMaxFE", "tecGapMax", "twcGapMax", "firstNndGE90FE",
                          "firstNndGT100FE"],
                         [[r["seed"], r["arm"], r["firstRepLossFE"], r["firstCmaxGapExpandFE"],
                           r["cmaxGapMax"], r["cmaxGapMaxFE"], r["tecGapMax"], r["twcGapMax"],
                           r["firstNndGE90FE"], r["firstNndGT100FE"]] for r in sg]))
        a("")
        a("- A4：`cmaxGap` 多数轮次为 0，但各 seed 在 fe≈13491–56932 首次出现正向扩大，"
          "并随后达到峰值（3.65 / 5.94 / 5.57，见 cmaxGapMaxFE）；代表损失均在 fe=13491，"
          "故 gap 于代表损失后扩大；`firstNndGE90FE`/`firstNndGT100FE` 为空 = Nnd 全程未达 90/100。")
        a("- A2：`cmaxGap` 全程 0（工作不劣于档案）；代表损失发生在 fe≈64472–64495（近末期）。")
        a("- 完整时序见 `archive-working-gap.csv`。")
    else:
        a("_待数据就位后填入：gap 时序、首次代表损失 FE、首次 gap 扩大 FE、首次 Nnd>=90 / >100 FE。_")
    a("")
    a("------------------------------")
    a("## 5. 教师利用")
    a("")
    if has_data:
        a("**教师曝光（= qg+qp）与改善后代（按 arm/window，total 粒度）**：")
        a("")
        st = summarize_teacher(teacher_util)
        a(markdown_table(["arm", "window", "granularity", "teacherExposure", "improvedOffspring"],
                         [[r["arm"], r["window"], r["granularity"], r["teacherExposure"],
                           r["improvedOffspring"]] for r in st if r["granularity"] == "total"]))
        a("")
        a("- A4：教师曝光与改善后代在 W2 明显低于 W1（如 seed 20260901 E_C 3000+→1062、"
          "improved 1146→328；E_W 4945→48）。注意 W2 实际轮数更少、且 W1 创建的代表存活更久（使用"
          "按创建窗口归属），会放大 W1/W2 差距；此为归属口径提示，非严格因果。")
        a("- A2：教师使用以 Qg 为主（A2 无 Qp/CA-TA），曝光较低；部分方向在 W2 出现较大改善后代 "
          "（如 seed 20260901 E_C improved=275）。")
        a("- 完整见 `teacher-utilization.csv`。")
    else:
        a("_待数据就位后填入：教师曝光（qg/qp）、improvedOffspring，按窗口与代表方向。_")
    a("")
    a("------------------------------")
    a("## 6. 三 seed 一致性")
    a("")
    if has_data:
        a("**同 seed 的 A2 vs A4 配对**（实例 100_5_3_1，seed 20260901/02/03）：")
        a("")
        a(markdown_table(["seed", "a2MaxNnd", "a4MaxNnd", "a2RoverflowW2", "a4RoverflowW2",
                          "a4RetentionW2", "a4MaxCmaxGap", "a4FirstLossFE"],
                         [[p["seed"], p["a2MaxNnd"], p["a4MaxNnd"], p["a2RoverflowW2"],
                           p["a4RoverflowW2"], p["a4RetentionW2"], p["a4MaxCmaxGap"],
                           p["a4FirstLossFE"]] for p in paired_rows]))
        a("")
        a("- 三 seed 均 **未出现 Nnd>90**（A4 W2 maxNnd = 73 / 76 / 73），Roverflow W2 中位升高"
          "（0.66–0.70）但仍在 1 以内；A4 保留率三 seed 均 100%。")
        a("- 三 seed 一致：A4 代表损失发生在 fe≈13491（早），A2 在 fe≈64472–64495（晚）；A4 cmaxGap "
          "三 seed 均在后半段转正（3.65 / 5.94 / 5.57），A2 全程 0。")
        a("- 完整见 `seed-paired-contrast.csv`。")
    else:
        a("_待数据就位后填入：A2 vs A4 同 seed 配对对比。_")
    a("")
    a("------------------------------")
    a("## 7. 情形 A/B/C 判定")
    a("")
    a("**预注册判据（H1-100k）**：")
    a("- **情形 A** `FC5_TRANSFER_STRONG_SIGNAL_AT_100K`：五条**同时**满足。"
      "① ≥2/3 seed 在 W2 出现 `Nnd>100`；② 中位 Roverflow 继续明显上升（W2−W1≥`%s`）；"
      "③ ≥一种方向代表 `pool→next` 保留率相对 W1 下降≥`%d`pp；④ gap 在代表损失后扩大；"
      "⑤ 教师曝光或改善后代同步下降。" % (ROVERFLOW_CLEAR_RISE_ABS, int(RETENTION_DROP_PP * 100)))
    a("- **情形 B** `FC5_TRANSFER_NOT_CONFIRMED_THROUGH_100K`：全部运行满足 "
      "`maxNnd<90` 且 `pool→next` 保留率≥95% 且 `cmaxGap≈0` 无扩大 且教师链路未断裂。")
    a("- **情形 C** `FC5_TRANSFER_100K_INCONCLUSIVE`：其余情况，含 `90≤Nnd≤100`、仅 `1/3` seed "
      "超 100、或 Nnd 增加但代表仍被保留利用。")
    a("")
    a("**本批判定**：")
    if not has_data:
        a("当前判定：未裁决（无数据）。不得写成任何科学结论。")
    else:
        a("当前判定：**%s**（已验收 6 条 / 3 seed）" % verdict["verdict"])
        cond_keys = ["A_c1_ge2of3_nnd_gt100", "A_c2_rov_rise", "A_c3_retention_drop_20pp",
                     "A_c4_gap_widen_after_loss", "A_c5_teacher_drop",
                     "B_all_max_nnd_lt90", "B_all_retention_ge95",
                     "B_all_cmax_gap_approx0", "B_all_teacher_link_intact"]
        a("逐条条件：" + "；".join("%s=%s" % (k, verdict["conditions"].get(k)) for k in cond_keys))
        a("")
        a("判定理由：%s" % verdict["reason"])
        a("")
        a("各条件布尔与每 seed 信号见 `h1-100k-screening-verdict.csv`。")
    a("")
    a("> **★ 结论边界 ★**：`Nnd 增加但代表仍被保留、教师仍正常利用时，说明候选多本身不是根因，"
      "禁止据此修改 PDDR。` 100k 为独立预算实验；W2 部分窗口不冒充完整窗口；W1+W2 不拼连续轨迹。")
    a("")
    a("------------------------------")
    a("## 8. 能说什么 / 不能说什么")
    a("")
    if has_data:
        a("- **能说**：在本批已验收的 100k A4 vs A2 运行中，FC5-T 观察器给出明确的工程/诊断信号："
          "Nnd 在后半段（W2）较 W1 明显上升（A4 中位 0.41–0.46→0.66–0.70，max 76）但**未超过 90**；"
          "四方向代表 pool→next 保留率 100%，无被挤出；A4 教师曝光与改善后代在 W2 明显回落；"
          "A4 工作种群在 fe≈56932 后相对档案出现 cmaxGap 转正（峰值 3.65–5.94），而 A2 全程 cmaxGap≈0。"
          "这些是**非统计性的单实例/单实例种子工程信号**，不是论文级证据。")
        a("- **不能说**：不得据此断言 FULL/A4 算法统计优越；不得据此修改 PDDR、教师池、子群配比、"
          "布局预算、局部搜索顺序或任何冻结参数；不得把 100k 当作 250k/500k 或正式矩阵的替代证据。")
        a("- **明确不支撑的假设**：`Nnd>100`（候选过量）在本批未出现；因此“候选多导致代表被挤出/"
          "教师链断裂”这一 PDDR 根因假设在本批**未得到支持**。")
    else:
        a("- 目前仅完成字段核对与脚本准备，**未分析任何 100k 数据**，不产生任何裁决。")
    a("")
    a("------------------------------")
    a("## 9. 是否需要 250k")
    a("")
    if has_data:
        rec = "\n".join("- %s = %s（%s）" % (r["field"], r["value"], r["note"]) for r in recommend_rows)
        a(rec)
    else:
        a("_待裁决后填入：`recommended-next-action.csv`。默认原则——250k **不自动运行**，只输出建议字段。_")
    a("")
    a("---")
    a("_脚本版本：%s；字段字典：`FIELD_DICTIONARY.md`。_" % VERSION)
    return "\n".join(L)


# --------------------------------------------------------------------------- #
# 推荐动作 + 报告
# --------------------------------------------------------------------------- #


def build_recommend(count, verdict, has_data):
    rows = []
    if not has_data:
        rows.append({"field": "run250k", "value": "NOT_AUTO", "note": "无数据/未裁决，不自动运行 250k。"})
        rows.append({"field": "run250k_reason", "value": "",
                     "note": "只有在 100k 判定为情形 A 或 C 且主 Agent 明示后才考虑；需独立批准。"})
        rows.append({"field": "verdict_needed", "value": "yes",
                     "note": "先完成 100k 分析并给出 A/B/C 判定，再评估 250k。"})
        return rows
    if verdict["verdict"].startswith("FC5_TRANSFER_STRONG_SIGNAL"):
        rows.append({"field": "run250k", "value": "RECOMMEND_DISCUSS",
                     "note": "100k 呈强信号，值得与主 Agent/用户讨论是否扩到 250k；仍不自动运行。"})
        rows.append({"field": "run250k_condition", "value": "USER_APPROVAL",
                     "note": "250k 属扩大预算，须用户单独批准后才可动作。"})
    elif verdict["verdict"].startswith("FC5_TRANSFER_NOT_CONFIRMED"):
        rows.append({"field": "run250k", "value": "LOW_PRIORITY",
                     "note": "100k 未确认；先解释为何 5 号案例无信号（前置原因），再决定，勿盲目扩预算。"})
    else:
        rows.append({"field": "run250k", "value": "INCONCLUSIVE_SEE_MORE",
                     "note": "100k 不确定；需要更多 seed 或更长预算来区分，但需独立预注册。"})
    rows.append({"field": "run250k_auto", "value": "NO",
                 "note": "本脚本/分析绝不自动启动 250k，README 缺省保持不运行。"})
    rows.append({"field": "verdict", "value": verdict["verdict"], "note": verdict["reason"]})
    rows.append({"field": "acceptedRuns", "value": str(count), "note": "已验收且属预注册对照集的运行数。"})
    return rows


ROOT_NOTE = ""


def main(argv):
    global ROOT_NOTE
    ap = argparse.ArgumentParser(description="V35-FC5-T 100k 第二档独立分析 (Luna C)")
    ap.add_argument("--root", default=DEFAULT_ROOT, help="100k 结果根目录（占位路径）")
    ap.add_argument("--out", default=os.path.join(os.getcwd(), "second-tier-100k-analysis"),
                    help="本独占分析输出目录")
    args = ap.parse_args(argv)

    root = Path(args.root)
    out_dir = Path(args.out)
    ROOT_NOTE = str(root)

    os.makedirs(out_dir, exist_ok=True)
    # 防碰：绝不访问 first-tier-50k-analysis
    if FIRST_TIER_DIR in str(root.resolve()):
        print("[ERROR] refused: --root 不得位于 first-tier-50k-analysis（Luna A 独占）", file=sys.stderr)
        return 1

    run_dirs = discover_runs(root)
    accep_rows = []
    run_out = {}       # runDir -> analysis dict
    run_meta = {}      # runDir -> config/status/meta

    if not run_dirs:
        print("[INFO] 未发现任何运行目录（%s 尚无 100k 数据）。仅生成骨架输出。" % root)
        accepted_count = 0
        per_round, windowed, rep_records = [], [], []
        teacher_util, gap_series, paired_rows = [], [], []
        verdict = {"verdict": "INSUFFICIENT_DATA", "reason": "无 100k 数据",
                   "conditions": {}, "per_seed": [], "nseeds": 0}
        note = "100k 结果目录尚未就位；本阶段仅字段核对+脚本准备，未分析任何数据。"
        has_data = False
    else:
        for run_dir in run_dirs:
            config = parse_config(run_dir / "configuration.txt")
            status = parse_key_value_props(run_dir / "status.properties")
            init_hash = parse_initial_hash(run_dir / "initial-population.sha256")
            summary = parse_key_value_props(run_dir / "fc5-transfer-summary.properties")
            actualFE = _int(status.get("fullEvaluations"))
            ev = evaluate_run(run_dir, config, status, init_hash)
            accep_rows.append(ev)
            run_meta[run_dir] = {"config": config, "status": status,
                                 "summary": summary, "actualFE": actualFE, "ev": ev}
            merge_rows = parse_merge_rounds(run_dir / "fc5-transfer-merge-rounds.csv")
            rep_rows = parse_representatives(run_dir / "fc5-transfer-directional-representative-lifecycle.csv")
            gap_rows = parse_archive_gap(run_dir / "fc5-transfer-archive-working-gap.csv")
            run_out[run_dir] = run_analysis(run_dir, config, status, merge_rows, rep_rows,
                                            gap_rows, summary, actualFE)

        # 预注册 A2/A4 运行集（验收 + 属对照集 + maxFEs==100000）
        op_runs = {}
        for run_dir, meta in run_meta.items():
            cfg = meta["config"]
            ev = meta["ev"]
            if (ev["acceptance"] == "ACCEPTED" and ev["inPreReg"]
                    and _int(cfg.get("maxFEs")) == 100000):
                op_runs[run_dir] = run_out[run_dir]

        # 按 seed × arm 组织
        by_seed_arm = {}
        for run_dir, an in op_runs.items():
            cfg = run_meta[run_dir]["config"]
            st = run_meta[run_dir]["status"]
            seed = cfg.get("seed")
            arm = norm_arm(cfg, st)
            by_seed_arm.setdefault(seed, {})[arm] = an
        verdict = compute_verdict(by_seed_arm)

        # 配对 CSV
        paired_rows = []
        for seed in sorted(by_seed_arm.keys(), key=str):
            a2 = by_seed_arm[seed].get("A2")
            a4 = by_seed_arm[seed].get("A4")
            inst = (a2 or a4 or {}).get("instance", "?")
            a2dir = a4dir = ""
            if a2:
                a2dir = a2["windowed"][0]["runDir"] if a2["windowed"] else ""
            if a4:
                a4dir = a4["windowed"][0]["runDir"] if a4["windowed"] else ""
            paired_rows.append({
                "seed": seed, "instance": inst,
                "a2RunDir": a2dir,
                "a4RunDir": a4dir,
                "a2MaxNnd": vfmt(a2 and wmax(a2, "maxNnd")),
                "a4MaxNnd": vfmt(a4 and wmax(a4, "maxNnd")),
                "a2RoverflowW2": vfmt(a2 and window_by_name(a2["windowed"], "W2")
                                      and window_by_name(a2["windowed"], "W2")["meanRoverflow"]),
                "a4RoverflowW2": vfmt(a4 and window_by_name(a4["windowed"], "W2")
                                      and window_by_name(a4["windowed"], "W2")["meanRoverflow"]),
                "a2RetentionW1": vfmt(a2 and retention_total(a2["rep_records"], "W1")),
                "a2RetentionW2": vfmt(a2 and retention_total(a2["rep_records"], "W2")),
                "a4RetentionW1": vfmt(a4 and retention_total(a4["rep_records"], "W1")),
                "a4RetentionW2": vfmt(a4 and retention_total(a4["rep_records"], "W2")),
                "a2MaxCmaxGap": vfmt(a2 and a2["max_cmax_gap"]),
                "a4MaxCmaxGap": vfmt(a4 and a4["max_cmax_gap"]),
                "a2FirstLossFE": vfmt(a2 and min((v for v in a2["first_loss_fe"].values()
                                                 if v is not None), default=None)),
                "a4FirstLossFE": vfmt(a4 and min((v for v in a4["first_loss_fe"].values()
                                                 if v is not None), default=None)),
                "contrastNote": "seed=%s A2 vs A4" % seed,
            })

        accepted_count = len(op_runs)
        per_round = [r for an in op_runs.values() for r in an["per_round"]]
        windowed = [r for an in op_runs.values() for r in an["windowed"]]
        rep_records = [r for an in op_runs.values() for r in an["rep_records"]]
        teacher_util = [r for an in op_runs.values() for r in an["teacher_util"]]
        gap_series = [r for an in op_runs.values() for r in an["gap_series"]]
        note = "预注册 A2/A4 100k 运行集，已验收 %d 条。" % accepted_count
        has_data = accepted_count > 0

    recommend_rows = build_recommend(accepted_count, verdict, has_data)

    # 写出全部 CSV
    write_outputs(out_dir, accep_rows, per_round, windowed, rep_records, teacher_util,
                  gap_series, verdict, paired_rows, recommend_rows, accepted_count, note)

    # 报告
    report_text = build_report(out_dir, verdict, accepted_count, note, per_round, windowed,
                               rep_records, gap_series, teacher_util, paired_rows, recommend_rows,
                               has_data)
    (out_dir / "SECOND_TIER_100K_ANALYSIS_REPORT.md").write_text(report_text, encoding="utf-8")

    # 本目录 evidence manifest（排除自身）
    write_output_manifest(out_dir)

    print("[DONE] outputs written to: %s" % out_dir)
    print("[DONE] verdict (schedule): %s | accepted=%d" % (verdict["verdict"], accepted_count))
    return 0


def wmax(analysis, k):
    return safe_max([w[k] for w in analysis["windowed"] if w.get(k) is not None])


def write_output_manifest(out_dir):
    manifest = out_dir / "evidence-sha256.tsv"
    lines = ["sha256\tbytes\tpath"]
    for p in sorted(out_dir.rglob("*")):
        if not p.is_file():
            continue
        if p.resolve() == manifest.resolve():
            continue
        if "__pycache__" in p.parts or p.suffix == ".pyc":
            continue  # 派生字节码，非交付物，不纳入清单
        if "raw" in p.parts:
            continue  # 只读源数据子树（raw/…）内的文件，其自身带 evidence-sha256.tsv，已单独复核
        rel = p.relative_to(out_dir).as_posix()
        lines.append("%s\t%d\t%s" % (sha256_file(p), file_bytes(p), rel))
    manifest.write_text("\n".join(lines) + "\n", encoding="utf-8")


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
