# FC-6 Stage 1：Build A（FC-5.2 + R_retain 观察，无 BP-PDDR）三 seed 核验

- 日期：2026-08-19（训练机 `/home/inspur/aicomp/zhangbo-fc6-20260818`，tmux fc6-stage1）
- 配置：`20_2_3_1` / 500000 FE / population 100 / `--local-fe-budget 0.25:0.65`（PACING 正式配置）/ seed 20260822/23/24
- Build A jar：`jmetal-exec-5.8-BUILD-A-fc52-rretain.jar`（sha256 前 16 位 `54927ddf30e885aa`；与 FC-5.2 最终版源码一致 + fc52 R_retain 观察扩展，PDDR/archive/VNS/Qg/Qp/CFVF 全部未改）

## 1. 行为等价（观察代码零漂移）

| seed | fullEvaluations | frontSize | 与 fc2-500k-20_2_3_1 pacing front 比对 |
|---|---:|---:|---|
| 20260822 | 500000 | 622 | **逐字节一致** |
| 20260823 | 500000 | 613 | **逐字节一致** |
| 20260824 | 500000 | 601 | **逐字节一致** |

3/3 front 与 FC-2 官方基线完全一致 → Build A 复现基线轨迹，观察代码未进入任何决策路径；同时验证训练机/本地跨机器确定性。

## 2. minCmax 极值死亡链（3/3 同型确认）

| seed | bestEver Cmax | 出现 | 来源 | 局部接受 | 进池 | PDDR | archive | final |
|---|---:|---|---|---|---|---|---|---|
| 20260822 | **174.4366** | fe=288564 / cycle 41 | INTRA_FACTORY_VNS | ACCEPT:ACCEPTED | yes | **REJECT:score=1** | never | no |
| 20260823 | **169.6270** | fe=219476 / cycle 32 | INTRA_FACTORY_VNS | ACCEPT:ACCEPTED | yes | **REJECT:score=1** | never | no |
| 20260824 | **191.2079** | fe=496557 / cycle 62 | INTRA_FACTORY_VNS | ACCEPT:ACCEPTED | yes | **REJECT:score=0.2** | never | no |

- 三 seed 同一条链：VNS 产出 → 完整评估 → 局部接受（G1 纯 Cmax 改善）→ 进入 PDDR 合并池 → 因 q==0 且 1/(p+1) 项被中心支配点压制而 REJECT → 从未被 archive 观察 → 最终输出无。
- **FC-5.2 结论细化**：seed22/23 的极值 score=1（不支配任何人）；seed24 的 191.2079 score=0.2（支配池内 4 个候选、1/(4+1)=0.2）——非支配极值只要支配计数低于中心点就会被压过，机制相同、数值随池组成变化。**三个 seed 全部确认"PDDR 边界丢失"**。
- 死亡后同一 lineage（seed22:1472 / seed23:974 / seed24:1649）被 VNS 再次产出时 local=REJECT:NOT_BETTER（父代 Cmax 已持平），不再进池——极值知识就此永久丢失。
- bestEver 候选即其所在轮次池内 minCmax 且为 q==0 → **minCmax 角色基线 R_retain = 0/3**。

## 3. 基线 R_retain（fc52 观察，62 轮 × 3 槽 = 186 边界槽/seed）

| seed | boundaryPool | boundarySurvived | R_retain(aggregate) |
|---|---:|---:|---:|
| 20260822 | 186 | 155 | 0.8333 |
| 20260823 | 186 | 141 | 0.7581 |
| 20260824 | 186 | 120 | 0.6452 |

- 聚合值 >0 的原因是 minTEC/minTWC 极值多数按 score 本身即可入选（其 TEC/TWC 极值常伴随可观的支配计数），死亡集中于 **minCmax 角色**（见上表 0/3）。
- 说明：本指标为观察性镜像（与 select 规则的实现一致性校验）；Build B 中 R_retain 应=1.0（规则生效即保留），性能问题由 Stage 2/3 的 Cmax/HV/IGD 门回答。

## 4. 结论

Stage 1 门**通过（3/3）**：三个 seed 的 best-ever evaluated Cmax 极值全部死于 PDDR 合并池准入，与 FC-5.2 seed22 结论一致 → 根因 B"PDDR 边界丢失"在全 seed 成立 → 进入 Stage 2（BP-PDDR 20-job 验证）。