# V35-GAP-LEVERAGE-AUDIT-V1 预登记（纯离线 0-FE 杠杆审计）

- 日期：2026-08-30
- 状态：`AUDIT_IN_PROGRESS`（0 FE；无训练机动作；无算法/冻结语义修改）
- 上游：Gap Probe V2 完成（16/16 ACCEPTED，GAP_GT_15，RED=false）、D-110、AGENTS §22

## 1. 审计问题（唯一）

> 冻结 A4 在困难实例 100_5_3_1 上相对官方经典核的 Pareto 覆盖差距
> （gapHV +63.5~67.8%、gapIGD +260~311%），是否存在**单一、可实施、可反驳**
> 的 repair family 旋钮？

## 2. 候选假设（封闭集，裁决时只能选一）

```text
H_CFVF_QP_GUIDANCE        CFVF 产生量大但个人/社会教师在困难实例上导致方向覆盖收缩
H_CATA_BUDGET_COORDINATION CA-TA 与 inherited LS 的预算/时序在困难实例上消耗大而贡献不足
H_CREDIT_TIMING           Qp/CA-TA 奖励或更新时序在困难实例上形成错误强化
NO_ACTIONABLE_LEVERAGE    证据不足或全部可触达旋钮杠杆不足
```

## 3. 数据源（全部为已验收本地证据，只读）

```text
D1 Gap Probe V2 500k×8（A4/A0 × 2实例 × 2seed，04-v2-remote-500k-runs/sync/）
   —— mechanismSummary 全计数器、ca-ta-lite-events.log、dscr-teacher-uses.csv、
      dscr-events.csv、passive-archive.csv、cmax-audit-curves.csv、front.csv
D2 250k 中程诊断（V35-FC5-MIDHORIZON-250K/01-root-cause-analysis/remote-results/）
   —— checkpoint-metrics.csv（逐检查点 HV/IGD）、paired-performance.csv、
      teacher-concentration、pddr-cycle-summary、cata-contribution-summary
D3 F1 失败重放（V35-PFC5-F1-FAILURE-REPLAY/03-raw-run/remote/）
   —— 100_5_3_1/seed20260901 冻结 A4 500k 全套事件工件
D4 50k/100k 转移诊断（V35-FC5-100JOB-TRANSFER/）
D5 外部基线对照（04-v2…/sync/ 外部 4 条 + 20k 机制门数据）
```

## 4. 分工与文件所有权（互斥）

```text
Agent A（失败起点/覆盖/事件差异）→ 产出 normal-hard-contrast.csv 的事实行
Agent B（CFVF+Qp链审计）        → 产出 module-leverage-matrix.csv 的 B 行
Agent C（CA-TA+LS+预算审计）    → 产出 module-leverage-matrix.csv 的 C 行
总控（本Agent）                → 复核数字、historical-route-exclusion.csv、
                                  single-repair-family-decision.md、
                                  proposed-development-run-registry.csv、
                                  EXTERNAL_ADAPTER_MEMORY_DEBT.md、最终台账
禁止任何 Agent 写入他人文件或修改算法/治理文件。
```

## 5. 每个候选的强制字段

正常/困难观测差异；证据文件与字段来源；旋钮可影响的事件比例（覆盖率）；
结构可达性；预计收益方向；对随机流/FE/PDDR/冻结语义的风险；最小开发实验量；
明确证伪方式。

## 6. 硬边界

禁止重提 BP-PDDR/REGION_AWARE/ORDER_SWAP；禁止删除 CFVF/Dual-Q/CA-TA；
禁止同时修改两个机制；禁止按单实例直接调参；禁止把相关性写成最终因果；
禁止 DOE/Validation/正式矩阵/新训练。若多候选杠杆都不足 ⇒ `NO_ACTIONABLE_LEVERAGE`
（诚实结果，不是失败）。

## 7. 裁决规则（预先锁定）

```text
R1 可达性：旋钮必须在现有代码结构中存在明确注入点（v1 CAL 审计证明
   teacher-identity 旋钮仅覆盖 1.12% 教师事件 → 已排除的路线不得重提）。
R2 覆盖率：旋钮直接/可证明传播影响的事件比例 ≥10%（D-110 杠杆门）。
R3 方向性：正常/困难对比必须显示该机制在困难实例上的行为差异方向
   与覆盖收缩假设一致。
R4 可反驳：最小实验（C0/C1/C2/C3 单旋钮 × 2 实例 × 2 新 seed，先 20k 后 50k）
   必须存在明确的证伪条件。
R5 互斥：若多个候选过 R1-R4，选覆盖率×证据强度最高者，其余降级登记。
```
