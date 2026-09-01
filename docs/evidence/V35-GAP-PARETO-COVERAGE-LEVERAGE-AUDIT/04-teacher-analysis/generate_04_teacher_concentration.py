# -*- coding: utf-8 -*-
"""
V35-GAP-PARETO-COVERAGE-LEVERAGE-AUDIT — Agent C / 04-teacher-analysis
只读审计脚本：从 250k LOCAL-FE-PACING 18 条 run 的 dscr-teacher-uses.csv /
dscr-events.csv 复算 teacher 集中度（FE 窗口化），不修改任何源数据。

数据源 A（绝对路径，只读扫描确认）：
  E:\\学习\\李明哲-毕业材料\\张博改进\\docs\\evidence\\V35-GAP-LOCAL-FE-PACING-REPAIR\\16-remote-250k-runs\\sync\\
    seed-{20260916,20260917,20260918}\\results\\run-GAPL250K-{C0,C2,C3}-{50_2_3_1,100_5_3_1}-<seed>\\
      - dscr-teacher-uses.csv  (~6200 行: decisionCycle,generation,FE,group,teacherId,teacherObjectives,dominated,dominatorCount)
      - dscr-events.csv        (~12392 行: ...stale,replacementId,...,directionScore,dominanceAge,teacherExposure)

口径（冻结）：
  - teacher 身份 = teacherId 字段（六向量状态串）的 SHA-256 全文 —— 真实状态指纹，
    不使用 poolOrdinal / index%4 / 文件序号。注意：该身份是"教师状态快照"身份，
    状态随代更新而改变；uniqueTeacherCount 计的是唯一教师状态数（README 详述）。
  - FE 窗口：(0,50000], (50000,100000], (100000,150000], (150000,200000], (200000, terminal]
    terminal = max(250000, 本 run 观测到的最大 FE)；即窗口左开右闭，首窗含 FE=0。
  - normalizedEntropy = Shannon(teacher 使用频次) / log2(uniqueTeacherCount)；uniqueTeacherCount<=1 时记 0。
  - replacementRate = dscr-events 中 stale==true 的行占比（与 mechanismSummary replacements=225 在
    seed-20260916/C0/100_5_3_1 逐一核对一致；脚本对每条 run 输出核对结果）。
  - dscr-teacher-uses 仅覆盖 Qg（教师选择）事件：teacherUses=qgSelections=6200；
    Qp 作用域的教师暴露在 250k 冻结 Jar 中未导出（NOT_EXPORTED，README 登记）。

输出（同目录）：teacher-concentration-analysis.csv
"""
import csv, glob, hashlib, math, os, statistics
from collections import Counter, defaultdict

AUDIT = r"E:\学习\李明哲-毕业材料\张博改进\docs\evidence\V35-GAP-PARETO-COVERAGE-LEVERAGE-AUDIT"
OUT = os.path.join(AUDIT, "04-teacher-analysis")
SYNC = r"E:\学习\李明哲-毕业材料\张博改进\docs\evidence\V35-GAP-LOCAL-FE-PACING-REPAIR\16-remote-250k-runs\sync"
ROLES = ["G1_CMAX", "G4_BALANCED", "G2_TEC", "G3_TWC"]
WINDOWS = [(0, 50000), (50000, 100000), (100000, 150000), (150000, 200000)]  # 末窗终端在主循环补


def split_top(line):
    """按括号深度 0 的逗号切分；向量字段 [..] 内的逗号不切分。"""
    parts, depth, cur = [], 0, []
    for ch in line:
        if ch == "[":
            depth += 1
            cur.append(ch)
        elif ch == "]":
            depth -= 1
            cur.append(ch)
        elif ch == "," and depth == 0:
            parts.append("".join(cur))
            cur = []
        else:
            cur.append(ch)
    parts.append("".join(cur))
    return parts


def read_run_files(run_dir):
    tu_path = os.path.join(run_dir, "dscr-teacher-uses.csv")
    de_path = os.path.join(run_dir, "dscr-events.csv")
    tu = []
    with open(tu_path, encoding="utf-8") as f:
        f.readline()
        for line in f:
            p = split_top(line.rstrip("\n"))
            # 0 decisionCycle,1 generation,2 FE,3 group,4 teacherId(blob),5 teacherObjectives,6 dominated,7 dominatorCount
            tu.append((int(p[2]), p[3], p[4], p[6]))
    de = []
    with open(de_path, encoding="utf-8") as f:
        f.readline()
        for line in f:
            p = split_top(line.rstrip("\n"))
            # 2 FE,8 stale,11 directionScore,14 dominanceAge；部分行 directionScore=NOT_APPLICABLE
            def ff(x):
                try:
                    return float(x)
                except ValueError:
                    return None
            de.append((int(p[2]), p[8], ff(p[11]), ff(p[14])))
    return tu, de, tu_path, de_path


def window_of(fe, windows):
    for lo, hi in windows:
        if lo < fe <= hi:
            return (lo, hi)
    return None


def entropy_norm(counts):
    n = sum(counts.values())
    k = len(counts)
    if n == 0 or k <= 1:
        return 0.0, 0.0
    h = -sum((c / n) * math.log2(c / n) for c in counts.values())
    return h, h / math.log2(k)


def main():
    run_dirs = sorted(glob.glob(os.path.join(SYNC, "seed-*", "results", "run-GAPL250K-*")))
    assert len(run_dirs) == 18, "expected 18 runs, found %d" % len(run_dirs)
    rows, checks = [], []
    for rd in run_dirs:
        name = os.path.basename(rd)  # run-GAPL250K-C0-100_5_3_1-20260916
        arm, instance, seed = name.split("-")[2], name.split("-")[3], name.split("-")[4]
        tu, de, tu_path, de_path = read_run_files(rd)
        max_fe = max(f for f, *_ in tu)
        windows = WINDOWS + [(200000, max(250000, max_fe))]
        # mechanismSummary 核对：teacherUses / replacements（dscr= 子段为 | 分隔，需二次解析）
        ms = {}
        for line in open(os.path.join(rd, "status.properties"), encoding="utf-8", errors="replace"):
            if line.startswith("mechanismSummary="):
                for kv in line.strip().split("=", 1)[1].split(","):
                    if "=" in kv:
                        k, _, v = kv.partition("=")
                        ms[k] = v
        if "dscr" in ms:
            for kv in ms["dscr"].split("|"):
                if "=" in kv:
                    k, _, v = kv.partition("=")
                    ms["dscr." + k] = v
        tu_by_win = defaultdict(list)
        for fe, group, blob, dominated in tu:
            tu_by_win[window_of(fe, windows)].append((group, blob, dominated))
        de_by_win = defaultdict(list)
        for fe, stale, score, age in de:
            de_by_win[window_of(fe, windows)].append((stale, score, age))
        for lo, hi in windows:
            ev = tu_by_win.get((lo, hi), [])
            if not ev:
                continue
            n = len(ev)
            teacher_counts = Counter(hashlib.sha256(blob.encode("utf-8")).hexdigest()
                                     for _, blob, _ in ev)
            group_counts = Counter(g for g, _, _ in ev)
            h, hnorm = entropy_norm(teacher_counts)
            ordered = teacher_counts.most_common()
            top1 = ordered[0][1] / n if ordered else 0.0
            top5 = sum(c for _, c in ordered[:5]) / n if ordered else 0.0
            dev = de_by_win.get((lo, hi), [])
            rows.append({
                "dataSource": "A:V35-GAP-LOCAL-FE-PACING-REPAIR/16-remote-250k-runs/sync",
                "sourceTeacherUsesCsv": tu_path,
                "sourceDscrEventsCsv": de_path,
                "instance": instance, "seed": seed, "arm": arm, "budget": 250000,
                "instanceClass": "NORMAL_50JOB" if instance.startswith("50_") else "HARD_100JOB",
                "feWindowStart": lo, "feWindowEnd": hi,
                "totalSelections": n,
                "uniqueTeacherCount": len(teacher_counts),
                "top1Share": "%.6f" % top1,
                "top5Share": "%.6f" % top5,
                "shannonEntropy": "%.6f" % h,
                "normalizedEntropy": "%.6f" % hnorm,
                "splitG1_CMAX": "%.6f" % (group_counts.get("G1_CMAX", 0) / n),
                "splitG4_BALANCED": "%.6f" % (group_counts.get("G4_BALANCED", 0) / n),
                "splitG2_TEC": "%.6f" % (group_counts.get("G2_TEC", 0) / n),
                "splitG3_TWC": "%.6f" % (group_counts.get("G3_TWC", 0) / n),
                "dominatedTeacherRatio": "%.6f" % (sum(1 for _, _, d in ev if d == "true") / n),
                "teacherIdentityBasis": "sha256(teacherId six-vector state blob)",
                "dscrEventCount": len(dev),
                "meanDirectionScore": ("%.6f" % (sum(s for _, s, _ in dev if s is not None) / max(1, sum(1 for _, s, _ in dev if s is not None)))) if any(s is not None for _, s, _ in dev) else "NOT_APPLICABLE",
                "replacementRate": ("%.6f" % (sum(1 for st, _, _ in dev if st == "true") / len(dev))) if dev else "NOT_APPLICABLE",
                "meanDominanceAge": ("%.6f" % (sum(a for _, _, a in dev if a is not None) / max(1, sum(1 for _, _, a in dev if a is not None)))) if any(a is not None for _, _, a in dev) else "NOT_APPLICABLE",
                "qpScopeCoverage": "NOT_EXPORTED(dscr-teacher-uses covers Qg scope only; Qp teacher exposure not exported by frozen jar at 250k)",
            })
        # 全 run 级核对
        stale_total = sum(1 for _, st, _, _ in de if st == "true")
        checks.append((name, len(tu), ms.get("dscr.teacherUses"), stale_total, ms.get("dscr.replacements"),
                       max_fe, ms.get("qgSelections")))
    header = ["dataSource", "sourceTeacherUsesCsv", "sourceDscrEventsCsv", "instance", "seed", "arm",
              "budget", "instanceClass", "feWindowStart", "feWindowEnd", "totalSelections",
              "uniqueTeacherCount", "top1Share", "top5Share", "shannonEntropy", "normalizedEntropy",
              "splitG1_CMAX", "splitG4_BALANCED", "splitG2_TEC", "splitG3_TWC",
              "dominatedTeacherRatio", "teacherIdentityBasis", "dscrEventCount",
              "meanDirectionScore", "replacementRate", "meanDominanceAge", "qpScopeCoverage"]
    out = os.path.join(OUT, "teacher-concentration-analysis.csv")
    with open(out, "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f)
        w.writerow(header)
        for r in rows:
            w.writerow([r[h_] for h_ in header])
    print("wrote teacher-concentration-analysis.csv (%d data rows)" % len(rows))
    print("\n== 全 run 核对（teacherUses/qgSelections/replacements 与 mechanismSummary）==")
    for name, ntu, mu_tu, nst, mu_rep, mfe, qg in checks:
        ok1 = "OK" if str(ntu) == str(mu_tu) else "MISMATCH"
        ok2 = "OK" if str(nst) == str(mu_rep) else "MISMATCH"
        print("%s: teacherUses rows=%s vs summary=%s [%s]; replacements stale=%s vs summary=%s [%s]; maxFE=%d; qgSelections=%s"
              % (name, ntu, mu_tu, ok1, nst, mu_rep, ok2, mfe, qg))
    # 数据级对照（C0 正式语义臂）：困难 vs 正常 的逐窗口中位（跨 3 seed）
    print("\n== C0 臂 数据级对照（3 seed 中位）==")
    for inst in ["100_5_3_1", "50_2_3_1"]:
        for lo, hi in windows:
            vals = {}
            for r in rows:
                if r["arm"] == "C0" and r["instance"] == inst \
                        and r["feWindowStart"] == lo and r["feWindowEnd"] == hi:
                    vals.setdefault("top1", []).append(float(r["top1Share"]))
                    vals.setdefault("hne", []).append(float(r["normalizedEntropy"]))
                    vals.setdefault("unik", []).append(int(r["uniqueTeacherCount"]))
            if vals:
                print("%s window(%d,%d]: medianTop1Share=%.4f medianNormEntropy=%.4f medianUniqueTeachers=%.0f"
                      % (inst, lo, hi, statistics.median(vals["top1"]),
                         statistics.median(vals["hne"]), statistics.median(vals["unik"])))


if __name__ == "__main__":
    main()
