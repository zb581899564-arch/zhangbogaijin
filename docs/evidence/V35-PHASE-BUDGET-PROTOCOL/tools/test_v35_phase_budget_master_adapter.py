#!/usr/bin/env python3
"""No-Java contract tests for the phase-budget external adapter."""

import importlib.util
import sys
import tempfile
from pathlib import Path


SOURCE = Path(__file__).with_name("v35_phase_budget_master_adapter.py")
SPEC = importlib.util.spec_from_file_location("phase_adapter", SOURCE)
assert SPEC and SPEC.loader
MODULE = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = MODULE
SPEC.loader.exec_module(MODULE)


def check(requested, actual, decoder, kind, accepted, remaining):
    decision = MODULE.classify_budget(requested, actual, decoder)
    assert decision.termination_kind == kind
    assert decision.accepted is accepted
    assert decision.remaining_fe == remaining
    assert decision.q_phase_fe == 5000


def main():
    check(50000, 50000, 50000, "EXACT_MAX_FE", True, 0)
    check(50000, 48269, 48269, "PHASE_CONSISTENT_TAIL_STOP", True, 1731)
    check(100000, 96025, 96025, "PHASE_CONSISTENT_TAIL_STOP", True, 3975)
    check(50000, 45000, 45000, "INVALID", False, 5000)
    check(50000, 0, 0, "INVALID", False, 50000)
    check(50000, 48269, 48268, "INVALID", False, 1731)
    with tempfile.TemporaryDirectory() as temporary:
        root = Path(temporary)
        entries = []
        for arm, actual in zip(("A0", "A1", "A2", "A3", "A4"), (50000, 49900, 49800, 49700, 48269)):
            entries.append({"arm": arm, "initialPopulationHash": "same",
                            "decision": MODULE.classify_budget(50000, actual, actual)})
        result = MODULE.audit_group(entries, root)
        assert result["groupStatus"] == "VALID"
        assert (root / "budget-utilization.csv").is_file()
        bad = list(entries)
        bad[-1] = dict(bad[-1], decision=MODULE.classify_budget(50000, 45000, 45000))
        try:
            MODULE.audit_group(bad, root / "bad")
        except MODULE.PhaseBudgetError:
            pass
        else:
            raise AssertionError("invalid group unexpectedly accepted")
    print("V35_PHASE_BUDGET_ADAPTER_TEST_PASSED")


if __name__ == "__main__":
    main()
