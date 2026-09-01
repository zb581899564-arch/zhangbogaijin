# Observer V5 final engineering decision

V5 passes the schema correction gate, local 2k gate, remote 20k OFF/ON behavioral-equivalence gate, run-manifest checks, strict B0 reconstruction, lifecycle completeness, and decomposed memory gate.

```ini
v4FailureReplayAccepted=true
v4SourceAttributionSchemaCompliant=false
v5ObserverImplemented=true
v5ObserverBehavioralEquivalent=true
v5ObserverSchemaCompliant=true
v5MemoryGatePassed=true
v5ObserverJarFrozen=true
sourceAttribution500kEligible=true
correctedSaHard500kStarted=false
saNormalStarted=false
sourceAttributionRootCauseEstablished=false
formalMatrixRunning=false
formalJarChanged=false
```

The next scientific action, if separately approved, is one corrected V5 SA-HARD 500k run using the already frozen HARD case. Its first gate is deterministic failure-class reproduction. SA-NORMAL remains blocked until that corrected HARD run and its source-attribution evidence are accepted.

This engineering decision does not establish that CFVF, Qp, CA-TA, PDDR, or any other module is a root cause.
