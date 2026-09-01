#!/usr/bin/env python3
"""Extract a normalized run ledger from heterogeneous status.properties files."""

from __future__ import annotations

import csv
import hashlib
import re
from pathlib import Path

ROOT = Path("/home/inspur/aicomp")
OUTPUT = ROOT / "zhangbo-v35-paper-evidence-catalog-20260823" / "remote-run-ledger.csv"
CAMPAIGNS = {
    "zhangbo-cmax-audit-20k-20260812": "PILOT_DIAGNOSTIC",
    "zhangbo-fc6-20260818": "PAPER_PARAMETER_SELECTION",
    "zhangbo-fc6a1-20260819": "LEGACY_EXCLUDED",
    "zhangbo-fc6a4-order-20260820": "NEGATIVE_RESULT_APPENDIX",
    "zhangbo-fc6b-region-20260820": "TEMPORARY_OR_RETRY",
    "zhangbo-fc6b-region-20260820-r1": "TEMPORARY_OR_RETRY",
    "zhangbo-fc6b-region-20260820-r2": "TEMPORARY_OR_RETRY",
    "zhangbo-fc6b-region-20260820-r3": "NEGATIVE_RESULT_APPENDIX",
    "zhangbo-java-p9-decoder-timing-500k-20260811": "LEGACY_EXCLUDED",
    "zhangbo-java-p9-five-additional-500k-20260810": "LEGACY_EXCLUDED",
    "zhangbo-java-p9-pilot-20260810": "LEGACY_EXCLUDED",
    "zhangbo-java-p9-single-500k-20260810": "LEGACY_EXCLUDED",
    "zhangbo-p86-pair-100k-20260811": "LEGACY_EXCLUDED",
    "zhangbo-runtime-audit-100k-20260810": "LEGACY_EXCLUDED",
    "zhangbo-v35-doe1-20260820": "PAPER_PARAMETER_SELECTION",
    "zhangbo-v35-doe1-heldout-20260822": "PAPER_PARAMETER_SELECTION",
    "zhangbo-v35-p25a-main-variant-20260814": "NEGATIVE_RESULT_APPENDIX",
    "zhangbo-v35-p25d-8alg-50k-20260815": "LEGACY_EXCLUDED",
    "zhangbo-v35-p25e-corrected-50k-20260815": "PILOT_DIAGNOSTIC",
    "zhangbo-v35-stage2-master-v2-20260823": "PILOT_DIAGNOSTIC",
    "zhangbo-v35-stage2-phasebudget-20260823": "TEMPORARY_OR_RETRY",
    "zhangbo-v35-stage2-phasebudget-20260823-r2": "TEMPORARY_OR_RETRY",
    "zhangbo-v35-stage2-phasebudget-20260823-r3": "REPRODUCIBILITY_ONLY",
}


def parse_properties(path: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        if not line or line.lstrip().startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        result[key.strip()] = value.strip()
    return result


def first(props: dict[str, str], *keys: str) -> str:
    for key in keys:
        if props.get(key, "") != "":
            return props[key]
    return ""


def digest_if_exists(path: Path) -> str:
    if not path.is_file():
        return ""
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(4 * 1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest().upper()


def infer(path: Path, props: dict[str, str]) -> tuple[str, str, str]:
    text = str(path)
    inst = first(props, "instance", "p25eInstance")
    if not inst:
        matches = re.findall(r"(?<!\d)(\d+_\d+_\d+_1)(?!\d)", text)
        inst = matches[-1] if matches else ""
    seed = first(props, "seed", "p25eSeed")
    if not seed:
        match = re.search(r"seed[-_](\d{8})", text)
        seed = match.group(1) if match else ""
    arm = ""
    for part in reversed(path.parts):
        if re.fullmatch(r"A[0-5]", part) or part in {
            "HMOPSO_QGS_F", "HMOPSO_QLS_F", "MOPSO_F", "MOPSODS_DE_F",
            "MOHEADE_F", "NSGA_II_F", "SPEA2_F", "ZHANGBO_A4",
        }:
            arm = part
            break
    if not arm:
        arm = first(props, "arm", "algorithm", "p25eAlgorithm", "mode")
    return inst, seed, arm


def main() -> None:
    rows = []
    for campaign, paper_class in CAMPAIGNS.items():
        campaign_root = ROOT / campaign
        if not campaign_root.is_dir():
            continue
        for status_path in sorted(campaign_root.rglob("status.properties"), key=lambda p: str(p)):
            props = parse_properties(status_path)
            instance, seed, arm = infer(status_path, props)
            run_dir = status_path.parent
            actual_fe = first(props, "fullEvaluations", "p25eFullEvaluations", "successfulEvaluations")
            decoder_calls = first(props, "decoderCalls", "p25eDecoderCalls", "successfulDecoderCalls")
            status = first(props, "status", "p25eStatus", "runStatus")
            initial_hash = first(props, "initialPopulationHash", "p25eInitialPopulationHash")
            stage2_paired = (
                campaign == "zhangbo-v35-stage2-master-v2-20260823"
                and instance == "100_2_3_1"
                and seed.isdigit()
                and 20260808 <= int(seed) <= 20260819
                and arm in {"A0", "A1", "A2", "A3", "A4"}
                and status == "COMPLETED"
            )
            rows.append({
                "campaignId": campaign,
                "runId": str(run_dir.relative_to(campaign_root)),
                "instance": instance,
                "seed": seed,
                "armAlgorithm": arm,
                "status": status,
                "actualFE": actual_fe,
                "decoderCalls": decoder_calls,
                "initialPopulationHash": initial_hash,
                "frontHash": digest_if_exists(run_dir / "front.csv"),
                "configurationHash": digest_if_exists(run_dir / "configuration.txt"),
                "remotePath": str(run_dir),
                "paperUseClass": paper_class,
                "referenceEligible": "true" if stage2_paired else "false",
            })
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    temp = OUTPUT.with_suffix(".csv.partial")
    fields = (
        "campaignId", "runId", "instance", "seed", "armAlgorithm", "status",
        "actualFE", "decoderCalls", "initialPopulationHash", "frontHash",
        "configurationHash", "remotePath", "paperUseClass", "referenceEligible",
    )
    with temp.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=fields, lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)
    temp.replace(OUTPUT)


if __name__ == "__main__":
    main()
