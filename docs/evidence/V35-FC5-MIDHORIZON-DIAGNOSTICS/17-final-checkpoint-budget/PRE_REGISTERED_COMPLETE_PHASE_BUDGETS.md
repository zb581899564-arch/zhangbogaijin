# V35 mid-horizon diagnostic: pre-registered complete-phase budgets

Status: PRE-REGISTERED BEFORE FALLBACK EXECUTION

The diagnostic driver remains phase-consistent and keeps
`allowTerminalPartialFormalQPhase=false`. A fallback changes only the
diagnostic stopping cap; it does not enable a partial formal Q phase and does
not change any algorithm decision, parameter, or formal frozen-Jar setting.

## 2k gate

- Nominal gate: `2000 FE`
- Initial population: `100 FE`
- Complete Table-9 Q phase: `5000 FE`
- Registered effective cap: `5100 FE`
- Checkpoint schedule: nominal final point `2000 FE`
- Reason: a nominal 2000-FE run cannot finish the first complete Q phase. The
  effective cap is exactly `100 + 5000`, so the fallback observes one complete
  phase and never accepts a partial phase.

## 20k gate

- Nominal gate: `20000 FE`
- Previously observed A4 last complete boundary: `actualFE=15258`
- Remaining FE at the nominal cap: `4742 FE`
- Complete Table-9 Q phase: `5000 FE`
- Registered effective cap: `20258 FE` (`15258 + 5000`)
- Checkpoint schedule: `5000,10000,15000,20000 FE`
- Reason: the nominal A4 run stopped before the next complete Q phase because
  `4742 < 5000`. The effective cap admits exactly one more complete phase;
  the final nominal 20k checkpoint is still evaluated only at an atomic
  boundary and must satisfy the `<5000 FE` observation window.

## Common restrictions

- `formalBudgetSemantics=PHASE_CONSISTENT_BUDGET_TERMINATION`
- `allowTerminalPartialFormalQPhase=false`
- No partial phase, synthetic checkpoint, pool-ordinal proxy, or post-hoc
  acceptance is allowed.
- The nominal gate identity remains part of the evidence run ID and report;
  the effective cap is explicitly recorded as a registered diagnostic
  fallback.

