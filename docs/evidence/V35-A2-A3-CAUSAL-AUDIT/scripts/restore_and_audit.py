#!/usr/bin/env python3
"""Recompute the A2/A3 audit from the immutable G-drive tarball.

Only the selected 24 formal run directories and the pilot aggregate output
are copied into a temporary directory.  The temporary copy is removed when
the process exits; no Java/SSH/remote command is invoked.
"""

from __future__ import annotations

import argparse
import importlib.util
import sys
import tarfile
import tempfile
from pathlib import Path


SEEDS = [f"202608{n:02d}" for n in range(8, 20)]
ARMS = ("A2", "A3")
TOP = "zhangbo-v35-stage2-master-v2-20260823"
FORMAL_PREFIX = f"{TOP}/results/formal-a0-a4-4500/100_2_3_1"
PILOT_PREFIX = f"{TOP}/pilot-a0-a4/output"


def selected(name: str) -> bool:
    normalized = name.rstrip("/")
    for seed in SEEDS:
        for arm in ARMS:
            prefix = f"{FORMAL_PREFIX}/seed-{seed}/{arm}"
            if normalized == prefix or normalized.startswith(prefix + "/"):
                return True
    return normalized == PILOT_PREFIX or normalized.startswith(PILOT_PREFIX + "/")


def safe_extract_selected(archive: Path, destination: Path) -> int:
    count = 0
    destination = destination.resolve()
    with tarfile.open(archive, "r:gz") as handle:
        for member in handle:
            if not selected(member.name) or not member.isfile():
                continue
            target = (destination / member.name).resolve()
            if destination != target and destination not in target.parents:
                raise RuntimeError(f"archive member escapes temporary root: {member.name}")
            target.parent.mkdir(parents=True, exist_ok=True)
            source = handle.extractfile(member)
            if source is None:
                raise RuntimeError(f"cannot read archive member: {member.name}")
            target.write_bytes(source.read())
            count += 1
    return count


def run_analyzer(script_path: Path, restored_root: Path, output_dir: Path) -> int:
    spec = importlib.util.spec_from_file_location("a2_a3_audit", script_path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load analyzer: {script_path}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    old_argv = sys.argv
    try:
        sys.argv = [str(script_path), "--restored-root", str(restored_root), "--output-dir", str(output_dir)]
        return int(module.main())
    finally:
        sys.argv = old_argv


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--archive", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    args = parser.parse_args()
    archive = args.archive.resolve()
    if not archive.is_file():
        raise SystemExit(f"archive not found: {archive}")
    analyzer = Path(__file__).with_name("analyze_a2_a3.py")
    with tempfile.TemporaryDirectory(prefix="v35-a2-a3-restore-") as temporary:
        restored_root = Path(temporary)
        count = safe_extract_selected(archive, restored_root)
        print(f"selected_files_restored_to_temporary_root={count}")
        return run_analyzer(analyzer, restored_root, args.output_dir.resolve())


if __name__ == "__main__":
    raise SystemExit(main())
