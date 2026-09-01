import csv
import hashlib
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent / "results"
OFF = ROOT / "obs20k-OFF"
ON = ROOT / "obs20k-ON"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def props(path):
    out = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if "=" in line:
            key, value = line.split("=", 1)
            out[key] = value
    return out


def verify_manifest(run):
    with (run / "evidence-sha256.tsv").open(newline="", encoding="utf-8") as fh:
        rows = list(csv.DictReader(fh, delimiter="\t"))
    for row in rows:
        path = run / row["path"]
        assert path.exists(), path
        assert sha(path) == row["sha256"].lower(), path
    return len(rows)


def strict_nd(rows):
    out = []
    for i, row in enumerate(rows):
        if not any(
            i != j
            and all(other[k] <= row[k] for k in range(3))
            and any(other[k] < row[k] for k in range(3))
            for j, other in enumerate(rows)
        ):
            out.append(row)
    return sorted(set(out))


manifest_entries = {run.name: verify_manifest(run) for run in (OFF, ON)}
for run in (OFF, ON):
    gate = props(run / "formal-gate.properties")
    assert gate["status"] == "COMPLETED"
    assert gate["failures"] == "NONE"
    assert gate["actualFE"] == gate["decoderCalls"] == "15258"

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


def masked_configuration(path):
    ignored = ("observerMode=", "telemetryLedgerRows=", "telemetryPddrRounds=")
    return "\n".join(
        line for line in path.read_text(encoding="utf-8").splitlines()
        if not line.startswith(ignored)
    )


def masked_status(path):
    lines = []
    for line in path.read_text(encoding="utf-8").splitlines():
        if line.startswith(("algorithmRunNanos=", "decoderTiming=")):
            continue
        for key in (
            "algorithmRunNanos", "baseDecodeNanos", "leftShiftNanos",
            "rightShiftNanos", "decoderTotalNanos", "frameworkOverheadNanos",
        ):
            line = re.sub(r"(?<=[=,])" + key + r"=\d+", key + "=<MEASURED>", line)
        lines.append(line)
    return "\n".join(lines)


assert masked_configuration(OFF / "configuration.txt") == masked_configuration(ON / "configuration.txt")
assert masked_status(OFF / "status.properties") == masked_status(ON / "status.properties")

with (ON / "source-ledger.csv").open(newline="", encoding="utf-8") as fh:
    ledger = list(csv.DictReader(fh))
assert len(ledger) == 15258
assert {r["nominalFE"] for r in ledger} == {"25000"}
assert {"generation", "outerCycle", "qRound"}.issubset(ledger[0])
assert all(r["firstLevelSource"] != "UNSET" for r in ledger)

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
required_events = {
    "GENERATED",
    "DESCENDANT",
    "IMPROVING_DESCENDANT",
    "MERGE_POOL",
    "PDDR_SELECTED",
    "WORKING_POPULATION",
    "PERSONAL_ARCHIVE",
    "QG_TEACHER",
    "QP_TEACHER",
    "QP_ACTION",
}
event_types = {r["eventType"] for r in lifecycle}
assert required_events.issubset(event_types)

gate_on = props(ON / "formal-gate.properties")
assert int(gate_on["telemetryLifecycleRows"]) == len(lifecycle)

memory_off = props(OFF / "memory-summary.properties")
memory_on = props(ON / "memory-summary.properties")
off_peak = int(memory_off["heapUsedPeak"])
on_peak = int(memory_on["heapUsedPeak"])
bounded_cap = 2359296
unflushed_cap = 25000 * 1024
safety = 256 * 1024 * 1024
estimate = off_peak + max(0, on_peak - off_peak) + bounded_cap + unflushed_cap + safety
ratio = estimate / float(4 * 1024**3)
assert ratio < 0.60

print("REMOTE_20K_V5_VERIFICATION=PASSED")
print("manifestEntriesOFF=%d" % manifest_entries[OFF.name])
print("manifestEntriesON=%d" % manifest_entries[ON.name])
print("byteEqualFiles=%d" % len(byte_equal))
print("maskedEquivalentFiles=2")
print("ledgerRows=%d" % len(ledger))
print("lifecycleRows=%d" % len(lifecycle))
print("lifecycleEventTypes=%d" % len(event_types))
print("b0StrictNdSize=%d" % len(b0))
print("estimated500kPeak=%d" % estimate)
print("memoryRatio=%.12f" % ratio)
