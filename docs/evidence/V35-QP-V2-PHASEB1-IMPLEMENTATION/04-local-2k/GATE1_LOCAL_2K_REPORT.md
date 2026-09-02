# V35 Phase B1: Gate 1 Local 2k Verification Report

- **Date**: 2026-09-02
- **Instance**: `20_2_3_1`
- **Seed**: `20260822`
- **Max Evaluations (Requested)**: 2,000
- **Budget Protocol**: Phase-Consistent Budget Termination (`0 < actualFE = decoderCalls <= MaxFEs`, `0 <= remainingFE < 5000`)
- **Status**: **PASSED**
- **Experimental JAR SHA-256**: `B0799FCA46B9DCA4512A20F9784BB7A3328D9D669B77030F7DC647E396836DD3`
- **Formal Algorithm JAR SHA-256**: `8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9` (Frozen & Untouched)

---

## 1. Multi-Arm Execution Summary (2k FE)

Under phase-consistent budget termination, initial population evaluation consumes 100 FEs, and because remaining FE ($1900$) is less than the minimum Q-phase block ($5000$), the algorithm safely terminates at `actualFE = 100` before Q-phase begins.

| Profile | Profile Description | Requested FE | Actual FE | Front Size | Front CSV SHA-256 | Behavioral Equivalence |
|---|---|---|---|---|---|---|
| `REF_A4` | Formal A4 Baseline | 2000 | 100 | 37 | `B16A070CA939B7534AB863CB0172E7C07CD8519D351B8AD04E545E0848422072` | Reference |
| `QP_V2_K1` | Qp-v2 $K=1$ Top-1 Pool | 2000 | 100 | 37 | `B16A070CA939B7534AB863CB0172E7C07CD8519D351B8AD04E545E0848422072` | **BYTE_IDENTICAL** |
| `QP_V2_K2` | Qp-v2 $K=2$ Uniform | 2000 | 100 | 37 | `B16A070CA939B7534AB863CB0172E7C07CD8519D351B8AD04E545E0848422072` | **BYTE_IDENTICAL** |
| `QP_V2_K3` | Qp-v2 $K=3$ Uniform | 2000 | 100 | 37 | `B16A070CA939B7534AB863CB0172E7C07CD8519D351B8AD04E545E0848422072` | **BYTE_IDENTICAL** |
| `QP_V2_K4` | Qp-v2 $K=4$ Uniform | 2000 | 100 | 37 | `B16A070CA939B7534AB863CB0172E7C07CD8519D351B8AD04E545E0848422072` | **BYTE_IDENTICAL** |

---

## 2. Invariant Audit Findings
1. **$K=1$ Strict Equivalence**: `QP_V2_K1` produced byte-identical `front.csv` and summary metrics identical to `REF_A4`.
2. **Initial Search Phase Invariant**: Pre-Q-phase behavior is completely invariant across all profiles.
3. **Budget Safety**: No over-evaluation, no fractional Q-phase invocations.
