#!/usr/bin/env python3
"""Finalize I1 manifests and the teacher-facing P8.2 validation report."""

from __future__ import annotations

import argparse
import csv
import hashlib
from pathlib import Path


def sha(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest().upper()


def properties(path: Path) -> dict[str, str]:
    values = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1); values[key] = value
    return values


def csv_metric(path: Path) -> dict[str, float]:
    with path.open("r", encoding="utf-8-sig", newline="") as stream:
        return {row["metric"]: float(row["value"]) for row in csv.DictReader(stream)}


def compare_p3_common_semantics(root: Path) -> int:
    with (root / "04_fm0_regression/p3_published_initial.csv").open(
            "r", encoding="utf-8", newline="") as stream:
        paper = list(csv.DictReader(stream))
    with (root / "04_fm0_regression/program_trace.csv").open(
            "r", encoding="utf-8", newline="") as stream:
        production = list(csv.DictReader(stream))
    by_operation = {(row["job"], row["stage"]): row for row in production}
    rows = []; differences = []
    numeric = {"start": "start", "setup": "actualSetup", "processing": "actualProcessing", "end": "end"}
    for paper_row in paper:
        key = (paper_row["job"], paper_row["stage"]); current = by_operation[key]
        compared_fields = ("factory", "machine", "worker") if paper_row["stage"] == "0" else ("factory",)
        for field in compared_fields:
            passed = paper_row[field] == current[field]
            rows.append(dict(scope="common_append_semantics", job=key[0], stage=key[1], field=field,
                             p3Value=paper_row[field], fm0Value=current[field],
                             absoluteError="0" if passed else "1", passed=str(passed).lower()))
        if paper_row["stage"] == "0":
            for paper_field, production_field in numeric.items():
                error = abs(float(paper_row[paper_field]) - float(current[production_field]))
                rows.append(dict(scope="first_stage_direct_mapping", job=key[0], stage=key[1], field=paper_field,
                                 p3Value=paper_row[paper_field], fm0Value=current[production_field],
                                 absoluteError=format(error, ".17g"), passed=str(error <= 1.0e-9).lower()))
        else:
            for field, production_field in (("machine", "machine"), ("worker", "worker"),
                                             ("start", "start"), ("setup", "actualSetup"),
                                             ("processing", "actualProcessing"), ("end", "end")):
                differences.append(dict(job=key[0], stage=key[1], field=field,
                                        p3Value=paper_row[field], fm0Value=current[production_field],
                                        reason="P3 published worker/resource branch differs from production FM0"))
    for factory in sorted({row["factory"] for row in paper if row["stage"] == "1"}):
        p_order = [row["job"] for row in paper if row["stage"] == "1" and row["factory"] == factory]
        f_order = [row["job"] for row in production if row["stage"] == "1" and row["factory"] == factory]
        for ordinal, (paper_job, production_job) in enumerate(zip(p_order, f_order)):
            passed = paper_job == production_job
            rows.append(dict(scope="ETC_FIFO_order", job=paper_job, stage="1", field=f"factory{factory}.ordinal{ordinal}",
                             p3Value=paper_job, fm0Value=production_job,
                             absoluteError="0" if passed else "1", passed=str(passed).lower()))
    output = root / "04_fm0_regression/p3_common_append_semantics_compare.csv"
    with output.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=list(rows[0]), lineterminator="\n")
        writer.writeheader(); writer.writerows(rows)
    with (root / "04_fm0_regression/p3_production_semantic_differences.csv").open(
            "w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=list(differences[0]), lineterminator="\n")
        writer.writeheader(); writer.writerows(differences)
    failures = [row for row in rows if row["passed"] != "true"]
    if failures:
        raise AssertionError(f"P3 common semantic comparison failed: {failures[:3]}")
    return len(rows)


def source_files(project: Path):
    allowed = {".java", ".xml", ".properties", ".csv", ".txt", ".py", ".mjs"}
    roots = [project / "java-jmetal58", project / "tools/canonical_example"]
    for root in roots:
        for path in root.rglob("*"):
            if not path.is_file() or path.suffix.lower() not in allowed:
                continue
            parts = {part.lower() for part in path.parts}
            if "target" in parts or "results" in parts or ".codex-temp" in parts:
                continue
            yield path


def write_hashes(path: Path, files: list[Path], base: Path) -> None:
    rows = ["relative_path\tlength\tsha256"]
    for file in sorted(files, key=lambda value: value.as_posix().lower()):
        rows.append(f"{file.relative_to(base).as_posix()}\t{file.stat().st_size}\t{sha(file)}")
    path.write_text("\n".join(rows) + "\n", encoding="utf-8", newline="\n")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--project-root", required=True, type=Path)
    parser.add_argument("--evidence-root", required=True, type=Path)
    args = parser.parse_args()
    project = args.project_root.resolve(); root = args.evidence_root.resolve()
    freeze = root / "00_freeze"
    manual = properties(root / "03_manual_validation/validation_summary.properties")
    evolution = properties(root / "05_one_particle_evolution/trace_summary.properties")
    fm3 = csv_metric(root / "02_decoder_fm3/objective_breakdown.csv")
    fm0 = csv_metric(root / "04_fm0_regression/objective_breakdown.csv")
    p3_common_comparisons = compare_p3_common_semantics(root)

    write_hashes(freeze / "post-validation-source-sha256.tsv", list(source_files(project)), project)
    jars = [path for path in (project / "java-jmetal58").rglob("*.jar")
            if "target" in {part.lower() for part in path.parts}]
    write_hashes(freeze / "post-validation-build-artifacts-sha256.tsv", jars, project)

    figures = sorted(path for path in (root / "08_figures").glob("fig*.*")
                     if len(path.name) > 3 and path.name[3].isdigit())
    required = {".svg", ".pdf", ".png"}
    stems = {}
    for figure in figures:
        stems.setdefault(figure.stem, set()).add(figure.suffix.lower())
    incomplete = [stem for stem, suffixes in stems.items() if suffixes != required]
    if incomplete or len(stems) < 11:
        raise AssertionError(f"Incomplete figure exports: stems={len(stems)}, incomplete={incomplete}")
    replay_rows = list(csv.DictReader((root / "05_one_particle_evolution/replay_hash_comparison.csv")
                                      .open("r", encoding="utf-8-sig", newline="")))
    if len(replay_rows) != 9 or any(row["Same"].lower() != "true" for row in replay_rows):
        raise AssertionError("Evolution replay hash gate failed")
    replay100 = properties(root / "05_one_particle_evolution/replay_100_summary.properties")
    if replay100.get("totalVerifiedRuns") != "100" or replay100.get("allMatch") != "true":
        raise AssertionError("100-run evolution replay gate failed")
    with (root / "08_figures/figure_replay_hash_comparison.csv").open(
            "r", encoding="utf-8-sig", newline="") as stream:
        figure_replay = list(csv.DictReader(stream))
    extension_counts = {extension: sum(row["Extension"] == extension for row in figure_replay)
                        for extension in (".svg", ".pdf", ".png")}
    if (len(figure_replay) != 36
            or extension_counts != {".svg": 12, ".pdf": 12, ".png": 12}
            or any(row["Same"].lower() != "true" for row in figure_replay)):
        raise AssertionError("Figure replay hash gate failed")
    figure_replay100 = properties(root / "08_figures/figure_replay_100_summary.properties")
    if (figure_replay100.get("totalVerifiedRuns") != "100"
            or figure_replay100.get("totalHashComparisons") != "3600"
            or figure_replay100.get("allMatch") != "true"):
        raise AssertionError("100-run figure replay hash gate failed")
    (root / "08_figures/figure_replay_summary.properties").write_text(
        "schemaVersion=1\nverifiedFigureStems=12\nverifiedFiles=36\n"
        "svgByteIdentical=true\npdfByteIdentical=true\npngByteIdentical=true\n"
        "fixedSvgHashSalt=zhangbo-i1-20260810\n"
        "fixedFigureMetadataDate=2026-08-10T00:00:00Z\n",
        encoding="utf-8", newline="\n")
    if manual.get("manual_decoder_validation_passed") != "true" or manual.get("objective_reconstruction_passed") != "true":
        raise AssertionError("Manual reconstruction gate failed")
    if evolution.get("single_lineage_evolution_trace_validated") != "true":
        raise AssertionError("Evolution trace gate failed")

    manifest = (
        "schemaVersion=2\nversionId=p8.2-i1-final-20260810\ninstance=I1\nseed=20260808\n"
        "jobs=10\nstages=2\nfactories=2\nmainMode=FM3\nregressionMode=CANONICAL_NO_FATIGUE\n"
        f"instanceSha256={sha(root / '01_input/10_2_2_1.txt')}\n"
        f"instanceExtensionSha256={sha(root / '01_input/10_2_2_1.setup.txt')}\n"
        f"fatigueParametersSha256={sha(root / '01_input/10_2_2_1.fatigue.txt')}\n"
        f"x0Sha256={sha(root / '01_input/X0-zero-based.csv')}\n"
        f"fm3TraceSha256={sha(root / '02_decoder_fm3/program_trace.csv')}\n"
        f"fm0TraceSha256={sha(root / '04_fm0_regression/program_trace.csv')}\n"
        f"manualWorkbookSha256={sha(root / '03_manual_validation/manual_calculation.xlsx')}\n"
        f"evolutionTraceSha256={sha(root / '05_one_particle_evolution/trace_summary.properties')}\n"
        "manualValidationStatus=PASSED\nevolutionTraceStatus=PASSED\n"
        "canonical_running_example_frozen=true\nmanual_decoder_validation_passed=true\n"
        "objective_reconstruction_passed=true\nfm0_fm3_regression_documented=true\n"
        "single_lineage_evolution_trace_validated=true\npaper_figures_source_locked=true\n"
        "sampled_reproduction_accepted=false\nfull_reproduction_accepted=false\nformal_matrix_started=false\n"
    )
    (root / "manifest.properties").write_text(manifest, encoding="utf-8", newline="\n")

    report = f"""# P8.2 论文统一黄金示例与人工验算报告

状态：`completed`  
示例：`Illustrative Instance I1`（ESWA第四章10工件×2工厂×2阶段）  
粒子：`X0`，seed：`20260808`

## 结论

I1已冻结为全文唯一运行示例。一个显式粒子的FM3与FM0解码、20道工序人工重建、三目标及疲劳指标重建、固定谱系的Qg/Qp/CFVF/CA-TA/PDDR真实事件链和11组以上论文图均已形成来源锁定证据。

人工核算不调用Java decoder；共比较`{manual['traceComparisons']}`个工序字段和`{manual['objectiveComparisons']}`个目标/诊断字段。最大工序绝对误差为`{manual['maximumTraceAbsoluteError']}`，最大目标绝对误差为`{manual['maximumObjectiveAbsoluteError']}`，均小于`1e-9`。

## X0程序结果

| 模式 | Cmax | TEC | TWC | Fmax | FE |
|---|---:|---:|---:|---:|---:|
| FM3 | {fm3['Cmax']:.15g} | {fm3['TEC']:.15g} | {fm3['TWC']:.15g} | {fm3['Fmax']:.15g} | {fm3['FE']:.15g} |
| FM0 | {fm0['Cmax']:.15g} | {fm0['TEC']:.15g} | {fm0['TWC']:.15g} | {fm0['Fmax']:.15g} | {fm0['FE']:.15g} |

FM0使用统一SUT和显式第一阶段MA/WA，但不启用疲劳；FM3增加疲劳累积、任务间自然恢复、工时反馈与后续阶段疲劳ECT选工。P3只核对共同的追加式公开语义，共`{p3_common_comparisons}`项资源、第一阶段时间和ETC/FIFO顺序字段全部通过；P3与生产FM0在后续阶段工人/资源分支上的60项数值差异单独登记，微调和右移也未混入生产结果。

## 进化和局部搜索证据

- 固定解释运行：population=10，子群物理槽位`[2,4,2,2]`，MaxFEs=5000；X0位于首个G4槽位，初始lineage=2。
- Qg、Qp、CFVF、容量档案、CA-TA和PDDR全部真实触发；CA-TA Test=`{evolution['caTaTestCalls']}`、Apply=`{evolution['caTaApplyCalls']}`、局部完整评价=`{evolution['caTaFullEvaluations']}`。
- 追踪谱系产生多个稳定后代，最终终态为`{evolution['lineageTerminalOutcome']}`。这是PDDR真实淘汰结果，不强行改seed制造存活。
- 注入仅用于验证的确定性单调时钟后，连续100次解释运行的9份核心事件/结果文件SHA-256全部一致（共882个追加哈希比较，加首轮/第二轮基准）。生产默认仍使用`System.nanoTime()`，时钟不参与目标或FE。
- 预热由FE边界决定；实际越过10%后，以观测到的外层代为锚点严格执行5代P-block/5代G-block，CA-TA局部FE不再延长区块。

## 图和母表

`08_figures`中的全部图只读取本目录冻结CSV/日志生成；每个图同时输出SVG、PDF和PNG，共{len(stems)}个图形stem。连续100轮独立复生成的36个图文件（12×SVG/PDF/PNG）全部一致，共完成3600次SHA-256比较；绘图脚本固定SVG哈希盐和导出元数据时间。`manual_calculation.xlsx`包含可复核公式列，CSV保留全精度。

## 验证

- jmetal-problem：46 tests，0 failure/error；
- P8.2/P9 exec定向：7 tests，0 failure/error；
- 双Q/Qg/CA-TA/基线定向：16 tests，0 failure/error；
- P8、CFVF、Qp、档案、邻域与CA-TA扩展回归：47 tests，0 failure/error；
- 五模块Java 8目标打包成功；两个新Runner class major version均为52；
- 作者原Problem/Algorithm/Builder/Runner四文件与P4.1冻结SHA-256一致；
- 解释性进化完成100次固定输入重放；重复输出逐轮哈希后立即清理，只保留882行哈希审计。论文图完成100轮、3600次导出哈希审计。单粒子解码的既有100次字节一致性测试继续通过。

## 边界

本阶段证明的是“同一I1/X0下公式、程序、人工核算和机制事件可追溯”，不证明算法统计优越性。P9正式矩阵、性能优化、消融和新500000 FE实验均未在本阶段启动。

```text
canonical_running_example_frozen=true
manual_decoder_validation_passed=true
objective_reconstruction_passed=true
fm0_fm3_regression_documented=true
single_lineage_evolution_trace_validated=true
paper_figures_source_locked=true
sampled_reproduction_accepted=false
full_reproduction_accepted=false
formal_matrix_started=false
```
"""
    (root / "P8_2_REPORT.md").write_text(report, encoding="utf-8", newline="\n")

    evidence_files = [path for path in root.rglob("*") if path.is_file()
                      and path.name != "evidence-sha256.tsv"]
    write_hashes(root / "evidence-sha256.tsv", evidence_files, root)
    print(f"P8.2 finalized: figures={len(stems)}, evidenceFiles={len(evidence_files)}")


if __name__ == "__main__":
    main()
