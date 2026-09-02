import csv
import glob
import os
import hashlib
from pathlib import Path

root = Path("docs/evidence/V35-QP-V2-PHASEB1-IMPLEMENTATION/05-remote-20k/results")
runs = sorted([p for p in root.iterdir() if p.is_dir()])

print(f"Total runs found: {len(runs)}")

def sha(p):
    return hashlib.sha256(p.read_bytes()).hexdigest().upper()

for r in runs:
    sum_file = r / "summary.properties"
    props = {}
    with open(sum_file, "r", encoding="utf-8") as f:
        for line in f:
            if "=" in line and not line.startswith("#"):
                k, v = line.strip().split("=", 1)
                props[k] = v
    front_file = r / "front.csv"
    front_sha = sha(front_file)
    telem_file = r / "qp-pool-telemetry.csv"
    telem_rows = 0
    if telem_file.exists():
        with open(telem_file, "r", encoding="utf-8") as tf:
            telem_rows = sum(1 for _ in tf) - 1
    print(f"Run: {r.name:32s} | Profile: {props.get('profile'):8s} | Inst: {props.get('instance'):9s} | ActualFE: {props.get('actualFEs'):5s} | FrontSize: {props.get('frontSize'):3s} | FrontSHA: {front_sha[:16]}... | TelemRows: {telem_rows:5d} | ExtraRng: {props.get('totalExtraRngDraws'):4s} | NonCanon: {props.get('nonCanonicalSelections'):4s}")