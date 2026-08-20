# P8.4 I1/X0 Shift Validation

- semantic version: `fatigue-shift-v1`
- mode: `LEFT_RIGHT`
- seed: `20260808`
- FCLS: 0 accepted / 14 evaluated candidates
- FCRS: 4 accepted / 44 evaluated candidates
- action trace: `b8bcae18d49c14fe94d38cf530032cb7719b97171c06977566fdc1f9d0a0b95e`
- final schedule: `fed1257f003480ca53f279bd521fdb7615c4af0bd5bf583bcbdf23e531e75b4c`
- evaluation trace: `1b9972b32e252ed5831fc662744a10816f0c089a92f96949155cf58b9cdb30dd`
- external FE: S0=1, LEFT_RIGHT=1; internal propagation adds no FE

## Honest acceptance result

`I1/X0` does **not** pass the two-direction illustration gate: FCLS has no Pareto-safe accepted event. Figure 13 therefore truthfully shows `S1=S0`; no left-shift example is fabricated. P8.4 remains `in_progress` until the user decides whether a second fixed I1 particle may be used for the FCLS illustration.

The synthetic regression fixture separately proves that FCLS can accept a genuine Pareto-safe left move. The I1 result is a property of X0 under the locked acceptance rule.
