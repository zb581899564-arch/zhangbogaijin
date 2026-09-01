# V35 Final 论文实验 DAG

## 冻结前提

DOE-1 已关闭并冻结：

```text
FINAL_SEARCH_MIXTURE = [G1_CMAX,G4_BALANCED,G2_TEC,G3_TWC] = [20,40,20,20]
```

Final 主线固定为 FM3、`DEGENERATE_SINGLE_FAMILY`、`SEQUENCE_INDEPENDENT`、
`ShiftMode=NONE`、`GLOBAL_ORIGINAL`、`CA-TA-Lite → inherited LS`、A4-Pacing、
双Q `P=5/G=5`、`rho=0` 和方向教师池关闭。`ORDER_SWAP`、`REGION_AWARE`、
`BP_RESERVED_LEGACY`、active Shift、PF-SDST 与 `rho>0` 均不再进入 Final 主线。

## 依赖图

```text
DOE-1 CLOSED
  ├── A. Final freeze + campaign infrastructure ──┐
  ├── C. A0-A4 semantic audit + 2k smoke          │
  ├── D. offline metrics/statistics pipeline       │
  └── E. paper methods/experiment skeleton         │
                                                    ├── B. formal V35 vs HMOPSO-QGS-F raw fronts
                                                    │      (only after frozen artifact + matrix audit)
                                                    │
                C semantic audit ──────────────────┴── human approval request ──> A0-A4 formal ablation

B raw fronts + approved ablation raw fronts ──> per-instance empirical PFref
                                               ──> HV/IGD/C-metric/statistics
```

## 调度分类

| 轨道 | 分类 | 可否立即工作 | 正式 500k 门 |
|---|---|---|---|
| A Final Freeze / Campaign | A：独立 | 是 | 不改变算法语义 |
| C A0-A4 消融语义与 smoke | A：独立 | 是 | 必须等待人工批准 |
| D 指标与统计流水线 | A：独立 | 是 | 等 raw fronts 齐备才出统计结论 |
| E 论文方法与实验骨架 | A：独立 | 是 | 不得虚构未运行结果 |
| B 两算法正式比较 | B：可准备 | 冻结与正式矩阵审计后 | 矩阵歧义或 EXP-1/FC-8 前置未闭合时阻断 |
| A0-A4 正式消融 | C：人工批准 | 否 | C 的语义审计和用户批准后才可启动 |

## 文件所有权

| Owner | 可写范围 | 禁止触碰 |
|---|---|---|
| A | `docs/evidence/V35-FINAL-FREEZE/`、campaign scheduler/infrastructure | 核心算法语义、比较/消融配置 |
| B | formal comparison runner/config、`docs/evidence/V35-FORMAL-EXPERIMENTS/` | A0-A4 语义、统计结论 |
| C | ablation profile/config/tests、`docs/evidence/V35-ABLATION/` | 默认 Final 算法行为、正式 campaign 启动 |
| D | offline analysis/statistics 工具与 `docs/evidence/V35-ANALYSIS/` | 任何算法或 runner 行为 |
| E | `docs/paper/` 与论文草稿 | 代码、数值结果、结论性措辞 |

如确需修改同一核心文件，必须停在该改动前，通知总控并用独立 worktree 或串行合并；不得并发覆盖。

## 强制数据纪律

- 每条物理运行由不可重复的 `RunKey=algorithm+config+instance+seed+budget` 标识。
- 同一 `(instance,seed)` 的公平比较共享显式初始种群；每个 JVM 独立。
- raw final fronts 优先；每个实例只有在所有参与算法与 seed 都完成后才构造一次经验 `PFref`。
- 未经批准，不添加 treatment、seed、参数、算法或机制；失败尝试保留并与有效证据隔离。
- `formal_matrix_started`、`sampled_reproduction_accepted`、`full_reproduction_accepted` 在本阶段均维持 `false`。
