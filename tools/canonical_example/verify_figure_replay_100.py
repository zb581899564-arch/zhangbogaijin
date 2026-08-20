#!/usr/bin/env python3
"""Verify deterministic I1 SVG/PDF/PNG exports over repeated clean renders."""

from __future__ import annotations

import argparse
import csv
import hashlib
import shutil
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path


INPUT_DIRECTORIES = (
    "01_input",
    "02_decoder_fm3",
    "04_fm0_regression",
    "05_one_particle_evolution",
    "06_local_search",
    "07_environment_selection",
)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest().upper()


def run_command(command: list[str], cwd: Path) -> None:
    completed = subprocess.run(command, cwd=cwd, text=True,
                               stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    if completed.returncode:
        raise RuntimeError(f"Command failed ({completed.returncode}): {' '.join(command)}\n{completed.stdout}")


def render_worker(project: Path, evidence: Path, temporary: Path,
                  expected: dict[str, str], run_ids: list[int]) -> list[dict[str, object]]:
    worker_root = temporary / f"worker-{run_ids[0]:03d}"
    worker_root.mkdir(parents=True, exist_ok=True)
    for directory in INPUT_DIRECTORIES:
        shutil.copytree(evidence / directory, worker_root / directory, dirs_exist_ok=True)

    rows: list[dict[str, object]] = []
    for ordinal, run_id in enumerate(run_ids, start=1):
        figures = worker_root / "08_figures"
        if figures.exists():
            shutil.rmtree(figures)
        run_command([sys.executable, "tools/canonical_example/generate_i1_figures.py",
                     "--evidence-root", str(worker_root)], project)
        run_command([sys.executable, "tools/canonical_example/build_evolution_evidence.py",
                     "--project-root", str(project), "--evidence-root", str(worker_root)], project)
        actual = {path.name: sha256(path) for path in figures.iterdir()
                  if path.is_file() and path.suffix.lower() in {".svg", ".pdf", ".png"}}
        mismatches = sorted(name for name, expected_hash in expected.items()
                            if actual.get(name) != expected_hash)
        extras = sorted(set(actual) - set(expected))
        rows.append({
            "run": run_id,
            "verifiedFiles": len(expected),
            "mismatchCount": len(mismatches),
            "extraCount": len(extras),
            "allMatch": str(not mismatches and not extras).lower(),
            "mismatches": ";".join(mismatches + extras),
        })
        if ordinal % 5 == 0 or ordinal == len(run_ids):
            print(f"FIGURE_REPLAY_PROGRESS worker={run_ids[0]:03d} completed={ordinal}/{len(run_ids)}",
                  flush=True)
    shutil.rmtree(worker_root)
    return rows


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--project-root", required=True, type=Path)
    parser.add_argument("--evidence-root", required=True, type=Path)
    parser.add_argument("--runs", type=int, default=100)
    parser.add_argument("--workers", type=int, default=4)
    args = parser.parse_args()

    project = args.project_root.resolve()
    evidence = args.evidence_root.resolve()
    figures = evidence / "08_figures"
    expected = {path.name: sha256(path) for path in figures.iterdir()
                if path.is_file() and path.suffix.lower() in {".svg", ".pdf", ".png"}}
    if len(expected) != 36:
        raise AssertionError(f"Expected 36 frozen figure files, got {len(expected)}")

    temporary = project / ".codex-temp" / "p82-figure-replay-100"
    if temporary.exists():
        shutil.rmtree(temporary)
    temporary.mkdir(parents=True)
    chunks = [[] for _ in range(args.workers)]
    for run_id in range(1, args.runs + 1):
        chunks[(run_id - 1) % args.workers].append(run_id)

    rows: list[dict[str, object]] = []
    try:
        with ThreadPoolExecutor(max_workers=args.workers) as pool:
            futures = [pool.submit(render_worker, project, evidence, temporary, expected, chunk)
                       for chunk in chunks if chunk]
            for future in futures:
                rows.extend(future.result())
    finally:
        if temporary.exists():
            shutil.rmtree(temporary)

    rows.sort(key=lambda row: int(row["run"]))
    output = figures / "figure_replay_100_audit.csv"
    with output.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=list(rows[0]), lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)
    all_match = len(rows) == args.runs and all(row["allMatch"] == "true" for row in rows)
    (figures / "figure_replay_100_summary.properties").write_text(
        "schemaVersion=1\n"
        f"totalVerifiedRuns={len(rows)}\n"
        f"filesPerRun={len(expected)}\n"
        f"totalHashComparisons={len(rows) * len(expected)}\n"
        f"allMatch={str(all_match).lower()}\n",
        encoding="utf-8", newline="\n")
    if not all_match:
        raise AssertionError("At least one figure replay differed from the frozen exports")
    print(f"FIGURE_REPLAY_100_PASSED runs={len(rows)} comparisons={len(rows) * len(expected)}")


if __name__ == "__main__":
    main()
