#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Offline correction of the FC5-T 100k retention and teacher-use analysis.

This script never runs the optimizer.  It reads the six accepted 100k runs and
reconstructs only quantities supported by the persisted telemetry.
"""

from __future__ import print_function

import argparse
import csv
import hashlib
import math
from collections import Counter, defaultdict
from pathlib import Path


REP_TAIL_COLUMNS = 20
POPULATION = 100


def as_int(value, default=-1):
    try:
        return int(value)
    except (TypeError, ValueError):
        return default


def as_float(value, default=float("nan")):
    try:
        return float(value)
    except (TypeError, ValueError):
        return default


def as_bool(value):
    return str(value).strip().lower() == "true"


def window_of_fe(fe):
    return "W1_0_50K" if fe <= 50000 else "W2_50K_100K"


def parse_props(path):
    result = {}
    if not path.exists():
        return result
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            result[key.strip()] = value.strip()
    return result


def parse_representatives(path):
    rows = []
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("seed,cycle,fe,representative"):
            continue
        parts = line.split(",")
        if len(parts) < 27:
            raise ValueError("Malformed representative row in %s" % path)
        head = parts[:6]
        tail = parts[-REP_TAIL_COLUMNS:]
        rows.append({
            "seed": as_int(head[0]), "cycle": as_int(head[1]),
            "fe": as_int(head[2]), "representative": head[3],
            "poolIndex": as_int(head[4]), "source": head[5],
            "fingerprint": ",".join(parts[6:-REP_TAIL_COLUMNS]),
            "Cmax": as_float(tail[0]), "TEC": as_float(tail[1]),
            "TWC": as_float(tail[2]), "pddrScore": as_float(tail[3]),
            "pddrRank": as_int(tail[4]), "poolPresent": as_bool(tail[5]),
            "pddrSelected": as_bool(tail[6]), "rejectReason": tail[7],
            "nextPopulationSlot": as_int(tail[8]), "nextSemanticRole": tail[9],
            "qgTeacherUses": as_int(tail[10], 0),
            "qpTeacherUses": as_int(tail[11], 0),
            "teacherUseCycles": tail[12],
            "improvedOffspringCount": as_int(tail[13], 0),
            "lastImprovementFE": as_int(tail[14]),
            "lastImprovementTeacherKind": tail[15],
            "lastImprovementRequestingRole": tail[16],
            "lastTeacherFE": as_int(tail[17]), "lastTeacherRole": tail[18],
            "retiredAtCycle": as_int(tail[19]),
        })
    return rows


def parse_merge_rounds(path):
    rows = []
    with path.open("r", encoding="utf-8", newline="") as handle:
        for row in csv.DictReader(handle):
            rows.append({key: as_int(row[key]) for key in
                         ("seed", "cycle", "fe", "Nmerge", "Nunique", "Nnd")})
            rows[-1]["Roverflow"] = as_float(row["Roverflow"])
    return rows


def parse_archive_gaps(path):
    rows = []
    with path.open("r", encoding="utf-8", newline="") as handle:
        for row in csv.DictReader(handle):
            parsed = {"seed": as_int(row["seed"]), "cycle": as_int(row["cycle"]),
                      "fe": as_int(row["fe"])}
            for key in ("workingBestCmax", "archiveBestCmax", "cmaxGap",
                        "workingBestTEC", "archiveBestTEC", "tecGap",
                        "workingBestTWC", "archiveBestTWC", "twcGap"):
                parsed[key] = as_float(row[key])
            rows.append(parsed)
    return rows


def parse_dscr_teacher_uses(path):
    """The unquoted teacherId contains commas; parse fixed head and tail."""
    rows = []
    if not path.exists():
        return rows
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("decisionCycle,generation"):
            continue
        parts = line.split(",")
        if len(parts) < 8:
            raise ValueError("Malformed DSCR teacher row in %s" % path)
        rows.append({
            "decisionCycle": as_int(parts[0]), "generation": as_int(parts[1]),
            "fe": as_int(parts[2]), "group": parts[3],
            "teacherId": ",".join(parts[4:-3]),
            "teacherObjectives": parts[-3], "dominated": as_bool(parts[-2]),
            "dominatorCount": as_int(parts[-1], 0),
        })
    return rows


def write_csv(path, rows, fields):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)


def ratio(numerator, denominator):
    return "" if denominator == 0 else "%.12g" % (float(numerator) / denominator)


def entropy(counts):
    total = sum(counts)
    if total <= 0:
        return 0.0, 0.0
    probabilities = [float(value) / total for value in counts if value > 0]
    raw = -sum(value * math.log(value) for value in probabilities)
    normalized = raw / math.log(len(probabilities)) if len(probabilities) > 1 else 0.0
    return raw, normalized


def run_dirs(root):
    return sorted(path.parent for path in root.rglob("fc5-transfer-directional-representative-lifecycle.csv"))


def arm_name(run_dir):
    return "A2" if run_dir.name == "A2_CFVF" else "A4"


def unique_representatives(rows):
    grouped = defaultdict(list)
    for row in rows:
        grouped[(row["cycle"], row["fingerprint"])].append(row)
    result = []
    for (cycle, fingerprint), members in sorted(grouped.items()):
        selected_values = {item["pddrSelected"] for item in members}
        slots = {item["nextPopulationSlot"] for item in members}
        if len(selected_values) != 1 or len(slots) != 1:
            raise ValueError("Duplicate direction records disagree for cycle/fingerprint")
        teacher_sequences = {item["teacherUseCycles"] for item in members}
        if len(teacher_sequences) != 1:
            raise ValueError("Duplicate direction records disagree on teacher-use sequence")
        result.append({
            "cycle": cycle, "fe": members[0]["fe"], "fingerprint": fingerprint,
            "labels": ";".join(sorted(item["representative"] for item in members)),
            "selected": members[0]["pddrSelected"],
            "slot": members[0]["nextPopulationSlot"],
            "role": members[0]["nextSemanticRole"],
            "retiredAtCycle": max(item["retiredAtCycle"] for item in members),
            "teacherUseCycles": members[0]["teacherUseCycles"],
            "qgTeacherUses": max(item["qgTeacherUses"] for item in members),
            "qpTeacherUses": max(item["qpTeacherUses"] for item in members),
            "improvedAnyDirection": any(item["improvedOffspringCount"] > 0 for item in members),
            "directionalImprovementEvents": sum(item["improvedOffspringCount"] for item in members),
        })
    return result


def transition_rows(seed, arm, rows, granularity):
    max_cycle = max(row["cycle"] for row in rows)
    events = []
    if granularity == "DIRECTION_LABEL_EVENT":
        source = [{
            "cycle": row["cycle"], "fe": row["fe"], "fingerprint": row["fingerprint"],
            "labels": row["representative"], "selected": row["pddrSelected"],
            "slot": row["nextPopulationSlot"], "role": row["nextSemanticRole"],
            "retiredAtCycle": row["retiredAtCycle"],
            "teacherUseCycles": row["teacherUseCycles"],
            "qgTeacherUses": row["qgTeacherUses"], "qpTeacherUses": row["qpTeacherUses"],
            "improvedAnyDirection": row["improvedOffspringCount"] > 0,
            "directionalImprovementEvents": row["improvedOffspringCount"],
        } for row in rows]
    else:
        source = unique_representatives(rows)
    for row in source:
        selected = bool(row["selected"])
        entered_next = selected and row["slot"] > 0
        next_cycle_eligible = entered_next and row["cycle"] < max_cycle
        next_cycle_survived = (next_cycle_eligible and
                               (row["retiredAtCycle"] < 0 or row["retiredAtCycle"] > row["cycle"] + 1))
        exposure = row["qgTeacherUses"] + row["qpTeacherUses"]
        events.append({
            "seed": seed, "arm": arm, "window": window_of_fe(row["fe"]),
            "granularity": granularity, "cycle": row["cycle"], "fe": row["fe"],
            "labels": row["labels"], "fingerprintSha256": hashlib.sha256(
                row["fingerprint"].encode("utf-8")).hexdigest(),
            "poolPresent": 1, "pddrSelected": int(selected),
            "enteredNextPopulation": int(entered_next),
            "nextCycleEligible": int(next_cycle_eligible),
            "survivedNextCycle": int(next_cycle_survived) if next_cycle_eligible else "",
            "teacherUsed": int(exposure > 0), "teacherExposure": exposure,
            "improvedAnyDirection": int(row["improvedAnyDirection"]),
            "directionalImprovementEvents": row["directionalImprovementEvents"],
        })
    return events


def aggregate_transitions(events):
    result = []
    grouped = defaultdict(list)
    for event in events:
        grouped[(event["seed"], event["arm"], event["window"], event["granularity"])].append(event)
    for key, rows in sorted(grouped.items()):
        pool = len(rows)
        selected = sum(row["pddrSelected"] for row in rows)
        entered = sum(row["enteredNextPopulation"] for row in rows)
        next_eligible = sum(row["nextCycleEligible"] for row in rows)
        next_survived = sum(row["survivedNextCycle"] for row in rows
                            if row["survivedNextCycle"] != "")
        used = sum(row["teacherUsed"] for row in rows if row["enteredNextPopulation"])
        improved = sum(row["improvedAnyDirection"] for row in rows if row["teacherUsed"])
        exposure = sum(row["teacherExposure"] for row in rows)
        improvements = sum(row["directionalImprovementEvents"] for row in rows)
        result.append({
            "seed": key[0], "arm": key[1], "window": key[2], "granularity": key[3],
            "poolRepresentatives": pool, "pddrSelected": selected,
            "poolToPddrRate": ratio(selected, pool),
            "enteredNextPopulation": entered, "pddrToNextRate": ratio(entered, selected),
            "poolToNextRate": ratio(entered, pool),
            "nextCycleEligible": next_eligible, "survivedNextCycle": next_survived,
            "nextToNextCycleRate": ratio(next_survived, next_eligible),
            "nextPopulationTeacherEligible": entered, "teacherUsed": used,
            "nextToTeacherRate": ratio(used, entered),
            "teacherEligible": used, "teacherProducedDirectionalImprovement": improved,
            "teacherToImprovementRate": ratio(improved, used),
            "teacherExposure": exposure, "directionalImprovementEvents": improvements,
            "directionalImprovementEventsPerExposure": ratio(improvements, exposure),
        })
    return result


def rejected_events(seed, arm, rows, gaps):
    by_cycle = defaultdict(list)
    for row in rows:
        by_cycle[row["cycle"]].append(row)
    gap_by_cycle = {row["cycle"]: row for row in gaps}
    output = []
    for row in rows:
        if row["pddrSelected"]:
            continue
        peers = by_cycle[row["cycle"]]
        same_fp = [peer for peer in peers if peer["fingerprint"] == row["fingerprint"]
                   and peer["representative"] != row["representative"]]
        selected_same_fp = [peer for peer in same_fp if peer["pddrSelected"]]
        gap = gap_by_cycle.get(row["cycle"], {})
        direction = {"E_C": "Cmax", "E_E": "TEC", "E_W": "TWC", "E_B": "BALANCED"}[row["representative"]]
        archive_key = {"E_C": "archiveBestCmax", "E_E": "archiveBestTEC", "E_W": "archiveBestTWC"}.get(row["representative"])
        working_key = {"E_C": "workingBestCmax", "E_E": "workingBestTEC", "E_W": "workingBestTWC"}.get(row["representative"])
        objective_key = {"E_C": "Cmax", "E_E": "TEC", "E_W": "TWC"}.get(row["representative"])
        archive_equal = "NOT_COMPUTABLE_FOR_BALANCED"
        working_equal = "NOT_COMPUTABLE_FOR_BALANCED"
        if objective_key and gap:
            archive_equal = str(abs(gap[archive_key] - row[objective_key]) <= 1e-9).lower()
            working_equal = str(abs(gap[working_key] - row[objective_key]) <= 1e-9).lower()
        output.append({
            "seed": seed, "arm": arm, "cycle": row["cycle"], "fe": row["fe"],
            "representative": row["representative"], "direction": direction,
            "source": row["source"], "fingerprintSha256": hashlib.sha256(
                row["fingerprint"].encode("utf-8")).hexdigest(),
            "Cmax": row["Cmax"], "TEC": row["TEC"], "TWC": row["TWC"],
            "pddrScore": row["pddrScore"], "pddrRank": row["pddrRank"],
            "cutoffRank": POPULATION, "rankBeyondCutoff": row["pddrRank"] - POPULATION,
            "cutoffScore": "UNAVAILABLE_FULL_POOL_SCORE_LEDGER_NOT_PERSISTED",
            "displacedBy": "UNAVAILABLE_SELECTED_IDENTITY_LEDGER_NOT_PERSISTED",
            "equivalentDirectionalLabels": ";".join(sorted(peer["representative"] for peer in same_fp)),
            "equivalentDirectionalPeerSelected": str(bool(selected_same_fp)).lower(),
            "equivalentSelectedNonRepresentative": "UNAVAILABLE_SELECTED_FINGERPRINT_SET_NOT_PERSISTED",
            "workingDirectionEqualAfter": working_equal,
            "archiveDirectionEqualAfter": archive_equal,
            "rejectReason": row["rejectReason"],
        })
    return output


def normalized_teacher_metrics(seed, arm, actual_fe, merge_rows, rep_rows, dscr_rows):
    result = []
    cycles_by_window = Counter(window_of_fe(row["fe"]) for row in merge_rows)
    fe_span = {"W1_0_50K": min(50000, actual_fe),
               "W2_50K_100K": max(0, actual_fe - 50000)}

    # All Qg uses are reconstructed directly from dscr-teacher-uses.csv.
    for window in ("W1_0_50K", "W2_50K_100K"):
        rows = [row for row in dscr_rows if window_of_fe(row["fe"]) == window]
        counts = Counter(row["teacherId"] for row in rows)
        ordered = sorted(counts.values(), reverse=True)
        total = sum(ordered)
        raw_entropy, normalized_entropy = entropy(ordered)
        result.append({
            "seed": seed, "arm": arm, "window": window, "scope": "ALL_QG_TEACHERS",
            "pddrCycles": cycles_by_window[window], "feSpan": fe_span[window],
            "exposures": total, "exposuresPerCycle": ratio(total, cycles_by_window[window]),
            "exposuresPer1000FE": ratio(total * 1000, fe_span[window]),
            "uniqueTeachers": len(counts), "top1Share": ratio(sum(ordered[:1]), total),
            "top5Share": ratio(sum(ordered[:5]), total), "entropy": "%.12g" % raw_entropy,
            "normalizedEntropy": "%.12g" % normalized_entropy,
            "directionalImprovementEvents": "NOT_AVAILABLE_IN_DSCR_TEACHER_LOG",
            "improvementEventsPerExposure": "NOT_AVAILABLE_IN_DSCR_TEACHER_LOG",
        })

    # Directional representatives: de-duplicate a solution serving several labels.
    unique_rows = unique_representatives(rep_rows)
    cycle_to_fe = {row["cycle"]: row["fe"] for row in merge_rows}
    # A fingerprint may be selected in several PDDR rounds.  The observer then
    # increments every still-live historical Representative record for one real
    # teacher use.  Summing origin records would therefore multiply exposures.
    # For each (fingerprint, use-cycle), the maximum multiplicity across origin
    # records is the observable lower-ambiguity reconstruction of real uses.
    use_multiplicity = defaultdict(Counter)
    for row in unique_rows:
        observed = Counter(as_int(value) for value in row["teacherUseCycles"].split(";") if value)
        for cycle, count in observed.items():
            use_multiplicity[(row["fingerprint"], cycle)]["max"] = max(
                use_multiplicity[(row["fingerprint"], cycle)].get("max", 0), count)
    exposures_by_window = defaultdict(Counter)
    for (fingerprint, cycle), counts in use_multiplicity.items():
        fe = cycle_to_fe.get(cycle)
        if fe is not None:
            exposures_by_window[window_of_fe(fe)][fingerprint] += counts["max"]
    for window in ("W1_0_50K", "W2_50K_100K"):
        counts = exposures_by_window[window]
        ordered = sorted(counts.values(), reverse=True)
        total = sum(ordered)
        raw_entropy, normalized_entropy = entropy(ordered)
        result.append({
            "seed": seed, "arm": arm, "window": window,
            "scope": "DIRECTIONAL_REPRESENTATIVE_TEACHERS",
            "pddrCycles": cycles_by_window[window], "feSpan": fe_span[window],
            "exposures": total, "exposuresPerCycle": ratio(total, cycles_by_window[window]),
            "exposuresPer1000FE": ratio(total * 1000, fe_span[window]),
            "uniqueTeachers": len(counts), "top1Share": ratio(sum(ordered[:1]), total),
            "top5Share": ratio(sum(ordered[:5]), total), "entropy": "%.12g" % raw_entropy,
            "normalizedEntropy": "%.12g" % normalized_entropy,
            "directionalImprovementEvents": "ONLY_LIFETIME_TOTAL_AVAILABLE",
            "improvementEventsPerExposure": "NOT_WINDOW_IDENTIFIABLE",
        })
    return result


def sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest().upper()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    parser.add_argument("--reference", required=True, type=Path)
    args = parser.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)

    transition_events = []
    rejected = []
    teacher_metrics = []
    run_summary = []
    for run_dir in run_dirs(args.root):
        arm = arm_name(run_dir)
        reps = parse_representatives(run_dir / "fc5-transfer-directional-representative-lifecycle.csv")
        merge = parse_merge_rounds(run_dir / "fc5-transfer-merge-rounds.csv")
        gaps = parse_archive_gaps(run_dir / "fc5-transfer-archive-working-gap.csv")
        dscr = parse_dscr_teacher_uses(run_dir / "dscr-teacher-uses.csv")
        status = parse_props(run_dir / "status.properties")
        seed = reps[0]["seed"]
        actual_fe = as_int(status.get("fullEvaluations", status.get("actualFE", merge[-1]["fe"])))
        transition_events.extend(transition_rows(seed, arm, reps, "DIRECTION_LABEL_EVENT"))
        transition_events.extend(transition_rows(seed, arm, reps, "UNIQUE_DIRECTIONAL_REPRESENTATIVE"))
        rejected.extend(rejected_events(seed, arm, reps, gaps))
        teacher_metrics.extend(normalized_teacher_metrics(seed, arm, actual_fe, merge, reps, dscr))
        run_summary.append({
            "seed": seed, "arm": arm, "actualFE": actual_fe, "pddrCycles": len(merge),
            "maxNnd": max(row["Nnd"] for row in merge),
            "roundsNndOver100": sum(row["Nnd"] > 100 for row in merge),
            "directionLabelEvents": len(reps),
            "directionLabelRejected": sum(not row["pddrSelected"] for row in reps),
            "uniqueDirectionRepresentatives": len(unique_representatives(reps)),
            "uniqueDirectionRejected": sum(not row["selected"] for row in unique_representatives(reps)),
        })

    aggregates = aggregate_transitions(transition_events)
    write_csv(args.out / "run-level-correction-summary.csv", run_summary,
              ["seed", "arm", "actualFE", "pddrCycles", "maxNnd", "roundsNndOver100",
               "directionLabelEvents", "directionLabelRejected", "uniqueDirectionRepresentatives",
               "uniqueDirectionRejected"])
    write_csv(args.out / "directional-transition-events.csv", transition_events,
              list(transition_events[0].keys()))
    write_csv(args.out / "directional-retention-corrected.csv", aggregates, list(aggregates[0].keys()))
    write_csv(args.out / "rejected-representative-events.csv", rejected, list(rejected[0].keys()))
    write_csv(args.out / "teacher-utilization-normalized.csv", teacher_metrics,
              list(teacher_metrics[0].keys()))

    reference_rows = list(csv.DictReader(args.reference.open("r", encoding="utf-8", newline="")))
    bounds = []
    for key in ("Cmax", "TEC", "TWC"):
        values = [float(row[key]) for row in reference_rows]
        bounds.append((min(values), max(values)))
    checkpoint = [{
        "instance": "100_5_3_1", "historicalReferencePath": str(args.reference),
        "historicalReferenceSha256": sha256(args.reference), "historicalReferenceSize": len(reference_rows),
        "idealCmax": bounds[0][0], "idealTEC": bounds[1][0], "idealTWC": bounds[2][0],
        "nadirCmax": bounds[0][1], "nadirTEC": bounds[1][1], "nadirTWC": bounds[2][1],
        "hvReferencePoint": "(1.1,1.1,1.1)",
        "checkpointFrontVectorsAvailable": "false",
        "lifecycleHvIgdStatus": "NOT_COMPUTABLE_FROM_CURRENT_100K_ARTIFACTS",
        "reason": "Only final front and scalar Cmax curves were persisted; no full checkpoint fronts exist.",
    }]
    write_csv(args.out / "checkpoint-metric-availability.csv", checkpoint, list(checkpoint[0].keys()))

    all_unique = [row for row in aggregates if row["granularity"] == "UNIQUE_DIRECTIONAL_REPRESENTATIVE"]
    rejected_total = sum(row["uniqueDirectionRejected"] for row in run_summary)
    verdict = [{
        "hypothesis": "H1a_ND_OVERFLOW", "status": "NOT_CONFIRMED_AT_100K",
        "evidence": "No PDDR round had Nnd>100; maximum Nnd=%d." % max(row["maxNnd"] for row in run_summary),
        "causalClaimAllowed": "false",
    }, {
        "hypothesis": "H1b_REPRESENTATIVE_SURVIVAL_UTILIZATION", "status": "LOCAL_FAILURE_EVENTS_OBSERVED_TRANSFER_UNRESOLVED",
        "evidence": "%d unique directional representatives were rejected; next-cycle and teacher-use losses are non-zero, but no positive-case 100k contrast or checkpoint HV/IGD lifecycle is available." % rejected_total,
        "causalClaimAllowed": "false",
    }, {
        "hypothesis": "FC5_TRANSFER_OVERALL", "status": "INCONCLUSIVE_AFTER_DENOMINATOR_CORRECTION",
        "evidence": "H1a is not confirmed; H1b is observable but cannot be tied temporally to HV/IGD degradation with current artifacts.",
        "causalClaimAllowed": "false",
    }]
    write_csv(args.out / "h1a-h1b-corrected-verdict.csv", verdict, list(verdict[0].keys()))

    # Evidence manifest excludes itself and the human-authored report added later.
    manifest = args.out / "evidence-sha256.tsv"
    files = sorted(path for path in args.out.iterdir() if path.is_file() and path.name != manifest.name)
    with manifest.open("w", encoding="utf-8", newline="") as handle:
        handle.write("sha256\tbytes\tfile\n")
        for path in files:
            handle.write("%s\t%d\t%s\n" % (sha256(path), path.stat().st_size, path.name))


if __name__ == "__main__":
    main()
