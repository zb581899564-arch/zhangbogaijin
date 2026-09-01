# A0--A4 2000 FE semantic smoke checks

- version: `v35-final-a0-a4-2000fe-smoke-v1`
- instance: `20_2_3_1`; seed: `20260822`; population: `10`; maxFEs: `2000`
- common initial population SHA-256: `0c8ed9ab0c7500e3e33df659fe52f536b7f5af7a17726bd1749939220c51da01`

| Arm | Result | Notes |
|---|---|---|
| A0 | PASS | FE=2000; Qg=200; PDDR=1; localFE=1490; DSCR=0; CFVF=0; Qp/PA_i=0; CA-TA=0/0 |
| A1 | PASS | FE=2000; Qg=200; PDDR=1; localFE=1490; DSCR=200; CFVF=0; Qp/PA_i=0; CA-TA=0/0 |
| A2 | PASS | FE=2000; Qg=200; PDDR=1; localFE=1490; DSCR=200; CFVF=500; Qp/PA_i=0; CA-TA=0/0 |
| A3 | PASS | FE=2000; Qg=200; PDDR=1; localFE=1490; DSCR=200; CFVF=500; Qp/PA_i=10; CA-TA=0/0 |
| A4 | PASS | FE=1525; Qg=400; PDDR=2; localFE=475; DSCR=400; CFVF=1000; Qp/PA_i=20; CA-TA=25/15 |

All assertions are runtime gates.  PASS does not authorize any formal run or claim an empirical performance effect.
