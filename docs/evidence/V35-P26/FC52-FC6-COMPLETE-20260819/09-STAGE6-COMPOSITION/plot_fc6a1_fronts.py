#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""FC-6A.1 Pareto front visualization (QGS vs A4-Pacing BASE).

Plots the final nondominated fronts from the stage6 composition-audit batch
({20,100}-job x {QGS,BASE} x 3 seeds) as 2D projections + a 3D view, per
instance and per seed, plus a per-instance merged view (3 seeds overlaid).
"""
import csv
import os
import sys

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

RAW = ("E:/学习/李明哲-毕业材料/张博改进/docs/evidence/V35-P26/"
       "FC52-FC6-COMPLETE-20260819/09-STAGE6-COMPOSITION/raw")
OUT = ("E:/学习/李明哲-毕业材料/张博改进/docs/evidence/V35-P26/"
       "FC52-FC6-COMPLETE-20260819/09-STAGE6-COMPOSITION/figures")
SEEDS = ("20260822", "20260823", "20260824")
INSTANCES = ("20_2_3_1", "100_2_3_1")
ARM_STYLE = {
    "QGS": {"color": "#d62728", "marker": "o", "label": "QGS (original)"},
    "BASE": {"color": "#1f77b4", "marker": "^", "label": "A4-Pacing BASE"},
}
os.makedirs(os.path.join(OUT, "per_seed"), exist_ok=True)
os.makedirs(os.path.join(OUT, "merged"), exist_ok=True)


def load_front(instance, arm, seed):
    if arm == "QGS":
        path = os.path.join(RAW, instance, arm, "seed-" + seed,
                            "runs", "seed-" + seed, "HMOPSO_QGS_F", "front.csv")
    else:
        path = os.path.join(RAW, instance, arm, "seed-" + seed, "front.csv")
    pts = []
    with open(path, encoding="utf-8") as fh:
        for row in csv.reader(fh):
            if not row or row[0] == "Cmax":
                continue
            try:
                pts.append((float(row[0]), float(row[1]), float(row[2])))
            except (ValueError, IndexError):
                pass
    return pts


def axes_setup(ax, xlabel, ylabel):
    ax.set_xlabel(xlabel, fontsize=9)
    ax.set_ylabel(ylabel, fontsize=9)
    ax.grid(True, alpha=0.3)
    ax.tick_params(labelsize=8)


def plot_run_2d(axs, points, style, ms=14, alpha=0.75):
    x, y, z = zip(*points)
    axs[0].scatter(x, y, s=ms, color=style["color"], marker=style["marker"],
                   alpha=alpha, edgecolors="none")
    axs[1].scatter(y, z, s=ms, color=style["color"], marker=style["marker"],
                   alpha=alpha, edgecolors="none")
    axs[2].scatter(x, z, s=ms, color=style["color"], marker=style["marker"],
                   alpha=alpha, edgecolors="none")


def plot_run_3d(ax, points, style, alpha=0.5):
    x, y, z = zip(*points)
    ax.scatter(x, y, z, s=8, color=style["color"], marker=style["marker"],
               alpha=alpha, edgecolors="none")


def per_seed_figure(instance, seed):
    fig = plt.figure(figsize=(12.5, 4.4))
    axs = [fig.add_subplot(1, 4, i + 1) for i in range(3)]
    ax3d = fig.add_subplot(1, 4, 4, projection="3d")
    for arm in ("QGS", "BASE"):
        pts = load_front(instance, arm, seed)
        style = dict(ARM_STYLE[arm])
        style["label"] = "%s (n=%d)" % (style["label"], len(pts))
        plot_run_2d(axs, pts, style)
        plot_run_3d(ax3d, pts, style)
    labels = [("Cmax", "TEC"), ("TEC", "TWC"), ("Cmax", "TWC")]
    for ax, (xl, yl) in zip(axs, labels):
        axes_setup(ax, xl, yl)
    ax3d.set_xlabel("Cmax", fontsize=8)
    ax3d.set_ylabel("TEC", fontsize=8)
    ax3d.set_zlabel("TWC", fontsize=8)
    ax3d.tick_params(labelsize=7)
    ax3d.view_init(elev=22, azim=-58)
    handles, lab = axs[0].get_legend_handles_labels()
    fig.legend(handles, lab, loc="upper center", ncol=2, fontsize=9,
               frameon=False, bbox_to_anchor=(0.5, 1.02))
    fig.suptitle("Pareto front — instance %s, seed %s (FC-6A.1 stage6 batch, 500k FE)"
                 % (instance, seed), fontsize=11, y=1.06)
    fig.tight_layout()
    out = os.path.join(OUT, "per_seed", "front_%s_seed%s.png" % (instance, seed))
    fig.savefig(out, dpi=150, bbox_inches="tight")
    plt.close(fig)
    print("wrote", out)


def merged_figure(instance):
    fig = plt.figure(figsize=(12.5, 4.4))
    axs = [fig.add_subplot(1, 4, i + 1) for i in range(3)]
    ax3d = fig.add_subplot(1, 4, 4, projection="3d")
    for arm in ("QGS", "BASE"):
        allpts = []
        for seed in SEEDS:
            allpts.extend(load_front(instance, arm, seed))
        style = dict(ARM_STYLE[arm])
        style["label"] = "%s x3 seeds (n=%d)" % (style["label"], len(allpts))
        plot_run_2d(axs, allpts, style, ms=10, alpha=0.45)
        plot_run_3d(ax3d, allpts, style, alpha=0.3)
    labels = [("Cmax", "TEC"), ("TEC", "TWC"), ("Cmax", "TWC")]
    for ax, (xl, yl) in zip(axs, labels):
        axes_setup(ax, xl, yl)
    ax3d.set_xlabel("Cmax", fontsize=8)
    ax3d.set_ylabel("TEC", fontsize=8)
    ax3d.set_zlabel("TWC", fontsize=8)
    ax3d.tick_params(labelsize=7)
    ax3d.view_init(elev=22, azim=-58)
    handles, lab = axs[0].get_legend_handles_labels()
    fig.legend(handles, lab, loc="upper center", ncol=2, fontsize=9,
               frameon=False, bbox_to_anchor=(0.5, 1.02))
    fig.suptitle("Pareto front — instance %s, 3 seeds overlaid (FC-6A.1 stage6)"
                 % instance, fontsize=11, y=1.06)
    fig.tight_layout()
    out = os.path.join(OUT, "merged", "front_%s_merged.png" % instance)
    fig.savefig(out, dpi=150, bbox_inches="tight")
    plt.close(fig)
    print("wrote", out)


def main():
    for instance in INSTANCES:
        for seed in SEEDS:
            per_seed_figure(instance, seed)
        merged_figure(instance)


if __name__ == "__main__":
    main()
