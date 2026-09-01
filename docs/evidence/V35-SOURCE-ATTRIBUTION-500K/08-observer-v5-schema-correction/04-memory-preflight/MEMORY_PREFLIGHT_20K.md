# V5 memory preflight

The decomposed memory model uses the measured OFF peak as the bounded algorithm baseline. It does not multiply the baseline by the 500k/20k budget ratio.

```ini
baselineAlgorithmPeak=1086804816
observerOnPeak=986673664
observerMeasuredDelta=0
observerBoundedResidentCap=2359296
observerUnflushedBufferCap=25600000
safetyMargin=268435456
estimated500kPeak=1383199568
assignedJavaHeap=4294967296
estimatedRatio=0.322051245719
memoryGateThreshold=0.60
memoryGatePassed=true
```

The bounded-resident estimate includes the ND samples, forensic reservoir, lineage map, four lifecycle registries, parent raw/objective caches, and the lifecycle writer buffer. Source, PDDR, and lifecycle ledgers are disk-streamed.

This is an engineering capacity gate, not a guarantee that every OS/JVM allocation will equal the point estimate. A future 500k run must continue to report measured heap, disk size, and termination evidence.
