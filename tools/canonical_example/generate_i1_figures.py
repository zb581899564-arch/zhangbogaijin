#!/usr/bin/env python3
"""Generate source-locked publication figures for the I1 running example."""

from __future__ import annotations

import argparse
import csv
import math
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch, Rectangle


SVG_DATE = "2026-08-10T00:00:00Z"
PDF_DATE = datetime(2026, 8, 10, tzinfo=timezone.utc)


COLORS = {
    "blue": "#0072B2", "orange": "#E69F00", "green": "#009E73",
    "red": "#D55E00", "purple": "#CC79A7", "sky": "#56B4E9",
    "yellow": "#F0E442", "black": "#222222", "gray": "#BDBDBD",
}


def read_csv(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as stream:
        return list(csv.DictReader(stream))


def save_all(fig: plt.Figure, stem: Path) -> None:
    stem.parent.mkdir(parents=True, exist_ok=True)
    fig.savefig(stem.with_suffix(".svg"), bbox_inches="tight",
                metadata={"Date": SVG_DATE})
    fig.savefig(stem.with_suffix(".pdf"), bbox_inches="tight",
                metadata={"CreationDate": PDF_DATE, "ModDate": PDF_DATE})
    fig.savefig(stem.with_suffix(".png"), dpi=300, bbox_inches="tight")
    plt.close(fig)


def setup_style() -> None:
    plt.rcParams.update({
        "font.family": "DejaVu Serif", "font.size": 9, "axes.titlesize": 11,
        "axes.labelsize": 9, "legend.fontsize": 8, "figure.dpi": 120,
        "axes.spines.top": False, "axes.spines.right": False,
        "svg.hashsalt": "zhangbo-i1-20260810",
        "pdf.fonttype": 42, "ps.fonttype": 42,
    })


def structure_figure(root: Path, out: Path) -> None:
    machines = read_csv(root / "01_input/machine_data.csv")
    workers = read_csv(root / "01_input/worker_fatigue_data.csv")
    fig, ax = plt.subplots(figsize=(9.0, 4.6))
    ax.set_xlim(0, 10); ax.set_ylim(0, 5); ax.axis("off")
    for f in range(2):
        x0 = 0.35 + f * 5.0
        ax.add_patch(FancyBboxPatch((x0, 0.35), 4.3, 4.2, boxstyle="round,pad=0.02",
                                    facecolor="#F7FAFC", edgecolor=COLORS["blue"], linewidth=1.5))
        ax.text(x0 + 2.15, 4.25, f"Factory F{f + 1}", ha="center", va="center", weight="bold")
        for k in range(2):
            y = 2.35 - k * 1.55
            ax.add_patch(FancyBboxPatch((x0 + 0.25, y), 3.8, 1.1, boxstyle="round,pad=0.02",
                                        facecolor="#EAF2F8" if k == 0 else "#EEF7EE",
                                        edgecolor=COLORS["black"], linewidth=0.8))
            stage_m = [r for r in machines if int(r["factory"]) == f and int(r["stage"]) == k]
            stage_w = [r for r in workers if int(r["factory"]) == f and int(r["stage"]) == k and r["eligible"] == "true"]
            machine_text = ", ".join(f"M{int(r['machine']) + 1}(v={float(r['speed']):.1f})" for r in stage_m)
            worker_text = ", ".join(f"W{int(r['worker']) + 1}(e={float(r['efficiency']):.1f})" for r in stage_w)
            ax.text(x0 + 0.45, y + 0.78, f"Stage {k + 1}", weight="bold")
            ax.text(x0 + 0.45, y + 0.48, machine_text, fontsize=8)
            ax.text(x0 + 0.45, y + 0.20, worker_text, fontsize=8)
        ax.annotate("", xy=(x0 + 2.15, 2.25), xytext=(x0 + 2.15, 2.37),
                    arrowprops=dict(arrowstyle="->", color=COLORS["red"], lw=1.5))
    ax.text(5.0, 4.85, "I1 resource structure", ha="center", va="top", fontsize=12, weight="bold")
    save_all(fig, out / "fig01_i1_resource_structure")


def encoding_figure(root: Path, out: Path) -> None:
    vectors = {r["vector"]: r["values"].split(",") for r in read_csv(root / "01_input/X0-zero-based.csv")}
    fig, ax = plt.subplots(figsize=(10.2, 3.3))
    ax.set_xlim(-1.2, 10); ax.set_ylim(-0.8, 4.5); ax.axis("off")
    row_colors = ["#D9EAF7", "#FDEBD0", "#E8F5E9", "#F3E5F5"]
    for r, name in enumerate(("JS", "FA", "MA", "WA")):
        y = 3.4 - r
        ax.text(-0.35, y + 0.32, name, ha="right", va="center", weight="bold")
        for p, value in enumerate(vectors[name]):
            ax.add_patch(Rectangle((p, y), 0.92, 0.65, facecolor=row_colors[r], edgecolor="#4D4D4D", lw=0.7))
            display = int(value) + 1
            ax.text(p + 0.46, y + 0.32, str(display), ha="center", va="center")
    for p in range(10):
        ax.text(p + 0.46, 4.18, str(p + 1), ha="center", fontsize=8, color="#555555")
    ax.text(-0.35, 4.18, "Position", ha="right", fontsize=8, color="#555555")
    ax.text(4.6, -0.38, "All values are displayed one-based; runtime storage is zero-based.",
            ha="center", fontsize=8, style="italic")
    ax.set_title("Golden particle X0: position-aligned four-vector encoding", weight="bold")
    save_all(fig, out / "fig02_x0_four_vector_encoding")


def gantt_figure(trace_path: Path, out_stem: Path, title: str) -> None:
    trace = read_csv(trace_path)
    lanes = sorted({(int(r["factory"]), int(r["stage"]), int(r["machine"])) for r in trace})
    lane_index = {lane: i for i, lane in enumerate(lanes)}
    fig, ax = plt.subplots(figsize=(11.5, max(4.8, 0.5 * len(lanes))))
    for row in trace:
        lane = (int(row["factory"]), int(row["stage"]), int(row["machine"]))
        y = lane_index[lane]
        start = float(row["start"]); setup = float(row["actualSetup"]); process = float(row["actualProcessing"])
        ax.barh(y, setup, left=start, height=0.62, color=COLORS["orange"], edgecolor="white", linewidth=0.5)
        ax.barh(y, process, left=start + setup, height=0.62, color=COLORS["blue"], edgecolor="white", linewidth=0.5)
        ax.text(start + (setup + process) / 2, y, f"J{int(row['job']) + 1}/W{int(row['worker']) + 1}",
                ha="center", va="center", fontsize=6.5, color="white", weight="bold")
    ax.set_yticks(range(len(lanes)))
    ax.set_yticklabels([f"F{f + 1}-S{k + 1}-M{m + 1}" for f, k, m in lanes])
    ax.invert_yaxis(); ax.set_xlabel("Time"); ax.set_ylabel("Machine timeline")
    ax.set_title(title, weight="bold")
    ax.grid(axis="x", alpha=0.22)
    handles = [Rectangle((0, 0), 1, 1, facecolor=COLORS["orange"]), Rectangle((0, 0), 1, 1, facecolor=COLORS["blue"])]
    ax.legend(handles, ["Setup", "Processing"], loc="upper right", frameon=False)
    save_all(fig, out_stem)


def fatigue_figure(root: Path, out: Path) -> None:
    trace = read_csv(root / "02_decoder_fm3/program_trace.csv")
    cmax = max(float(r["end"]) for r in trace)
    grouped: dict[tuple[int, int], list[dict[str, str]]] = defaultdict(list)
    for row in trace:
        grouped[(int(row["factory"]), int(row["worker"]))].append(row)
    fig, axes = plt.subplots(2, 1, figsize=(10.5, 6.2), sharex=True)
    palette = [COLORS["blue"], COLORS["orange"], COLORS["green"], COLORS["red"]]
    for f, ax in enumerate(axes):
        for worker in range(4):
            ops = sorted(grouped[(f, worker)], key=lambda r: float(r["start"]))
            t_values, f_values = [0.0], [0.0]
            previous_end, previous_fatigue, previous_mu = 0.0, 0.0, None
            for op in ops:
                start = float(op["start"]); at_start = float(op["fatigueAtStart"])
                if start > previous_end and previous_mu is not None:
                    for i in range(1, 31):
                        t = previous_end + (start - previous_end) * i / 30
                        t_values.append(t); f_values.append(previous_fatigue * math.exp(-previous_mu * (t - previous_end)))
                elif start > previous_end:
                    t_values.extend([start]); f_values.extend([at_start])
                lam = float(op["lambda"]); duration = float(op["actualAT"])
                for i in range(1, 41):
                    dt = duration * i / 40
                    value = at_start + (1 - at_start) * (1 - math.exp(-lam * dt))
                    t_values.append(start + dt); f_values.append(value)
                previous_end = float(op["end"]); previous_fatigue = float(op["fatigueAfter"])
                previous_mu = float(op["muCurrentStage"])
            if previous_mu is not None and cmax > previous_end:
                for i in range(1, 31):
                    t = previous_end + (cmax - previous_end) * i / 30
                    t_values.append(t); f_values.append(previous_fatigue * math.exp(-previous_mu * (t - previous_end)))
            ax.plot(t_values, f_values, color=palette[worker], lw=1.6, label=f"W{worker + 1}")
        ax.axhline(0.8, color="#555555", ls="--", lw=1, label="Fwarn=0.80" if f == 0 else None)
        ax.axhline(0.9, color="#999999", ls=":", lw=1, label="Fsafe=0.90" if f == 0 else None)
        ax.set_ylabel(f"Factory {f + 1}\nFatigue")
        ax.set_ylim(0, 1.0); ax.grid(alpha=0.2)
        ax.legend(ncol=6, frameon=False, loc="upper right")
    axes[-1].set_xlabel("Time")
    axes[0].set_title("Worker fatigue accumulation and natural recovery under FM3", weight="bold")
    save_all(fig, out / "fig05_worker_fatigue_trajectories")


def comparison_figure(root: Path, out: Path) -> None:
    rows = read_csv(root / "04_fm0_regression/fm0_vs_fm3.csv")
    metrics = [r["metric"] for r in rows[:3]]
    fm0 = [float(r["FM0"]) for r in rows[:3]]
    fm3 = [float(r["FM3"]) for r in rows[:3]]
    normalized_fm0 = [1.0] * 3
    normalized_fm3 = [b / a for a, b in zip(fm0, fm3)]
    fig, ax = plt.subplots(figsize=(6.8, 4.3))
    x = range(3); width = 0.34
    ax.bar([i - width / 2 for i in x], normalized_fm0, width, color=COLORS["gray"], label="FM0")
    ax.bar([i + width / 2 for i in x], normalized_fm3, width, color=COLORS["blue"], label="FM3")
    for i, value in enumerate(normalized_fm3):
        ax.text(i + width / 2, value + 0.015, f"{value:.3f}×", ha="center", fontsize=8)
    ax.set_xticks(list(x)); ax.set_xticklabels(metrics); ax.set_ylabel("Ratio to FM0")
    ax.set_ylim(0, max(normalized_fm3) * 1.15); ax.grid(axis="y", alpha=0.2)
    ax.legend(frameon=False); ax.set_title("FM0 vs FM3 objective comparison for X0", weight="bold")
    save_all(fig, out / "fig06_fm0_fm3_objective_comparison")


def decode_flow_figure(out: Path) -> None:
    steps = [
        "JS/FA\njob and factory", "Stage 1\nexplicit MA/WA", "Later stages\nECT/FIFO/FAM",
        "Common machine–worker\navailability", "Natural\nrecovery", "PT0+SET0\nand multiplier",
        "Fatigue\naccumulation", "Objectives\nand diagnostics",
    ]
    fig, ax = plt.subplots(figsize=(11.5, 2.5)); ax.set_xlim(0, len(steps)); ax.set_ylim(0, 1); ax.axis("off")
    for i, step in enumerate(steps):
        color = "#EAF2F8" if i < 4 else "#E8F5E9"
        ax.add_patch(FancyBboxPatch((i + 0.07, 0.28), 0.86, 0.44, boxstyle="round,pad=0.02",
                                    facecolor=color, edgecolor=COLORS["blue"] if i < 4 else COLORS["green"], lw=1))
        ax.text(i + 0.5, 0.5, step, ha="center", va="center", fontsize=7.8)
        if i < len(steps) - 1:
            ax.annotate("", xy=(i + 1.06, 0.5), xytext=(i + 0.94, 0.5), arrowprops=dict(arrowstyle="->", lw=1))
    ax.set_title("FM3 canonical decoding sequence", weight="bold")
    save_all(fig, out / "fig03_fm3_decoding_flow")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--evidence-root", type=Path, required=True)
    args = parser.parse_args()
    root = args.evidence_root.resolve(); out = root / "08_figures"
    setup_style()
    structure_figure(root, out)
    encoding_figure(root, out)
    decode_flow_figure(out)
    gantt_figure(root / "02_decoder_fm3/program_trace.csv", out / "fig04_fm3_machine_gantt", "I1/X0 machine Gantt chart under FM3")
    fatigue_figure(root, out)
    comparison_figure(root, out)
    gantt_figure(root / "04_fm0_regression/program_trace.csv", out / "fig06b_fm0_machine_gantt", "I1/X0 machine Gantt chart under FM0")
    print(f"FIGURES_CREATED {out}")


if __name__ == "__main__":
    main()
