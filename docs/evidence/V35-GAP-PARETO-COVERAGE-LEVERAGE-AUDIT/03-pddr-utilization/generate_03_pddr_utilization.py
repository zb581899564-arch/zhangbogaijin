# -*- coding: utf-8 -*-
"""
V35-GAP-PARETO-COVERAGE-LEVERAGE-AUDIT — Agent C / 03-pddr-utilization
只读审计脚本：从既有数据源复算 PDDR 利用链证据，不修改任何源数据。

数据源（绝对路径，均已在只读扫描中确认）：
  B. V35-FC5-MIDHORIZON-250K 01-root-cause-analysis/remote-results/
     - archive-working-gap-events.csv / archive-working-gap-summary.csv （A2/A4, 100_2_4_1+100_5_3_1, seed 20260901-03, budget=250000）
     - directional-lifecycle-events.csv / directional-lifecycle-summary.csv
  C. V35-FC5-MIDHORIZON-DIAGNOSTICS 各 accepted run 的 telemetry-pddr-full-ledger.csv
     （2k/20k/50k, A2/A4, 实例 100_2_4_1 + 100_5_3_1；无 50_2_3_1 候选级数据，已 grep 核实）

输出（同目录）：
  archive-working-gap.csv
  pddr-working-population-utilization.csv
  directional-representative-lifecycle.csv
"""
import csv, os, statistics, sys
from collections import Counter, defaultdict

AUDIT = r"E:\学习\李明哲-毕业材料\张博改进\docs\evidence\V35-GAP-PARETO-COVERAGE-LEVERAGE-AUDIT"
OUT = os.path.join(AUDIT, "03-pddr-utilization")
B = r"E:\学习\李明哲-毕业材料\张博改进\docs\evidence\V35-FC5-MIDHORIZON-250K\01-root-cause-analysis\remote-results"
C = r"E:\学习\李明哲-毕业材料\张博改进\docs\evidence\V35-FC5-MIDHORIZON-DIAGNOSTICS"
B_BUDGET = 250000
ROLES = ["G1_CMAX", "G4_BALANCED", "G2_TEC", "G3_TWC"]
REP_FLAG = {"G1_CMAX": "isDirectionalCmaxRepresentative",
            "G2_TEC": "isDirectionalTecRepresentative",
            "G3_TWC": "isDirectionalTwcRepresentative",
            "G4_BALANCED": "isBalancedRepresentative"}
NO_NORMAL = "EVIDENCE_FIELD_LIMITATION"  # 无 50_2_3_1（正常实例）候选级数据（grep 全目录核实）


def read_csv(path):
    with open(path, encoding="utf-8-sig", newline="") as f:
        return list(csv.DictReader(f))


def write_csv(path, header, rows):
    with open(path, "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f)
        w.writerow(header)
        for r in rows:
            w.writerow([r.get(h, "") for h in header])
    print("wrote %s (%d data rows)" % (os.path.basename(path), len(rows)))


# ----------------------------------------------------------------------------
# 1) archive-working-gap.csv — 复算 B 源事件并对源汇总做一致性核验
# ----------------------------------------------------------------------------
def build_archive_working_gap():
    ev_path = os.path.join(B, "archive-working-gap-events.csv")
    sm_path = os.path.join(B, "archive-working-gap-summary.csv")
    events = read_csv(ev_path)
    rows = []
    for e in events:
        rows.append({
            "instance": e["instance"], "seed": e["seed"], "arm": e["arm"],
            "budget": B_BUDGET,
            "cycle": e["cycle"], "FE": e["FE"], "windowEndFE": e["windowEndFE"],
            "cmaxGap": e["cmaxGap"], "tecGap": e["tecGap"], "twcGap": e["twcGap"],
            "workingSize": e["workingSize"], "archiveSize": e["archiveSize"],
            "sourceCsv": ev_path,
        })
    header = ["instance", "seed", "arm", "budget", "cycle", "FE", "windowEndFE",
              "cmaxGap", "tecGap", "twcGap", "workingSize", "archiveSize", "sourceCsv"]
    write_csv(os.path.join(OUT, "archive-working-gap.csv"), header, rows)

    # 复算汇总并与源 summary 核验（一致性检查，不替换源数据）
    src_sum = read_csv(sm_path)
    src_map = {(s["instance"], s["seed"], s["arm"], s["windowEndFE"]): s for s in src_sum}
    grp = defaultdict(list)
    for e in events:
        grp[(e["instance"], e["seed"], e["arm"], e["windowEndFE"])].append(e)
    max_diff = 0.0
    checked = 0
    for k, es in sorted(grp.items()):
        s = src_map.get(k)
        if not s:
            continue
        checked += 1
        recomputed = {
            "medianCmaxGap": statistics.median(float(x["cmaxGap"]) for x in es),
            "maxCmaxGap": max(float(x["cmaxGap"]) for x in es),
            "medianTecGap": statistics.median(float(x["tecGap"]) for x in es),
            "maxTecGap": max(float(x["tecGap"]) for x in es),
            "medianTwcGap": statistics.median(float(x["twcGap"]) for x in es),
            "maxTwcGap": max(float(x["twcGap"]) for x in es),
            "medianArchiveSize": statistics.median(float(x["archiveSize"]) for x in es),
        }
        for kk, vv in recomputed.items():
            max_diff = max(max_diff, abs(vv - float(s[kk])))
    print("[check] archive-working-gap summary recomputation: %d groups compared, max abs diff = %.10f"
          % (checked, max_diff))
    # 量级描述（数据级，供 README）
    hard = [e for e in events if e["instance"] == "100_5_3_1" and e["arm"] == "A4"]
    nz = [e for e in hard if float(e["cmaxGap"]) > 0 or float(e["tecGap"]) > 0 or float(e["twcGap"]) > 0]
    print("[info] B source rows=%d; A4 hard(100_5_3_1) event rows=%d, rows with any gap>0: %d"
          % (len(events), len(hard), len(nz)))
    return max_diff, checked


# ----------------------------------------------------------------------------
# 2) pddr-working-population-utilization.csv — C 源 pddr-full-ledger 逐 run 复算
# ----------------------------------------------------------------------------
LEDGER_RUNS = [
    # (dataSourceDir, runDir, budget)
    (os.path.join(C, "09-real-2k-equivalence", "runs"), "2k-100_5_3_1-20260901-A2-ON", 2000),
    (os.path.join(C, "09-real-2k-equivalence", "runs"), "2k-100_5_3_1-20260901-A4-ON", 2000),
    (os.path.join(C, "10-real-20k-equivalence", "runs"), "20k-100_2_4_1-20260901-A2-ON", 20000),
    (os.path.join(C, "10-real-20k-equivalence", "runs"), "20k-100_2_4_1-20260901-A4-ON", 20000),
    (os.path.join(C, "10-real-20k-equivalence", "runs"), "20k-100_5_3_1-20260901-A2-ON", 20000),
    (os.path.join(C, "10-real-20k-equivalence", "runs"), "20k-100_5_3_1-20260901-A4-ON", 20000),
    (os.path.join(C, "18-final-2k-20k-50k-gates"), "A2-2k-effective-5100-ON-final", 2000),
    (os.path.join(C, "18-final-2k-20k-50k-gates"), "A4-2k-effective-5100-ON-final", 2000),
    (os.path.join(C, "18-final-2k-20k-50k-gates"), "A2-20k-100_2_4_1-ON-final", 20000),
    (os.path.join(C, "18-final-2k-20k-50k-gates"), "A2-20k-100_5_3_1-ON-final", 20000),
    (os.path.join(C, "18-final-2k-20k-50k-gates"), "A4-20k-effective-20258-100_2_4_1-ON-final", 20000),
    (os.path.join(C, "18-final-2k-20k-50k-gates"), "A4-20k-effective-20258-100_5_3_1-ON-final", 20000),
    (os.path.join(C, "18-final-2k-20k-50k-gates"), "A2-50k-ON-final", 50000),
    (os.path.join(C, "18-final-2k-20k-50k-gates"), "A4-50k-ON-final", 50000),
    (os.path.join(C, "23-a4-50k-terminal-validation"), "A4-50k-ON-s20260901", 50000),
    (os.path.join(C, "26-final-runtime-jar-validation"), "A4-50k-ON-s20260901-121FBB49", 50000),
]
# 排除：18-final 的 -v1/-v2/-v3/-v4/-defaultheap 探索变体、14-final-sequence-audit 单行文件、
# 02-implementation/pddr-full-ledger.csv（合成测试数据 FP_A4_001，非真实运行）。
# 18-final A*-50k-ON-final 与 23/26 的 A4-50k-ON-s20260901 为同一配置的验收/换 jar 重验，行级重复关系在 README 登记。


def parse_props(path):
    props = {}
    if not os.path.exists(path):
        return props
    for line in open(path, encoding="utf-8", errors="replace"):
        line = line.strip()
        if "=" in line and not line.startswith("#"):
            k, _, v = line.partition("=")
            props[k.strip()] = v.strip()
    return props


def build_pddr_utilization():
    # 第一遍：全局建立 instanceHash -> instance 映射（来自目录名含实例名的 run 与 behavior-summary）
    inst_hash_map = {}
    pre = {}
    for base, run, budget in LEDGER_RUNS:
        led = os.path.join(base, run, "telemetry-pddr-full-ledger.csv")
        if not os.path.exists(led):
            print("[WARN] missing ledger: %s" % led)
            continue
        data = read_csv(led)
        props = parse_props(os.path.join(base, run, "behavior-summary.properties"))
        pre[(base, run)] = (data, props)
        instance = props.get("instance", "").replace(".txt", "")
        if not instance:  # 目录名自带实例名的 run
            for tok in run.split("-"):
                if tok.startswith("100_") or tok.startswith("50_"):
                    instance = tok
        ih = data[0].get("instanceHash", "")
        if instance and ih:
            inst_hash_map.setdefault(ih, instance)
    rows = []
    for (base, run), (data, props) in pre.items():
        budget = dict(((b, r), bg) for b, r, bg in LEDGER_RUNS)[(base, run)]
        led = os.path.join(base, run, "telemetry-pddr-full-ledger.csv")
        instance = props.get("instance", "").replace(".txt", "")
        seed = props.get("seed", data[0].get("seed", ""))
        arm = props.get("arm", data[0].get("arm", ""))
        instance_hash = data[0].get("instanceHash", "")
        if not instance:
            for tok in run.split("-"):
                if tok.startswith("100_") or tok.startswith("50_"):
                    instance = tok
        if not instance and instance_hash in inst_hash_map:
            instance = inst_hash_map[instance_hash]
        if not instance:
            instance = "EVIDENCE_FIELD_LIMITATION"
        # ledger 有两个 schema 世代：旧(09/10)=selected/stableFingerprint；新(18/23/26)=selectedByPddr/candidateFingerprint
        sel_key = "selectedByPddr" if "selectedByPddr" in data[0] else "selected"
        fp_key = "candidateFingerprint" if "candidateFingerprint" in data[0] else "stableFingerprint"
        schema_gen = "NEW(selectedByPddr+candidateFingerprint)" if sel_key == "selectedByPddr" \
            else "OLD(selected+stableFingerprint)"
        cycles = sorted(set(int(r["cycle"]) for r in data))
        sel_total = sum(1 for r in data if r[sel_key] == "true")
        fe_min = min(int(r["actualFE"]) for r in data)
        fe_max = max(int(r["actualFE"]) for r in data)
        for role in ROLES:
            pool = [r for r in data if r["semanticRoleBefore"] == role]
            sel = [r for r in pool if r[sel_key] == "true"]
            rep = [r for r in pool if r.get(REP_FLAG[role]) == "true"]
            pool_n, sel_n = len(pool), len(sel)
            # C 源 ledger 语义：selectedByPddr=true 即被 PDDR 选入下一 working population
            # （核验：A4-50k 每 cycle selectedByPddr=true 恰 100 = working population 规模）。
            # semanticRoleAfter 在全部 run 中恒为 NOT_APPLICABLE，不能作为 working-population 字段。
            work_n = sel_n
            consistency = "OK" if sel_total == 100 * len(cycles) else \
                "CHECK:selectedTotal=%d,cycles=%d,expected=%d" % (sel_total, len(cycles), 100 * len(cycles))
            rows.append({
                "dataSource": "C:V35-FC5-MIDHORIZON-DIAGNOSTICS",
                "runId": run,
                "sourceLedger": led,
                "instance": instance,
                "instanceHash": instance_hash,
                "seed": seed,
                "arm": arm,
                "budget": budget,
                "instanceClass": "HARD_100JOB" if instance.startswith("100_") else
                                 ("NORMAL_50JOB" if instance.startswith("50_") else "UNKNOWN"),
                "directionRole": role,
                "poolEnteredCount": pool_n,
                "selectedByPddrCount": sel_n,
                "enteredWorkingPopulationCount": work_n,
                "retentionRate": ("%.6f" % (sel_n / pool_n)) if pool_n else "NOT_APPLICABLE",
                "directionalRepresentativeCount": len(rep),
                "cyclesObserved": len(cycles),
                "actualFEMin": fe_min,
                "actualFEMax": fe_max,
                "identityBasis": "%s in ledger" % fp_key,
                "ledgerSchema": schema_gen,
                "hardVsNormalComparison": NO_NORMAL,
                "workingPopulationEntryBasis": "selectedByPddr=true (=working-population entry; semanticRoleAfter always NOT_APPLICABLE)",
                "runLevelConsistencyCheck": consistency,
            })
    header = ["dataSource", "runId", "sourceLedger", "instance", "instanceHash", "seed", "arm",
              "budget", "instanceClass", "directionRole", "poolEnteredCount", "selectedByPddrCount",
              "enteredWorkingPopulationCount", "retentionRate", "directionalRepresentativeCount",
              "cyclesObserved", "actualFEMin", "actualFEMax", "identityBasis", "ledgerSchema",
              "hardVsNormalComparison", "workingPopulationEntryBasis", "runLevelConsistencyCheck"]
    write_csv(os.path.join(OUT, "pddr-working-population-utilization.csv"), header, rows)
    return rows


# ----------------------------------------------------------------------------
# 3) directional-representative-lifecycle.csv — B 源生命周期事件按真实指纹复算
# ----------------------------------------------------------------------------
def build_directional_lifecycle():
    ev = read_csv(os.path.join(B, "directional-lifecycle-events.csv"))
    grp = defaultdict(list)
    for e in ev:
        grp[(e["instance"], e["seed"], e["arm"], e["representative"], e["fingerprintSha256"])].append(e)
    rows = []
    for (inst, seed, arm, rep, fp), es in sorted(grp.items()):
        fes = [int(x["FE"]) for x in es]
        windows = sorted(set(int(x["cohortWindowEndFE"]) for x in es))
        pool = any(int(x["poolPresent"]) == 1 for x in es)
        sel = any(int(x["pddrSelected"]) == 1 for x in es)
        work = any(int(x["enteredNextPopulation"]) == 1 for x in es)
        surv = any(int(x["survivedNextCycle"]) == 1 for x in es)
        teach = any(int(x["teacherUsed"]) == 1 for x in es)
        impr = any(int(x["improvedOffspring"]) == 1 for x in es)
        # improvedOffspringCount/teacherExposure 是 cohort（代表出生窗口）口径，跨窗口求和会重复计数 → 取 max
        rows.append({
            "dataSource": "B:V35-FC5-MIDHORIZON-250K/01-root-cause-analysis/remote-results",
            "sourceEventsCsv": os.path.join(B, "directional-lifecycle-events.csv"),
            "instance": inst, "seed": seed, "arm": arm, "budget": B_BUDGET,
            "representative": rep,
            "fingerprintSha256": fp,
            "identityBasis": "fingerprintSha256 (stable fingerprint, B源导出)",
            "source": es[0]["source"],
            "cohortWindows": "|".join(str(w) for w in windows),
            "cyclesObserved": len(es),
            "firstObservedFE": min(fes),
            "lastSeenFE": max(fes),
            "enteredMergePool": 1 if pool else 0,
            "selectedByPddr": 1 if sel else 0,
            "enteredWorkingPopulation": 1 if work else 0,
            "retentionRate": ("%.6f" % ((1 if work else 0) / pool)) if pool else "NOT_APPLICABLE",
            "survivedNextCycle": 1 if surv else 0,
            "teacherUsed": 1 if teach else 0,
            "teacherExposureMax": max(int(x["teacherExposure"]) for x in es),
            "offspringImproved": 1 if impr else 0,
            "improvedOffspringCountMax": max(int(x["improvedOffspringCount"]) for x in es),
            "retiredAtCycleMax": max(int(x["retiredAtCycle"]) for x in es),
            "hardVsNormalComparison": NO_NORMAL,
        })
    header = ["dataSource", "sourceEventsCsv", "instance", "seed", "arm", "budget",
              "representative", "fingerprintSha256", "identityBasis", "source", "cohortWindows",
              "cyclesObserved", "firstObservedFE", "lastSeenFE", "enteredMergePool",
              "selectedByPddr", "enteredWorkingPopulation", "retentionRate", "survivedNextCycle",
              "teacherUsed", "teacherExposureMax", "offspringImproved", "improvedOffspringCountMax",
              "retiredAtCycleMax", "hardVsNormalComparison"]
    write_csv(os.path.join(OUT, "directional-representative-lifecycle.csv"), header, rows)
    return rows


if __name__ == "__main__":
    print("== 03-pddr-utilization generation (read-only audit) ==")
    build_archive_working_gap()
    u = build_pddr_utilization()
    l = build_directional_lifecycle()
    # 数据级摘要（供 README / 汇报，不做假设裁决）
    a4_250k_pool = [r for r in u if r["budget"] == 250000]
    tot_pool = sum(r["poolEnteredCount"] for r in u)
    tot_sel = sum(r["selectedByPddrCount"] for r in u)
    lc_pool = sum(1 for r in l if r["enteredMergePool"] == 1)
    lc_work = sum(1 for r in l if r["enteredWorkingPopulation"] == 1)
    lc_teach = sum(1 for r in l if r["teacherUsed"] == 1)
    lc_impr = sum(1 for r in l if r["offspringImproved"] == 1)
    print("[summary] C-ledger rows: %d runs x4 roles=%d; pool total=%d selected total=%d (%.2f%%)"
          % (len(LEDGER_RUNS), len(u), tot_pool, tot_sel, 100.0 * tot_sel / max(tot_pool, 1)))
    print("[summary] B lifecycle fingerprints=%d; enteredMergePool=%d, enteredWorkingPopulation=%d, teacherUsed=%d, offspringImproved=%d"
          % (len(l), lc_pool, lc_work, lc_teach, lc_impr))
    print("done.")
