import csv
import hashlib
import json
import sys
import tempfile
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parents[1] / "tools" / "python"
sys.path.insert(0, str(TOOLS))
import v35_master_v2 as master


def base_run(arm, ordinal=0):
    row = {
        "runId": f"r-{arm}-{ordinal}", "arm": arm, "instance": "20_2_3_1", "seed": 20260808,
        "maxFEs": 500000, "jarSha256": "a" * 64,
        "armProfileSha256": (arm.lower() * 64)[:64], "snapshotSha256": "b" * 64,
        "initialPopulationHashV35": "c" * 64, "initialPopulationHashP8": "d" * 64,
        "problemConfigurationSha256": "e" * 64,
        "algorithmSemanticsVersion": "v35-final-a0-a4-ablation-v1",
        "budgetProtocolVersion": "v35-phase-consistent-budget-v1", "purpose": "FORMAL",
        "output": f"/tmp/{arm}", "command": ["java", "ZhangBoV35FormalAblationArmRunner"],
    }
    row["runKey"] = master.run_key(row)
    return row


class MasterV2Test(unittest.TestCase):
    def manifest(self):
        return {"schema": master.SCHEMA, "outputRoot": "/tmp", "runs":
                [base_run(arm) for arm in master.ARMS]}

    def test_complete_roster(self):
        groups = master.validate_manifest(self.manifest())
        self.assertEqual(1, len(groups))

    def test_duplicate_arm_rejected(self):
        manifest = self.manifest()
        replacement = base_run("A0", 2)
        manifest["runs"][-1] = replacement
        with self.assertRaises(master.GateError):
            master.validate_manifest(manifest)

    def test_run_key_binds_profile_snapshot_and_problem(self):
        original = base_run("A2")
        for field in ("armProfileSha256", "snapshotSha256", "problemConfigurationSha256"):
            changed = dict(original)
            changed[field] = "f" * 64
            self.assertNotEqual(original["runKey"], master.run_key(changed), field)

    def test_group_snapshot_mismatch_rejected(self):
        manifest = self.manifest()
        manifest["runs"][3]["snapshotSha256"] = "f" * 64
        manifest["runs"][3]["runKey"] = master.run_key(manifest["runs"][3])
        with self.assertRaises(master.GateError):
            master.validate_manifest(manifest)

    def test_evidence_manifest_is_recomputed_not_merely_present(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            payload = root / "front.csv"
            payload.write_text("Cmax,TEC,TWC\n1,2,3\n", encoding="utf-8")
            master.write_evidence_manifest(root)
            master.verify_evidence_manifest(root)
            payload.write_text("tampered\n", encoding="utf-8")
            with self.assertRaises(master.GateError):
                master.verify_evidence_manifest(root)

    def test_fe_range_equal_q_phase_rejected(self):
        runs = self.manifest()["runs"]
        audited = [{"actualFE": 500000 - index * 1250} for index in range(5)]
        with self.assertRaises(master.GateError):
            master.validate_group(("20_2_3_1", 20260808), runs, audited)


if __name__ == "__main__":
    unittest.main()
