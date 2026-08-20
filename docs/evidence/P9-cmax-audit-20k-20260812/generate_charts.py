import csv
from collections import Counter
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np


ROOT = Path(__file__).resolve().parent


def read_csv(name):
    with (ROOT / name).open(encoding="utf-8") as stream:
        return list(csv.DictReader(stream))


curves = read_csv("cmax-curves.csv")
records = read_csv("cmax-record-lifecycle.csv")


def values(key):
    return [float(row[key]) if row[key] else np.nan for row in curves]


fe = values("fe")
figure, axis = plt.subplots(figsize=(9.2, 5.2), constrained_layout=True)
axis.step(fe, values("bestCmaxGlobal"), where="post", linewidth=2.2,
          label="Best Cmax: global archive")
axis.step(fe, values("bestCmaxG1"), where="post", linewidth=2.0,
          label="Best Cmax ever observed in G1")
axis.step(fe, values("bestCmaxGenerated"), where="post", linewidth=2.0,
          label="Best Cmax: generated")
axis.step(fe, values("bestCmaxSurvived"), where="post", linewidth=2.0,
          label="Best Cmax: PDDR-survived")
axis.plot(fe, values("currentBestCmaxG1"), "--", linewidth=1.8, color="#7f7f7f",
          label="Current G1 best")
axis.axvline(6750, color="#d62728", linestyle=":", linewidth=1.5)
axis.annotate("last new Cmax record\nFE 6750",
              xy=(6750, 201.278740141651), xytext=(9200, 216),
              arrowprops={"arrowstyle": "->", "color": "#d62728"}, color="#a61b1b")
axis.set_xlabel("Full evaluations (FE)")
axis.set_ylabel("Cmax (lower is better)")
axis.set_title("Observation-only Cmax audit — FULL, 20_2_3_1, seed 20260808")
axis.grid(alpha=0.22)
axis.legend(fontsize=8.2, ncol=2)
figure.savefig(ROOT / "cmax-audit-curves.svg")
figure.savefig(ROOT / "cmax-audit-curves.png", dpi=180)
plt.close(figure)

non_initial = [row for row in records if row["mechanism"] != "INITIAL"]
stages = ["Generated record", "Entered candidate set", "PDDR retained",
          "Personal archive", "Global archive", "Next round survived"]
counts = [
    len(non_initial),
    sum(row["enteredCandidateSet"] == "true" for row in non_initial),
    sum(row["pddrRetained"] == "true" for row in non_initial),
    sum(row["personalArchive"] == "true" for row in non_initial),
    sum(row["globalArchive"] == "true" for row in non_initial),
    sum(row["nextRoundSurvival"] == "YES" for row in non_initial),
]
figure, axes = plt.subplots(1, 2, figsize=(10.8, 4.6), constrained_layout=True)
colors = ["#4c78a8", "#4c78a8", "#4c78a8", "#4c78a8", "#f58518", "#4c78a8"]
axes[0].barh(stages[::-1], counts[::-1], color=colors[::-1])
axes[0].set_xlim(0, max(counts) + 1.5)
axes[0].set_xlabel("Strict new-Cmax records")
axes[0].set_title("Record lifecycle (initial population excluded)")
for index, count in enumerate(counts[::-1]):
    axes[0].text(count + 0.12, index, str(count), va="center")

sources = Counter(row["mechanism"] + "/" + row["operator"] for row in records)
labels = list(sources.keys())
source_counts = list(sources.values())
axes[1].bar(range(len(labels)), source_counts, color="#59a14f")
axes[1].set_xticks(range(len(labels)), labels, rotation=27, ha="right")
axes[1].set_ylabel("Strict new-Cmax records")
axes[1].set_title("Where Cmax records were generated")
for index, count in enumerate(source_counts):
    axes[1].text(index, count + 0.08, str(count), ha="center")
figure.savefig(ROOT / "cmax-audit-lifecycle.svg")
figure.savefig(ROOT / "cmax-audit-lifecycle.png", dpi=180)
plt.close(figure)

print({"lifecycle": counts, "sources": dict(sources),
       "last_record_fe": records[-1]["evaluation"], "best_cmax": records[-1]["cmax"]})
