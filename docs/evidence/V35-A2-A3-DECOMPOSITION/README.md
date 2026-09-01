# V35-A3-D：A2→A3 最小因果拆分诊断

本目录只服务于“为什么 A2→A3 在既有先导中退化”的因果定位；不是 DOE、不是正式
消融、不是论文统计样本。正式语义保持 `FM3`、`ShiftMode=NONE`、单一产品族、序列无关
SUT、`[20,40,20,20]`、`GLOBAL_ORIGINAL` PDDR 与 `CA-TA-Lite → inherited LS`。

四臂的唯一递进差异为：

| Arm | 新增内容 |
|---|---|
| D0 | A2 精确控制：无个人档案、无 Qp、无双Q |
| D1 | 容量6谱系个人档案 + 确定性方向 pbest |
| D2 | D1 + 四动作 Qp 与同步 Qg/Qp 学习 |
| D3 | D2 + 既有 10% 预热、P5/G5 硬冻结、rho=0 |

Qp 方向奖励固定为 `LEGACY_UNCLIPPED`；裁剪奖励与其它后续机制均不在本诊断中。
所有重跑均通过 `sourceRunId=V35-A2-A3-CAUSAL-AUDIT` 关联既有 A2/A3 证据，只因新增
统一因果遥测而执行，绝不作为新增独立样本。

## 目录

- `02-early-implementation-smoke/`：范围更正后的早期 D0 50k 烟测，不计入12条。
- `03-preflight/`：固定 2k 配置、快照与预算边界贯通门。
- `04-runs/`：12 条预登记 50k 运行。
- `05-analysis/`：统一参考前沿、指标、相邻配对与根因裁决。

## 当前裁决（2026-08-24）

12条预登记50k运行全部完成，且每条均满足`actualFE=decoderCalls=50000`、同seed四臂初群哈希
一致、非法解/重复评价/CFVF repair为0。共同参考与相邻独立reference都显示：D0→D1及D1→D2
分别触发稳定退化门，而D2→D3没有触发。因此唯一诚实的根因标签为
`COMPOSITE_BLOCK_UNRESOLVED`；D3的P5/G5冻结不是本次退化的唯一根因，但个人档案方向领导和
未裁剪Qp选择/奖励仍是后续单变量验证对象。完整验收见`05-analysis/ACCEPTANCE_REPORT.md`，
方法见`ANALYSIS_METHOD.md`。

## 2k 门的已知限制

在 population=100 且 Q phase=100×50=5000 FE 的 phase-consistent 终止协议下，2k 请求
只能完成初始的100次评价，不能产生一个完整Q阶段。因此它仅验证配置、同初群、运行器、
证据和预算尾停；Qp、档案与双Q的实际触发必须由后续预登记50k运行验收。
