import csv
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent / "gate-workdir" / "runs"
OFF = ROOT / "obs2k-OFF"
ON = ROOT / "obs2k-ON"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def strict_nd(rows):
    out = []
    for i, row in enumerate(rows):
        dominated = False
        for j, other in enumerate(rows):
            if i == j:
                continue
            if all(other[k] <= row[k] for k in range(3)) and any(
                other[k] < row[k] for k in range(3)
            ):
                dominated = True
                break
        if not dominated:
            out.append(row)
    return sorted(set(out))


byte_equal = [
    "front.csv",
    "passive-archive.csv",
    "cmax-audit-curves.csv",
    "cmax-audit-records.csv",
    "cmax-audit-summary.txt",
    "ca-ta-lite-events.log",
    "dscr-events.csv",
    "dscr-summary.properties",
    "dscr-teacher-uses.csv",
    "bottleneck-pressure-events.csv",
    "initial-population.sha256",
    "profile.sha256",
    "budget-termination.properties",
    "pddr-observation.properties",
]
for name in byte_equal:
    assert sha(OFF / name) == sha(ON / name), name

with (ON / "source-ledger.csv").open(newline="", encoding="utf-8") as fh:
    ledger = list(csv.DictReader(fh))
assert len(ledger) == 100
required = {"actualFE", "nominalFE", "generation", "outerCycle", "qRound"}
assert required.issubset(ledger[0])
assert all(int(row["nominalFE"]) == 25000 for row in ledger)

initial = [(float(r["Cmax"]), float(r["TEC"]), float(r["TWC"])) for r in ledger[:100]]
with (ON / "checkpoints" / "checkpoint-0-observed-full-front.csv").open(
    newline="", encoding="utf-8"
) as fh:
    b0 = sorted(
        set((float(r["Cmax"]), float(r["TEC"]), float(r["TWC"])) for r in csv.DictReader(fh))
    )
assert b0 == strict_nd(initial)

with (ON / "source-lifecycle-events.csv").open(newline="", encoding="utf-8") as fh:
    lifecycle = list(csv.DictReader(fh))
assert len(lifecycle) == 100
assert {r["eventType"] for r in lifecycle} == {"GENERATED"}

with (ON / "checkpoints" / "checkpoint-registry.csv").open(
    newline="", encoding="utf-8"
) as fh:
    registry = list(csv.DictReader(fh))
b0_registry = [r for r in registry if r["checkpointTargetFE"] == "0"]
assert len(b0_registry) == 2
assert all(int(r["frontSize"]) == len(b0) for r in b0_registry)

print("LOCAL_2K_V5_VERIFICATION=PASSED")
print("byteEqualFiles=14")
print(f"ledgerRows={len(ledger)}")
print(f"lifecycleRows={len(lifecycle)}")
print(f"b0StrictNdSize={len(b0)}")
