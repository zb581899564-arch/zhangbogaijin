# V35-GAP-LOCAL-FE-PACING-250K — Remote Execution Report

- Campaign: `V35-GAP-LOCAL-FE-PACING-250K` (18 runs = 3 seeds x 2 instances x 3 arms C0/C2/C3)
- Remote host: `aic-inspur-home` (inspur@inspur-NP5570M5), campaign dir `/home/inspur/aicomp/zhangbo-v35-local-fe-pacing-250k-20260831` (newly created, nothing overwritten)
- Execution date: 2026-08-31 (all times CST, +08:00)
- Local evidence dir: `16-remote-250k-runs/` (sync archive `v35-250k-sync.tar.gz`, extracted tree `sync/`, CSVs, this report, `evidence-sha256.tsv`)

## 1. Timeline

| Time | Event |
|---|---|
| 17:08 | SSH connectivity verified (whoami=inspur, host=inspur-NP5570M5) |
| 17:10–17:18 | `mkdir -p` remote campaign dir; `scp -r staging-250k/*` (139 MB, 37 payload files + manifest) |
| 17:19 | T1 SHA256 verification vs `upload-sha256.tsv`: **37/37 OK, 0 mismatch** (LANG=C `sha256sum -c`) |
| 17:21:42 | T2 read-only preflight (values below) |
| 17:21:59 | First `nohup bash run-all-250k.sh` launched (PID 4163458); RESOURCE_PRECHECK logged |
| 17:21:59 | seed-20260916 x 50_2_3_1 arms C0/C2/C3 **failed immediately (exit=1, no results dir, no .partial dir)** — see section 4 |
| 17:26:57–17:28:08 | seed-20260916 x 100_5_3_1 triplet completed exit=0 |
| 17:27:48–17:28:33 | Coordinator pushed regenerated binding files (all 6) + updated `upload-sha256.tsv` to remote (authorized repair, section 4) |
| 17:28:40 | T1 re-verification against updated manifest: **37/37 OK** |
| 17:28:08–17:41:35 | Remaining 15 runs of first invocation completed exit=0; first `ALL_18_RUNS_DONE 17:41:35` (15/18 dirs present) |
| 17:42:10 | Retry invocation #1 of `run-all-250k.sh` (retry 1 of max 2 per arm): 15 existing dirs auto-SKIP, 3 missing 20260916 50_2_3_1 arms re-run |
| 17:44:57–17:45:28 | 3 retry arms all exit=0; second `ALL_18_RUNS_DONE 17:45:28` |
| 17:46–17:48 | T4 acceptance: 18/18 runs PASS (checks 1–7), 6/6 fair groups PASS |
| 17:49–17:53 | T5: `tar -czf` of `seed-*/results/ + logs/` (31 MB), scp to local, sha256 verified end-to-end, extracted to `sync/` |

## 2. Pre-flight (T2, read-only, 17:21:42)

| Item | Value | Threshold | Verdict |
|---|---|---|---|
| java -version | OpenJDK 11.0.27 (Ubuntu 11.0.27+6-post-Ubuntu-0ubuntu120.04) | — | OK |
| nproc | 32 | — | OK |
| free -g | total 125 G, available 119 G | >= 40 G | OK |
| df -h /home/inspur | /dev/sda2 879G, 248G free (71% used) | — | OK |
| uptime load | 0.13, 0.08, 0.08 | <= 16 | OK |
| legacy java/jmetal processes | 0 | 0 | OK |

## 3. SHA256 verification (T1)

- Original upload: 37/37 files `OK` vs `upload-sha256.tsv` (paths relative to staging-250k root), 0 failures, 0 missing.
- After coordinator's authorized binding regeneration: re-run against updated manifest — **37/37 OK** (6 binding entries updated by coordinator; jar/instance/setup/fatigue/snapshot/script entries unchanged).
- Sync archive integrity: remote `/tmp/v35-250k-sync.tar.gz` sha256 `b6b1fb98c5cd9848a27085b9822ea7e6c250b41fdc428e8ee56d2aaf7cedcf1b` == local copy sha256 (verified after scp).

## 4. First-group failure, authorized repair, and retries

- **Failure**: all three arms (C0/C2/C3) of the first fair group (seed-20260916 x 50_2_3_1) aborted at startup with `IllegalStateException: binding.setupFileSha256 expected=f9bde51a5f873896291527676bcbfaf8291c72b91175a6828dd722eaa54df54 (63 chars) actual=f9bde51a5f873896291527676bcbfaf8291c72b91175a6828dd722eaa54df54e (64 chars)`.
- **Root cause** (confirmed remotely): the staged `50_2_3_1.binding.properties` for all 3 seeds contained a `setupFileSha256` truncated to 63 hex chars (preregistration CSV transcription truncation). The actual `50_2_3_1.setup.txt` file hash matched the preregistered `upload-sha256.tsv` exactly — the scientific input was correct; only the binding's expected-value field was malformed. `100_5_3_1` bindings were valid (64 chars).
- **Authorized repair** (user decision, AskUserQuestion answer "修复绑定并继续"): coordinator regenerated all 6 binding files from the true file hashes and pushed them directly to the remote campaign dir, also updating the remote `upload-sha256.tsv`. No scientific input file (instance/setup/fatigue/snapshot/jars) was modified. This deviation is recorded here as the evidence chain note.
- **Failed attempts preserved**: the 3 failed first attempts exited before any output directory was created (no `results/` dirs, no `.partial-*` dirs to preserve). Their evidence is retained in `sync/logs/run-all-250k.log` (`END ... exit=1` lines at 17:21:59) and in the arm logs (which were overwritten by the successful retry in the normal course of the script — the failure stack trace is quoted in section 4 above and was also captured in this report).
- **Retries**: retry invocation #1 at 17:42:10 (each affected arm retry count = 1 of max 2). All 3 arms completed exit=0. No further retries needed. No second scheduler was ever run concurrently; the retry was started only after the first invocation printed `ALL_18_RUNS_DONE`.
- **.partial-* inventory**: 5 transient in-flight `.partial-*` dirs existed during execution (seed-20260917 group, first invocation); all were renamed to final run dirs on completion. **0 `.partial-*` dirs remain** on remote. None were deleted manually.

## 5. Acceptance (T4) — 18/18 PASS

All values below read from run-directory files on the remote (formal-gate.properties / budget-termination.properties / status.properties / pddr-observation.properties / profile.txt / checkpoints/checkpoint-registry.csv / front.csv / configuration.txt / initial-population.sha256).

| Run | Arm | Inst | Seed | actualFE | terminationKind | outerCycles | checkpointRows | Acceptance |
|---|---|---|---|---|---|---|---|---|
| run-GAPL250K-C0-100_5_3_1-20260916 | C0 | 100_5_3_1 | 20260916 | 250000 | EXACT_MAX_FE | 31 | 4 | PASS |
| run-GAPL250K-C2-100_5_3_1-20260916 | C2 | 100_5_3_1 | 20260916 | 249003 | PHASE_CONSISTENT_TAIL_STOP | 34 | 4 | PASS |
| run-GAPL250K-C3-100_5_3_1-20260916 | C3 | 100_5_3_1 | 20260916 | 250000 | EXACT_MAX_FE | 36 | 4 | PASS |
| run-GAPL250K-C0-50_2_3_1-20260916 | C0 | 50_2_3_1 | 20260916 | 250000 | EXACT_MAX_FE | 31 | 4 | PASS |
| run-GAPL250K-C2-50_2_3_1-20260916 | C2 | 50_2_3_1 | 20260916 | 249003 | PHASE_CONSISTENT_TAIL_STOP | 34 | 4 | PASS |
| run-GAPL250K-C3-50_2_3_1-20260916 | C3 | 50_2_3_1 | 20260916 | 250000 | EXACT_MAX_FE | 36 | 4 | PASS |
| run-GAPL250K-C0-100_5_3_1-20260917 | C0 | 100_5_3_1 | 20260917 | 250000 | EXACT_MAX_FE | 31 | 4 | PASS |
| run-GAPL250K-C2-100_5_3_1-20260917 | C2 | 100_5_3_1 | 20260917 | 249003 | PHASE_CONSISTENT_TAIL_STOP | 34 | 4 | PASS |
| run-GAPL250K-C3-100_5_3_1-20260917 | C3 | 100_5_3_1 | 20260917 | 250000 | EXACT_MAX_FE | 36 | 4 | PASS |
| run-GAPL250K-C0-50_2_3_1-20260917 | C0 | 50_2_3_1 | 20260917 | 250000 | EXACT_MAX_FE | 31 | 4 | PASS |
| run-GAPL250K-C2-50_2_3_1-20260917 | C2 | 50_2_3_1 | 20260917 | 249003 | PHASE_CONSISTENT_TAIL_STOP | 34 | 4 | PASS |
| run-GAPL250K-C3-50_2_3_1-20260917 | C3 | 50_2_3_1 | 20260917 | 250000 | EXACT_MAX_FE | 36 | 4 | PASS |
| run-GAPL250K-C0-100_5_3_1-20260918 | C0 | 100_5_3_1 | 20260918 | 250000 | EXACT_MAX_FE | 31 | 4 | PASS |
| run-GAPL250K-C2-100_5_3_1-20260918 | C2 | 100_5_3_1 | 20260918 | 249003 | PHASE_CONSISTENT_TAIL_STOP | 34 | 4 | PASS |
| run-GAPL250K-C3-100_5_3_1-20260918 | C3 | 100_5_3_1 | 20260918 | 250000 | EXACT_MAX_FE | 36 | 4 | PASS |
| run-GAPL250K-C0-50_2_3_1-20260918 | C0 | 50_2_3_1 | 20260918 | 250000 | EXACT_MAX_FE | 31 | 4 | PASS |
| run-GAPL250K-C2-50_2_3_1-20260918 | C2 | 50_2_3_1 | 20260918 | 249003 | PHASE_CONSISTENT_TAIL_STOP | 34 | 4 | PASS |
| run-GAPL250K-C3-50_2_3_1-20260918 | C3 | 50_2_3_1 | 20260918 | 250000 | EXACT_MAX_FE | 36 | 4 | PASS |

Uniform per-run gate results (all 18 runs):

- formal-gate.properties: `status=COMPLETED`, `failures=NONE`, `observerExecutionErrors=0`, `checkpointRows=4`, `actualFE==decoderCalls`, 0 < actualFE <= 250000 — PASS 18/18
- budget-termination.properties: `phaseBoundAccepted=true`, remainingFE = 0 (C0/C3) or 997 (C2) < 5000, utilizationRate = 1.000000000000 (C0/C3) or 0.996012000000 (C2) > 0.98, terminationKind in {EXACT_MAX_FE, PHASE_CONSISTENT_TAIL_STOP} — PASS 18/18
- status.properties: `illegalSolutions=0`, `duplicateEvaluations=0`; mechanismSummary: `cfvfRepairs=0`, `directionalPoolRequests=0`, `directionalPoolFiltered=0`, `shadowSamples=0`, `shadowEvaluations=0`; decoderTiming: `leftShiftNanos=0`, `rightShiftNanos=0` — PASS 18/18
- front.csv non-empty (275–574 data rows per run, header excluded) — PASS 18/18
- pddr-observation.properties: `pddrSelectionMode=GLOBAL_ORIGINAL` — PASS 18/18
- profile.txt: `localFeBudget.betaMax` = 0.650000 (C0) / 0.450000 (C2) / 0.350000 (C3) as assigned; `localFeBudget.betaMin=0.250000`; `maxFEs=250000` — PASS 18/18
- checkpoints/checkpoint-registry.csv: exactly 4 checkpoint-decision-front rows with targets {50000,100000,150000,200000}; every checkpoint row `checkpointObservedFE==checkpointTargetFE` and `overshootFE==0`; exactly 2 terminal rows with frontSize > 0 — PASS 18/18

## 6. Fairness groups (T4 items 8–9) — 6/6 PASS

| Instance | Seed | actualFE spread | spread<5000 | V35/P8/snapshot identical across C0/C2/C3 | Verdict |
|---|---|---|---|---|---|
| 50_2_3_1 | 20260916 | 997 | TRUE | TRUE | PASS |
| 100_5_3_1 | 20260916 | 997 | TRUE | TRUE | PASS |
| 50_2_3_1 | 20260917 | 997 | TRUE | TRUE | PASS |
| 100_5_3_1 | 20260917 | 997 | TRUE | TRUE | PASS |
| 50_2_3_1 | 20260918 | 997 | TRUE | TRUE | PASS |
| 100_5_3_1 | 20260918 | 997 | TRUE | TRUE | PASS |

All groups: identical `initial-population.sha256` V35 hash, identical P8 hash, identical `snapshotSha256` (from configuration.txt, matching the preregistered snapshot hashes). FE spread in every group is 997 (C2=249003 PHASE_CONSISTENT_TAIL_STOP vs C0=C3=250000 EXACT_MAX_FE), well under the 5000 bound. Full hashes in `fairness-group-audit.csv`.

## 7. Post-run remote state

- 18/18 `results/run-GAPL250K-*` dirs present; 0 `.partial-*` dirs remain; 0 java processes; no scheduler processes (verified via `ps`).
- Remote campaign dir retained intact (results + logs + inputs + bindings + jars + snapshots).

## 8. Machine-readable summary

```ini
runsCompleted=18
runsAccepted=18
fairGroupsPassed=6/6
executionVerdict=COMPLETED
```
