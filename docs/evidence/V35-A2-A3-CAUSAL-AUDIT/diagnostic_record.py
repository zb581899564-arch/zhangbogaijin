"""Pure-observation parser for the Qp event stream already exposed by Java.

This module does not run the optimizer and does not alter a solution.  It is
kept in the audit directory so a future runner export can be checked before
any 50k/remote execution is considered.
"""

from __future__ import annotations

import csv
import io
import json
from collections import Counter
from typing import Dict, Iterable, List, Mapping, Sequence


EVENT_TYPES = {"select", "reward", "observeFrozen", "update", "softUpdate", "lineage", "random"}
REQUIRED_COMMON = ("event", "group", "type")
REQUIRED_BY_TYPE = {
    "select": ("lineage", "state", "mask", "action", "pbest"),
    "reward": ("lineage", "action", "dom", "direction", "archive", "risk", "total", "archiveSurvived"),
    "observeFrozen": ("lineage", "action", "nextState", "nextMask", "archiveSurvived"),
    "update": ("state", "action"),
    "softUpdate": ("state", "action"),
    "lineage": ("lineage", "requested", "resolved"),
    "random": ("lineage", "draw"),
}


class DiagnosticRecordError(ValueError):
    """Raised when a purported Qp observation is incomplete or malformed."""


def _parse_key_values(text: str) -> Dict[str, str]:
    values: Dict[str, str] = {}
    for token in text.strip().split(","):
        if not token:
            continue
        if "=" not in token:
            raise DiagnosticRecordError(f"token has no '=': {token!r}")
        key, value = token.split("=", 1)
        key = key.strip()
        if not key:
            raise DiagnosticRecordError("empty event key")
        values[key] = value.strip()
    return values


def parse_event_line(line: str) -> Dict[str, str]:
    """Parse one canonical ``ZhangBoQpController`` event line."""
    values = _parse_key_values(line)
    missing = [key for key in REQUIRED_COMMON if not values.get(key)]
    if missing:
        raise DiagnosticRecordError(f"missing common fields: {','.join(missing)}")
    event_type = values["type"]
    if event_type not in EVENT_TYPES:
        raise DiagnosticRecordError(f"unknown event type: {event_type}")
    missing = [key for key in REQUIRED_BY_TYPE.get(event_type, ()) if not values.get(key)]
    if missing:
        raise DiagnosticRecordError(f"missing {event_type} fields: {','.join(missing)}")
    return values


def parse_event_stream(text: str) -> List[Dict[str, str]]:
    """Parse non-empty event lines, retaining source order."""
    events: List[Dict[str, str]] = []
    for line_number, line in enumerate(text.splitlines(), start=1):
        if not line.strip():
            continue
        try:
            events.append(parse_event_line(line))
        except DiagnosticRecordError as exc:
            raise DiagnosticRecordError(f"line {line_number}: {exc}") from exc
    return events


def summarize_events(events: Sequence[Mapping[str, str]]) -> Dict[str, object]:
    """Return deterministic, count-only diagnostics for a parsed event stream."""
    types = Counter(event["type"] for event in events)
    actions = Counter(event.get("action", "") for event in events if event.get("action"))
    groups = Counter(event["group"] for event in events)
    rewards = [float(event["total"]) for event in events if event["type"] == "reward"]
    archive_survived = [
        event["archiveSurvived"].lower() == "true"
        for event in events
        if "archiveSurvived" in event
    ]
    return {
        "eventCount": len(events),
        "eventTypes": dict(sorted(types.items())),
        "actions": dict(sorted(actions.items())),
        "groups": dict(sorted(groups.items())),
        "rewardCount": len(rewards),
        "rewardTotalMean": sum(rewards) / len(rewards) if rewards else None,
        "archiveSurvivedCount": sum(archive_survived),
        "archiveObservedCount": len(archive_survived),
    }


def to_jsonl(events: Iterable[Mapping[str, str]]) -> str:
    """Serialize events without changing key/value semantics."""
    return "".join(json.dumps(dict(event), ensure_ascii=False, sort_keys=True) + "\n" for event in events)


def to_csv(events: Sequence[Mapping[str, str]]) -> str:
    """Serialize a rectangular diagnostic view suitable for local audit."""
    keys = sorted({key for event in events for key in event})
    output = io.StringIO()
    writer = csv.DictWriter(output, fieldnames=keys, extrasaction="ignore")
    writer.writeheader()
    writer.writerows(event for event in events)
    return output.getvalue()
