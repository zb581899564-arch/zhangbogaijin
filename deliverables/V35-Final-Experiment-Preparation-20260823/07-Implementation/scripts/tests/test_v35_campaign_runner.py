import json
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
import v35_campaign_runner as runner  # noqa: E402


FREEZE_HASH = "a" * 64


def manifest_for(command, *, max_attempts=1, isolation_id=None):
    run = {
        "algorithm": "V35_DIAGNOSTIC",
        "configHash": "b" * 64,
        "instance": "20_2_3_1",
        "seed": 20260822,
        "budget": 100,
        "safetyClass": "short_benchmark",
        "benchmarkId": "unit-test",
        "maxAttempts": max_attempts,
        "command": command,
    }
    if isolation_id is not None:
        run["isolationId"] = isolation_id
    return {
        "schemaVersion": 1,
        "campaignId": "unit-test-campaign",
        "maxParallel": 2,
        "maxDiagnosticBudget": 2000,
        "frozenBoundaryHash": FREEZE_HASH,
        "freezeEvidence": "docs/evidence/V35-FINAL-FREEZE/FREEZE_MANIFEST.json",
        "runs": [run],
    }


class V35CampaignRunnerTest(unittest.TestCase):
    def write_manifest(self, root, manifest):
        path = root / "campaign.json"
        path.write_text(json.dumps(manifest), encoding="utf-8")
        return path

    def test_duplicate_run_key_is_rejected(self):
        manifest = manifest_for([sys.executable, "-c", "pass"])
        manifest["runs"].append(dict(manifest["runs"][0]))
        with self.assertRaisesRegex(runner.CampaignError, "duplicate RunKey"):
            runner.validate_manifest(manifest)

    def test_completed_run_is_skipped_on_resume(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            manifest_path = self.write_manifest(root, manifest_for([sys.executable, "-c", "pass"]))
            state_dir = root / "state"
            first = runner.run_campaign(manifest_path, state_dir, False, False, False, False)
            second = runner.run_campaign(manifest_path, state_dir, True, False, False, False)
            self.assertEqual((first["started"], first["completed"], first["failed"]), (1, 1, 0))
            self.assertEqual((second["started"], second["skipped"]), (0, 1))
            state = json.loads((state_dir / "campaign-state.json").read_text(encoding="utf-8"))
            record = next(iter(state["runs"].values()))
            self.assertEqual(record["status"], "COMPLETED")

    def test_failed_run_retries_without_overwriting_first_attempt(self):
        command = [sys.executable, "-c", "import os,sys;sys.exit(0 if os.environ['V35_CAMPAIGN_ATTEMPT']=='2' else 9)"]
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            manifest_path = self.write_manifest(root, manifest_for(command, max_attempts=2))
            state_dir = root / "state"
            first = runner.run_campaign(manifest_path, state_dir, False, False, False, False)
            second = runner.run_campaign(manifest_path, state_dir, True, True, False, False)
            self.assertEqual((first["failed"], second["completed"]), (1, 1))
            run_dir = next((state_dir / "runs").iterdir())
            self.assertTrue((run_dir / "attempt-0001" / "stderr.log").exists())
            self.assertTrue((run_dir / "attempt-0002" / "stdout.log").exists())
            self.assertTrue((run_dir / "failed-attempt-0001.json").exists())
            self.assertTrue((run_dir / "completed.json").exists())

    def test_formal_run_is_refused_without_explicit_switch(self):
        manifest = manifest_for([sys.executable, "-c", "pass"])
        manifest["runs"][0]["safetyClass"] = "formal"
        with self.assertRaisesRegex(runner.CampaignError, "--allow-formal"):
            runner.validate_manifest(manifest)


if __name__ == "__main__":
    unittest.main()
