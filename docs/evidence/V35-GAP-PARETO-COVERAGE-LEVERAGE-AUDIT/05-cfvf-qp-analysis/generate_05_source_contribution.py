# -*- coding: utf-8 -*-
"""
V35-GAP-PARETO-COVERAGE-LEVERAGE-AUDIT — Agent C / 05-cfvf-qp-analysis
只读审计脚本：从 250k LOCAL-FE-PACING 18 条 run 的 mechanismSummary（计数级）与
cmax-audit-records.csv（tracked 候选级）复算各来源模块的生成量与贡献可得性。

数据源 A（绝对路径，只读扫描确认）：
  E:\\学习\\李明哲-毕业材料\\张博改进\\docs\\evidence\\V35-GAP-LOCAL-FE-PACING-REPAIR\\16-remote-250k-runs\\sync\\
    seed-{20260916,20260917,20260918}\\results\\run-GAPL250K-{C0,C2,C3}-{50_2_3_1,100_5_3_1}-<seed>\\
      - status.properties(mechanismSummary) / pddr-observation.properties / cmax-audit-records.csv

冻结 Jar 限制（直接证据）：
  - pddr-observation.properties: poolLevelAttribution=NOT_EXPORTED_BY_FROZEN_JAR
    → 逐来源 pool/ND/HV 贡献在 250k 冻结语义下未导出（H4 所需核心字段 NOT_EXPORTED）。
  - cmax-audit-records.csv 每 run 仅 ~43 个 tracked 候选（cmax 审计子集，偏向 G1_CMAX），
    且 mechanism ∈ {FIXED_VNS, CFVF, INITIAL}，不含 Qg/Qp/CA-TA 来源 → ndContribution 仅 tracked 子集可算。
  - ca-ta-lite-events.log 环形缓冲截断（保留 ~4094 行，实际 caTaTest+caTaApply=7533 事件）；
    p6 环形缓冲 p6EventsRetained=4096 / p6EventsTotal=182923。
  - offspringImproved 字段 A 源不存在（仅 B directional-lifecycle 的 4 方向代表、
    C 源 23-a4-50k cata-contribution/teacher-use 事件存在）。

输出（同目录）：source-contribution-analysis.csv
"""
import csv, glob, os
from collections import Counter, defaultdict

AUDIT = r"E:\学习\李明哲-毕业材料\张博改进\docs\evidence\V35-GAP-PARETO-COVERAGE-LEVERAGE-AUDIT"
OUT = os.path.join(AUDIT, "05-cfvf-qp-analysis")
SYNC = r"E:\学习\李明哲-毕业材料\张博改进\docs\evidence\V35-GAP-LOCAL-FE-PACING-REPAIR\16-remote-250k-runs\sync"

COUNTER_KEYS = [
    ("CFVF", "cfvfOffspring"),
    ("Qg", "qgSelections"),
    ("Qp", "qpActions"),
    ("CA-TA-Test", "caTaLiteTest"),
    ("CA-TA-Apply", "caTaLiteApply"),
]
MECH_TO_SOURCE = {"CFVF": "CFVF", "FIXED_VNS": "inheritedLS", "INITIAL": "initial"}


def parse_props(path):
    props = {}
    if not os.path.exists(path):
        return props
    for line in open(path, encoding="utf-8", errors="replace"):
        line = line.strip()
        if line.startswith("mechanismSummary="):
            for kv in line.split("=", 1)[1].split(","):
                if "=" in kv:
                    k, _, v = kv.partition("=")
                    props[k] = v
        elif "=" in line and not line.startswith("#"):
            k, _, v = line.partition("=")
            props[k.strip()] = v.strip()
    return props


def non_dominated_flags(objs):
    """在给定目标向量集合内计算严格非支配（最小化三目标）。返回每点是否 ND（子集内口径）。"""
    n = len(objs)
    flags = []
    for i in range(n):
        a = objs[i]
        nd = True
        for j in range(n):
            if i == j:
                continue
            b = objs[j]
            if all(b[k] <= a[k] for k in range(3)) and any(b[k] < a[k] for k in range(3)):
                nd = False
                break
        flags.append(nd)
    return flags


def main():
    run_dirs = sorted(glob.glob(os.path.join(SYNC, "seed-*", "results", "run-GAPL250K-*")))
    assert len(run_dirs) == 18, "expected 18 runs, found %d" % len(run_dirs)
    rows, checks = [], []
    for rd in run_dirs:
        name = os.path.basename(rd)
        arm, instance, seed = name.split("-")[2], name.split("-")[3], name.split("-")[4]
        st = parse_props(os.path.join(rd, "status.properties"))
        obs = parse_props(os.path.join(rd, "pddr-observation.properties"))
        rec_path = os.path.join(rd, "cmax-audit-records.csv")
        recs = list(csv.DictReader(open(rec_path, encoding="utf-8")))
        # tracked 子集内的严格 ND（仅子集口径，不外推）
        objs = [(float(r["cmax"]), float(r["tec"]), float(r["twc"])) for r in recs]
        nd_flags = non_dominated_flags(objs)
        mech_nd = defaultdict(lambda: [0, 0])       # mechanism -> [ndCount, total]
        mech_pddr = defaultdict(lambda: [0, 0])     # [true, total]
        mech_surv = defaultdict(lambda: [0, 0])     # [YES, YES+NO]
        mech_garch = defaultdict(lambda: [0, 0])    # [true, total]
        for r, nd in zip(recs, nd_flags):
            m = r["mechanism"]
            mech_nd[m][0] += 1 if nd else 0
            mech_nd[m][1] += 1
            mech_pddr[m][0] += 1 if r["pddrRetained"] == "true" else 0
            mech_pddr[m][1] += 1
            if r["nextRoundSurvival"] in ("YES", "NO"):
                mech_surv[m][0] += 1 if r["nextRoundSurvival"] == "YES" else 0
                mech_surv[m][1] += 1
            mech_garch[m][0] += 1 if r["globalArchive"] == "true" else 0
            mech_garch[m][1] += 1
        # initial 规模 = runtimeSubSwarmSizes 之和
        init_n = 0
        if "runtimeSubSwarmSizes" in st:
            try:
                init_n = sum(int(x.split("=")[1]) for x in st["runtimeSubSwarmSizes"].split(";"))
            except Exception:
                init_n = 0
        gen_counts = {
            "CFVF": st.get("cfvfOffspring", "NOT_EXPORTED"),
            "Qg": st.get("qgSelections", "NOT_EXPORTED"),
            "Qp": st.get("qpActions", "NOT_EXPORTED"),
            "CA-TA-Test": st.get("caTaLiteTest", "NOT_EXPORTED"),
            "CA-TA-Apply": st.get("caTaLiteApply", "NOT_EXPORTED"),
            "inheritedLS": obs.get("inheritedLocalEventOps", "NOT_EXPORTED"),
            "initial": str(init_n) if init_n else "NOT_EXPORTED",
        }
        base_limit = ("ndContribution 仅 cmax-audit tracked 子集(N=%d/run, mechanism∈{FIXED_VNS,CFVF,INITIAL}); "
                      "poolLevelAttribution=NOT_EXPORTED_BY_FROZEN_JAR" % len(recs))
        for source in ["CFVF", "Qg", "Qp", "CA-TA-Test", "CA-TA-Apply", "inheritedLS", "initial"]:
            mech = [m for m, s in MECH_TO_SOURCE.items() if s == source]
            if mech:
                m = mech[0]
                nd_n, nd_tot = mech_nd.get(m, [0, 0])
                p_true, p_tot = mech_pddr.get(m, [0, 0])
                s_yes, s_den = mech_surv.get(m, [0, 0])
                g_true, g_tot = mech_garch.get(m, [0, 0])
                nd_val = ("%.6f" % (nd_n / nd_tot)) if nd_tot else "EVIDENCE_FIELD_LIMITATION"
                pddr_val = ("%.6f" % (p_true / p_tot)) if p_tot else "NOT_APPLICABLE_NO_TRACKED"
                surv_val = ("%.6f" % (s_yes / s_den)) if s_den else "NOT_APPLICABLE_NO_SELECTED"
                gar_val = ("%.6f" % (g_true / g_tot)) if g_tot else "NOT_APPLICABLE_NO_TRACKED"
                tracked_n = nd_tot
            else:
                nd_val = "EVIDENCE_FIELD_LIMITATION"
                pddr_val = "EVIDENCE_FIELD_LIMITATION"
                surv_val = "EVIDENCE_FIELD_LIMITATION"
                gar_val = "EVIDENCE_FIELD_LIMITATION"
                tracked_n = 0
            limits = [base_limit]
            if source == "inheritedLS":
                limits.append("inheritedLocalEventOps 环形缓冲截断(cap=4096; p6EventsTotal=%s, p6EventsRetained=%s); "
                              "真实 LS 事件总数 NOT_OBSERVABLE; formalLocalFE=%s 为 FE 消耗量非生成计数"
                              % (st.get("p6EventsTotal"), st.get("p6EventsRetained"), st.get("formalLocalFE")))
            if source in ("Qg", "Qp", "CA-TA-Test", "CA-TA-Apply"):
                limits.append("cmax-audit-records 无该来源 tracked 候选; 逐来源严格ND/HV贡献 NOT_EXPORTED(冻结Jar)")
            limits.append("offspringImprovedRate NOT_EXPORTED(A源无该字段; 仅B directional-lifecycle 4方向代表、"
                          "C 23-a4-50k cata-contribution/teacher-use 事件[50k,单seed]存在)")
            rows.append({
                "dataSource": "A:V35-GAP-LOCAL-FE-PACING-REPAIR/16-remote-250k-runs/sync",
                "sourceStatusProperties": os.path.join(rd, "status.properties"),
                "sourceCmaxAuditCsv": rec_path,
                "instance": instance, "seed": seed, "arm": arm, "budget": 250000,
                "instanceClass": "NORMAL_50JOB" if instance.startswith("50_") else "HARD_100JOB",
                "sourceModule": source,
                "generationCount": gen_counts[source],
                "generationCountBasis": "mechanismSummary counter" if source != "inheritedLS"
                                        else "pddr-observation.properties counter (RING-BUFFER-CAPPED)",
                "trackedCandidateCount": tracked_n,
                "ndContribution_ndWithinTrackedSubset": nd_val,
                "pddrRetainedRate": pddr_val,
                "nextRoundSurvivalRate": surv_val,
                "globalArchiveRate": gar_val,
                "offspringImprovedRate": "NOT_EXPORTED",
                "evidenceLimitations": "; ".join(limits),
            })
        checks.append((name, len(recs), st.get("cfvfOffspring"), st.get("qgSelections"),
                       st.get("qpActions"), st.get("caTaLiteTest"), st.get("caTaLiteApply"),
                       obs.get("inheritedLocalEventOps"), st.get("archiveInsertions"),
                       obs.get("poolLevelAttribution")))
    header = ["dataSource", "sourceStatusProperties", "sourceCmaxAuditCsv", "instance", "seed", "arm",
              "budget", "instanceClass", "sourceModule", "generationCount", "generationCountBasis",
              "trackedCandidateCount", "ndContribution_ndWithinTrackedSubset", "pddrRetainedRate",
              "nextRoundSurvivalRate", "globalArchiveRate", "offspringImprovedRate", "evidenceLimitations"]
    out = os.path.join(OUT, "source-contribution-analysis.csv")
    with open(out, "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f)
        w.writerow(header)
        for r in rows:
            w.writerow([r[h_] for h_ in header])
    print("wrote source-contribution-analysis.csv (%d data rows)" % len(rows))
    print("\n== 全 run 计数核对 ==")
    for c in checks:
        print("%s: tracked=%s cfvf=%s qg=%s qp=%s caTaTest=%s caTaApply=%s inheritedLocalOps=%s archiveInsertions=%s poolLevelAttribution=%s"
              % c)
    # 数据级摘要：C0 臂 tracked 子集 ND 率 per mechanism（3 seed 中位）
    print("\n== C0 臂 tracked 子集 ND 率（3 seed 中位, 仅子集口径）==")
    agg = defaultdict(list)
    for r in rows:
        if r["arm"] == "C0" and isinstance(r["ndContribution_ndWithinTrackedSubset"], str) \
                and r["ndContribution_ndWithinTrackedSubset"].replace(".", "").isdigit():
            agg[r["sourceModule"]].append(float(r["ndContribution_ndWithinTrackedSubset"]))
    import statistics
    for s, vals in sorted(agg.items()):
        print("%s: median=%.4f (n=%d)" % (s, statistics.median(vals), len(vals)))


if __name__ == "__main__":
    main()
