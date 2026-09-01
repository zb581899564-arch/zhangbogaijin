# Master Campaign 启动计划（可执行但 fail-closed）

本文件是唯一启动顺序。它不授权改算法、改参数或跳过 Gate。

## Gate 输入（由其他 Track 提供，Track E 只读取）

| 条件 | 必需状态 | 绑定产物 |
|---|---|---|
| Final source | `FINAL_SOURCE_FREEZE=ACCEPTED` | Final commit/tag、clean worktree、jar SHA、canonical config SHA |
| Formal manifest | `FORMAL_MANIFEST_FREEZE=ACCEPTED` | 45 行实例、20 seed、900 初群 snapshot、manifest SHA |
| Production preflight | `A0_A4_PRODUCTION_PREFLIGHT=ACCEPTED` | 五臂 50k phase-bound：`0<actualFE=decoderCalls<=MaxFEs`、尾段 `<5000`、共同初群与机制闭合 |
| Throughput | `FORMAL_MAX_PARALLEL=ACCEPTED` | 冻结 jar 下真实远端 20k--50k benchmark 和安全并发 X |
| Semantics | `FINAL_A0_A4_SEMANTICS=ACCEPTED` | A0 为公平适配 HMOPSO-QGS-F；A4 为 frozen Final |

所有五项均为 `ACCEPTED` 才能由 Master 渲染真实 manifest。任何 `RUNNING`、`FAILED`、
`BLOCKED`、未验证 hash、路径漂移、超预算、decoder 不闭合或尾段不小于一个完整 Q phase
都保持 `BLOCKED_PENDING_GATE_ACCEPTANCE`，不得运行。`requestedFE != actualFE` 仅在
`PHASE_CONSISTENT_TAIL_STOP` 条件完整满足时可接受；不得补评价、调整局部预算或改变算法行为。

## 渲染规则

1. 读取 Final jar SHA 及每臂 canonical config SHA；二者进入每条 RunKey。
2. 读取 Track B 的 `FORMAL_INSTANCE_MANIFEST.csv`、`FORMAL_SEEDS.txt` 和
   `FORMAL_INITIAL_POPULATION_MANIFEST.csv`；应得到 45、20、900 三个精确计数。
3. 对每个 instance/seed 物化五个 arm；总数严格为 `45*20*5=4500`。
4. 每条 arm 的 `snapshotPath` 必须是同一个 `(instance,seed)` snapshot。Java arm runner
   必须调用 `ZhangBoV35FormalInitialPopulationFreezeRunner.readSnapshot(...)`，并拒绝
   `problem.createSolution()`；同时记录 V35/P8 两个初群哈希。
5. 只保留 A0--A4。A0 和 A4 标注 `reusedForMainComparison=true`，不新增 A0/A4 compare run。
6. `maxParallel` 只能等于 Track C 冻结的 `FORMAL_MAX_PARALLEL`。所有运行均在远端单一
   campaign scheduler 中启动，避免每条 run 独立 SSH。

## 执行与恢复

渲染产物必须先通过 **Stage2 Master adapter**。该 adapter 的职责是：验证本文定义的
显式 Master RunKey，检查 snapshot/provenance，并调用已接入 `readSnapshot(...)` 的正式 arm
runner；随后才可生成给通用进程调度器的 argv/状态清单。现有
`scripts/v35_campaign_runner.py` 只负责进程隔离、attempt、resume 和原子 marker；其内部
hash key 不能替代本文的 Master RunKey，也不能单独构成正式 launcher。

Master adapter 产生的 scheduler input 通过 `master-campaign-manifest.schema.json` 与下面检查后，
才可调用：

```text
python scripts/v35_campaign_runner.py --manifest <rendered-master.json> \
  --state-dir <remote-state-dir> --resume --retry-failed --allow-formal
```

命令中的 `--allow-formal` 不是绕过授权：渲染前的 Gate 审计、Master adapter 的显式 RunKey/
snapshot gate、formal arm runner 的 config/provenance gate 和远端 frozen jar hash 都必须已通过。
没有这些输入时，当前的 `master-campaign.locked.json` 缺少 `freezeEvidence` 且 `runs=[]`，被
scheduler 拒绝；adapter 缺失时也必须拒绝，确保不能误启动。

## 原子完成和失败闭环

- 每个尝试写入独立 attempt 目录；只有 raw evidence 完整、各门通过并原子写入 completion
  marker 后才为 `COMPLETED`；
- `COMPLETED` RunKey 永远 skip；
- `FAILED` 保存真实 FE 和错误，可在冻结的 maxAttempts 内重试；
- hash、初群、语义、phase-bound FE、非法/重复评价或 front 完整性问题使整组五臂 `INVALID`，停止该
  `(instance,seed)` 的 paired analysis；
- 基础设施错误只保留/重试对应 RunKey，不能覆盖既有 attempt。

## 运行后顺序

每个实例的 raw groups 全部验收后才能进入对应 PFref；全矩阵 raw acceptance 完成前，
不得写正式效能结论、统计检验或论文结果数值。
