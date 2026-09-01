# V35 MASTER FORMAL LAUNCH REPORT

日期：2026-08-23  
结论：`NOT_LAUNCHED__PHASE_BOUND_GATE3_ACCEPTED_PENDING_REGRESSION_AND_THROUGHPUT`

> 更新（2026-08-23）：用户已批准方案 C。此前 strict-exact Gate3 阻断仅作为
> `legacy_pre_phase_budget_protocol` 保留。本阶段不改 jar、不启用 partial Q phase；新的
> 五臂 50k Gate3 已在 `v35-phase-consistent-budget-v1` 下通过，远端吞吐尚未开始。

## 1. 阶段裁决

用户已批准 Stage2：FC-6 与 DOE-1 已闭合，历史 FC-8/FC-9 被标记为
`SUPERSEDED_BY_FC6_AND_DOE1_EVIDENCE`，不再作为 V35 FINAL 的前置。本报告按新的 Stage2
硬门审计，而不是沿用历史门阻断。

本轮没有启动任何 500k 正式运行、远端吞吐 benchmark 或 4,500 条 Master 矩阵。原因不是
历史 FC gate，而是新的 Gate3 精确 FE 生产预检未通过。

## 2. 冻结身份

| 项目 | 值 |
|---|---|
| clean source tag | `v35-final-doe1-frozen` |
| tag target commit | `2b3316b21512ff9d1d7f3db972f016ba02edac6e` |
| isolated clean source directory | `E:\学习\李明哲-毕业材料\_isolated-v35-final-doe1-freeze-20260823` |
| source/config bundle SHA-256 | `ac92eda152348ce11861ec5c2f223e6a9c7643afd50cbaa5d48189d1fc41f0fd` |
| frozen A4 config SHA-256 | `cff6bbca0a8357ae848e625710c0ba39a1c9419becd84ef4e95f8bb6f88db09e` |
| deployment fat jar SHA-256 | `8dad8f40266feeaa4cdb9b47dbe4e342d9064847df32c7f2933149b9b6bad8b9` |
| bytecode target | Java 8 / major version 52 |

构建使用隔离的 JDK 17.0.12；本机没有 Java 11.0.27。由于 classfile 为 major 52，满足 Java 11
加载的字节码兼容条件；直接 Java 11 本机运行未能进行，已如实记录。

## 3. Final 与公平起点

```text
V35 FINAL = A4-Pacing + FM3 + DEGENERATE_SINGLE_FAMILY
          + SEQUENCE_INDEPENDENT + ShiftMode=NONE
          + GLOBAL_ORIGINAL + CA-TA-Lite -> inherited LS
          + dual-Q P=5/G=5 + Qg/Qp + DSCR + CFVF + PA_i
          + rho=0 + directionalTeacherPool=false
          + population=100 + mixture=[20,40,20,20] + MaxFEs=500000
```

| 项目 | 结果 |
|---|---|
| 实例 manifest | 45/45 已冻结 |
| seed roster | 20/20，`20260808..20260827` |
| 共享初群 | 900/900 `.fourvec` snapshot，覆盖每个 instance×seed |
| 公平约束 | 五个 arm 都必须通过 `readSnapshot(...)` 读取同一快照；禁止 `createSolution()` 自行初始化 |
| A0 身份 | 规范、确定性、公平适配 HMOPSO-QGS-F，不是 `author_actual` |
| A4 身份 | frozen V35 FINAL，方向教师池关闭 |

正式 Runner v2 和专用 Master renderer 已强制验证 jar、config、snapshot、V35/P8 双初群哈希及
provenance。RunKey 为：

```text
Arm + Instance + Seed + MaxFEs + JarSHA + ConfigSHA
```

计划物理矩阵为 `A0..A4 × 45 instances × 20 seeds = 4500`。A0/A4 同时复用作主算法比较，
不会另建重复的 1,800 条两算法矩阵。

## 4. Gate 验收

| Gate | 结果 | 证据 |
|---|---|---|
| Final Source Freeze | `ACCEPTED` | `V35-FINAL-FREEZE/FINAL_SOURCE_FREEZE.md` |
| Formal Manifest Freeze | `ACCEPTED` | `V35-FORMAL-MANIFEST/FORMAL_FAIRNESS_FREEZE.md` |
| A0–A4 Final Semantics | `ACCEPTED` | `V35-A0-A4-FINAL-SEMANTICS/FINAL_A0_A4_SEMANTICS.md` |
| A0–A4 Production Preflight | `ACCEPTED_PENDING_REGRESSION` | `V35-PHASE-BUDGET-PROTOCOL/03-gate3-preflight/GATE3_PHASE_BOUND_REPORT.md` |
| Formal Max Parallel | `PENDING_REGRESSION_AND_THROUGHPUT` | 远端 benchmark 尚未启动 |

## 5. Gate3 事实与阻断理由

冻结 Jar 上的预注册 50k 与允许的 100k 扩展生产预检：

```text
arm=A4
instance=20_2_3_1
seed=20260828 (不在正式 seed roster)
population=100
requestedFE=50000
actualFE=decoderCalls=48269
```

该运行是 `COMPLETED`；非法解、重复完整评价、非有限目标和 repair 均为 0；Qg、DSCR、CFVF、Qp、
PA_i、P5/G5、CA-TA-Lite 与 inherited LS 均真实触发，Shift/教师池/shadow 均为 0。

该报告的 strict-exact 阻断已被方案 C 替代。A4 因 `Q_Times=50`、population
100、`allowTerminalPartialFormalQPhase=false` 与共享动态 Local-FE 窗口，只能在完整 Q phase 可容纳时
继续；剩余预算不足一个完整 5,000-FE Q phase 时，算法合法安全停止于 48,269。允许的
100k 扩展也安全停止于 96,025 FE。启用 partial phase 会改变冻结 Q/LS 时序，未被执行。
早先的 20k 工具链诊断（15,258 FE）保留为历史证据，但不再作为 Gate3 的唯一裁决依据。

因此 Gate3 不是“近似通过”，也不是基础设施故障；在政策裁决前必须 fail-closed。

## 6. 未启动项与下一人工裁决

```text
A0–A3 50k production preflight = 0
remote 4/8/12/16 JVM benchmark = 0
formal 500k raw runs = 0
Master campaign completed/failed/invalid = 0/0/0
PFref / HV / IGD / statistical conclusion = 未生成
```

下一步不是调参或修改算法。必须由用户明确二选一：

1. 已采用：保持冻结算法，并将正式预算验收政策改为真实 phase-consistent 尾段口径；
2. 当前仍不能启动的原因改为 A0--A4 snapshot-bound Master launcher 缺口，详见
   `../V35-PHASE-BUDGET-PROTOCOL/06-formal-launch-readiness/FORMAL_MATRIX_BLOCKER.md`。

在未作出上述治理裁决前，Master Campaign 不得启动。
