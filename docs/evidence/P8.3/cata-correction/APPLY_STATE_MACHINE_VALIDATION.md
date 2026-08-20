# CA-TA Apply state-machine validation

The pre-fix regression first failed with two observable defects:

- an Apply decision requested `K*nTest*multiplier` repetitions on the same parent;
- raw wall-clock ordering selected a different operator from the v2 median-normalized cost definition.

After correction:

- Test evaluates every valid neighborhood exactly `nTest` times;
- the controller stores `remainingApplyCalls=K*nTest*applyMultiplier` across later parent calls;
- each Apply invocation evaluates exactly one candidate and decrements the remaining count once;
- exhausted horizons, changed masks, and three consecutive failures begin a new Test epoch;
- deterministic request keys include master seed, outer generation, parent slot, lineage, context epoch, call ordinal, and neighborhood;
- timing includes preview, candidate construction, and the complete evaluation;
- cost is `0.5*(averageTime/medianAverageTime)+0.5*(averageFE/medianAverageFE)`.

Verification command:

```text
mvn -q -pl jmetal-algorithm -am -Djacoco.skip=true -DfailIfNoTests=false -Dmaven.javadoc.skip=true -Dtest=ZhangBoCaTaControllerTest,ZhangBoCaTaComponentsTest,ZhangBoCaTaIntegrationSmokeTest test
```

Result: `10 tests, 0 failures, 0 errors`.

The ordinary 2000-FE smoke entered CA-TA and reported non-zero Test and Apply activity while respecting the budget. The fixed-initial-population replay used an injected deterministic monotonic clock and reproduced CA-TA events plus Qg/Qp table hashes byte-for-byte.
