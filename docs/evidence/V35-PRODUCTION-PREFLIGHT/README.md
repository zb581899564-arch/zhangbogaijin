# V35 Stage2 Production Preflight

## Scope

This directory is the sole local evidence location for Stage2 Track C.  It is
a diagnostic gate, not a formal experiment and not an ablation result set.

The diagnostic runner uses the frozen `V35FinalAblationProfile` without
changing algorithm source, scientific parameters, local-FE pacing, Q-times or
search order.  It runs one arm per JVM with:

```text
population = 100
instance = 20_2_3_1
diagnostic seed = 20260828
requestedFE = 50000 (only 100000 is permitted as a documented fallback)
```

The seed is intentionally outside the formal statistical roster.  The instance
is used only as a workload representative; no performance comparison, PFref,
or paper conclusion is generated here.

## Required gates per A0--A4 arm

```text
status = COMPLETED
requestedFE = actualFE
decoderCalls = actualFE
illegalSolutions = 0
duplicateEvaluations = 0
nonFiniteObjectives = 0
```

The driver also asserts the expected enabled/disabled mechanism events for the
selected A0--A4 profile, verifies `ShiftMode=NONE`, `GLOBAL_ORIGINAL`, and a
disabled directional teacher pool.  Any failure is retained as evidence and
stops the remote campaign; it must not be repaired by changing algorithm
semantics.

## Freeze binding

`PREFLIGHT_ACCEPTED` may only be written after the jar SHA-256 equals Track A's
final freeze manifest.  A candidate jar may be used only for tooling checks and
is labelled `CANDIDATE_JAR_NOT_PRODUCTION_EVIDENCE`.

## Contents

- `tools/V35ProductionPreflight.java`: external, Java-8-compatible diagnostic
  entry point.  It invokes public project APIs only; it is not project source.
- `tools/run-stage2-remote.sh`: one-SSH remote scheduler template.  It runs
  preflight serially and the 4/8/12/16 A4 throughput levels locally on the
  training host.
- `runs/`: created only by a successful run or a retained failed attempt.
- `PRELIGHT_REPORT.md`: created after the final jar is bound and results are
  independently checked.
