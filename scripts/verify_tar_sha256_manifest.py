#!/usr/bin/env python3
"""Verify every file named by an embedded GNU sha256sum manifest in a tar.gz."""

from __future__ import annotations

import argparse
import hashlib
import tarfile
from pathlib import PurePosixPath


def digest(stream) -> str:
    value = hashlib.sha256()
    for block in iter(lambda: stream.read(8 * 1024 * 1024), b""):
        value.update(block)
    return value.hexdigest().lower()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("archive")
    parser.add_argument("--root", required=True)
    manifest_group = parser.add_mutually_exclusive_group(required=True)
    manifest_group.add_argument("--manifest")
    manifest_group.add_argument("--manifest-file")
    args = parser.parse_args()

    prefix = args.root.rstrip("/") + "/"
    manifest_name = prefix + args.manifest.lstrip("/") if args.manifest else None
    if args.manifest_file:
        manifest_text = open(args.manifest_file, "r", encoding="utf-8").read()
    else:
        with tarfile.open(args.archive, "r:gz") as archive:
            manifest_member = archive.getmember(manifest_name)
            manifest_stream = archive.extractfile(manifest_member)
            if manifest_stream is None:
                raise RuntimeError("manifest is not a regular file")
            manifest_text = manifest_stream.read().decode("utf-8")

    expected = {}
    manifest_lines = manifest_text.splitlines()
    is_tsv = bool(manifest_lines and manifest_lines[0].startswith("relativePath\tbytes\tsha256"))
    if is_tsv:
        manifest_lines = manifest_lines[1:]
    for raw in manifest_lines:
        if not raw.strip():
            continue
        if is_tsv:
            rel, _size, sha = raw.split("\t", 2)
        else:
            sha, rel = raw.split(None, 1)
        rel = rel.strip()
        if rel.startswith("*"):
            rel = rel[1:]
        if rel.startswith("./"):
            rel = rel[2:]
        normalized = str(PurePosixPath(rel))
        if normalized.startswith("../") or normalized == "..":
            raise RuntimeError(f"unsafe manifest path: {rel}")
        expected[prefix + normalized] = sha.lower()

    verified = 0
    failures = []
    seen = set()
    with tarfile.open(args.archive, "r|gz") as archive:
        for member in archive:
            expected_sha = expected.get(member.name)
            if expected_sha is None:
                continue
            stream = archive.extractfile(member)
            if stream is None:
                failures.append((member.name, "not-a-file"))
                continue
            actual = digest(stream)
            seen.add(member.name)
            if actual != expected_sha:
                failures.append((member.name, actual))
            else:
                verified += 1
    for missing in sorted(set(expected) - seen):
        failures.append((missing, "missing"))

    print(f"verified={verified}")
    print(f"failures={len(failures)}")
    for path, actual in failures[:20]:
        print(f"failure={path}\t{actual}")
    if failures:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
