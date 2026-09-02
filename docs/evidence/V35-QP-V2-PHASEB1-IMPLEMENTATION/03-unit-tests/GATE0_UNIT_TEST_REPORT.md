# V35 Phase B1: Gate 0 Unit Test Suite Report

- **Date**: 2026-09-02
- **Candidate Evaluated**: Qp-v2 Candidate A (`CANDIDATE_A_TOPK_UNIFORM`)
- **Status**: **PASSED (14/14 tests)**
- **Java Compatibility**: JDK 1.8 (Class Major Version 52)
- **Experimental JAR**: `docs/evidence/V35-QP-V2-PHASEB1-IMPLEMENTATION/02-implementation/jmetal-algorithm-5.8-V35-QP-V2-PHASEB1.jar`
- **Experimental JAR SHA-256**: `B0799FCA46B9DCA4512A20F9784BB7A3328D9D669B77030F7DC647E396836DD3`
- **Formal Algorithm JAR SHA-256**: `8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9` (100% untouched)

---

## 1. Test Suite Results Summary

| Test # | Invariant Tested | Expected Outcome | Observed Outcome | Status |
|---|---|---|---|---|
| **Test 1** | $K$ value range validation | Accepts $K \in \{1,2,3,4\}$, rejects all others | Validated 1..4 OK; rejected $\le 0$ and $\ge 5$ | **PASS** |
| **Test 2** | Action pool ordering comparators | Sorted by action-specific metrics; tie-break on fingerprint ascending | Exact ordering verified for DIRECTIONAL, EPSILON, COMPLEMENTARY | **PASS** |
| **Test 3** | Pool truncation when entries $< K$ | No duplicate padding; bounded by actual entries | 2 entries with $K=4$ yielded pool size 2 | **PASS** |
| **Test 4** | KEEP pool singleton invariant | Always singleton containing canonical KEEP | Size 1 across all $K \in \{1,2,3,4\}$ | **PASS** |
| **Test 5** | COMPLEMENTARY valid count preserved | No fabrication if social norm is 0 | Empty pool preserved when social norm = 0 | **PASS** |
| **Test 6** | $K=1$ strict canonical equivalence | Pool[0] strictly equals canonical A4 candidate | Byte/FP identical for all valid actions | **PASS** |
| **Test 7** | $K=1$ zero RNG draws | 0 extra RNG draws when selecting from size 1 pool | Exactly 0 RNG draws confirmed | **PASS** |
| **Test 8** | $K=2..4$ exactly one RNG draw when pool $\ge 2$ | Exactly 1 draw via `JavaRandomGenerator` | Exactly 1 RNG draw confirmed | **PASS** |
| **Test 9** | Action mask invariant across $K$ | Mask 100% determined by canonical A4 | Identical boolean mask across $K=1,2,3,4$ | **PASS** |
| **Test 10** | Invariant frozen mechanisms preserved | PA capacity $L=6$, Dual-Q $P=5/G=5$, $\rho=0$ | All parameters identical to baseline | **PASS** |
| **Test 11** | Canonical text and hash uniqueness | Distinct text and hash for each $K \in \{1,2,3,4\}$ | 4 unique canonical texts verified | **PASS** |
| **Test 12** | Baseline profile rejects $K=2..4$ | Formal baseline profile forces $K=1$, disabled | Validated CANONICAL_A4 invariants | **PASS** |
| **Test 13** | Input order permutation stability | Shuffled PA entries produce identical pools | All permutations yielded identical pools | **PASS** |
| **Test 14** | Degenerate bounds and stability | Flat bounds tie-break on fingerprint deterministically | Deterministic lexicographical tie-break | **PASS** |

---

## 2. Conclusion
All 14 unit test cases passed with zero errors, confirming mathematical and engineering compliance of Candidate A top-$K$ candidate pool implementation before proceeding to algorithm execution.
