#!/usr/bin/env python3
"""Finalize the V35 paper-evidence ledgers after verified cleanup."""

from __future__ import annotations

import csv
from pathlib import Path


MASTER = Path(r"E:\学习\李明哲-毕业材料\张博改进\docs\PAPER_EVIDENCE_MASTER")
ARCHIVE = Path(r"G:\ResearchArchive\ZhangBo-V35-Paper-Evidence-20260823")


def read_csv(path: Path, delimiter: str = ",") -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8-sig", newline="") as stream:
        return list(csv.DictReader(stream, delimiter=delimiter))


def write_csv(path: Path, rows: list[dict[str, object]], fields: list[str], delimiter: str = ",") -> None:
    temporary = path.with_suffix(path.suffix + ".partial")
    with temporary.open("w", encoding="utf-8-sig", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=fields, delimiter=delimiter, lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)
    temporary.replace(path)


def normalized(value: str) -> str:
    return value.replace("\\", "/").rstrip("/").casefold()


def local_class(path: str) -> str:
    normalized_path = normalized(path)
    if any(token in normalized_path for token in ("v35-p25d", "p8.4", "p8.6", "/p9-", "/p9/", "legacy")):
        return "LEGACY_EXCLUDED"
    if any(token in normalized_path for token in ("v35-stage2-pilot", "v35-p25e", "v35-p21")):
        return "PILOT_DIAGNOSTIC"
    if any(token in normalized_path for token in ("v35-doe1", "v35-p24.2", "fc6", "heldout")):
        return "PAPER_PARAMETER_SELECTION"
    if any(token in normalized_path for token in ("paper_evidence/i0-v35", "paper_evidence/i1", "/docs/evidence/p1", "/docs/evidence/p2", "/docs/evidence/p5", "/docs/evidence/p6")):
        return "MAIN_METHOD_EVIDENCE"
    if any(token in normalized_path for token in ("regression-sandbox", "/target/", "/.codex-temp")):
        return "TEMPORARY_OR_RETRY"
    return "REPRODUCIBILITY_ONLY"


def main() -> None:
    cleanup_path = MASTER / "cleanup-execution.csv"
    cleanup = read_csv(cleanup_path)
    remote_cleanup = read_csv(MASTER / "inventory" / "remote-cleanup-execution.tsv", "\t")
    existing_ids = {row["targetId"] for row in cleanup}
    for row in remote_cleanup:
        target_id = "REMOTE_" + row["campaignId"]
        if target_id not in existing_ids:
            cleanup.append(
                {
                    "targetId": target_id,
                    "host": "aic-inspur-home",
                    "path": row["path"],
                    "bytesBefore": row["bytesBefore"],
                    "archivePath": str(ARCHIVE / "remote-campaigns" / f"{row['campaignId']}.tar.gz"),
                    "archiveSha256": row["archiveSha256"],
                    "result": row["result"],
                    "recoverable": row["recoverable"],
                }
            )
    chunk_id = "REMOTE_TRANSFER_CHUNKS_zhangbo-v35-doe1-20260820"
    if chunk_id not in {row["targetId"] for row in cleanup}:
        cleanup.append(
            {
                "targetId": chunk_id,
                "host": "aic-inspur-home",
                "path": "/home/inspur/aicomp/zhangbo-v35-paper-evidence-catalog-20260823/chunks/zhangbo-v35-doe1-20260820",
                "bytesBefore": "887844682",
                "archivePath": str(ARCHIVE / "remote-campaigns" / "zhangbo-v35-doe1-20260820.tar.gz"),
                "archiveSha256": "BF12E1ED5F8AE5B640FE886C18046BD6E9BAC7F52BD4F8A7F3217BCEE23EC1C8",
                "result": "DELETED_REBUILDABLE_TRANSFER_CHUNKS",
                "recoverable": "true",
            }
        )
    cleanup.sort(key=lambda row: (row["host"], row["targetId"]))
    cleanup_fields = ["targetId", "host", "path", "bytesBefore", "archivePath", "archiveSha256", "result", "recoverable"]
    write_csv(cleanup_path, cleanup, cleanup_fields)

    cleanup_by_path = {normalized(row["path"]): row for row in cleanup}
    cleanup_by_campaign = {
        row["targetId"].removeprefix("REMOTE_"): row
        for row in cleanup
        if row["targetId"].startswith("REMOTE_") and not row["targetId"].startswith("REMOTE_TRANSFER_CHUNKS_")
    }

    candidates_path = MASTER / "cleanup-candidates.csv"
    candidates = read_csv(candidates_path)
    for row in candidates:
        match = cleanup_by_path.get(normalized(row["path"]))
        if match:
            row["status"] = "COMPLETED_RECOVERABLE"
            row["plannedAction"] = match["result"]
            row["requiredArchive"] = match["archivePath"]
    write_csv(candidates_path, candidates, list(candidates[0]))

    campaigns = read_csv(MASTER / "campaign-ledger.csv")
    campaign_meta = {Path(row["remotePath"]).name: row for row in campaigns if row["remotePath"]}
    pre_summary = read_csv(MASTER / "inventory" / "pre-cleanup" / "remote-campaign-summary.csv")
    post_summary = {row["campaignId"]: row for row in read_csv(MASTER / "inventory" / "post-cleanup" / "remote-campaign-summary.csv")}
    remote_manifest = {row["campaignId"]: row for row in read_csv(ARCHIVE / "manifests" / "remote-archive-manifest.tsv", "\t")}
    locations: list[dict[str, object]] = []
    for before in pre_summary:
        campaign_id = before["campaignId"]
        after = post_summary[campaign_id]
        meta = campaign_meta.get(campaign_id, {})
        deleted = after["status"] == "MISSING" and campaign_id in cleanup_by_campaign
        locations.append(
            {
                "campaignId": campaign_id,
                "remotePath": before["remotePath"],
                "status": "DELETED_WITH_RESTORE_PATH" if deleted else after["status"],
                "fileCountBefore": before["fileCount"],
                "totalBytesBefore": before["totalBytes"],
                "fileCountAfter": after["fileCount"],
                "totalBytesAfter": after["totalBytes"],
                "paperUseClass": meta.get("paperUseClass", "REVIEW_REQUIRED"),
                "referenceEligible": meta.get("referenceEligible", "false"),
                "localPath": meta.get("localPath", ""),
                "retentionClass": meta.get("retentionClass", "REVIEW"),
                "archivePath": str(ARCHIVE / "remote-campaigns" / f"{campaign_id}.tar.gz"),
                "archiveSha256": remote_manifest[campaign_id]["archiveSha256"],
            }
        )
    location_fields = [
        "campaignId", "remotePath", "status", "fileCountBefore", "totalBytesBefore",
        "fileCountAfter", "totalBytesAfter", "paperUseClass", "referenceEligible",
        "localPath", "retentionClass", "archivePath", "archiveSha256",
    ]
    write_csv(MASTER / "remote-location-map.csv", sorted(locations, key=lambda row: row["campaignId"]), location_fields)

    artifact_rows: list[dict[str, object]] = []
    local_cleanup = [(path, row) for path, row in cleanup_by_path.items() if row["host"] == "local"]
    for row in read_csv(MASTER / "inventory" / "pre-cleanup" / "local-artifact-ledger.tsv", "\t"):
        path_normal = normalized(row["path"])
        match = next((record for prefix, record in local_cleanup if path_normal == prefix or path_normal.startswith(prefix + "/")), None)
        paper_class = local_class(row["path"])
        artifact_rows.append(
            {
                "host": row["host"], "campaignId": "", "path": row["path"], "bytes": row["bytes"],
                "sha256": row["sha256"], "paperUseClass": paper_class, "referenceEligible": "false",
                "retentionClass": "DELETED_RECOVERABLE" if match else "PRESERVED_OR_ARCHIVED",
                "archivePath": match["archivePath"] if match else str(ARCHIVE),
                "cleanupAction": match["result"] if match else "PRESERVED",
            }
        )
    for row in read_csv(MASTER / "inventory" / "pre-cleanup" / "remote-artifact-ledger.tsv", "\t"):
        campaign_id = row["campaignId"]
        meta = campaign_meta.get(campaign_id, {})
        match = cleanup_by_campaign.get(campaign_id)
        artifact_rows.append(
            {
                "host": row["host"], "campaignId": campaign_id, "path": row["path"], "bytes": row["bytes"],
                "sha256": row["sha256"], "paperUseClass": meta.get("paperUseClass", "REVIEW_REQUIRED"),
                "referenceEligible": meta.get("referenceEligible", "false"),
                "retentionClass": "DELETED_RECOVERABLE" if match else meta.get("retentionClass", "PRESERVED"),
                "archivePath": str(ARCHIVE / "remote-campaigns" / f"{campaign_id}.tar.gz"),
                "cleanupAction": match["result"] if match else "PRESERVED_REMOTE_EXPANDED",
            }
        )
    artifact_fields = [
        "host", "campaignId", "path", "bytes", "sha256", "paperUseClass", "referenceEligible",
        "retentionClass", "archivePath", "cleanupAction",
    ]
    write_csv(MASTER / "artifact-ledger.tsv", artifact_rows, artifact_fields, "\t")

    local_bytes = sum(int(row["bytesBefore"]) for row in cleanup if row["host"] == "local")
    remote_bytes = sum(int(row["bytesBefore"]) for row in cleanup if row["host"] == "aic-inspur-home")
    summary = [
        {"key": "artifactRows", "value": str(len(artifact_rows))},
        {"key": "remoteCampaigns", "value": str(len(locations))},
        {"key": "remotePresentAfter", "value": str(sum(row["status"] == "PRESENT" for row in locations))},
        {"key": "remoteDeletedRecoverable", "value": str(sum(row["status"] == "DELETED_WITH_RESTORE_PATH" for row in locations))},
        {"key": "localCleanupBytes", "value": str(local_bytes)},
        {"key": "remoteCleanupBytesIncludingTransferChunks", "value": str(remote_bytes)},
        {"key": "generatedAt", "value": "2026-08-24T00:00:00+08:00"},
    ]
    write_csv(MASTER / "FINAL_LEDGER_BUILD_SUMMARY.csv", summary, ["key", "value"])


if __name__ == "__main__":
    main()
