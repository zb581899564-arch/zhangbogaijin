# V35 Stage2 Remote Throughput Benchmark

This directory contains evidence for the production-capacity diagnostic only.
It does not start a formal matrix and it does not report algorithm quality.

The benchmark runs the same frozen jar and A4 diagnostic workload at 4, 8, 12,
and 16 independent JVMs.  Each JVM is pinned to one non-overlapping logical
CPU and runs 20,000 successful full evaluations.  The remote scheduler records
per-run FE, wall time, CPU time, maximum RSS, GC summary, failure state, and
the jar SHA-256.

`FORMAL_MAX_PARALLEL` is accepted only if:

1. the deployed jar hash equals the Track A final freeze hash;
2. every run at the selected level satisfies phase-consistent FE/decoder closure
   (`actualFE=decoderCalls<=MaxFEs` and `remainingFE<qPhaseFE`);
3. no JVM exits non-zero or reports non-finite/illegal/duplicate evaluations;
4. no host pressure signal is observed (OOM, swap use, host memory exhaustion,
   or sustained CPU oversubscription beyond the assigned logical cores).

Otherwise the evidence retains the failed level and reports the greatest safe
lower level, or `NOT_RECOMMENDED`.

The remote scheduler uses `v35-phase-consistent-budget-v1`. Its 20k workloads
are throughput diagnostics, so they need not have the greater-than-99% utilization
property reserved for 500k formal runs. They must still pass the phase-bound gate.

## 2026-08-23 acceptance

All four levels completed with no failed run: 4/4, 8/8, 12/12 and 16/16.  The
highest actually tested safe level is therefore `FORMAL_MAX_PARALLEL=16`; no
claim is made for higher levels. Full Gate3/throughput values, host-pressure
snapshot and the remote evidence-manifest hash are in
`../V35-PHASE-BUDGET-PROTOCOL/05-remote-throughput/REMOTE_GATE3_THROUGHPUT_ACCEPTANCE.md`.
This capacity acceptance does not itself launch a formal matrix.
