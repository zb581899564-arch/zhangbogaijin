#!/usr/bin/env python3
"""Deterministic zero-FE audit proving that Observer V4 violates the frozen schema."""
from __future__ import print_function

import csv
import json
import os
import sys


HERE = os.path.dirname(os.path.abspath(__file__))
PACKAGE = os.path.dirname(HERE)
CAMPAIGN = os.path.dirname(PACKAGE)
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(CAMPAIGN)))
V4_IMPL = os.path.join(CAMPAIGN, "01-observer-implementation", "src", "org", "uma",
                       "jmetal", "algorithm", "multiobjective", "mypso", "v35",
                       "V35SourceAttributionObserver.java")
V4_RUN = os.path.join(CAMPAIGN, "07-sa-hard-500k", "sync", "results",
                      "SA-HARD-500k")
LEDGER = os.path.join(V4_RUN, "source-ledger.csv")

REQUIRED_LEDGER_COLUMNS = {
    "actualFE", "nominalFE", "generation", "outerCycle", "qRound",
    "rawSource", "firstLevelSource", "candidateFingerprint", "Cmax", "TEC", "TWC",
}
REQUIRED_LIFECYCLE_EVENTS = {
    "GENERATED", "MERGE_POOL", "PDDR_SELECTED", "WORKING_POPULATION",
    "PERSONAL_ARCHIVE", "QG_TEACHER", "QP_TEACHER", "DESCENDANT",
    "IMPROVING_DESCENDANT",
}


def main():
    with open(LEDGER, newline="", encoding="utf-8") as stream:
        columns = set(next(csv.reader(stream)))
    with open(V4_IMPL, encoding="utf-8") as stream:
        source = stream.read()

    lifecycle = os.path.join(V4_RUN, "source-lifecycle-events.csv")
    b0 = os.path.join(V4_RUN, "checkpoints", "checkpoint-0-decision-front.csv")
    result = {
        "schema": "v35-source-attribution-observer-schema-v1",
        "ledgerMissingColumns": sorted(REQUIRED_LEDGER_COLUMNS - columns),
        "lifecycleLedgerPresent": os.path.isfile(lifecycle),
        "b0DecisionFrontExported": os.path.isfile(b0),
        "qpActionHardcodedNA": 'String qpAction = "NA";' in source,
        "parentVectorLookupUsesChildLineage": "parentRawCache.get(lineageId)" in source,
        "requiredLifecycleEvents": sorted(REQUIRED_LIFECYCLE_EVENTS),
    }
    violations = []
    if result["ledgerMissingColumns"]:
        violations.append("MISSING_LEDGER_COLUMNS")
    if not result["lifecycleLedgerPresent"]:
        violations.append("MISSING_LIFECYCLE_LEDGER")
    if not result["b0DecisionFrontExported"]:
        violations.append("B0_NOT_EXPORTED")
    if result["qpActionHardcodedNA"]:
        violations.append("QP_ACTION_NOT_OBSERVED")
    if result["parentVectorLookupUsesChildLineage"]:
        violations.append("PARENT_VECTOR_LOOKUP_KEY_ERROR")
    result["violations"] = violations
    result["verdict"] = "V4_SCHEMA_VIOLATION_CONFIRMED" if violations else "PASS"

    out_json = os.path.join(HERE, "v4-contract-audit.json")
    with open(out_json, "w", encoding="utf-8", newline="\n") as stream:
        json.dump(result, stream, ensure_ascii=False, indent=2, sort_keys=True)
        stream.write("\n")
    print(result["verdict"])
    return 1 if violations else 0


if __name__ == "__main__":
    sys.exit(main())

