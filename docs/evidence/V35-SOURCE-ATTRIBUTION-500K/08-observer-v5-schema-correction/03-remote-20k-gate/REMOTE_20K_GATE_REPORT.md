# V5 remote 20k OFF/ON gate

## Execution

```ini
host=aic-inspur-home
remoteDir=/home/inspur/aicomp/zhangbo-v35-source-attribution-observer-v5-gate-20260901
instance=100_5_3_1
seed=20260901
profile=C0_BETA_MAX_065
requestedMaxFE=20000
offActualFE=15258
onActualFE=15258
termination=PHASE_CONSISTENT_TAIL_STOP
```

Both independent JVMs completed with `failures=NONE`. The input upload manifest was verified on the training machine before execution.

## Behavioral equivalence and schema completeness

```ini
byteEqualBehaviorFiles=14
maskedEquivalentFiles=2
sourceLedgerRows=15258
lifecycleRows=72686
lifecycleEventTypes=10
observerExecutionErrors=0
unsetSourceRows=0
pddrRounds=2
b0StrictNdSize=11
b0IndependentRecalculation=PASSED
runManifestOFF=22/22
runManifestON=35/35
remote20kGate=PASSED
```

The lifecycle ledger contains all required event types: `GENERATED`, `DESCENDANT`, `IMPROVING_DESCENDANT`, `MERGE_POOL`, `PDDR_SELECTED`, `WORKING_POPULATION`, `PERSONAL_ARCHIVE`, `QG_TEACHER`, `QP_TEACHER`, and `QP_ACTION`.

`configuration.txt` and `status.properties` are also equivalent after masking only observer provenance and wall-clock/decoder timing measurements. All algorithm event counts, event hashes, Q-table hashes, FE values, and final sets remain equal.

The source ledger contains exactly one row per successful decoder call, all rows carry the frozen 25k nominal window, and no source is `UNSET`. Verification is reproducible with `verify_remote_20k.py`.

The remote directory is independent and remains intact. No 500k run was started.
