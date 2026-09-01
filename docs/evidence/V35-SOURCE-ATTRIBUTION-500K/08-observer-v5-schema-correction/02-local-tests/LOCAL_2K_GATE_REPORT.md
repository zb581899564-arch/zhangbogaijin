# V5 local 2k gate

Both OFF and ON runs completed in independent JVMs with the same frozen snapshot. Because 2k is shorter than one 5k atomic Q phase, both runs legitimately stopped at 100 initial evaluations.

```ini
schemaSelfTest=PASSED
compiledClasses=44
javaMajor52Failures=0
offActualFE=100
onActualFE=100
byteEqualBehaviorFiles=14
sourceLedgerRows=100
lifecycleRows=100
b0StrictNdSize=11
b0IndependentRecalculation=PASSED
local2kGate=PASSED
```

The 14 byte-equal files include the final front, passive archive, Cmax audit outputs, CA-TA/DSCR/bottleneck event outputs, initial population hash, profile hash, budget termination, and PDDR observation. Measurement and observer-only files are not part of behavioral equivalence.

Verification is reproducible with `verify_local_2k.py`.
