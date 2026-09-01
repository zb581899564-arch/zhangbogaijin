import csv
import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
import v35_analysis as analysis


class V35AnalysisMathTest(unittest.TestCase):
    def test_strict_pareto_deduplicates_and_preserves_tradeoffs(self):
        front = analysis.nondominated([(1, 1, 1), (2, 2, 2), (1, 1, 1), (0, 3, 3)])
        self.assertEqual([(0.0, 3.0, 3.0), (1.0, 1.0, 1.0)], front)

    def test_p8_identity_convention(self):
        front = [(1, 3, 2), (2, 1, 3), (3, 2, 1)]
        minimum, maximum = analysis.bounds(front)
        values = analysis.metrics(front, front, minimum, maximum)
        self.assertAlmostEqual(0.0, values["igd"], places=12)
        self.assertAlmostEqual(1.0, values["c_forward"], places=12)
        self.assertAlmostEqual(1.0, values["c_reverse"], places=12)
        self.assertGreater(values["hv"], 0.0)

    def test_manual_one_point_hypervolume_is_1331(self):
        # Hand check: normalized (0,0,0) owns 1.1^3 of the reference box.
        front = [(5, 5, 5)]
        minimum, maximum = analysis.bounds(front)
        values = analysis.metrics(front, front, minimum, maximum)
        self.assertAlmostEqual(1.331, values["hv"], places=12)
        self.assertAlmostEqual(0.0, values["spacing"], places=12)

    def test_exact_wilcoxon_and_paired_effect_positive_is_better(self):
        result = analysis.wilcoxon_signed_rank([1.0, 2.0, 3.0, 4.0])
        effect = analysis.paired_effect([1.0, 2.0, 3.0, 4.0])
        self.assertEqual(4, result["n"])
        self.assertEqual(10.0, result["w_plus"])
        self.assertAlmostEqual(0.125, result["p_value"], places=12)
        self.assertEqual(4, effect["wins"])
        self.assertAlmostEqual(1.0, effect["a12"], places=12)
        self.assertAlmostEqual(1.0, effect["cliffs_delta"], places=12)


class V35AnalysisPipelineTest(unittest.TestCase):
    def _metadata_row(self, run_id, algorithm, seed, front):
        return {
            "run_id": run_id, "algorithm": algorithm, "instance": "tiny_2_2_1", "seed": seed,
            "status": "COMPLETED", "config_hash": algorithm + "-CONFIG", "budget": "2000",
            "initial_population_hash": "INIT-" + seed, "instance_sha256": "INSTANCE-HASH",
            "instance_extension_sha256": "EXT-HASH", "fatigue_manifest_sha256": "FATIGUE-HASH",
            "decoder_mode": "FM3", "family_mode": "DEGENERATE_SINGLE_FAMILY",
            "setup_mode": "SEQUENCE_INDEPENDENT", "shift_mode": "NONE", "objectives": "0|1|6",
            "front_path": front, "wall_clock_ms": "100", "cpu_time_ms": "90",
        }

    def _write_front(self, directory, name, points):
        path = directory / name
        with path.open("w", newline="", encoding="utf-8") as handle:
            writer = csv.writer(handle)
            writer.writerow(["Cmax", "TEC", "TWC"])
            writer.writerows(points)
        return path.name

    def test_raw_front_pipeline_writes_metrics_and_no_conclusion(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            rows = [
                self._metadata_row("a-s1", "A0", "1", self._write_front(root, "a1.csv", [(1, 4, 4), (4, 1, 4)])),
                self._metadata_row("b-s1", "A4", "1", self._write_front(root, "b1.csv", [(1, 3, 3), (3, 1, 3)])),
                self._metadata_row("a-s2", "A0", "2", self._write_front(root, "a2.csv", [(2, 4, 4), (4, 2, 4)])),
                self._metadata_row("b-s2", "A4", "2", self._write_front(root, "b2.csv", [(2, 3, 3), (3, 2, 3)])),
            ]
            manifest = root / "manifest.csv"
            with manifest.open("w", newline="", encoding="utf-8") as handle:
                writer = csv.DictWriter(handle, fieldnames=analysis.REQUIRED_COLUMNS)
                writer.writeheader()
                writer.writerows(rows)
            output = root / "out"
            status = analysis.compute(manifest, output, ("A0", "A4"), "A0", formal=False)
            self.assertEqual("NON_FORMAL_DIAGNOSTIC_ONLY", status["analysis_kind"])
            self.assertFalse(status["automatic_conclusions_emitted"])
            with (output / "per-run-metrics.csv").open(encoding="utf-8") as handle:
                self.assertEqual(4, len(list(csv.DictReader(handle))))
            self.assertTrue((output / "reference-fronts" / "tiny_2_2_1.csv").is_file())
            self.assertTrue((output / "statistics-pairwise.csv").is_file())
            saved = json.loads((output / "analysis-status.json").read_text(encoding="utf-8"))
            self.assertFalse(saved["automatic_conclusions_emitted"])

    def test_rejects_mismatched_initial_population_hash(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            rows = [
                self._metadata_row("a-s1", "A0", "1", self._write_front(root, "a.csv", [(1, 2, 3)])),
                self._metadata_row("b-s1", "A4", "1", self._write_front(root, "b.csv", [(1, 2, 3)])),
            ]
            rows[1]["initial_population_hash"] = "WRONG"
            manifest = root / "manifest.csv"
            with manifest.open("w", newline="", encoding="utf-8") as handle:
                writer = csv.DictWriter(handle, fieldnames=analysis.REQUIRED_COLUMNS)
                writer.writeheader()
                writer.writerows(rows)
            with self.assertRaisesRegex(analysis.AnalysisError, "fairness metadata mismatch"):
                analysis.compute(manifest, root / "out", ("A0", "A4"), "A0", formal=False)

    def test_formal_mode_refuses_a_non_twenty_seed_matrix(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            rows = [
                self._metadata_row("a-s1", "A0", "1", self._write_front(root, "a.csv", [(1, 2, 3)])),
                self._metadata_row("b-s1", "A4", "1", self._write_front(root, "b.csv", [(1, 2, 3)])),
            ]
            manifest = root / "manifest.csv"
            with manifest.open("w", newline="", encoding="utf-8") as handle:
                writer = csv.DictWriter(handle, fieldnames=analysis.REQUIRED_COLUMNS)
                writer.writeheader()
                writer.writerows(rows)
            with self.assertRaisesRegex(analysis.AnalysisError, "exactly 20 completed paired seeds"):
                analysis.compute(manifest, root / "out", ("A0", "A4"), "A0", formal=True)


if __name__ == "__main__":
    unittest.main()
