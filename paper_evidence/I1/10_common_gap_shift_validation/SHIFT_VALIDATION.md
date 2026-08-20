# P8.6 I1/X0 Common-Gap Shift Validation

- semantic version: `fatigue-shift-v2-common-gap`
- mode: `LEFT_RIGHT`
- seed: `20260808`
- FCLS: 1 accepted / 24 evaluated candidates
- FCRS: 3 accepted / 74 evaluated candidates
- action trace: `2d1348dcbeb47b610f2bc1b547708305cb993d525b6274737c8fdea6b21c7707`
- final schedule: `dc9f5e55e3ce74fb1bfc8565bdbcd0efa9fc789c68a177a4ef2654845127cb6a`
- evaluation trace: `ef90f413e1282e2c3243bc897728f0d3ee87155f3f763ba745d8ef9cb2632fe8`
- external FE: S0=1, LEFT_RIGHT=1; internal propagation adds no FE

## Honest acceptance result

I1/X0 contains accepted FCLS and FCRS events.

FCLS uses the earliest feasible machine-worker common gap and only requires Cmax not to worsen. FCRS preserves the frozen post-left Cmax and requires TEC or TWC gain.
