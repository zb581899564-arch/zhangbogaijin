# V35 Phase B1: Gate 2 Remote 20k Engineering Gate Report

- **Date**: 2026-09-02
- **Execution Target**: Training Machine (`aic-inspur-home`, 125 GB RAM, OpenJDK 11.0.27)
- **Status**: **PASSED (10/10 runs completed with 0 errors)**
- **Budget Protocol**: Phase-Consistent Budget Termination (`0 < actualFE = decoderCalls <= MaxFEs`, `0 <= remainingFE < 5000`)
- **Experimental JAR SHA-256**: `B0799FCA46B9DCA4512A20F9784BB7A3328D9D669B77030F7DC647E396836DD3`
- **Formal Algorithm JAR SHA-256**: `8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9` (100% frozen & untouched)

---

## 1. Remote 20k Matrix Execution Results

The engineering gate executed 2 test instances $\times$ 5 arms = 10 runs under requested MaxFEs = 20,000 with full telemetry enabled.

| Instance | Seed | Profile | Actual FE | Front Size | Front CSV SHA-256 | Total Qp Selections | Pool $\ge 2$ Selections | Non-Canonical Selections | Extra RNG Draws | Equivalence / Trigger Status |
|---|---|---|---|---|---|---|---|---|---|---|
| `20_2_3_1` | `20260822` | `REF_A4` | 15,258 | 94 | `8B4500588BDFA77CD2E23F740BE8889D4530B86DFE4D368C9F7D0DF37B40BA46` | 8,100 | 0 | 0 | 0 | Formal Baseline Reference |
| `20_2_3_1` | `20260822` | `QP_V2_K1` | 15,258 | 94 | `8B4500588BDFA77CD2E23F740BE8889D4530B86DFE4D368C9F7D0DF37B40BA46` | 8,100 | 0 | 0 | 0 | **BYTE_IDENTICAL to REF_A4** |
| `20_2_3_1` | `20260822` | `QP_V2_K2` | 15,258 | 126 | `9FD97BE06736C548CFA8F3ECA4F8F0491FE38B42DFABAC8CD73DE6EEFFFB2D66` | 8,100 | 2,199 | 1,108 | 2,199 | **Mechanism Triggered** |
| `20_2_3_1` | `20260822` | `QP_V2_K3` | 15,258 | 126 | `9FD97BE06736C548CFA8F3ECA4F8F0491FE38B42DFABAC8CD73DE6EEFFFB2D66` | 8,100 | 2,199 | 1,108 | 2,199 | **Mechanism Triggered** |
| `20_2_3_1` | `20260822` | `QP_V2_K4` | 15,258 | 126 | `9FD97BE06736C548CFA8F3ECA4F8F0491FE38B42DFABAC8CD73DE6EEFFFB2D66` | 8,100 | 2,199 | 1,108 | 2,199 | **Mechanism Triggered** |
| `100_5_3_1` | `20260901` | `REF_A4` | 15,258 | 87 | `F2172A378BCB5FC51EC621C9DFABDC3CCEDE2A56A3871167B665FCEF5C67216A` | 8,100 | 0 | 0 | 0 | Formal Baseline Reference |
| `100_5_3_1` | `20260901` | `QP_V2_K1` | 15,258 | 87 | `F2172A378BCB5FC51EC621C9DFABDC3CCEDE2A56A3871167B665FCEF5C67216A` | 8,100 | 0 | 0 | 0 | **BYTE_IDENTICAL to REF_A4** |
| `100_5_3_1` | `20260901` | `QP_V2_K2` | 15,258 | 92 | `4F91BC1E2D0E4EDCA31677D472EEB9258D54476942E1A5288CEE6DDF2F8354EC` | 8,100 | 172 | 87 | 172 | **Mechanism Triggered** |
| `100_5_3_1` | `20260901` | `QP_V2_K3` | 15,258 | 92 | `4F91BC1E2D0E4EDCA31677D472EEB9258D54476942E1A5288CEE6DDF2F8354EC` | 8,100 | 172 | 87 | 172 | **Mechanism Triggered** |
| `100_5_3_1` | `20260901` | `QP_V2_K4` | 15,258 | 92 | `4F91BC1E2D0E4EDCA31677D472EEB9258D54476942E1A5288CEE6DDF2F8354EC` | 8,100 | 172 | 87 | 172 | **Mechanism Triggered** |

---

## 2. Key Scientific & Engineering Findings

1. **$K=1$ Strict Behavioral Equivalence**:
   - On both `20_2_3_1` / seed `20260822` and `100_5_3_1` / seed `20260901`, `QP_V2_K1` produced **100% byte-identical `front.csv`** compared to `REF_A4`.
   - `QP_V2_K1` telemetry recorded **exactly 0 extra RNG draws** and **0 non-canonical selections**, completely satisfying the $K=1$ equivalence gate.

2. **$K \ge 2$ Top-$K$ Mechanism Activation**:
   - On `20_2_3_1`, $K \ge 2$ triggered 2,199 pool selections where pool size $\ge 2$, resulting in 1,108 non-canonical exploratory leader choices (13.68% exploration rate) and exactly 2,199 extra RNG draws (1 draw per multi-candidate pool).
   - On `100_5_3_1`, $K \ge 2$ triggered 172 pool selections with pool size $\ge 2$, resulting in 87 non-canonical exploratory leader choices and exactly 172 extra RNG draws.
   - For 20k runs where PA entries passing action thresholds are $\le 2$, $K=2, 3, 4$ yield identical behavior because $\min(\text{availableEntries}, K) = 2$.

3. **Budget & Accounting Safety**:
   - All runs terminated at `actualFE = 15,258`, leaving `remainingFE = 4,742 < 5,000`, strictly conforming to Phase-Consistent Budget Termination rules.
   - No crashes, deadlocks, NaN values, or missing artifacts were observed.

4. **Conclusion**:
   - Gate 2 Remote 20k Engineering Gate is **PASSED**.
   - Qp-v2 Candidate A implementation is ready for Phase B2 evaluation upon task authorization.
