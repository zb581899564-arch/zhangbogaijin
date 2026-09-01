# V35 Master Formal Raw-Run Campaign

这是 V35 Final 第二阶段唯一的 A0--A4 正式原始运行总入口。它不保存任何
由未完成矩阵、诊断试验或历史分支导出的性能结论。

```text
Roster = A0, A1, A2, A3, A4
Matrix = 45 instances x 20 frozen seeds x 5 arms = 4500 physical runs
Budget = 500000 successful decoder evaluations per nominal run
```

`A0` 同时是“规范、确定性、公平适配 HMOPSO-QGS-F”的主比较基线，`A4` 同时是
V35 Final 主算法。因此 A0/A4 的同一 `RunKey` 只能运行一次，后续主比较只引用
其 raw-run 目录，严禁另建 1800 条 A0/A4 物理运行。

## 当前状态

本目录目前只包含启动契约、注册表结构和分析/论文接线；**没有任何正式 raw run**。
`MASTER_CAMPAIGN_INTEGRATION.md` 与
`../../MASTER_FORMAL_CAMPAIGN_STATUS.md` 是当前唯一状态说明。

## 目录约定

```text
00-control/                 Gate、冻结输入与只读 launch 契约
01-registry/                只追加 RunKey 注册表与成对失效账本
02-raw-runs/<RunKey>/       每个物理 arm 的最小原始证据
03-audit-detailed/          预先声明的少量详细审计副本（可选）
04-raw-acceptance/          全矩阵完成后的完整性/公平性/FE 验收
05-analysis-ablation/       A0--A4 pooled empirical PFref 与统计母表
06-analysis-main/           A0/A4-only pooled empirical PFref 与统计母表
07-paper-integration/       只读取已冻结母表的论文结果接线
```

不得将 `REGION_AWARE`、`BP_RESERVED_LEGACY`、`ORDER_SWAP`、active Shift、PF-SDST、
方向教师池、压力严格掩码、`rho>0`、作者诊断或任何历史运行混入本目录的 raw-run、
reference 或统计输入。

## 所有权

本目录归 Track E 所有。Track A/B/C/D 的只读冻结产物经 SHA-256 绑定后由本目录引用，
不复制、覆盖或修改它们。
