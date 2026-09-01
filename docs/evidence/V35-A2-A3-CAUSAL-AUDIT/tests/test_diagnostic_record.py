import importlib.util
import json
import pathlib
import unittest


MODULE_PATH = pathlib.Path(__file__).parents[1] / "diagnostic_record.py"
SPEC = importlib.util.spec_from_file_location("audit_diagnostic_record", MODULE_PATH)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


SELECT = (
    "event=1,group=G1,type=select,lineage=11,state=3,E=3,H=0,R=1,"
    "mask=1111,action=KEEP,epsilon=0.1,rho=0.2,rhoThreshold=0.2,pbest=abc,"
    "selectionMode=GREEDY_FROZEN"
)
REWARD = (
    "event=2,group=G1,type=reward,lineage=11,action=KEEP,dom=0.2,"
    "direction=0.4,archive=1.0,risk=-0.1,total=0.65,nextState=3,"
    "nextMask=1111,archiveSurvived=true"
)
UPDATE = "event=3,group=G1,type=update,state=3,action=0,alpha=0.3,count=1,target=0.65"


class DiagnosticRecordTests(unittest.TestCase):
    def test_parse_and_summary_are_count_only(self):
        events = MODULE.parse_event_stream("\n".join((SELECT, REWARD, UPDATE)))
        self.assertEqual(3, len(events))
        summary = MODULE.summarize_events(events)
        self.assertEqual(3, summary["eventCount"])
        self.assertEqual(1, summary["eventTypes"]["select"])
        self.assertEqual(1, summary["eventTypes"]["reward"])
        self.assertEqual(1, summary["archiveSurvivedCount"])
        self.assertAlmostEqual(0.65, summary["rewardTotalMean"])

    def test_missing_observation_field_fails_closed(self):
        with self.assertRaises(MODULE.DiagnosticRecordError):
            MODULE.parse_event_line("event=1,group=G1,type=reward,action=KEEP,total=0")

    def test_unknown_event_type_fails_closed(self):
        with self.assertRaises(MODULE.DiagnosticRecordError):
            MODULE.parse_event_line("event=1,group=G1,type=not-a-real-event")

    def test_roundtrip_does_not_mutate_event_values(self):
        events = MODULE.parse_event_stream("\n".join((SELECT, REWARD)))
        before = json.dumps(events, sort_keys=True)
        jsonl = MODULE.to_jsonl(events)
        restored = [json.loads(line) for line in jsonl.splitlines()]
        self.assertEqual(events, restored)
        self.assertEqual(before, json.dumps(events, sort_keys=True))


if __name__ == "__main__":
    unittest.main()
