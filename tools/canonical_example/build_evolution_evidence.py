#!/usr/bin/env python3
"""Build source-locked I1 evolution/local-search evidence and figures 7--11."""

from __future__ import annotations

import argparse
import ast
import copy
import csv
import importlib.util
import re
from datetime import datetime, timezone
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch


SVG_DATE = "2026-08-10T00:00:00Z"
PDF_DATE = datetime(2026, 8, 10, tzinfo=timezone.utc)


def load_manual_module(root: Path):
    path = root / "tools/canonical_example/manual_reconstruct.py"
    spec = importlib.util.spec_from_file_location("i1_manual", path)
    module = importlib.util.module_from_spec(spec)
    assert spec and spec.loader
    spec.loader.exec_module(module)
    return module


def lines(path: Path) -> list[str]:
    return [line.rstrip("\n") for line in path.read_text(encoding="utf-8").splitlines()]


def marker(text: str, start: str, end: str | None = None) -> str:
    begin = text.index(start) + len(start)
    finish = text.index(end, begin) if end else len(text)
    return text[begin:finish]


def fingerprint(text: str) -> dict[str, list[int]]:
    pieces = text.split("|")
    if len(pieces) != 4:
        raise ValueError(f"Expected four vectors, got {text}")
    names = ("JS", "FA", "MA", "WA")
    return {name: list(ast.literal_eval(piece)) for name, piece in zip(names, pieces)}


def objectives(text: str) -> list[float]:
    return [float(value) for value in ast.literal_eval(text)]


def write_csv(path: Path, rows: list[dict[str, object]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    fields = list(rows[0])
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=fields, lineterminator="\n")
        writer.writeheader(); writer.writerows(rows)


def save_all(fig: plt.Figure, stem: Path) -> None:
    fig.savefig(stem.with_suffix(".svg"), bbox_inches="tight",
                metadata={"Date": SVG_DATE})
    fig.savefig(stem.with_suffix(".pdf"), bbox_inches="tight",
                metadata={"CreationDate": PDF_DATE, "ModDate": PDF_DATE})
    fig.savefig(stem.with_suffix(".png"), dpi=300, bbox_inches="tight")
    plt.close(fig)


def decode_vectors(manual, base: dict[str, object], vectors: dict[str, list[int]]):
    data = copy.deepcopy(base); data["x0"] = vectors
    return manual.decode(data, "FM3")


def gantt(ax, trace: list[dict[str, object]], title: str) -> None:
    lanes = sorted({(int(r["factory"]), int(r["stage"]), int(r["machine"])) for r in trace})
    index = {lane: i for i, lane in enumerate(lanes)}
    colors = plt.get_cmap("tab10")
    for row in trace:
        lane = (int(row["factory"]), int(row["stage"]), int(row["machine"]))
        y = index[lane]
        start = float(row["start"]); setup = float(row["actualSetup"])
        processing = float(row["actualProcessing"])
        ax.barh(y, setup, left=start, height=.62, color="#F0B44D", edgecolor="black", linewidth=.35)
        ax.barh(y, processing, left=start + setup, height=.62,
                color=colors(int(row["job"]) % 10), edgecolor="black", linewidth=.35)
        ax.text(start + setup + processing / 2, y, f"J{int(row['job']) + 1}/W{int(row['worker']) + 1}",
                ha="center", va="center", fontsize=5.8, color="white", weight="bold")
    ax.set_yticks(range(len(lanes)))
    ax.set_yticklabels([f"F{f+1}-S{k+1}-M{m+1}" for f, k, m in lanes], fontsize=7)
    ax.set_xlabel("Time"); ax.set_title(title, weight="bold", fontsize=10); ax.grid(axis="x", alpha=.2)


def box(ax, xy, width, height, title, body, color):
    ax.add_patch(FancyBboxPatch(xy, width, height, boxstyle="round,pad=0.02",
                                facecolor=color, edgecolor="#333", linewidth=.8))
    ax.text(xy[0] + width/2, xy[1] + height*.68, title, ha="center", va="center", weight="bold")
    ax.text(xy[0] + width/2, xy[1] + height*.28, body, ha="center", va="center", fontsize=7)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--project-root", required=True, type=Path)
    parser.add_argument("--evidence-root", required=True, type=Path)
    parser.add_argument("--input-root", type=Path,
                        help="Frozen I1 input root; defaults to evidence-root")
    args = parser.parse_args()
    project = args.project_root.resolve(); root = args.evidence_root.resolve()
    input_root = args.input_root.resolve() if args.input_root else root
    manual = load_manual_module(project); base = manual.load_inputs(input_root)
    e5 = root / "05_one_particle_evolution"; e6 = root / "06_local_search"
    e7 = root / "07_environment_selection"; figures = root / "08_figures"
    e7.mkdir(parents=True, exist_ok=True)
    figures.mkdir(parents=True, exist_ok=True)

    qp_lines = lines(e5 / "qp_tracked_lineage_events.log")
    cfvf_headers = [x for x in lines(e5 / "cfvf_tracked_lineage_events.log")
                    if x.startswith("generation=")]
    cata_lines = lines(e6 / "cata_tracked_lineage_events.log")
    qp_lineages = {
        int(match.group(1))
        for line in qp_lines
        for match in [re.search(r"type=select,lineage=(\d+),", line)]
        if match
    }
    accepted = []
    for line in cata_lines:
        match = re.search(r"generation=(\d+),slot=(\d+),lineage=(\d+),.*accepted=true", line)
        if match and int(match.group(3)) in qp_lineages:
            trace_prefix = f"generation={int(match.group(1)) * 10}:"
            lineage_token = f":lineage={match.group(3)}:"
            matching_cfvf = [entry for entry in cfvf_headers
                             if entry.startswith(trace_prefix) and lineage_token in entry]
            if matching_cfvf:
                accepted.append((line, match, matching_cfvf[0]))
    if not accepted:
        raise ValueError("No accepted CA-TA event has matching Qp and CFVF lineage evidence")
    cata_result, selected_match, cfvf = accepted[0]
    outer_generation = int(selected_match.group(1))
    selected_slot = int(selected_match.group(2))
    selected_lineage = int(selected_match.group(3))
    trace_generation = outer_generation * 10
    group = marker(cfvf, f"generation={trace_generation}:", ":particle=")

    qp_selects = [x for x in qp_lines
                  if f"group={group},type=select,lineage={selected_lineage}," in x]
    qp_rewards = [x for x in qp_lines
                  if f"group={group},type=reward,lineage={selected_lineage}," in x]
    if not qp_selects or not qp_rewards:
        raise ValueError("Selected lineage lacks same-group Qp select/reward evidence")
    qp_select = qp_selects[0]
    qp_reward = qp_rewards[0]
    qg_lines = lines(e5 / "qg_events.log")
    qg_actions = [x for x in qg_lines if f":{group}:action:" in x]
    qg_leaders = [x for x in qg_lines if f":{group}:leader:" in x]
    qg_index = min(outer_generation, len(qg_actions) - 1, len(qg_leaders) - 1)
    qg_action = qg_actions[qg_index]
    qg_leader = qg_leaders[qg_index]
    current_fp = fingerprint(marker(cfvf, ":currentFingerprint=", ":currentObjectives="))
    current_obj = objectives(marker(cfvf, ":currentObjectives=", ":pbestFingerprintActual="))
    pbest_fp = fingerprint(marker(cfvf, ":pbestFingerprintActual=", ":pbestObjectives="))
    pbest_obj = objectives(marker(cfvf, ":pbestObjectives=", ":gbestFingerprint="))
    gbest_fp = fingerprint(marker(cfvf, ":gbestFingerprint=", ":gbestObjectives="))
    gbest_obj = objectives(marker(cfvf, ":gbestObjectives=", ":offspringFingerprint="))
    child_fp = fingerprint(marker(cfvf, ":offspringFingerprint=", ":offspringObjectives="))
    child_obj = objectives(marker(cfvf, ":offspringObjectives="))

    cata_context = next(
        x for x in cata_lines
        if f"generation={outer_generation},slot={selected_slot},lineage={selected_lineage}," in x
        and ",context=" in x and ",decision=" in x
    )
    local_parent_fp = fingerprint(marker(cata_result, ",parentFingerprint=", ",parentObjectives="))
    local_parent_obj = objectives(marker(cata_result, ",parentObjectives=", ",localFingerprint="))
    local_fp = fingerprint(marker(cata_result, ",localFingerprint=", ",localObjectives="))
    local_obj = objectives(marker(cata_result, ",localObjectives=", ",fe="))

    current_trace, current_metrics = decode_vectors(manual, base, current_fp)
    child_trace, child_metrics = decode_vectors(manual, base, child_fp)
    parent_trace, parent_metrics = decode_vectors(manual, base, local_parent_fp)
    local_trace, local_metrics = decode_vectors(manual, base, local_fp)
    trace_fields = list(current_trace[0])
    for name, trace in (("cfvf_parent_trace.csv", current_trace), ("cfvf_global_child_trace.csv", child_trace)):
        write_csv(e5 / name, [{field: row[field] for field in trace_fields} for row in trace])
    for name, trace in (("local_parent_trace.csv", parent_trace), ("accepted_local_child_trace.csv", local_trace)):
        write_csv(e6 / name, [{field: row[field] for field in trace_fields} for row in trace])

    write_csv(e5 / "selected_q_events.csv", [
        {"controller": "Qg", "lineage": "social/group", "event": qg_action},
        {"controller": "Qg", "lineage": "social/group", "event": qg_leader},
        {"controller": "Qp", "lineage": selected_lineage, "event": qp_select},
        {"controller": "Qp", "lineage": selected_lineage, "event": qp_reward},
    ])
    write_csv(e5 / "selected_cfvf_transition.csv", [{
        "outerGeneration": outer_generation, "traceGeneration": trace_generation,
        "lineage": selected_lineage,
        "current": str(current_fp), "pbest": str(pbest_fp), "gbest": str(gbest_fp),
        "globalChild": str(child_fp), "currentObjectives": current_obj,
        "pbestObjectives": pbest_obj, "gbestObjectives": gbest_obj,
        "globalChildObjectives": child_obj,
    }])
    write_csv(e6 / "selected_cata_transition.csv", [{
        "outerGeneration": outer_generation, "lineage": selected_lineage,
        "contextEvent": cata_context,
        "resultEvent": cata_result, "parentObjectives": local_parent_obj,
        "localObjectives": local_obj,
    }])
    lineage_selected = [x for x in lines(e7 / "lineage_events.log")
                        if any(token in x for token in
                               ("old=2,", f"lineage={selected_lineage},",
                                f"old={selected_lineage},"))]
    write_csv(e7 / "selected_lineage_and_pddr.csv", [
        {"kind": "lineage", "event": x} for x in lineage_selected[:20]
    ] + [{"kind": "PDDR", "event": x} for x in lines(e7 / "pddr_events.log")[:16]])

    plt.rcParams.update({"font.family": "DejaVu Serif", "font.size": 8,
                         "svg.hashsalt": "zhangbo-i1-20260810",
                         "axes.spines.top": False, "axes.spines.right": False,
                         "pdf.fonttype": 42})
    qp_action = marker(qp_select, ",action=", ",epsilon=")
    qp_state = marker(qp_select, ",state=", ",E=")
    fig, ax = plt.subplots(figsize=(10, 4)); ax.set_xlim(0, 10); ax.set_ylim(0, 4); ax.axis("off")
    box(ax, (.3, 1.35), 2.0, 1.3, "Current X", f"f={tuple(round(x,2) for x in current_obj)}", "#EAF2F8")
    box(ax, (3.0, 2.25), 2.4, 1.25, "Qp cognitive leader",
        f"{qp_action}, state={qp_state}\nf={tuple(round(x,2) for x in pbest_obj)}", "#E8F5E9")
    box(ax, (3.0, .45), 2.4, 1.25, "Qg social leader",
        f"action from {group}\nf={tuple(round(x,2) for x in gbest_obj)}", "#FDEBD0")
    box(ax, (7.0, 1.35), 2.2, 1.3, "CFVF child XG", f"f={tuple(round(x,2) for x in child_obj)}", "#F3E5F5")
    for y in (2.65, 1.05):
        ax.annotate("", xy=(6.95, 2), xytext=(5.45, y), arrowprops=dict(arrowstyle="->", lw=1.3))
    ax.annotate("", xy=(2.95, 2), xytext=(2.35, 2), arrowprops=dict(arrowstyle="->", lw=1.3))
    ax.set_title(f"I1 lineage {selected_lineage}: Qp cognitive and Qg social leadership",
                 weight="bold")
    save_all(fig, figures / "fig07_qp_qg_leader_selection")

    fig, axes = plt.subplots(2, 1, figsize=(11.5, 7.4), sharex=True)
    gantt(axes[0], current_trace, "Before CFVF: current particle")
    gantt(axes[1], child_trace, "After CFVF: global child (FMW/MW/M/W actions)")
    fig.suptitle("CFVF transition for the tracked lineage", weight="bold")
    save_all(fig, figures / "fig08_cfvf_before_after_gantt")

    fig, ax = plt.subplots(figsize=(10.5, 4.3)); ax.set_xlim(0, 10.5); ax.set_ylim(0, 4); ax.axis("off")
    context = marker(cata_context, ",context=", ",factory=")
    factory = int(marker(cata_context, ",factory=", ",factoryMode=")) + 1
    decision = marker(cata_context, ",decision=", ",test=")
    neighborhoods = marker(cata_context, ",neighborhoods=", None)
    labels = [("Context", "\n".join(context.split("|"))), ("Need", f"factory=F{factory}\nweighted draw"),
              ("Decision", f"{decision}\n{neighborhoods}"),
              ("Apply", re.search(r"id=([^,]+)", cata_result).group(1)),
              ("Outcome", f"accepted=true\nQGain={float(marker(cata_result, ',qGain=', ',parentFingerprint=')):.4f}")]
    for i, (title, body) in enumerate(labels):
        x = .15 + i * 2.05; box(ax, (x, 1.25), 1.7, 1.45, title, body, ["#EAF2F8","#FFF2CC","#E8F5E9","#FCE4EC","#EDE7F6"][i])
        if i: ax.annotate("", xy=(x-.05, 1.98), xytext=(x-.32, 1.98), arrowprops=dict(arrowstyle="->"))
    ax.set_title("CA-TA context and Test-and-Apply decision (tracked lineage)", weight="bold")
    save_all(fig, figures / "fig09_cata_test_apply")

    fig, axes = plt.subplots(2, 1, figsize=(11.5, 7.4), sharex=True)
    gantt(axes[0], parent_trace, "Before accepted local search")
    gantt(axes[1], local_trace, "After accepted local search")
    fig.suptitle("Accepted CA-TA local move: schedule and fatigue-aware objectives", weight="bold")
    save_all(fig, figures / "fig10_local_search_before_after_gantt")

    fig, ax = plt.subplots(figsize=(12, 3.7)); ax.set_xlim(0, 12); ax.set_ylim(0, 3); ax.axis("off")
    flow = [("X", current_obj), ("pbest/Qp", pbest_obj), ("gbest/Qg", gbest_obj),
            ("XG", child_obj), ("XL", local_obj), ("PDDR", None)]
    for i, (title, obj) in enumerate(flow):
        body = "selected/split/delete" if obj is None else "(" + ", ".join(f"{x:.1f}" for x in obj) + ")"
        box(ax, (.15 + i*1.95, 1), 1.55, 1.05, title, body, "#F4F6F7")
        if i: ax.annotate("", xy=(.1+i*1.95, 1.52), xytext=(-.15+i*1.95, 1.52), arrowprops=dict(arrowstyle="->"))
    ax.set_title("One traced generation: X → cognitive/social leaders → XG → XL → PDDR", weight="bold")
    save_all(fig, figures / "fig11_complete_generation_flow")

    summary = (
        "schema=zhangbo-i1-derived-evolution-evidence-v1\n"
        "sourceOnly=structured_trace_logs\n"
        f"selectedLineage={selected_lineage}\n"
        f"selectedOuterGeneration={outer_generation}\n"
        f"selectedTraceGeneration={trace_generation}\n"
        f"selectedSubSwarm={group}\n"
        f"selectedQpAction={qp_action}\nselectedQpState={qp_state}\n"
        "qgObserved=true\nqpObserved=true\ncfvfObserved=true\n"
        "caTaObserved=true\npddrObserved=true\nfigures7To11Generated=true\n"
    )
    (e5 / "derived_evidence_summary.properties").write_text(summary, encoding="utf-8", newline="\n")
    print(summary, end="")


if __name__ == "__main__":
    main()
