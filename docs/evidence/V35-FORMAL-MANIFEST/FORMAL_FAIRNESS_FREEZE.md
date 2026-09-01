# V35 Formal Manifest and Fairness Freeze

```text
FORMAL_MANIFEST_FREEZE=ACCEPTED
schema=v35-formal-initial-population-v1
instances=45
seeds=20
initialPopulationSnapshots=900
populationSize=100
formalRunsCovered=4500 (A0-A4 x 45 x 20; no run started)
evaluationsPerformedDuringFreeze=0
```

## Frozen semantics

FM3; `DEGENERATE_SINGLE_FAMILY`; `SEQUENCE_INDEPENDENT`; `ShiftMode=NONE`; `GLOBAL_ORIGINAL`; `CA-TA-Lite -> inherited LS`; A4-Pacing; dual-Q P=5/G=5; Qg/Qp/DSCR/CFVF/PA_i enabled only according to the legal A0-A4 rung; `rho=0`; directional teacher pool disabled; population 100; `MaxFEs=500000`; mixture 20/40/20/20.

## Fairness contract

For every `(instanceId, seed)`, each A0-A4 arm must invoke `readSnapshot(...)` on the single listed `.fourvec` file, deep-copy the returned population for its private JVM, and record both logical hashes.  Calling `problem.createSolution()` in an arm is forbidden. The bridge validates all input SHA-256 values, semantic mode, vector lengths, JS permutation, factory/machine/worker domains, and both V35/P8 logical population hashes before returning.

## Inputs and manifests

- instance manifest SHA-256: `896f97e847226a13a3be4a1ca2e201b7cf2649d48378837dfabbdc3b21ef5227`
- formal seed list SHA-256: `0ed1024fe26728f4795575e87151a2ca3989e9ca10cc68547a95ed5077271c5c`
- initial-population manifest SHA-256: `0c72141da1f1c52e37d0124a05d6f2c6279e35d2e44d703b0ab2d07042e4a1ea`
- one physical snapshot per instance/seed: `initial-populations/<instance>/seed-<seed>.fourvec`
- materialization/verification use zero decoder evaluations and do not start a 500k run.

## Matrix accounting

The unique 45-instance matrix is `jobs={20,50,100,150,200}` x `stages={2,5,8}` x `factories={3,4,5}`, each with problem id 1.  The 20 seeds are the contiguous pre-registered range 20260808--20260827.  The manifest covers 45 x 20 = 900 shared starts, or 4,500 possible A0--A4 formal arms; this is a provenance/fairness freeze, not authorization to run them.
