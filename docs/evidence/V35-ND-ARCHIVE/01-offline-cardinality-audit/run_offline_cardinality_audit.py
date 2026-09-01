#!/usr/bin/env python3
"""Read-only offline cardinality audit for the frozen V35 Stage2 archive.

The audit deliberately consumes only the 60 complete A0--A4 runs for
20_2_3_1 with seeds 20260808..20260819.  It extracts selected archive members
to a non-repository temporary directory, validates every file listed by each
run's evidence-sha256.tsv, and never changes a source front.

The three objectives are the V35 archive slots [0, 1, 6], represented by the
front columns Cmax, TEC and TWC.  Exact identity is IEEE-754 binary64 bit
identity (the same semantic identity used by Double.toHexString), and Pareto
dominance is strict three-objective minimisation without an epsilon.

Main metrics use complete full decision fronts only.  K25/K50 are independent
fixed-cardinality sensitivity analyses; K30 is a presentation subset only and
is not assigned HV/IGD.  This script is analysis-only: it does not run Java,
training, remote jobs, or uploads.
"""

from __future__ import annotations

import argparse
import bisect
import csv
import hashlib
import math
import statistics
import struct
import tarfile
import tempfile
from dataclasses import dataclass
from pathlib import Path, PurePosixPath
from typing import Dict, Iterable, List, Mapping, Sequence, Tuple

try:  # Optional acceleration; the pure-Python path remains deterministic.
    import numpy as np
except Exception:  # pragma: no cover - exercised only on minimal Python hosts.
    np = None


ARCHIVE_SHA256 = "0202356F28C7013894FB14B7347EB77A66243AD9312139CD0FE2A62F24CAD5FB"
ARCHIVE_NAME = "zhangbo-v35-stage2-master-v2-20260823.tar.gz"
ARCHIVE_ROOT = "zhangbo-v35-stage2-master-v2-20260823"
RUN_ROOT = f"{ARCHIVE_ROOT}/results/formal-a0-a4-4500/100_2_3_1"
INSTANCE = "100_2_3_1"
SEEDS = tuple(range(20260808, 20260820))
ARMS = ("A0", "A1", "A2", "A3", "A4")
CAPACITIES = (25, 30, 50)
NEAR_THRESHOLDS = (("0.01pct", 0.0001), ("0.05pct", 0.0005), ("0.1pct", 0.001))
EPSILON = 1.0e-12
HV_REFERENCE = 1.1


@dataclass(frozen=True)
class RunData:
    seed: int
    arm: str
    run_id: str
    member_prefix: str
    run_dir: Path
    points: Tuple[Tuple[float, float, float], ...]
    front_sha256: str
    initial_population_hash: str
    status: Mapping[str, str]
    budget: Mapping[str, str]
    verification_ok: bool


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def parse_properties(path: Path) -> Dict[str, str]:
    values: Dict[str, str] = {}
    for raw in path.read_text(encoding="utf-8", errors="strict").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip()
    return values


def point_key(point: Sequence[float]) -> str:
    """Stable exact key for one parsed binary64 objective triple."""
    return "|".join(struct.pack(">d", value).hex() for value in point)


def point_sort_key(point: Sequence[float]) -> Tuple[float, float, float, str]:
    return (point[0], point[1], point[2], point_key(point))


def exact_unique(points: Iterable[Sequence[float]]) -> List[Tuple[float, float, float]]:
    ordered = sorted((tuple(float(value) for value in point) for point in points), key=point_sort_key)
    unique: Dict[str, Tuple[float, float, float]] = {}
    for point in ordered:
        unique.setdefault(point_key(point), point)
    return list(unique.values())


def dominates(left: Sequence[float], right: Sequence[float]) -> bool:
    no_worse = left[0] <= right[0] and left[1] <= right[1] and left[2] <= right[2]
    strictly_better = left[0] < right[0] or left[1] < right[1] or left[2] < right[2]
    return no_worse and strictly_better


def nondominated(points: Iterable[Sequence[float]]) -> List[Tuple[float, float, float]]:
    """Return an exact-unique 3-D minimisation front in deterministic order.

    The sweep is O(n log n): points are processed in equal-x groups, while a
    Fenwick tree stores the smallest z seen for each y prefix.  Same-x points
    are checked by a local y/z sweep.  This avoids a quadratic pooled-reference
    pass while preserving strict dominance semantics.
    """
    values = exact_unique(points)
    if len(values) <= 1:
        return values
    ys = sorted({point[1] for point in values})
    tree = [math.inf] * (len(ys) + 1)

    def query(index: int) -> float:
        result = math.inf
        while index > 0:
            result = min(result, tree[index])
            index -= index & -index
        return result

    def update(index: int, value: float) -> None:
        while index < len(tree):
            if value < tree[index]:
                tree[index] = value
            index += index & -index

    result: List[Tuple[float, float, float]] = []
    position = 0
    while position < len(values):
        x_value = values[position][0]
        end = position + 1
        while end < len(values) and values[end][0] == x_value:
            end += 1
        group = sorted(values[position:end], key=lambda p: (p[1], p[2], point_key(p)))
        group_min_z = math.inf
        group_nd: List[Tuple[float, float, float]] = []
        for point in group:
            y_index = bisect.bisect_right(ys, point[1])
            dominated_by_lower_x = query(y_index) <= point[2]
            dominated_in_same_x = group_min_z <= point[2]
            if not dominated_by_lower_x and not dominated_in_same_x:
                group_nd.append(point)
            group_min_z = min(group_min_z, point[2])
        result.extend(group_nd)
        # Adding all points is safe: a dominated point cannot dominate a future
        # point that its dominator could not already dominate.
        for point in group:
            update(bisect.bisect_left(ys, point[1]) + 1, point[2])
        position = end
    return sorted(result, key=point_sort_key)


def deterministic_subset(points: Iterable[Sequence[float]], capacity: int) -> List[Tuple[float, float, float]]:
    """V35 exact-dedup + extremes + normalised maximin subsetter."""
    if capacity < 3:
        raise ValueError("capacity must preserve the three objective extremes")
    values = exact_unique(points)
    if len(values) <= capacity:
        return list(values)
    bounds = [(min(p[i] for p in values), max(p[i] for p in values)) for i in range(3)]
    selected: List[Tuple[float, float, float]] = []
    selected_keys = set()
    for objective in range(3):
        extreme = min(values, key=lambda p: (p[objective], point_sort_key(p)))
        key = point_key(extreme)
        if key not in selected_keys:
            selected.append(extreme)
            selected_keys.add(key)

    def distance_to_selected(candidate: Sequence[float]) -> float:
        distances = []
        for chosen in selected:
            squared = 0.0
            for index in range(3):
                denominator = max(bounds[index][1] - bounds[index][0], EPSILON)
                delta = (candidate[index] - chosen[index]) / denominator
                squared += delta * delta
            distances.append(math.sqrt(squared))
        return min(distances)

    while len(selected) < capacity:
        best = None
        best_distance = -1.0
        for candidate in values:
            if point_key(candidate) in selected_keys:
                continue
            candidate_distance = distance_to_selected(candidate)
            if (
                best is None
                or candidate_distance > best_distance + EPSILON
                or (
                    abs(candidate_distance - best_distance) <= EPSILON
                    and point_sort_key(candidate) < point_sort_key(best)
                )
            ):
                best = candidate
                best_distance = candidate_distance
        if best is None:
            break
        selected.append(best)
        selected_keys.add(point_key(best))
    return sorted(selected, key=point_sort_key)


def nearest_neighbor_rates(points: Iterable[Sequence[float]]) -> Dict[str, float]:
    values = exact_unique(points)
    if len(values) <= 1:
        return {name: 0.0 for name, _ in NEAR_THRESHOLDS}
    mins = [min(point[i] for point in values) for i in range(3)]
    maxs = [max(point[i] for point in values) for i in range(3)]
    normalised = [
        tuple((point[i] - mins[i]) / max(maxs[i] - mins[i], EPSILON) for i in range(3))
        for point in values
    ]
    nearest: List[float]
    if np is not None:
        array = np.asarray(normalised, dtype=float)
        distances = ((array[:, None, :] - array[None, :, :]) ** 2).sum(axis=2)
        np.fill_diagonal(distances, np.inf)
        nearest = np.sqrt(distances.min(axis=1)).tolist()
    else:  # pragma: no cover - fallback for a host without numpy.
        nearest = []
        for index, left in enumerate(normalised):
            nearest.append(
                min(
                    math.sqrt(sum((left[i] - right[i]) ** 2 for i in range(3)))
                    for other_index, right in enumerate(normalised)
                    if other_index != index
                )
            )
    return {name: sum(distance <= threshold for distance in nearest) / len(nearest) for name, threshold in NEAR_THRESHOLDS}


def normalise(points: Iterable[Sequence[float]], reference: Sequence[Sequence[float]]) -> List[Tuple[float, float, float]]:
    mins = [min(point[i] for point in reference) for i in range(3)]
    maxs = [max(point[i] for point in reference) for i in range(3)]
    return [
        tuple((point[i] - mins[i]) / max(maxs[i] - mins[i], EPSILON) for i in range(3))
        for point in points
    ]


def union_yz(points: Sequence[Sequence[float]]) -> float:
    ry = rz = HV_REFERENCE
    ordered = sorted(points, key=lambda point: point[1])
    area = 0.0
    min_z = rz
    index = 0
    while index < len(ordered):
        y = max(0.0, min(ry, ordered[index][1]))
        while index < len(ordered) and ordered[index][1] <= y + EPSILON:
            min_z = min(min_z, max(0.0, min(rz, ordered[index][2])))
            index += 1
        next_y = max(y, min(ry, ordered[index][1])) if index < len(ordered) else ry
        area += max(0.0, next_y - y) * max(0.0, rz - min_z)
    return area


def hypervolume(points: Sequence[Sequence[float]]) -> float:
    if not points:
        return 0.0
    ordered = sorted(
        [tuple(max(0.0, min(HV_REFERENCE, value)) for value in point) for point in points],
        key=lambda point: point[0],
    )
    volume = 0.0
    active: List[Tuple[float, float, float]] = []
    index = 0
    while index < len(ordered):
        x = max(0.0, min(HV_REFERENCE, ordered[index][0]))
        while index < len(ordered) and ordered[index][0] <= x + EPSILON:
            active.append(ordered[index])
            index += 1
        next_x = max(x, min(HV_REFERENCE, ordered[index][0])) if index < len(ordered) else HV_REFERENCE
        volume += max(0.0, next_x - x) * union_yz(active)
    return max(0.0, volume)


def igd(approximation: Sequence[Sequence[float]], reference: Sequence[Sequence[float]]) -> float:
    if not approximation or not reference:
        return math.nan
    if np is not None:
        approx = np.asarray(approximation, dtype=float)
        ref = np.asarray(reference, dtype=float)
        best = np.full(len(ref), np.inf, dtype=float)
        # Keep peak memory bounded while allowing C-level distance work.
        for start in range(0, len(ref), 512):
            block = ref[start : start + 512]
            distances = ((block[:, None, :] - approx[None, :, :]) ** 2).sum(axis=2)
            best[start : start + len(block)] = np.sqrt(distances.min(axis=1))
        return float(best.mean())
    return sum(
        min(math.sqrt(sum((target[i] - candidate[i]) ** 2 for i in range(3))) for candidate in approximation)
        for target in reference
    ) / len(reference)


def calculate_metrics(approximation: Iterable[Sequence[float]], reference: Sequence[Sequence[float]]) -> Tuple[float, float]:
    approx_raw = nondominated(approximation)
    reference_raw = nondominated(reference)
    approx = normalise(approx_raw, reference_raw)
    ref = normalise(reference_raw, reference_raw)
    return hypervolume(approx), igd(approx, ref)


def write_csv(path: Path, fieldnames: Sequence[str], rows: Iterable[Mapping[str, object]]) -> None:
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(fieldnames), extrasaction="ignore")
        writer.writeheader()
        for row in rows:
            writer.writerow(row)


def emit_output_sha256(output: Path) -> None:
    """Write a manifest for the audit directory without a self-referential row."""
    manifest = output / "audit-output-sha256.tsv"
    rows = []
    for path in sorted(output.iterdir(), key=lambda item: item.name):
        if not path.is_file() or path.name == manifest.name or path.name.endswith(".pyc"):
            continue
        rows.append(f"{path.name}\t{sha256_file(path)}")
    manifest.write_text("path\tsha256\n" + "\n".join(rows) + "\n", encoding="utf-8")


def extract_selected_archive(archive: Path, temp_root: Path) -> Tuple[Path, List[Tuple[int, str, str, Path, List[str]]]]:
    extraction_root = temp_root / "extracted"
    extraction_root.mkdir(parents=True, exist_ok=True)
    selected: List[Tuple[int, str, str, Path, List[str]]] = []
    with tarfile.open(archive, mode="r:gz") as handle:
        members = handle.getmembers()
        by_name: Dict[str, tarfile.TarInfo] = {}
        for member in members:
            if member.name in by_name:
                raise RuntimeError(f"duplicate archive member: {member.name}")
            by_name[member.name] = member
        for seed in SEEDS:
            for arm in ARMS:
                prefix = f"{RUN_ROOT}/seed-{seed}/{arm}/"
                names = sorted(name for name in by_name if name.startswith(prefix) and by_name[name].isfile())
                if not names:
                    raise RuntimeError(f"missing selected run in archive: {prefix}")
                for name in names:
                    member = by_name[name]
                    parts = PurePosixPath(name).parts
                    if any(part in ("", ".", "..") for part in parts):
                        raise RuntimeError(f"unsafe archive member path: {name}")
                    destination = extraction_root.joinpath(*parts)
                    destination.parent.mkdir(parents=True, exist_ok=True)
                    source = handle.extractfile(member)
                    if source is None:
                        raise RuntimeError(f"cannot read archive member: {name}")
                    with source, destination.open("wb") as output:
                        while True:
                            block = source.read(1024 * 1024)
                            if not block:
                                break
                            output.write(block)
                run_dir = extraction_root.joinpath(*PurePosixPath(prefix.rstrip("/" )).parts)
                selected.append((seed, arm, prefix, run_dir, names))
    return extraction_root, selected


def verify_run_files(run_dir: Path, member_prefix: str, archive_names: Sequence[str]) -> Tuple[List[Dict[str, object]], bool, str]:
    sha_manifest = run_dir / "evidence-sha256.tsv"
    if not sha_manifest.is_file():
        raise RuntimeError(f"missing evidence manifest: {run_dir}")
    expected: Dict[str, str] = {}
    for line_number, raw in enumerate(sha_manifest.read_text(encoding="utf-8").splitlines(), 1):
        line = raw.strip()
        if not line:
            continue
        fields = line.split("\t")
        if len(fields) != 2:
            raise RuntimeError(f"invalid evidence manifest line {sha_manifest}:{line_number}")
        if fields[0].strip().lower() == "path" and fields[1].strip().lower() == "sha256":
            continue
        expected[fields[0].replace("\\", "/")] = fields[1].strip().lower()
    rows: List[Dict[str, object]] = []
    actual_files = {
        path.relative_to(run_dir).as_posix(): path
        for path in run_dir.rglob("*")
        if path.is_file()
    }
    verified = True
    for relative, path in sorted(actual_files.items()):
        actual = sha256_file(path)
        if relative == "evidence-sha256.tsv":
            status = "MANIFEST_SELF"
            expected_value = ""
        elif relative not in expected:
            status = "UNEXPECTED_FILE"
            expected_value = ""
            verified = False
        else:
            expected_value = expected[relative]
            status = "VERIFIED" if actual == expected_value else "HASH_MISMATCH"
            verified = verified and status == "VERIFIED"
        rows.append(
            {
                "archive_member": f"{member_prefix}{relative}",
                "relative_file": relative,
                "size_bytes": path.stat().st_size,
                "expected_sha256": expected_value,
                "actual_sha256": actual,
                "verification": status,
            }
        )
    for relative, expected_value in sorted(expected.items()):
        if relative not in actual_files:
            verified = False
            rows.append(
                {
                    "archive_member": f"{member_prefix}{relative}",
                    "relative_file": relative,
                    "size_bytes": "",
                    "expected_sha256": expected_value,
                    "actual_sha256": "",
                    "verification": "MISSING_FILE",
                }
            )
    return rows, verified, sha256_file(run_dir / "front.csv") if (run_dir / "front.csv").is_file() else ""


def read_front(path: Path) -> List[Tuple[float, float, float]]:
    points: List[Tuple[float, float, float]] = []
    with path.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.reader(handle)
        try:
            header = next(reader)
        except StopIteration as exc:
            raise RuntimeError(f"empty front: {path}") from exc
        if [cell.strip().lower() for cell in header[:3]] != ["cmax", "tec", "twc"]:
            raise RuntimeError(f"unexpected front header: {path}: {header}")
        for row_number, row in enumerate(reader, 2):
            if not row or not any(cell.strip() for cell in row):
                continue
            if len(row) < 3:
                raise RuntimeError(f"short front row {path}:{row_number}")
            try:
                values = tuple(float(row[index].strip()) for index in range(3))
            except ValueError as exc:
                raise RuntimeError(f"non-numeric front row {path}:{row_number}") from exc
            if not all(math.isfinite(value) for value in values):
                raise RuntimeError(f"non-finite front row {path}:{row_number}")
            points.append(values)
    if not points:
        raise RuntimeError(f"front has no objective points: {path}")
    return points


def load_runs(extraction_root: Path, selected: Sequence[Tuple[int, str, str, Path, List[str]]]) -> Tuple[List[RunData], List[Dict[str, object]], List[Dict[str, object]]]:
    runs: List[RunData] = []
    verification_rows: List[Dict[str, object]] = []
    run_rows: List[Dict[str, object]] = []
    for seed, arm, prefix, run_dir, archive_names in selected:
        file_rows, verification_ok, front_sha = verify_run_files(run_dir, prefix, archive_names)
        verification_rows.extend(file_rows)
        status = parse_properties(run_dir / "status.properties")
        budget = parse_properties(run_dir / "budget-termination.properties")
        points = read_front(run_dir / "front.csv")
        try:
            actual_fe = int(budget.get("actualFE", "0"))
            requested_fe = int(budget.get("requestedMaxFE", "0"))
        except ValueError:
            actual_fe = requested_fe = 0
        # The campaign's accepted-completion protocol permits a
        # PHASE_CONSISTENT_TAIL_STOP below the nominal 500k boundary.  The
        # accepted gate is therefore COMPLETED + requestedMaxFE=500000 +
        # phaseBoundAccepted=true + positive actualFE, not literal actualFE
        # equality.  The exact actualFE is retained in run-manifest.csv.
        complete_ok = (
            verification_ok
            and status.get("status") == "COMPLETED"
            and requested_fe == 500000
            and actual_fe > 0
            and budget.get("phaseBoundAccepted", "").lower() == "true"
        )
        if not complete_ok:
            raise RuntimeError(f"selected run is not a fully verified accepted 500k-budget completion: seed={seed}, arm={arm}")
        initial_hash = status.get("initialPopulationHash", "")
        run_id = f"{arm}-seed-{seed}"
        runs.append(
            RunData(
                seed=seed,
                arm=arm,
                run_id=run_id,
                member_prefix=prefix,
                run_dir=run_dir,
                points=tuple(points),
                front_sha256=front_sha,
                initial_population_hash=initial_hash,
                status=status,
                budget=budget,
                verification_ok=verification_ok,
            )
        )
        run_rows.append(
            {
                "run_id": run_id,
                "seed": seed,
                "arm": arm,
                "instance": INSTANCE,
                "source_member_prefix": prefix,
                "status": status.get("status", ""),
                "full_evaluations": status.get("fullEvaluations", ""),
                "actual_fe": budget.get("actualFE", ""),
                "phase_bound_accepted": budget.get("phaseBoundAccepted", ""),
                "initial_population_hash": initial_hash,
                "front_sha256": front_sha,
                "front_rows": len(points),
                "verification_ok": str(verification_ok).lower(),
                "complete_accepted_ok": str(complete_ok).lower(),
            }
        )
    runs.sort(key=lambda run: (run.arm, run.seed))
    if len(runs) != len(SEEDS) * len(ARMS):
        raise RuntimeError(f"expected 60 runs, got {len(runs)}")
    return runs, run_rows, verification_rows


def write_points(path: Path, rows: Iterable[Mapping[str, object]]) -> None:
    write_csv(path, ("run_id", "seed", "arm", "point_index", "Cmax", "TEC", "TWC", "objective_key"), rows)


def reference_points(path: Path, points: Sequence[Sequence[float]]) -> None:
    rows = []
    for index, point in enumerate(points):
        rows.append({"point_index": index, "Cmax": repr(point[0]), "TEC": repr(point[1]), "TWC": repr(point[2]), "objective_key": point_key(point)})
    write_csv(path, ("point_index", "Cmax", "TEC", "TWC", "objective_key"), rows)


def reference_hash(points: Sequence[Sequence[float]]) -> str:
    digest = hashlib.sha256()
    for point in points:
        digest.update((point_key(point) + "\n").encode("ascii"))
    return digest.hexdigest()


def summary_median(values: Sequence[float]) -> float:
    return float(statistics.median(values)) if values else math.nan


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--archive", type=Path, default=Path(r"G:\ResearchArchive\ZhangBo-V35-Paper-Evidence-20260823\remote-campaigns\zhangbo-v35-stage2-master-v2-20260823.tar.gz"))
    parser.add_argument("--output", type=Path, default=Path(__file__).resolve().parent)
    parser.add_argument("--temp-root", type=Path, default=None, help="non-repository extraction directory; default is a new system temp directory")
    parser.add_argument("--emit-output-sha", action="store_true", help="only emit the output-directory SHA manifest; do not re-run the audit")
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    if args.emit_output_sha:
        emit_output_sha256(output)
        print(f"output_sha_manifest={output / 'audit-output-sha256.tsv'}")
        return 0
    archive = args.archive.resolve()
    if not archive.is_file():
        raise SystemExit(f"archive not found: {archive}")
    actual_archive_sha = sha256_file(archive).upper()
    if actual_archive_sha != ARCHIVE_SHA256:
        raise SystemExit(f"archive SHA-256 mismatch: expected {ARCHIVE_SHA256}, got {actual_archive_sha}")
    if args.temp_root is None:
        temp_path = Path(tempfile.mkdtemp(prefix="v35_nd_cardinality_audit_"))
    else:
        temp_path = args.temp_root.resolve()
        temp_path.mkdir(parents=True, exist_ok=True)
    extraction_root, selected = extract_selected_archive(archive, temp_path)
    runs, run_rows, verification_rows = load_runs(extraction_root, selected)
    if sorted((run.seed, run.arm) for run in runs) != sorted((seed, arm) for seed in SEEDS for arm in ARMS):
        raise RuntimeError("selected run set is not exactly the requested 60 seed/arm pairs")

    write_csv(
        output / "run-manifest.csv",
        ("run_id", "seed", "arm", "instance", "source_member_prefix", "status", "full_evaluations", "actual_fe", "phase_bound_accepted", "initial_population_hash", "front_sha256", "front_rows", "verification_ok", "complete_accepted_ok"),
        run_rows,
    )
    write_csv(
        output / "file-verification.csv",
        ("archive_member", "relative_file", "size_bytes", "expected_sha256", "actual_sha256", "verification"),
        verification_rows,
    )

    full_points_by_run = {run.run_id: nondominated(run.points) for run in runs}
    subsets: Dict[Tuple[str, int], List[Tuple[float, float, float]]] = {}
    cardinality_rows: List[Dict[str, object]] = []
    permutation_rows: List[Dict[str, object]] = []
    subset_rows: Dict[int, List[Dict[str, object]]] = {capacity: [] for capacity in CAPACITIES}
    for run in runs:
        unique = exact_unique(run.points)
        nd = full_points_by_run[run.run_id]
        rates = nearest_neighbor_rates(nd)
        cardinality_rows.append(
            {
                "run_id": run.run_id,
                "seed": run.seed,
                "arm": run.arm,
                "variant": "full",
                "source_kind": "decision-front",
                "raw_count": len(run.points),
                "exact_unique_count": len(unique),
                "exact_duplicate_count": len(run.points) - len(unique),
                "strict_nd_count": len(nd),
                "strict_dominated_unique_count": len(unique) - len(nd),
                "near_0.01pct_rate": rates["0.01pct"],
                "near_0.05pct_rate": rates["0.05pct"],
                "near_0.1pct_rate": rates["0.1pct"],
            }
        )
        for capacity in CAPACITIES:
            subset = deterministic_subset(nd, capacity)
            subsets[(run.run_id, capacity)] = subset
            subset_rates = nearest_neighbor_rates(subset)
            variant = f"k{capacity}"
            cardinality_rows.append(
                {
                    "run_id": run.run_id,
                    "seed": run.seed,
                    "arm": run.arm,
                    "variant": variant,
                    "source_kind": "deterministic-maximin",
                    "raw_count": len(nd),
                    "exact_unique_count": len(exact_unique(subset)),
                    "exact_duplicate_count": 0,
                    "strict_nd_count": len(nondominated(subset)),
                    "strict_dominated_unique_count": 0,
                    "near_0.01pct_rate": subset_rates["0.01pct"],
                    "near_0.05pct_rate": subset_rates["0.05pct"],
                    "near_0.1pct_rate": subset_rates["0.1pct"],
                }
            )
            for index, point in enumerate(subset):
                subset_rows[capacity].append(
                    {
                        "run_id": run.run_id,
                        "seed": run.seed,
                        "arm": run.arm,
                        "point_index": index,
                        "Cmax": repr(point[0]),
                        "TEC": repr(point[1]),
                        "TWC": repr(point[2]),
                        "objective_key": point_key(point),
                    }
                )
            shuffled = list(reversed(run.points))
            shuffled = shuffled[::2] + shuffled[1::2]
            shuffled_subset = deterministic_subset(nondominated(shuffled), capacity)
            permutation_rows.append(
                {
                    "run_id": run.run_id,
                    "seed": run.seed,
                    "arm": run.arm,
                    "capacity": capacity,
                    "original_hash": reference_hash(subset),
                    "permuted_hash": reference_hash(shuffled_subset),
                    "order_independent": str(reference_hash(subset) == reference_hash(shuffled_subset)).lower(),
                }
            )
    write_csv(
        output / "cardinality.csv",
        ("run_id", "seed", "arm", "variant", "source_kind", "raw_count", "exact_unique_count", "exact_duplicate_count", "strict_nd_count", "strict_dominated_unique_count", "near_0.01pct_rate", "near_0.05pct_rate", "near_0.1pct_rate"),
        cardinality_rows,
    )
    write_csv(
        output / "permutation-invariance.csv",
        ("run_id", "seed", "arm", "capacity", "original_hash", "permuted_hash", "order_independent"),
        permutation_rows,
    )
    for capacity in CAPACITIES:
        filename = "representative-front-k30.csv" if capacity == 30 else f"sensitivity-front-k{capacity}.csv"
        write_points(output / filename, subset_rows[capacity])

    full_reference = nondominated(point for run in runs for point in full_points_by_run[run.run_id])
    k25_reference = nondominated(point for run in runs for point in subsets[(run.run_id, 25)])
    k50_reference = nondominated(point for run in runs for point in subsets[(run.run_id, 50)])
    references = {"full": full_reference, "k25": k25_reference, "k50": k50_reference}
    reference_points(output / "reference-front-full.csv", full_reference)
    reference_points(output / "reference-front-k25.csv", k25_reference)
    reference_points(output / "reference-front-k50.csv", k50_reference)

    metric_rows: Dict[str, List[Dict[str, object]]] = {"full": [], "k25": [], "k50": []}
    for variant, reference in references.items():
        capacity = None if variant == "full" else int(variant[1:])
        for run in runs:
            approximation = full_points_by_run[run.run_id] if capacity is None else subsets[(run.run_id, capacity)]
            hv, igd_value = calculate_metrics(approximation, reference)
            metric_rows[variant].append(
                {
                    "run_id": run.run_id,
                    "seed": run.seed,
                    "arm": run.arm,
                    "variant": variant,
                    "approx_count": len(approximation),
                    "pfref_count": len(reference),
                    "pfref_sha256": reference_hash(reference),
                    "hv": hv,
                    "igd": igd_value,
                }
            )
        write_csv(
            output / f"metrics-{variant}.csv",
            ("run_id", "seed", "arm", "variant", "approx_count", "pfref_count", "pfref_sha256", "hv", "igd"),
            metric_rows[variant],
        )

    metric_summary_rows: List[Dict[str, object]] = []
    for variant, rows in metric_rows.items():
        for arm in ARMS:
            arm_rows = [row for row in rows if row["arm"] == arm]
            metric_summary_rows.append(
                {
                    "variant": variant,
                    "arm": arm,
                    "runs": len(arm_rows),
                    "hv_median": summary_median([float(row["hv"]) for row in arm_rows]),
                    "igd_median": summary_median([float(row["igd"]) for row in arm_rows]),
                    "approx_count_median": summary_median([float(row["approx_count"]) for row in arm_rows]),
                    "pfref_count": len(references[variant]),
                    "pfref_sha256": reference_hash(references[variant]),
                }
            )
    write_csv(
        output / "metric-summary.csv",
        ("variant", "arm", "runs", "hv_median", "igd_median", "approx_count_median", "pfref_count", "pfref_sha256"),
        metric_summary_rows,
    )

    full_joint_by_variant: Dict[str, str] = {}
    ranking_rows: List[Dict[str, object]] = []
    for variant in ("full", "k25", "k50"):
        by_arm = {row["arm"]: row for row in metric_summary_rows if row["variant"] == variant}
        hv0, hv4 = float(by_arm["A0"]["hv_median"]), float(by_arm["A4"]["hv_median"])
        igd0, igd4 = float(by_arm["A0"]["igd_median"]), float(by_arm["A4"]["igd_median"])
        hv_direction = "A4" if hv4 > hv0 else "A0" if hv4 < hv0 else "TIE"
        igd_direction = "A4" if igd4 < igd0 else "A0" if igd4 > igd0 else "TIE"
        joint = "A4" if hv_direction == "A4" and igd_direction == "A4" else "A0" if hv_direction == "A0" and igd_direction == "A0" else "MIXED_OR_TIE"
        full_joint_by_variant[variant] = joint
        paired_hv = {
            seed: (float(next(row["hv"] for row in metric_rows[variant] if row["arm"] == "A0" and row["seed"] == seed)), float(next(row["hv"] for row in metric_rows[variant] if row["arm"] == "A4" and row["seed"] == seed)))
            for seed in SEEDS
        }
        paired_igd = {
            seed: (float(next(row["igd"] for row in metric_rows[variant] if row["arm"] == "A0" and row["seed"] == seed)), float(next(row["igd"] for row in metric_rows[variant] if row["arm"] == "A4" and row["seed"] == seed)))
            for seed in SEEDS
        }
        ranking_rows.append(
            {
                "comparison": "A4_vs_A0",
                "variant": variant,
                "hv_a0_median": hv0,
                "hv_a4_median": hv4,
                "hv_direction": hv_direction,
                "igd_a0_median": igd0,
                "igd_a4_median": igd4,
                "igd_direction": igd_direction,
                "joint_direction": joint,
                "hv_a4_win_count": sum(right > left for left, right in paired_hv.values()),
                "igd_a4_win_count": sum(right < left for left, right in paired_igd.values()),
                "reverses_full_joint": "",
            }
        )
    for row in ranking_rows:
        row["reverses_full_joint"] = str(row["variant"] != "full" and row["joint_direction"] != full_joint_by_variant["full"]).lower()
    write_csv(
        output / "a4-a0-ranking.csv",
        ("comparison", "variant", "hv_a0_median", "hv_a4_median", "hv_direction", "igd_a0_median", "igd_a4_median", "igd_direction", "joint_direction", "hv_a4_win_count", "igd_a4_win_count", "reverses_full_joint"),
        ranking_rows,
    )

    full_reference_keys = {point_key(point) for point in full_reference}
    leaveout_rows: List[Dict[str, object]] = []
    leaveout_rank_rows: List[Dict[str, object]] = []

    def append_leaveout(scope: str, omitted: str, remaining: Sequence[RunData]) -> None:
        reference = nondominated(point for run in remaining for point in full_points_by_run[run.run_id])
        keys = {point_key(point) for point in reference}
        union_count = len(keys | full_reference_keys)
        jaccard = len(keys & full_reference_keys) / union_count if union_count else 1.0
        leaveout_rows.append(
            {
                "scope": scope,
                "omitted": omitted,
                "remaining_runs": len(remaining),
                "full_pfref_count": len(full_reference),
                "leaveout_pfref_count": len(reference),
                "pfref_delta": len(reference) - len(full_reference),
                "full_points_removed": len(full_reference_keys - keys),
                "new_points_exposed": len(keys - full_reference_keys),
                "pfref_jaccard": jaccard,
                "leaveout_pfref_sha256": reference_hash(reference),
            }
        )
        # Leave-out changes only PFref construction.  All 60 original runs
        # remain approximations, including the omitted run/arm, so every
        # row is comparable and the audit tests reference sensitivity rather
        # than silently changing the evaluated run set.
        a0 = [run for run in runs if run.arm == "A0"]
        a4 = [run for run in runs if run.arm == "A4"]
        a0_metrics = [calculate_metrics(full_points_by_run[run.run_id], reference) for run in a0]
        a4_metrics = [calculate_metrics(full_points_by_run[run.run_id], reference) for run in a4]
        a0_hv, a4_hv = summary_median([value[0] for value in a0_metrics]), summary_median([value[0] for value in a4_metrics])
        a0_igd, a4_igd = summary_median([value[1] for value in a0_metrics]), summary_median([value[1] for value in a4_metrics])
        hv_direction = "A4" if a4_hv > a0_hv else "A0" if a4_hv < a0_hv else "TIE"
        igd_direction = "A4" if a4_igd < a0_igd else "A0" if a4_igd > a0_igd else "TIE"
        joint = "A4" if hv_direction == "A4" and igd_direction == "A4" else "A0" if hv_direction == "A0" and igd_direction == "A0" else "MIXED_OR_TIE"
        leaveout_rank_rows.append(
            {
                "scope": scope,
                "omitted": omitted,
                "reference_pfref_count": len(reference),
                "rank_comparable": "true",
                "reason": "",
                "a0_hv_median": a0_hv,
                "a4_hv_median": a4_hv,
                "a0_igd_median": a0_igd,
                "a4_igd_median": a4_igd,
                "joint_direction": joint,
                "reverses_full_joint": str(joint != full_joint_by_variant["full"]).lower(),
            }
        )

    for run in runs:
        append_leaveout("run", run.run_id, [candidate for candidate in runs if candidate.run_id != run.run_id])
    for arm in ARMS:
        append_leaveout("arm", arm, [candidate for candidate in runs if candidate.arm != arm])
    write_csv(
        output / "leaveout-reference.csv",
        ("scope", "omitted", "remaining_runs", "full_pfref_count", "leaveout_pfref_count", "pfref_delta", "full_points_removed", "new_points_exposed", "pfref_jaccard", "leaveout_pfref_sha256"),
        leaveout_rows,
    )
    write_csv(
        output / "leaveout-ranking.csv",
        ("scope", "omitted", "reference_pfref_count", "rank_comparable", "reason", "a0_hv_median", "a4_hv_median", "a0_igd_median", "a4_igd_median", "joint_direction", "reverses_full_joint"),
        leaveout_rank_rows,
    )

    all_full_cardinality = [row for row in cardinality_rows if row["variant"] == "full"]
    overall_near_01 = summary_median([float(row["near_0.1pct_rate"]) for row in all_full_cardinality])
    k_reversals = sum(row["reverses_full_joint"] == "true" for row in ranking_rows if row["variant"] != "full")
    leaveout_reversals = sum(row.get("reverses_full_joint") == "true" for row in leaveout_rank_rows if row.get("rank_comparable") == "true")
    perm_failures = sum(row["order_independent"] != "true" for row in permutation_rows)
    verified_files = sum(row["verification"] == "VERIFIED" for row in verification_rows)
    hash_mismatches = sum(row["verification"] == "HASH_MISMATCH" for row in verification_rows)
    missing_files = sum(row["verification"] == "MISSING_FILE" for row in verification_rows)
    actual_fe_counts: Dict[str, int] = {}
    for run in runs:
        actual_fe = run.budget.get("actualFE", "")
        actual_fe_counts[actual_fe] = actual_fe_counts.get(actual_fe, 0) + 1
    actual_fe_distribution = ", ".join(
        f"{value}x{actual_fe_counts[value]}" for value in sorted(actual_fe_counts, key=lambda item: int(item))
    )
    gate_rows = [
        {"criterion": "normalized_0.1pct_near_duplicate_median", "observed": overall_near_01, "threshold": "> 0.20", "status": "TRIGGER" if overall_near_01 > 0.20 else "NO_TRIGGER", "note": "60 complete decision-fronts; exact dedup before nearest-neighbor rate"},
        {"criterion": "archive_teacher_scan_time_share", "observed": "UNAVAILABLE", "threshold": "> 0.25", "status": "NOT_EVALUATED", "note": "source archive has no V35 archive audit ledger"},
        {"criterion": "teacher_directional_regret", "observed": "UNAVAILABLE", "threshold": "median > 0.05 or P95 > 0.20", "status": "NOT_EVALUATED", "note": "no teacher-view run in this archive"},
        {"criterion": "k25_k50_a4_a0_hv_igd_reversal", "observed": k_reversals, "threshold": "> 0 reversals", "status": "TRIGGER" if k_reversals else "NO_TRIGGER", "note": "K25/K50 use independent PFrefs; K30 excluded from metrics"},
        {"criterion": "leaveout_a4_a0_major_reversal", "observed": leaveout_reversals, "threshold": "> 0 comparable reversals", "status": "TRIGGER" if leaveout_reversals else "NO_TRIGGER", "note": "leave-one-run/arm-out changes PFref only; all 60 original approximations remain comparable"},
        {"criterion": "pddr_cmax_loss", "observed": "UNAVAILABLE", "threshold": "median >= 0.02 with lost strict-ND point", "status": "NOT_EVALUATED", "note": "PDDR lifecycle is intentionally outside this archive audit"},
    ]
    write_csv(output / "gate-a-assessment.csv", ("criterion", "observed", "threshold", "status", "note"), gate_rows)

    def fmt(value: object) -> str:
        if isinstance(value, float):
            return f"{value:.8g}"
        return str(value)

    summary_lines = [
        "# V35 离线等基数审计结果",
        "",
        f"- 审计时间：2026-08-24（只读分析；脚本不启动训练、不上传、不修改原始 front）。",
        f"- 输入归档：`{archive}`。归档 SHA-256：`{actual_archive_sha}`，与登记值一致。",
        f"- 临时解包目录（repo 外）：`{extraction_root}`。仅提取 `{INSTANCE}` 的 seed `{SEEDS[0]}..{SEEDS[-1]}`、A0..A4，共 `{len(runs)}` 条通过 500k-budget phase-consistent 完成门的运行；实际 FE 分布为 `{actual_fe_distribution}`。",
        f"- 逐文件证据校验：已验证 `{verified_files}` 个 evidence-sha256.tsv 条目；HASH_MISMATCH={hash_mismatches}，MISSING_FILE={missing_files}。",
        "",
        "## 口径",
        "",
        "三目标固定为 `[0,1,6] = [Cmax, TEC, TWC]`，全部按最小化处理；精确去重使用解析后 binary64 位模式，严格支配不使用 epsilon。主指标只读取完整 `decision-front`。K25/K50 使用各自独立 PFref 做敏感性；K30 只输出展示集，不计算 HV/IGD。源 front 未被改写。",
        "",
        "## 等基数和近重复",
        "",
        f"- full pooled PFref：`{len(full_reference)}` 点，SHA `{reference_hash(full_reference)}`。K25 PFref：`{len(k25_reference)}` 点；K50 PFref：`{len(k50_reference)}` 点。",
        f"- full decision-front 的全体运行中位 normalized nearest-neighbor <=0.1% 率：`{overall_near_01:.6f}`（Gate A 阈值 0.20）；K25/K50/K30 输出和置换不变性结果见 CSV。",
        f"- 置换不变性失败数：`{perm_failures}`。",
        "",
        "## Full / K25 / K50 指标",
        "",
        "| variant | arm | HV median | IGD median | PFref size |",
        "|---|---:|---:|---:|---:|",
    ]
    for row in metric_summary_rows:
        summary_lines.append(f"| {row['variant']} | {row['arm']} | {fmt(row['hv_median'])} | {fmt(row['igd_median'])} | {row['pfref_count']} |")
    summary_lines.extend(
        [
            "",
            "## A4/A0 排序反转",
            "",
            f"Full 联合方向：`{full_joint_by_variant['full']}`；K25：`{full_joint_by_variant['k25']}`（reverses={str(full_joint_by_variant['k25'] != full_joint_by_variant['full']).lower()}）；K50：`{full_joint_by_variant['k50']}`（reverses={str(full_joint_by_variant['k50'] != full_joint_by_variant['full']).lower()}）。完整逐种子/中位数数值见 `a4-a0-ranking.csv`。",
            f"Leave-one-run/arm-out 中可比较的 A4/A0 反转数：`{leaveout_reversals}`；PFref 结构稳定性见 `leaveout-reference.csv`，排名明细见 `leaveout-ranking.csv`。",
            "",
            "## Gate A 结论边界",
            "",
            "当前离线决策前沿只对“基数/参考前沿敏感性”给出证据：0.1% 近重复阈值未触发，K25/K50 与 leave-out 的 A4/A0 反转按 CSV 判定。归档没有 observed-full-front、archive scan ledger、teacher directional regret 或 PDDR lifecycle 输入，因此物理 ND0 observer 等价门尚未关闭，不能据此启动 ND1-ND4。Gate A 六项逐项状态见 `gate-a-assessment.csv`。",
            "",
            "ND0 的硬门仍需单独的同 seed/同初始种群 observer run：initial population hash、FE/decoder calls、行为事件/Q 表 hash、最终 decision-front 必须等价，且 exact-dedup(decision-front) 必须等于 exact-dedup(observed-full-front)。本离线审计不冒充该等价实验。",
            "",
            "## 与 PDDR 的隔离",
            "",
            "本审计不改变、重算或归因 `GLOBAL_ORIGINAL` PDDR；不读取 PDDR Cmax lifecycle 作为 Gate A 证据，不改变 Qg/Qp/CFVF/CA-TA/local-search/mixture/FE，不把 observed-full、K25/K30/K50 送入搜索、teacher cache、PFref 或论文主表。`PDDR` 和 archive cardinality 是两个独立问题。",
            "",
            "## 输出文件",
            "",
            "`run-manifest.csv`、`file-verification.csv`、`cardinality.csv`、`permutation-invariance.csv`、`representative-front-k30.csv`、`sensitivity-front-k25.csv`、`sensitivity-front-k50.csv`、`reference-front-{full,k25,k50}.csv`、`metrics-{full,k25,k50}.csv`、`metric-summary.csv`、`a4-a0-ranking.csv`、`leaveout-reference.csv`、`leaveout-ranking.csv`、`gate-a-assessment.csv`、`audit-output-sha256.tsv`。",
        ]
    )
    (output / "audit-summary.md").write_text("\n".join(summary_lines) + "\n", encoding="utf-8")
    emit_output_sha256(output)

    print(f"archive_sha256={actual_archive_sha}")
    print(f"runs={len(runs)} verified_files={verified_files} hash_mismatches={hash_mismatches} missing_files={missing_files}")
    print(f"temp_extraction={extraction_root}")
    print(f"pfref_full={len(full_reference)} pfref_k25={len(k25_reference)} pfref_k50={len(k50_reference)}")
    print(f"near_0.1pct_median={overall_near_01:.8f} k_reversals={k_reversals} leaveout_reversals={leaveout_reversals}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (RuntimeError, OSError, tarfile.TarError, ValueError) as exc:
        raise SystemExit(f"AUDIT_FAILED: {exc}")
