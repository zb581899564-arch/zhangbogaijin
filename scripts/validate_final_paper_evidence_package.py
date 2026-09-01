#!/usr/bin/env python3
"""Read-only validation for the final V35 evidence catalog and cold archive."""

from __future__ import annotations

import csv
import hashlib
import zipfile
from pathlib import Path


MASTER = Path(r"E:\学习\李明哲-毕业材料\张博改进\docs\PAPER_EVIDENCE_MASTER")
ARCHIVE = Path(r"G:\ResearchArchive\ZhangBo-V35-Paper-Evidence-20260823")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(8 * 1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest().upper()


def read_rows(path: Path, delimiter: str = "\t") -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8-sig", newline="") as stream:
        return list(csv.DictReader(stream, delimiter=delimiter))


def main() -> None:
    errors: list[str] = []
    evidence = read_rows(MASTER / "evidence-sha256.tsv")
    for row in evidence:
        path = MASTER / row["path"]
        if not path.is_file():
            errors.append(f"missing evidence: {row['path']}")
        elif path.stat().st_size != int(row["bytes"]) or sha256(path) != row["sha256"]:
            errors.append(f"evidence mismatch: {row['path']}")

    archives = read_rows(MASTER / "V35-PAPER-EVIDENCE-ARCHIVE-MANIFEST.tsv")
    for row in archives:
        path = Path(row["archivePath"])
        if not path.is_file():
            errors.append(f"missing archive: {path}")
        elif path.stat().st_size != int(row["bytes"]) or sha256(path) != row["sha256"]:
            errors.append(f"archive mismatch: {path}")

    master_files = sorted(path for path in MASTER.rglob("*") if path.is_file())
    catalog = ARCHIVE / "catalog"
    catalog_files = sorted(path for path in catalog.rglob("*") if path.is_file())
    if len(master_files) != len(catalog_files):
        errors.append(f"catalog count {len(catalog_files)} != master {len(master_files)}")
    for source in master_files:
        relative = source.relative_to(MASTER)
        target = catalog / relative
        if not target.is_file():
            errors.append(f"catalog missing: {relative.as_posix()}")
        elif sha256(source) != sha256(target):
            errors.append(f"catalog mismatch: {relative.as_posix()}")

    package = ARCHIVE / "packages" / "V35-PAPER-EVIDENCE-CATALOG-20260823.zip"
    package_properties: dict[str, str] = {}
    for line in (ARCHIVE / "packages" / "PACKAGE-SHA256.txt").read_text(encoding="utf-8").splitlines():
        if "=" in line:
            key, value = line.split("=", 1)
            package_properties[key] = value
    package_hash = sha256(package)
    if package_hash != package_properties.get("sha256"):
        errors.append("package hash mismatch")

    with zipfile.ZipFile(package, "r") as archive:
        entries = {entry.filename.rstrip("/"): entry for entry in archive.infolist() if not entry.is_dir()}
        if len(entries) != len(master_files):
            errors.append(f"zip entry count {len(entries)} != master {len(master_files)}")
        for source in master_files:
            relative = source.relative_to(MASTER).as_posix()
            entry = entries.get(relative)
            if entry is None:
                errors.append(f"zip missing: {relative}")
                continue
            digest = hashlib.sha256()
            with archive.open(entry, "r") as stream:
                for block in iter(lambda: stream.read(4 * 1024 * 1024), b""):
                    digest.update(block)
            if digest.hexdigest().upper() != sha256(source):
                errors.append(f"zip mismatch: {relative}")

    remote_downloads = read_rows(ARCHIVE / "manifests" / "remote-archive-download-validation.csv", ",")
    remote_locations = read_rows(MASTER / "remote-location-map.csv", ",")
    output = {
        "evidenceManifestRows": len(evidence),
        "archiveManifestRows": len(archives),
        "masterFiles": len(master_files),
        "catalogFiles": len(catalog_files),
        "zipFiles": len(entries),
        "remoteArchivesVerified": len(remote_downloads),
        "remotePresent": sum(row["status"] == "PRESENT" for row in remote_locations),
        "remoteDeletedRecoverable": sum(row["status"] == "DELETED_WITH_RESTORE_PATH" for row in remote_locations),
        "packageBytes": package.stat().st_size,
        "packageSha256": package_hash,
        "validationErrors": len(errors),
    }
    for key, value in output.items():
        print(f"{key}={value}")
    if errors:
        for error in errors[:50]:
            print(f"ERROR={error}")
        raise SystemExit(1)


if __name__ == "__main__":
    main()
