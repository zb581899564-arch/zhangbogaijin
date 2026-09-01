# V35 FINAL 第二阶段：Final Freeze 与 A0–A4 Master Campaign DAG

状态：`PAUSED_NO_PROMOTED_V35_FINAL_CANDIDATE`  
建立日期：2026-08-23  
授权来源：用户人工批准的“V35 FINAL 第二阶段总控任务”。

> 2026-08-23当前事实：正式Master已经安全停止，训练机不存在Stage2 Runner进程，暂停标记仍在。
> 已完成并保留12个完整A0--A4公平组（60条配对运行）；8条孤立完成和7个partial attempt排除。
> A3/PDDR纯观察审计已经完成；D-103规定必须先完成A2/A4跨规模多实例确认并作出Final候选裁决。
> 2026-08-25：D-103已完成但A4被100-job门否决；独立预注册的A0/A2 Final-candidate confirmation
> 也已完成，A2被总体Cmax、跨实例/规模及100-job门否决。旧A0--A4 Master不能恢复，必须先进行新的
> 科学路线裁决并获得用户新批准。唯一状态入口见
> `docs/PAPER_EVIDENCE_MASTER/CURRENT_SCIENTIFIC_STATE.md`。

## 已经 superseded 的历史阻断门

下列历史记录仅保留审计可追溯性，**不得**再阻断本阶段的 Final Freeze 或 Master Campaign：

```text
FC-8 Champion Gate = SUPERSEDED_BY_FC6_AND_DOE1_EVIDENCE
FC-9 before formal experiment = SUPERSEDED_BY_FC6_AND_DOE1_EVIDENCE
```

本阶段的唯一科学冻结语义为：

```text
V35 FINAL = A4-Pacing + FM3 + DEGENERATE_SINGLE_FAMILY
          + SEQUENCE_INDEPENDENT SUT + ShiftMode=NONE
          + GLOBAL_ORIGINAL PDDR + CA-TA-Lite -> inherited LS
          + dual-Q P=5/G=5 + Qg + Qp + DSCR + CFVF + PA_i
          + rho=0 + directionalTeacherPool=false
          + population=100 + mixture=[20,40,20,20] + MaxFEs=500000
```

`ORDER_SWAP`、`REGION_AWARE`、`BP_RESERVED_LEGACY`、`rho>0`、active Shift、
FCLS/FCRS、PF-SDST、序列相关设置时间、directional environmental selector、
crowding distance 与 300/600→100 survival selector 均为拒绝分支。

## 执行 DAG

```text
DOE-1 CLOSED (用户批准)
       |
       +-- A. Clean Final Source Freeze -----------------+
       +-- B. Formal Manifest / Seeds / Fairness Freeze -+--> G1 + G2 + G3 + benchmark
       +-- C. A0-A4 Production Preflight ---------------+             |
       +-- D. A0-A4 Final Semantic Review --------------+             v
       +-- E. Master Campaign + Analysis Integration ---+      MASTER FORMAL RAW-RUN CAMPAIGN
                                                                  A0,A1,A2,A3,A4
                                                                        |
                                                                        +--> raw-run acceptance
                                                                        +--> PFref_ablation
                                                                        +--> PFref_main(A0,A4)
                                                                        +--> statistics + paper tables
```

## 历史自动启动门（已被D-103覆盖）

下列工程门证明历史Master的执行链具备能力，但不再构成自动启动授权。D-103的A2/A4确认与用户新批准
是当前更高优先级硬门：

1. `FINAL_SOURCE_FREEZE`
2. `FORMAL_MANIFEST_FREEZE`
3. `A0_A4_PRODUCTION_PREFLIGHT`
4. `FORMAL_MAX_PARALLEL` 已由真实远端吞吐 benchmark 冻结
5. A0–A4 语义复核没有发现身份/公平性阻断缺陷

任一语义、实例矩阵、初始种群公平性、FE闭合或源冻结存在不能从已有项目资料唯一判定的问题，必须 `FAIL_CLOSED`；不得猜测或在运行中改算法。

## 唯一物理运行矩阵与复用纪律

```text
RunKey = Arm + Instance + Seed + MaxFEs + JarSHA + ConfigSHA
Roster = A0, A1, A2, A3, A4
```

完成的 RunKey 永不重复。A0 同时复用于规范公平适配 HMOPSO-QGS-F 主比较；A4 同时复用于 V35 FINAL 主比较。不得再单独重跑 A0 vs A4。

若 manifest 物化为 45 instances × 20 seeds，物理矩阵为 `5 × 45 × 20 = 4500` 条，而不是再叠加两算法比较矩阵。

## 文件所有权与并行边界

| Track | 责任 | 可写范围 |
|---|---|---|
| A | 干净源码冻结与 Jar | `docs/evidence/V35-FINAL-FREEZE/`、隔离 freeze worktree |
| B | 实例/seed/初群公平 manifest | `docs/evidence/V35-FORMAL-MANIFEST/`、formal manifest runner/config |
| C | Phase-bound Gate3、远端 benchmark | `docs/evidence/V35-PHASE-BUDGET-PROTOCOL/`、`docs/evidence/V35-REMOTE-BENCHMARK/`、远端 diagnostic 目录 |
| D | A0–A4 语义和证据复核 | `docs/evidence/V35-A0-A4-FINAL-SEMANTICS/` |
| E | master campaign、统计与写作接线 | `docs/evidence/V35-MASTER-CAMPAIGN/`、`docs/MASTER_FORMAL_CAMPAIGN_STATUS.md`、analysis/paper integration |

不得并发修改核心算法语义。任何需要修改 `ZhangBoMOHPSOQ`、规范 Decoder、PDDR 或冻结配置的工作必须停下并报告给总控。
