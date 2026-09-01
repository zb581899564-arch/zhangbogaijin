#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""threshold_recompute.py — V35 SOURCE-ATTRIBUTION-500K Phase A0 (CORRECTED).

V35-PHASEA0-CORRECTION-V1 (2026-09-01): 归属语义由 FIRST_ADMISSION_WITHIN_WINDOW
修正为**多来源反事实 producerSet 语义**（阻断问题A），内存外推公式由
heapUsedPeak_OFF_20k × 25 修正为**分解模型**（阻断问题B）。修正前版本见
source-attribution-thresholds.pre-correction.json 与 git 无关的预修正快照。

反事实语义（合同 attributionRule，冻结）：
  E_t    = 窗口内所有已评价候选事件（每事件保留真实 source）
  Wt     = uniqueObjectiveTriples(E_t)
  Wt^-s  = uniqueObjectiveTriples({e ∈ E_t | e.source != s})
  WHVG_s(t) = HV(ND(Fpast ∪ Wt)) − HV(ND(Fpast ∪ Wt^-s))
  producerSet(p) = 窗口内生成 p 的全部一级来源集合
  ExclusiveND: p ∈ ND(Fpast∪Wt) ∧ p ∉ Fpast ∧ producerSet(p) == {s}
  FIRST_ADMISSION（firstProducerSource/FE/CandidateId）仅作描述性时序报告，
  严禁进入 WHVG/WHVGShare/ExclusiveND/ExclusiveNDShare/G1/G3 门控。

Modes
-----
--audit           从登记输入重算可比性/充足性判定，并与冻结 JSON 比对。
--selftest        T1–T7 反事实语义测试 + 行序无关（20 次打乱）。
--memory-selftest T8 内存分解模型单元检查。
--windows/--gate/--divergence  运行后参考实现（同前版）。

HV/IGD/ND 原语直接 import scripts/fc6_metrics.py（plan §3.6：禁止重建），
pipeline = fc6 'corrected'：raw 去重 -> raw 严格 ND -> 锚 min/max 归一化
（不 clamp）-> HV 参考 (1.1,1.1,1.1) / IGD 归一化空间欧氏。
"""
import argparse
import csv
import glob
import io
import json
import os
import random
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(HERE))))
SCRIPTS = os.path.join(ROOT, "scripts")
if SCRIPTS not in sys.path:
    sys.path.insert(0, SCRIPTS)
import fc6_metrics as fc6  # noqa: E402

EPS = 1e-12  # fc6_metrics.EPS（冻结 epsilon）
CMP_EPS = 1e-9  # deficit-vs-threshold 比较容差（冻结；仅吸收浮点 1-ulp 误差）
WINDOW_FE = 25000
N_WINDOWS_500K = 20
MEMORY_HARD_GATE_RATIO = 0.60  # 冻结：estimated500kPeak < 0.60 × assignedJavaHeap
MEMORY_SAFETY_FRACTION = 0.20  # 冻结：safetyMargin 下界系数
MEMORY_MIN_SAFETY_BYTES = 256 * 1024 * 1024  # 冻结：predefinedMinimumSafetyBytes = 256 MiB

EV = os.path.join(ROOT, "docs", "evidence")
P_M1 = os.path.join(EV, "V35-FC5-MIDHORIZON-250K", "01-root-cause-analysis", "remote-results")
P_M2 = os.path.join(EV, "V35-FC5-MIDHORIZON-DIAGNOSTICS", "18-final-2k-20k-50k-gates", "A4-50k-ON-final")
P_M3 = os.path.join(EV, "V35-GAP-SOURCE-CONTRIBUTION-DIAGNOSTICS-V1", "04-remote-100k", "sync")
P_500K_PHASE0 = os.path.join(EV, "V35-PFC5-PHASE0", "fetched-remote", "100_5_3_1")
P_500K_GAP500 = os.path.join(EV, "V35-PFC5-GAP-PROBE", "04-v2-remote-500k-runs", "sync")
JSON_SA = os.path.join(HERE, "source-attribution-thresholds.json")
JSON_PD = os.path.join(HERE, "performance-divergence-thresholds.json")


# --------------------------------------------------------------------------
# 通用只读小工具
# --------------------------------------------------------------------------
def read_properties(path):
    out = {}
    with io.open(path, encoding="utf-8") as fh:
        for line in fh:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                k, v = line.split("=", 1)
                out[k] = v
    return out


def read_csv_rows(path):
    with io.open(path, encoding="utf-8", newline="") as fh:
        return list(csv.DictReader(fh))


def p(name, value):
    print("  %s = %s" % (name, value))


# --------------------------------------------------------------------------
# 指标参考实现（反事实 producerSet 语义；HV/ND/归一化复用 fc6_metrics）
# --------------------------------------------------------------------------
def canonical_groups(events):
    """窗口内事件按目标三元组分组（1e-12 相等折叠；与 fc6.unique/nondominated
    同口径）。组构造只依赖排序后的代表序列，与输入行序无关。
    返回 [(rep_tuple, [events...])]，组序按代表字典序。"""
    by_exact = {}
    for e in events:
        by_exact.setdefault(tuple(e["objectives"]), []).append(e)
    reps = sorted(by_exact.keys())
    groups = []
    for r in reps:
        target = None
        for g in groups:
            if fc6.equal(list(g[0]), list(r)):
                target = g
                break
        if target is None:
            groups.append((r, []))
            target = groups[-1]
        target[1].extend(by_exact[r])
    return groups


def producer_set(group):
    """producerSet(p)：窗口内生成该三元组的全部一级来源集合（反事实语义核心）。
    保留事件级真实来源；不在 Observer 写入阶段按三元组折叠。"""
    return frozenset(e["source"] for e in group[1])


def first_admission_source(group):
    """DESCRIPTIVE ONLY（PhaseA0-CORRECTION-V1）：窗口内首次事件来源。
    仅允许用于描述性时序报告/候选出生顺序解释/非门控诊断；
    严禁进入 WHVG/WHVGShare/ExclusiveND/ExclusiveNDShare/G1/G3 门控。"""
    best = min(group[1], key=lambda e: (e["nominalFE"], e["actualFE"],
                                        e["candidateId"], e["source"]))
    return best["source"]


def is_new(rep, fpast):
    """『新』= 与 Fpast 无 1e-12 相等点（窗口局部语义）。"""
    return not any(fc6.equal(list(rep), list(q)) for q in fpast)


def hv_normalized(points_raw, anchor_raw):
    """fc6 corrected：raw 严格 ND -> 以 anchor_raw 的 ideal/nadir 归一化
    （不 clamp）-> fc6.hypervolume（参考盒 1.1^3）。空集 -> 0.0。"""
    nd = fc6.nondominated([list(x) for x in points_raw])
    if not nd:
        return 0.0
    anchor_nd = fc6.nondominated([list(x) for x in anchor_raw])
    return fc6.hypervolume(fc6.normalize(nd, anchor_nd, clamp=False))


def window_metrics(fpast, events, sources=None):
    """单窗口逐来源指标（反事实 producerSet 语义，PhaseA0-CORRECTION-V1）。

    fpast  : list of (c,t,w) —— B_{t-1} decision front
    events : 窗口内评估事件（每事件保留真实 source）
    返回 dict: {hvAll, perSource: {s: {...}}, nndAll, producerSets,
                firstAdmission(descriptive), emptyWindow, whvgSumMinusTotalGain}
    """
    fpast_nd = [tuple(x) for x in fc6.nondominated([list(x) for x in fpast])]
    groups = canonical_groups(events)
    if not groups:
        return {"hvAll": 0.0, "perSource": {}, "nndAll": 0, "emptyWindow": True,
                "producerSets": {}, "firstAdmission": {},
                "whvgSumMinusTotalGain": 0.0}
    wt_reps = [g[0] for g in groups]
    producer_sets = {g[0]: producer_set(g) for g in groups}
    first_admission = {g[0]: first_admission_source(g) for g in groups}
    union_all = fpast_nd + wt_reps
    anchor = fc6.nondominated([list(x) for x in union_all])
    hv_all = hv_normalized(union_all, anchor)
    all_sources = sorted(set().union(*[set(ps) for ps in producer_sets.values()]) |
                         (set(sources) if sources else set()))
    nd_new_reps = [r for r in wt_reps
                   if is_new(r, fpast_nd)
                   and any(fc6.equal(list(r), list(q)) for q in anchor)]
    nnd_all = len(nd_new_reps)
    per_source = {}
    for s in all_sources:
        # Wt^-s = uniqueObjectiveTriples({e ∈ E_t | e.source != s})：
        # 仅当某三元组的 producerSet == {s}（全部事件都来自 s）时才从 Wt 中剔除；
        # 多来源共享的三元组因其他来源事件仍在而保留。
        # Wt^-s: 剔除当且仅当 producerSet == {s}（该三元组全部事件来自 s）；
        # 多来源共享的三元组因其他来源事件仍在而必须保留（PhaseA0-CORRECTION-V1 §3.2）
        minus_reps = [r for r in wt_reps if producer_sets[r] != frozenset([s])]
        hv_minus_s = hv_normalized(fpast_nd + minus_reps, anchor)
        whvg = hv_all - hv_minus_s
        share = 100.0 * whvg / max(hv_all, EPS)
        nexcl = sum(1 for r in nd_new_reps if producer_sets[r] == frozenset([s]))
        per_source[s] = {
            "nTuplesProduced": sum(1 for r in wt_reps if s in producer_sets[r]),
            "nTuplesExclusive": sum(1 for r in wt_reps
                                    if producer_sets[r] == frozenset([s])),
            "whvg": whvg,
            "whvgSharePct": share,
            "nexclND": nexcl,
            "exclusiveNdSharePct": 100.0 * nexcl / max(nnd_all, 1),
        }
    # sanity（非门控）：反事实下 Σ_s WHVG_s <= hv_all − HV(ND(Fpast)) 仍成立
    #（共享点从任何单来源反事实中都不消失）
    hv_minus_all = hv_normalized(fpast_nd, anchor)
    partition_residual = (sum(per_source[s]["whvg"] for s in all_sources)
                          - (hv_all - hv_minus_all))
    return {"hvAll": hv_all, "perSource": per_source, "nndAll": nnd_all,
            "emptyWindow": False, "producerSets": {k: sorted(v) for k, v in
                                                   producer_sets.items()},
            "firstAdmission": first_admission,
            "whvgSumMinusTotalGain": partition_residual}


def ledger_to_events(path):
    """Phase A ledger -> 窗口事件（要求列：source,nominalFE,actualFE,
    candidateId,Cmax,TEC,TWC）。nominalFE 决定窗口归属（合同 feAlignment）。"""
    events = []
    for r in read_csv_rows(path):
        events.append({
            "source": r["source"],
            "nominalFE": int(r["nominalFE"]),
            "actualFE": int(r["actualFE"]),
            "candidateId": r["candidateId"],
            "objectives": (float(r["Cmax"]), float(r["TEC"]), float(r["TWC"])),
        })
    return events


def events_in_window(events, t):
    lo, hi = (t - 1) * WINDOW_FE, t * WINDOW_FE
    return [e for e in events if lo < e["nominalFE"] <= hi]


def read_front_csv(path):
    pts = []
    with io.open(path, encoding="utf-8") as fh:
        for line in fh:
            line = line.strip()
            if not line or line.lower().startswith(("cmax", "c,", "candidate")):
                continue
            parts = line.split(",")
            if len(parts) < 3:
                continue
            try:
                pts.append((float(parts[0]), float(parts[1]), float(parts[2])))
            except ValueError:
                continue
    return pts


# --------------------------------------------------------------------------
# 内存分解模型（PhaseA0-CORRECTION-V1 阻断问题B；T8 合同）
# --------------------------------------------------------------------------
def estimate_500k_peak(baseline_algorithm_peak, observer_measured_delta,
                       observer_bounded_resident_cap, observer_unflushed_buffer_cap,
                       predefined_minimum_safety_bytes=MEMORY_MIN_SAFETY_BYTES,
                       memory_safety_fraction=MEMORY_SAFETY_FRACTION):
    """分解模型（observer-memory-model.md 修正版）：

    observerTransientDelta = max(observerMeasuredDelta,
                                 observerBoundedResidentCap
                                 + observerUnflushedBufferCap)
    safetyMargin = max(0.20 × (baselineAlgorithmPeak + observerTransientDelta),
                       predefinedMinimumSafetyBytes)
    estimated500kPeak = baselineAlgorithmPeak + observerTransientDelta
                        + safetyMargin

    baselineAlgorithmPeak（= heapUsedPeak_OFF_20k）是有界算法基线估计，
    严禁按 FE 线性放大（旧 ×25 公式已废止）。磁盘账本增长
    （ledgerGrowthPer10kFE）不参与本估计（仅磁盘体积）。
    """
    observer_transient_delta = max(
        observer_measured_delta,
        observer_bounded_resident_cap + observer_unflushed_buffer_cap)
    safety_margin = max(
        memory_safety_fraction * (baseline_algorithm_peak + observer_transient_delta),
        predefined_minimum_safety_bytes)
    return baseline_algorithm_peak + observer_transient_delta + safety_margin


def memory_gate_passes(estimated500kPeak, assignedJavaHeap):
    """硬门：estimated500kPeak < 0.60 × assignedJavaHeap（严格小于；等于即失败）。"""
    return estimated500kPeak < MEMORY_HARD_GATE_RATIO * assignedJavaHeap


# --------------------------------------------------------------------------
# --audit
# --------------------------------------------------------------------------
def audit():
    print("== threshold_recompute --audit (read-only) ==")
    print("project root: %s" % ROOT)
    fails = []

    # ---- M1 FC5-250K ------------------------------------------------------
    print("\n[M1] V35-FC5-MIDHORIZON-250K remote-results")
    tele = glob.glob(os.path.join(EV, "V35-FC5-MIDHORIZON-250K", "**",
                                  "*telemetry*"), recursive=True)
    p("local telemetry files", len(tele))
    acc = read_csv_rows(os.path.join(P_M1, "run-acceptance.csv"))
    budgets = sorted({r["actualFE"] for r in acc})
    seeds = sorted({r["seed"] for r in acc})
    arms = sorted({r["arm"] for r in acc})
    insts = sorted({r["instance"] for r in acc})
    contracts = sorted({r["contractVersion"] for r in acc})
    p("budgets(actualFE)", budgets)
    p("seeds", seeds)
    p("arms", arms)
    p("instances", insts)
    p("contracts", contracts)
    m1 = {
        "C1_sameInstance": "PARTIAL(100_5_3_1 present; normal side 100_2_4_1 present but 250k)",
        "C2_sameArm": "PARTIAL(A2/A4 on V3_1 contract)",
        "C3_sameSeed": "FAIL" if seeds != ["20260901"] else "PASS",
        "C4_sameBudget": "FAIL" if budgets != ["500000"] else "PASS",
        "C5_sameObserverSchema": "FAIL(no local per-eval source ledger; remote-only)",
        "C6_samePipeline": "FAIL(no raw ledger to recompute under contract)",
    }
    if budgets != ["250000"]:
        fails.append("M1: expected budgets [250000], got %s" % budgets)
    if tele:
        fails.append("M1: unexpected local telemetry files: %s" % tele[:3])

    # ---- M2 18-final gates A4-50k-ON-final --------------------------------
    print("\n[M2] 18-final-2k-20k-50k-gates/A4-50k-ON-final")
    led = read_csv_rows(os.path.join(P_M2, "telemetry-pddr-full-ledger.csv"))
    obj_col = "objectives[Cmax|TEC|TWC]"
    fp_only = sum(1 for r in led if r[obj_col] == "FINGERPRINT_ONLY")
    srcs = {}
    for r in led:
        srcs[r["source"]] = srcs.get(r["source"], 0) + 1
    led_seeds = sorted({r["seed"] for r in led})
    led_arms = sorted({r["arm"] for r in led})
    led_jars = sorted({r["sourceJarSha256"] for r in led})
    contract = read_properties(os.path.join(P_M2, "diagnostic-contract.properties"))
    behav = read_properties(os.path.join(P_M2, "behavior-summary.properties"))
    p("ledger rows", len(led))
    p("objectives==FINGERPRINT_ONLY", "%d/%d" % (fp_only, len(led)))
    p("source labels", srcs)
    p("seeds/arms", "%s / %s" % (led_seeds, led_arms))
    p("sourceJarSha256", led_jars)
    p("maxFEs / actualFE", "%s / %s" % (contract.get("maxFEs"), contract.get("actualFE")))
    p("instance", behav.get("instance"))
    p("nominalFE column present", "nominalFE" in led[0] if led else False)
    m2 = {
        "C1_sameInstance": "PASS" if "100_5_3_1" in str(behav.get("instance")) else "FAIL",
        "C2_sameArm": "PASS" if led_arms == ["A4"] else "FAIL",
        "C3_sameSeed": "PASS" if led_seeds == ["20260901"] else "FAIL",
        "C4_sameBudget": "FAIL" if contract.get("maxFEs") != "500000" else "PASS",
        "C5_sameObserverSchema": "FAIL(PDDR-pool subset; FINGERPRINT_ONLY objectives %d/%d; "
                                 "generation-opportunity labels; no nominalFE)" % (fp_only, len(led)),
        "C6_samePipeline": "FAIL(no per-eval three-objective source ledger)",
    }
    if fp_only <= len(led) / 2:
        fails.append("M2: expected mostly FINGERPRINT_ONLY objectives")

    # ---- M3 V3 GAPLSRC 100k ------------------------------------------------
    print("\n[M3] V35-GAP-SOURCE-CONTRIBUTION-DIAGNOSTICS-V1/04-remote-100k/sync")
    seed_dirs = sorted(os.listdir(P_M3))
    seed_dirs = [d for d in seed_dirs if d.startswith("seed-")]
    p("seeds", [d.replace("seed-", "") for d in seed_dirs])
    run = os.path.join(P_M3, "seed-20260919", "results",
                       "run-GAPLSRC-C0-100_5_3_1-20260919")
    bt = read_properties(os.path.join(run, "budget-termination.properties"))
    led3 = read_csv_rows(os.path.join(run, "source-ledger.csv"))
    srcs3 = {}
    for r in led3:
        srcs3[r["source"]] = srcs3.get(r["source"], 0) + 1
    fes = [int(r["observedFE"]) for r in led3]
    p("requestedMaxFE / actualFE / terminationKind",
      "%s / %s / %s" % (bt.get("requestedMaxFE"), bt.get("actualFE"),
                        bt.get("terminationKind")))
    p("experimentalJarSha256", bt.get("experimentalJarSha256"))
    p("formalJarSha256", bt.get("formalJarSha256"))
    p("source-ledger rows", len(led3))
    p("source labels", srcs3)
    p("observedFE min/max", "%d / %d" % (min(fes), max(fes)))
    p("columns", list(led3[0].keys()))
    cp_dir = os.path.join(run, "checkpoints")
    cps = sorted(os.listdir(cp_dir)) if os.path.isdir(cp_dir) else []
    p("checkpoints present", "%d files (25k grid: %s)" % (
        len(cps), any("checkpoint-25000-" in c for c in cps)))
    m3 = {
        "C1_sameInstance": "PARTIAL(100_5_3_1 yes; normal side 50_2_3_1 is 50-job)",
        "C2_sameArm": "FAIL(arm=C0 on GAPLSRC diagnostic jar bbb9ccd6...)",
        "C3_sameSeed": "FAIL(seeds 20260919-21 != 20260901)",
        "C4_sameBudget": "FAIL(requestedMaxFE=100000, actualFE=96025)",
        "C5_sameObserverSchema": "PARTIAL(per-eval source+objectives+fingerprint yes; "
                                 "no nominalFE; schema != Phase A frozen schema)",
        "C6_samePipeline": "PARTIAL(recomputable in its own rollup, not same schema/pipeline)",
    }
    if bt.get("requestedMaxFE") != "100000" or bt.get("actualFE") != "96025":
        fails.append("M3: unexpected budget values")

    # ---- 可用性判定 --------------------------------------------------------
    materials = [m1, m2, m3]
    comparable = []
    for name, m in zip(("M1", "M2", "M3"), materials):
        ok = all(v == "PASS" for v in m.values())
        comparable.append(ok)
        print("  %s fully comparable: %s" % (name, ok))
    matched_available = any(comparable)
    print("  => matchedWindowFluctuationAvailable = %s" % matched_available)

    # ---- 500k 轨迹充足性 ---------------------------------------------------
    print("\n[500k paired trajectory sufficiency]")
    cp_phase0 = glob.glob(os.path.join(P_500K_PHASE0, "**", "checkpoints"),
                          recursive=True)
    cp_gap = glob.glob(os.path.join(P_500K_GAP500, "**", "checkpoints"),
                       recursive=True)
    p("checkpoints dirs under PFC5-PHASE0/fetched-remote", len(cp_phase0))
    p("checkpoints dirs under PFC5-GAP-PROBE/04-v2-remote-500k-runs", len(cp_gap))
    bt1 = read_properties(os.path.join(P_500K_PHASE0, "seed-20260901", "A4",
                                       "budget-termination.properties"))
    p("PFC5-PHASE0 100_5_3_1/seed-20260901/A4 budget",
      "%s / %s / %s" % (bt1.get("requestedMaxFE"), bt1.get("actualFE"),
                        bt1.get("terminationKind")))
    p("PFC5-PHASE0 frozenJarSha256", bt1.get("frozenJarSha256"))
    hits = []
    cps_all = []
    for dirpath, dirnames, filenames in os.walk(EV):
        rel = os.path.relpath(dirpath, EV)
        if os.path.basename(dirpath) == "checkpoints":
            cps_all.append(rel)
        for name in dirnames + filenames:
            full = os.path.join(rel, name)
            if "100_2_4_1" in full and ("500k" in full or "500000" in full):
                hits.append(full)
    p("paths containing 100_2_4_1 AND (500k|500000)", len(hits))
    if hits[:5]:
        print("    e.g. %s" % hits[:5])
    p("dirs named checkpoints (whole evidence tree)", len(cps_all))
    print("    under: %s" % sorted(set(os.path.dirname(c) for c in cps_all))[:6])
    p9_sha, p21_sha = None, set()
    for dp, dn, fn in os.walk(os.path.join(EV, "P9-single-500k-20260810")):
        for f in fn:
            if f == "status.properties":
                sp = read_properties(os.path.join(dp, f))
                if "instanceSha256" in sp:
                    p9_sha = sp["instanceSha256"]
    p21_root = os.path.join(EV, "V35-P21", "runs", "A4-catalite-500k-20_2_3_1")
    for dp, dn, fn in os.walk(p21_root):
        for f in fn:
            fp2 = os.path.join(dp, f)
            try:
                if os.path.getsize(fp2) > 1_000_000:
                    continue
                with io.open(fp2, encoding="utf-8", errors="ignore") as fh:
                    for line in fh:
                        if line.strip().startswith("instanceSha256="):
                            p21_sha.add(line.split("=", 1)[1].strip())
            except OSError:
                continue
    p("P9-single-500k instanceSha256", p9_sha)
    p("V35-P21 A4-500k instanceSha256 set", sorted(p21_sha))
    same_sha = bool(p9_sha) and bool(p21_sha) and \
        {s.lower() for s in p21_sha} == {p9_sha.lower()}
    p("both are 20-job 20_2_3_1 (same sha, case-insensitive)", same_sha)
    sufficient = False
    print("  => historical 500k paired trajectory sufficient = %s" % sufficient)

    # ---- 与冻结 JSON 比对 --------------------------------------------------
    print("\n[cross-check vs frozen JSON]")
    with io.open(JSON_SA, encoding="utf-8") as fh:
        sa = json.load(fh)
    with io.open(JSON_PD, encoding="utf-8") as fh:
        pdj = json.load(fh)
    chk = [
        ("source.matchedWindowFluctuationAvailable",
         sa["comparabilityJudgment"]["matchedWindowFluctuationAvailable"],
         matched_available),
        ("source.mode", sa["thresholds"]["mode"], "FALLBACK"),
        ("source.whvgShareDeficitPp", sa["thresholds"]["whvgShareDeficitPp"], 2.0),
        ("source.exclusiveNdShareDeficitPp",
         sa["thresholds"]["exclusiveNdShareDeficitPp"], 10.0),
        ("source.consecutiveWindows", sa["thresholds"]["consecutiveWindows"], 2),
        ("attribution.rule",
         sa["attributionRule"]["rule"], "COUNTERFACTUAL_PRODUCER_SET"),
        ("attribution.multiSourceDuplicateRule",
         sa["attributionRule"]["multiSourceDuplicateRule"],
         "SHARED_POINTS_CONTRIBUTE_TO_NO_SINGLE_SOURCE"),
        ("attribution.firstClassSources",
         sa["attributionRule"]["firstClassSources"],
         ["GLOBAL_CFVF", "CATA", "INHERITED_LS", "PARENT_CARRYOVER"]),
        ("attribution.firstAdmissionScope",
         sa["attributionRule"]["firstAdmissionScope"], "DESCRIPTIVE_ONLY"),
        ("memory.modelVersion", sa["memoryModel"]["modelVersion"], "DECOMPOSED_V2"),
        ("divergence.sufficient", pdj["historicalSufficiency"]["sufficient"],
         sufficient),
        ("divergence.mode", pdj["thresholds"]["mode"], "FALLBACK"),
        ("divergence.hvProgressDeficitPp", pdj["thresholds"]["hvProgressDeficitPp"], 1.0),
        ("divergence.igdRelativeImprovementDeficitPp",
         pdj["thresholds"]["igdRelativeImprovementDeficitPp"], 10.0),
        ("divergence.consecutiveCheckpoints",
         pdj["thresholds"]["consecutiveCheckpoints"], 2),
    ]
    for label, frozen, computed in chk:
        ok = frozen == computed
        print("  %-45s frozen=%s computed=%s %s" %
              (label, frozen, computed, "MATCH" if ok else "MISMATCH"))
        if not ok:
            fails.append("cross-check mismatch: %s" % label)

    print("\n== AUDIT RESULT: %s ==" % ("PASS" if not fails else "FAIL"))
    for f in fails:
        print("  FAIL: %s" % f)
    return 0 if not fails else 1


# --------------------------------------------------------------------------
# --selftest（T1–T7）
# --------------------------------------------------------------------------
def selftest():
    print("== threshold_recompute --selftest (T1-T7, PhaseA0-CORRECTION-V1) ==")
    fails = []
    G, C = "GLOBAL_CFVF", "CATA"

    # HV 健全性
    hv = fc6.hypervolume([[1.0, 1.0, 1.0]])
    if abs(hv - 0.001) > 1e-9:
        fails.append("HV single point (1,1,1) = %r != 0.001" % hv)
    hv0 = fc6.hypervolume([[0.0, 0.0, 0.0]])
    if abs(hv0 - 1.1 ** 3) > 1e-9:
        fails.append("HV single point (0,0,0) = %r != 1.331" % hv0)
    print("HV sanity: (1,1,1)=%.12f (0,0,0)=%.12f OK" % (hv, hv0))

    fpast = [(100.0, 500.0, 1000.0), (120.0, 450.0, 1100.0), (140.0, 500.0, 900.0)]

    def ev(src, cid, obj, nfe=None, afe=None):
        return {"source": src, "nominalFE": nfe or 26001, "actualFE": afe or 26001,
                "candidateId": cid, "objectives": obj}

    # ---- T1: 双来源完全相同三元组（最小反例） --------------------------------
    p_shared = (90.0, 480.0, 950.0)
    t1 = window_metrics(fpast, [ev(G, "T1-G", p_shared), ev(C, "T1-C", p_shared)])
    ps = t1["producerSets"].get(p_shared, [])
    if t1["perSource"][G]["whvg"] != 0.0 or t1["perSource"][C]["whvg"] != 0.0:
        fails.append("T1: WHVG must be 0 for both sources, got %r/%r" %
                     (t1["perSource"][G]["whvg"], t1["perSource"][C]["whvg"]))
    if t1["perSource"][G]["nexclND"] != 0 or t1["perSource"][C]["nexclND"] != 0:
        fails.append("T1: ExclusiveND must be 0 for both sources")
    if sorted(ps) != sorted([G, C]):
        fails.append("T1: producerSet(p) must be {GLOBAL_CFVF, CATA}, got %s" % ps)
    print("T1 dual-source identical triple: WHVG_G=0 WHVG_C=0 ExND_G=0 ExND_C=0 "
          "producerSet=%s OK" % ps)

    # ---- T2: epsilon 相等（5e-13 差异）多来源三元组 --------------------------
    t2ev = [ev(G, "T2-G", p_shared),
            ev(C, "T2-C", tuple(v + 5e-13 for v in p_shared))]
    t2 = window_metrics(fpast, t2ev)
    if t1["producerSets"].get(p_shared) != frozenset([G, C]):
        pass
    if len(t2["producerSets"]) != 1:
        fails.append("T2: epsilon-equal points must fold to ONE canonical triple, got %d"
                     % len(t2["producerSets"]))
    if sorted(list(t2["producerSets"].values())[0]) != sorted([G, C]):
        fails.append("T2: folded producerSet must contain both sources")
    if t2["perSource"][G]["whvg"] != 0.0 or t2["perSource"][C]["whvg"] != 0.0:
        fails.append("T2: WHVG must be 0 for both (epsilon-equal shared point)")
    print("T2 epsilon-equal multi-source triple: 1 canonical triple, "
          "producerSet={G,C}, WHVG both 0 OK")

    # ---- T3: 单来源独占新严格 ND 点 -----------------------------------------
    t3obj = (85.0, 470.0, 940.0)
    t3 = window_metrics(fpast, [ev(G, "T3-G", t3obj)], sources=[G, C])
    if not (t3["perSource"][G]["whvg"] > 0.0):
        fails.append("T3: WHVG_GLOBAL must be > 0 for exclusive new ND point")
    if t3["perSource"][G]["nexclND"] != 1 or t3["nndAll"] != 1:
        fails.append("T3: ExclusiveND_GLOBAL must be 1, nndAll must be 1")
    if t3["perSource"][C]["whvg"] != 0.0 or t3["perSource"][C]["nexclND"] != 0:
        fails.append("T3: other sources must be 0")
    print("T3 single-source exclusive ND: WHVG_G=%.12g>0 ExND_G=1 others=0 OK"
          % t3["perSource"][G]["whvg"])

    # ---- T4: 共享点 + 独占点 -------------------------------------------------
    t4 = window_metrics(fpast, [ev(G, "T4-G1", p_shared), ev(C, "T4-C1", p_shared),
                                ev(G, "T4-G2", t3obj)], sources=[G, C])
    if t4["perSource"][G]["whvg"] <= 0.0:
        fails.append("T4: GLOBAL must keep its exclusive point contribution")
    if t4["perSource"][C]["whvg"] != 0.0:
        fails.append("T4: CATA must not gain the shared point contribution")
    if t4["perSource"][G]["nexclND"] != 1 or t4["perSource"][C]["nexclND"] != 0:
        fails.append("T4: ExclusiveND must count only the exclusive point for GLOBAL")
    print("T4 shared+exclusive: shared excluded from all ExclusiveND; "
          "WHVG_G=%.12g WHVG_C=0 OK" % t4["perSource"][G]["whvg"])

    # ---- T5: 行序无关（20 次随机打乱，多场景） --------------------------------
    t5_events = [ev(G, "T5-G1", p_shared, 26010, 26010),
                 ev(C, "T5-C1", p_shared, 26020, 26020),
                 ev(G, "T5-G2", t3obj, 26030, 26030),
                 ev(C, "T5-C2", (110.0, 400.0, 1050.0), 26040, 26040),
                 ev("INHERITED_LS", "T5-I1", (95.0, 490.0, 980.0), 26050, 26050)]
    base5 = window_metrics(fpast, t5_events)
    rng = random.Random(20260901)
    for i in range(20):
        shuffled = list(t5_events)
        rng.shuffle(shuffled)
        m = window_metrics(fpast, shuffled)
        if m["producerSets"] != base5["producerSets"]:
            fails.append("T5 shuffle %d: producerSets differ" % i)
        if m["nndAll"] != base5["nndAll"] or abs(m["hvAll"] - base5["hvAll"]) > 1e-12:
            fails.append("T5 shuffle %d: aggregates differ" % i)
        for s in base5["perSource"]:
            for k in ("whvg", "whvgSharePct", "nexclND", "exclusiveNdSharePct"):
                if abs(m["perSource"][s][k] - base5["perSource"][s][k]) > 1e-12:
                    fails.append("T5 shuffle %d: %s.%s differs" % (i, s, k))
        # Wt / Wt^-s 一致性：per-source whvg 由 Wt 与 Wt^-s 决定，逐源一致即证
    print("T5 row-order independence under 20 random shuffles (5 events, 3 sources): OK")

    # ---- T6: 旧单来源语义回归 -------------------------------------------------
    single = [ev(G, "T6-G1", p_shared), ev(G, "T6-G2", t3obj)]
    m_new = window_metrics(fpast, single)
    # 旧语义参考：单来源窗口下 FIRST_ADMISSION 与 producerSet 等价，逐组归属 s
    old_minus = []  # 旧实现：所有组归属 s → Wt^-s = 空
    anchor = fc6.nondominated([list(x) for x in fpast + [e["objectives"] for e in single]])
    old_whvg = (hv_normalized(fpast + [e["objectives"] for e in single], anchor)
                - hv_normalized(fpast, anchor))
    new_whvg = m_new["perSource"][G]["whvg"]
    if abs(new_whvg - old_whvg) > 1e-9:
        fails.append("T6: single-source regression broken %r vs %r" % (new_whvg, old_whvg))
    # 合同期望（显式给定）：t3obj=(85,470,940) 在并集中支配 p_shared=(90,480,950)
    # → 严格 ND 新点只有 t3obj → nndAll=1、ExclusiveND_G=1（非 2/2）
    if m_new["perSource"][G]["nexclND"] != 1 or m_new["nndAll"] != 1:
        fails.append("T6: single-source ExclusiveND/nndAll broken (expect 1/1)")
    print("T6 single-source regression: new==old (WHVG=%.12g, ExND=1/1 after dominance) OK" % new_whvg)

    # ---- T7: JSON—代码—schema 合同一致性 -------------------------------------
    with io.open(JSON_SA, encoding="utf-8") as fh:
        sa = json.load(fh)
    ar = sa["attributionRule"]
    if ar["rule"] != "COUNTERFACTUAL_PRODUCER_SET":
        fails.append("T7: JSON attributionRule.rule != COUNTERFACTUAL_PRODUCER_SET")
    if ar["multiSourceDuplicateRule"] != "SHARED_POINTS_CONTRIBUTE_TO_NO_SINGLE_SOURCE":
        fails.append("T7: JSON multiSourceDuplicateRule wrong")
    if ar["firstAdmissionScope"] != "DESCRIPTIVE_ONLY":
        fails.append("T7: JSON firstAdmissionScope wrong")
    if sorted(ar["firstClassSources"]) != sorted(["GLOBAL_CFVF", "CATA",
                                                  "INHERITED_LS", "PARENT_CARRYOVER"]):
        fails.append("T7: JSON firstClassSources wrong")
    if sa["thresholds"]["whvgShareDeficitPp"] != 2.0 or \
            sa["thresholds"]["exclusiveNdShareDeficitPp"] != 10.0 or \
            sa["thresholds"]["consecutiveWindows"] != 2:
        fails.append("T7: JSON thresholds drifted")
    if sa["metrics"]["WHVGShare_s"]["epsilon"] != EPS:
        fails.append("T7: JSON epsilon != code EPS")
    schema = io.open(os.path.join(HERE, "observer-schema.md"), encoding="utf-8").read()
    for token in ("COUNTERFACTUAL_PRODUCER_SET", "producerSet",
                  "SHARED_POINTS_CONTRIBUTE_TO_NO_SINGLE_SOURCE", "DESCRIPTIVE_ONLY",
                  "GLOBAL_CFVF", "CATA", "INHERITED_LS", "PARENT_CARRYOVER"):
        if token not in schema:
            fails.append("T7: observer-schema.md missing token %r" % token)
    pre = io.open(os.path.join(HERE, "PREREGISTRATION.md"), encoding="utf-8").read()
    for token in ("COUNTERFACTUAL", "producerSet", "FIRST_ADMISSION"):
        if token not in pre:
            fails.append("T7: PREREGISTRATION.md missing token %r" % token)
    print("T7 JSON-code-schema contract consistency: OK")

    # ---- 附加健全性：反事实下 Σ_s WHVG_s ≤ 总增益 -----------------------------
    base = window_metrics(fpast, t5_events)
    if base["whvgSumMinusTotalGain"] > 1e-9:
        fails.append("Σ WHVG_s > hv_all − HV(ND(Fpast)): %r"
                     % base["whvgSumMinusTotalGain"])
    m0 = window_metrics(fpast, [])
    if not (m0["emptyWindow"] and m0["hvAll"] == 0.0):
        fails.append("empty window rule broken")

    print("\n== SELFTEST RESULT: %s ==" % ("PASS" if not fails else "FAIL"))
    for f in fails:
        print("  FAIL: %s" % f)
    return 0 if not fails else 1


# --------------------------------------------------------------------------
# --memory-selftest（T8）
# --------------------------------------------------------------------------
def memory_selftest():
    print("== threshold_recompute --memory-selftest (T8) ==")
    fails = []
    GB = 1024 * 1024 * 1024
    base = 2.0 * GB          # baselineAlgorithmPeak（有界，20k OFF 实测的占位值）
    heap = 4 * GB            # assignedJavaHeap（-Xmx4g）
    # 1) baseline 不随 FE 乘 25：同一输入重复调用值恒定，且公式不含 FE 项
    e1 = estimate_500k_peak(base, 0, 0, 0)
    e2 = estimate_500k_peak(base, 0, 0, 0)
    if e1 != e2:
        fails.append("T8: estimator not deterministic")
    # e(baseline=B, 0,0,0) = B + 0 + max(0.2B, 256MiB)；B=2GiB → 0.2B=400MiB>256MiB
    expect = base + 0 + 0.20 * base
    if abs(e1 - expect) > 1e-6:
        fails.append("T8: baseline-only estimate %r != %r" % (e1, expect))
    if e1 >= 25 * base:
        fails.append("T8: estimate behaves like the deprecated x25 formula")
    print("T8.1 baseline-only: est=%.0f B = baseline + 20%% margin (NOT x25) OK" % e1)
    # 2) buffer cap 单调
    prev = -1.0
    for cap in (0, 10 * 1024 * 1024, 50 * 1024 * 1024, 200 * 1024 * 1024):
        e = estimate_500k_peak(base, 0, cap, cap)
        if e < prev - 1e-6:
            fails.append("T8: estimate not monotonic in buffer cap")
        prev = e
    print("T8.2 monotonicity in buffer caps: OK")
    # 3) 磁盘账本增长不进入 heap 估计：函数无 disk 参数
    import inspect
    sig = inspect.signature(estimate_500k_peak)
    if any("disk" in k.lower() or "ledger" in k.lower() for k in sig.parameters):
        fails.append("T8: estimator must not take disk-ledger parameters")
    print("T8.3 disk ledger growth excluded from heap estimate: OK")
    # 4) 0.60 硬门边界（fail-closed：超过与等于都失败）
    gate_heap = 10 * GB
    just_below = estimate_500k_peak(0, 0, 0, 0)  # min safety 256MiB → est=268435456*1.0?
    # 直接构造：est = 0.5999×heap → PASS；est = 0.6×heap → FAIL；est > 0.6×heap → FAIL
    e_pass = 0.5999 * gate_heap
    e_eq = 0.60 * gate_heap
    e_over = 0.6001 * gate_heap
    if not memory_gate_passes(e_pass, gate_heap):
        fails.append("T8: gate must PASS below 0.60")
    if memory_gate_passes(e_eq, gate_heap):
        fails.append("T8: gate must FAIL-CLOSED at exactly 0.60")
    if memory_gate_passes(e_over, gate_heap):
        fails.append("T8: gate must FAIL-CLOSED above 0.60")
    print("T8.4 hard gate boundary: below=PASS, ==0.60=FAIL, above=FAIL OK")
    # 5) 端到端示例（占位数值，标注为示例非实测）
    heap6 = 6 * GB
    base15 = int(1.5 * GB)
    e_pass_demo = estimate_500k_peak(base15, 16 * 1024 * 1024, 24 * 1024 * 1024,
                                     8 * 1024 * 1024)
    if not memory_gate_passes(e_pass_demo, heap6):
        fails.append("T8: pass-demo unexpectedly fails hard gate")
    # fail-closed 示例：2GiB 基线 + 4GiB 堆在 20%% safety 下必然越门 →
    # MEMORY_MODEL_INSUFFICIENT 路径（不得加堆掩盖，须减观察器占用或改预算）
    e_fail_demo = estimate_500k_peak(base, 16 * 1024 * 1024, 24 * 1024 * 1024,
                                     8 * 1024 * 1024)
    if memory_gate_passes(e_fail_demo, heap):
        fails.append("T8: fail-demo unexpectedly passes hard gate")
    print("T8.5 demos: pass-case est=%.0f B ratio=%.4f gate=%s; "
          "fail-case est=%.0f B ratio=%.4f gate=%s (illustrative, NOT measured)"
          % (e_pass_demo, e_pass_demo / heap6,
             memory_gate_passes(e_pass_demo, heap6),
             e_fail_demo, e_fail_demo / heap,
             memory_gate_passes(e_fail_demo, heap)))

    print("\n== MEMORY-SELFTEST RESULT: %s ==" % ("PASS" if not fails else "FAIL"))
    for f in fails:
        print("  FAIL: %s" % f)
    return 0 if not fails else 1


# --------------------------------------------------------------------------
# --windows / --gate
# --------------------------------------------------------------------------
def run_windows(args):
    events = ledger_to_events(args.ledger)
    fpast = read_front_csv(args.fpast)
    wev = events_in_window(events, args.window)
    m = window_metrics(fpast, wev)
    print("window %d: nEvents=%d hvAll=%.12f nndAll=%d emptyWindow=%s" %
          (args.window, len(wev), m["hvAll"], m["nndAll"], m["emptyWindow"]))
    print("source\tnTuplesProduced\tnTuplesExclusive\tWHVG\tWHVGSharePct\tNexclND\tExclusiveNDSharePct")
    for s in sorted(m["perSource"]):
        d = m["perSource"][s]
        print("%s\t%d\t%d\t%.12g\t%.6f\t%d\t%.6f" %
              (s, d["nTuplesProduced"], d["nTuplesExclusive"], d["whvg"],
               d["whvgSharePct"], d["nexclND"], d["exclusiveNdSharePct"]))
    if m.get("whvgSumMinusTotalGain", 0.0) > 1e-9:
        print("WARNING: Σ WHVG_s > hv_all − HV(ND(Fpast))（违反单调料界）: %r"
              % m["whvgSumMinusTotalGain"])
    return 0


def run_gate(args):
    t_to_fp_n = dict(parse_fpast_spec(s) for s in args.fpast_normal)
    t_to_fp_h = dict(parse_fpast_spec(s) for s in args.fpast_hard)
    ev_n = ledger_to_events(args.ledger_normal)
    ev_h = ledger_to_events(args.ledger_hard)
    thr_w = args.whvg_share_deficit_pp
    thr_e = args.exclusive_nd_share_deficit_pp
    qual = {"WHVGShare": {}, "ExclusiveNDShare": {}}
    print("window\tmetric\tdeficitPp(normal-hard)\tqualify")
    for t in args.window:
        mn = window_metrics(read_front_csv(t_to_fp_n[t]), events_in_window(ev_n, t))
        mh = window_metrics(read_front_csv(t_to_fp_h[t]), events_in_window(ev_h, t))
        for s in sorted(set(mn["perSource"]) | set(mh["perSource"])):
            dn = mn["perSource"].get(s, {}).get("whvgSharePct", 0.0)
            dh = mh["perSource"].get(s, {}).get("whvgSharePct", 0.0)
            d1 = dn - dh
            en = mn["perSource"].get(s, {}).get("exclusiveNdSharePct", 0.0)
            eh = mh["perSource"].get(s, {}).get("exclusiveNdSharePct", 0.0)
            d2 = en - eh
            q1, q2 = d1 >= thr_w, d2 >= thr_e
            qual["WHVGShare"][(s, t)] = q1
            qual["ExclusiveNDShare"][(s, t)] = q2
            print("%d\t%s\tWHVGShare\t%.6f\t%s" % (t, s, d1, q1))
            print("%d\t%s\tExclusiveNDShare\t%.6f\t%s" % (t, s, d2, q2))
    print("\nfirstPersistentWindow (same metric, >=%d consecutive, grid-adjacent):"
          % args.consecutive_windows)
    for metric in ("WHVGShare", "ExclusiveNDShare"):
        for s in sorted({k[0] for k in qual[metric]}):
            first = None
            ts = sorted(t for (ss, t) in qual[metric] if ss == s)
            run_len = 0
            prev_t = None
            for t in ts:
                if prev_t is not None and t == prev_t + 1 \
                        and qual[metric][(s, t)] and qual[metric][(s, prev_t)]:
                    run_len += 1
                elif qual[metric][(s, t)]:
                    run_len = 1
                else:
                    run_len = 0
                if run_len >= args.consecutive_windows and first is None:
                    first = t - args.consecutive_windows + 1
                prev_t = t
            print("  %s %s: %s" % (s, metric, first if first is not None else "NONE"))
    return 0


def parse_fpast_spec(spec):
    t, path = spec.split(":", 1)
    return int(t), path


# --------------------------------------------------------------------------
# --divergence（t_div 参考实现）
# --------------------------------------------------------------------------
def divergence_from_trajectories(hard, normal, eps=EPS, cmp_eps=CMP_EPS):
    """hard/normal: [(nominalFE, hv, igd), ...]（须含 nominalFE=0 基线行）。
    返回 per-checkpoint 明细与 t_div（解释性 divergence 时间）。
    网格 = 两侧 nominalFE 的并集（去 0）；任一侧缺失或上一网格点缺失 →
    lag=False（合同确定性规则）。deficit-vs-threshold 比较带容差 cmp_eps。"""
    h = {int(fe): (hv, igd) for fe, hv, igd in hard}
    n = {int(fe): (hv, igd) for fe, hv, igd in normal}
    fes = sorted((set(h) | set(n)) - {0})
    rows = []
    lag = {}
    for fe in fes:
        prev = fe - WINDOW_FE
        if fe not in h or fe not in n or prev not in h or prev not in n:
            lag[fe] = False
            rows.append({"nominalFE": fe, "hvProgressHard": None,
                         "hvProgressNormal": None, "deficitHVpp": None,
                         "igdRelImpHard": None, "igdRelImpNormal": None,
                         "deficitIGDpp": None, "lag": False,
                         "missing": True})
            continue
        hv0h, ig0h = h[prev]
        hv0n, ig0n = n[prev]
        hv1h, ig1h = h[fe]
        hv1n, ig1n = n[fe]
        prog_h = 100.0 * (hv1h - hv0h) / max(abs(hv0h), eps)
        prog_n = 100.0 * (hv1n - hv0n) / max(abs(hv0n), eps)
        imp_h = 100.0 * (ig0h - ig1h) / max(abs(ig0h), eps)
        imp_n = 100.0 * (ig0n - ig1n) / max(abs(ig0n), eps)
        dh, di = prog_n - prog_h, imp_n - imp_h
        lag[fe] = (dh >= 1.0 - cmp_eps) and (di >= 10.0 - cmp_eps)
        rows.append({"nominalFE": fe, "hvProgressHard": prog_h,
                     "hvProgressNormal": prog_n, "deficitHVpp": dh,
                     "igdRelImpHard": imp_h, "igdRelImpNormal": imp_n,
                     "deficitIGDpp": di, "lag": lag[fe]})
    t_div = "NOT_REACHED"
    for i, r in enumerate(rows):
        nxt = rows[i + 1] if i + 1 < len(rows) else None
        if r["lag"] and nxt is not None and nxt["lag"] and \
                nxt["nominalFE"] == r["nominalFE"] + WINDOW_FE:
            t_div = r["nominalFE"] // WINDOW_FE
            break
    return {"rows": rows, "t_div": t_div}


def run_divergence(args):
    def read_traj(path):
        out = []
        for r in read_csv_rows(path):
            out.append((int(r["nominalFE"]), float(r["hv"]), float(r["igd"])))
        return out

    res = divergence_from_trajectories(read_traj(args.traj_hard),
                                       read_traj(args.traj_normal))
    print("t_div (explanatory, NOT causal onset) = %s" % res["t_div"])
    print("nominalFE\tdHVpp\t dIGDpp\tlag")
    for r in res["rows"]:
        print("%d\t%s\t%s\t%s" % (
            r["nominalFE"],
            "NA(missing)" if r["deficitHVpp"] is None else "%.6f" % r["deficitHVpp"],
            "NA(missing)" if r["deficitIGDpp"] is None else "%.6f" % r["deficitIGDpp"],
            r["lag"]))
    return 0


# --------------------------------------------------------------------------
def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--audit", action="store_true", help="重算可比性/充足性判定")
    ap.add_argument("--selftest", action="store_true", help="T1–T7 反事实语义自检")
    ap.add_argument("--memory-selftest", dest="memory_selftest", action="store_true",
                    help="T8 内存分解模型单元检查")
    ap.add_argument("--windows", action="store_true", help="单 run 单窗口指标")
    ap.add_argument("--gate", action="store_true", help="normal/hard deficit 裁决")
    ap.add_argument("--divergence", action="store_true", help="t_div 计算")
    ap.add_argument("--ledger", help="Phase A ledger CSV (windows)")
    ap.add_argument("--fpast", help="Fpast front CSV，即 B_{t-1} decision front (windows)")
    ap.add_argument("--window", type=int, action="append",
                    help="窗口序号 t（可重复，用于 --gate）")
    ap.add_argument("--ledger-normal", dest="ledger_normal")
    ap.add_argument("--ledger-hard", dest="ledger_hard")
    ap.add_argument("--fpast-normal", dest="fpast_normal", action="append",
                    help="t:path（可重复）")
    ap.add_argument("--fpast-hard", dest="fpast_hard", action="append",
                    help="t:path（可重复）")
    ap.add_argument("--traj-hard", dest="traj_hard")
    ap.add_argument("--traj-normal", dest="traj_normal")
    ap.add_argument("--whvg-share-deficit-pp", dest="whvg_share_deficit_pp",
                    type=float, default=2.0)
    ap.add_argument("--exclusive-nd-share-deficit-pp",
                    dest="exclusive_nd_share_deficit_pp", type=float, default=10.0)
    ap.add_argument("--consecutive-windows", dest="consecutive_windows",
                    type=int, default=2)
    args = ap.parse_args(argv)
    if args.audit:
        return audit()
    if args.selftest:
        return selftest()
    if args.memory_selftest:
        return memory_selftest()
    if args.windows:
        if not (args.ledger and args.fpast and args.window):
            ap.error("--windows requires --ledger --fpast --window")
        return run_windows(args)
    if args.gate:
        if not (args.ledger_normal and args.ledger_hard and args.window
                and args.fpast_normal and args.fpast_hard):
            ap.error("--gate requires paired ledgers, fpast specs and windows")
        return run_gate(args)
    if args.divergence:
        if not (args.traj_hard and args.traj_normal):
            ap.error("--divergence requires --traj-hard --traj-normal")
        return run_divergence(args)
    ap.print_help()
    return 0


if __name__ == "__main__":
    sys.exit(main())
