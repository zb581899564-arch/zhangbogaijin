# V35-P25B 压力诊断、置信回退与掩码审计报告

## 结论

本工作包完成了压力诊断、BAL置信回退、shadow反事实审计和阈值选择的工程实现，但**held-out门未通过**。因此：

```text
pressure_diagnosis_engineering_validated=true
confidence_bal_fallback_validated=true
masked_action_shadow_audit_validated=true
diagnosis_thresholds_frozen=false
formal_matrix_started=false
sampled_reproduction_accepted=false
full_reproduction_accepted=false
pf_sdst_active_experiment=false
shift_formal_path_frozen=true
```

正式执行路径继续使用`BAL`并开放`N1–N5`，不启用单瓶颈严格掩码，不重建P24.2，也不启动新的500000 FE运行。

## 已实现语义

主循环固定为：

```text
已评价全局后代
→ Need选择目标工厂
→ 只读取该工厂轨迹并构造工件/机器/工人DAG
→ 计算SEQ/MAC/WOR/SET/FAT五项[0,1]压力
→ 绝对阈值与领先差距双门
→ 高置信严格掩码，低置信或异常BAL全开放
→ Test/Apply/Re-test
```

`BAL`不参与五项压力最大值竞争。空工厂、DAG循环、非有限值和不可计算状态均fail-closed到BAL。`P_SET`只表示序列无关`SUT[job][stage]`压力，产品族转换项仍为0。

## Shadow隔离

Shadow使用独立`ZhangBoCanonicalProductionProblem`、独立评价计数器和派生seed。它不增加主搜索FE，不进入PDDR、谱系档案、Qg/Qp、Cmax审计、正式前沿或Decoder累计计时。主候选与shadow候选分别构造，避免预评价对象被主路径复用。

定向集成测试证明shadow开启/关闭时以下主结果一致：

- 初始种群哈希；
- 主搜索完整评价数；
- CA-TA-Lite事件流；
- 最终前沿。

## 校准与held-out

校准集：I1和`20_2_3_1`，seed `20260814–20260816`，A4，分别5000/20000 FE。6条主运行全部完成。Shadow每20次合法调用抽样一次，每运行最多5000次独立反事实评价。

Q50/Q60/Q70/Q80/Q90构成25组阈值。校准阶段选择：

```text
tauAbs=0.7043115598804346
tauGap=0.035867209301071235
strictCoverage=0.1111111111111111
missedPositiveBestRate=0.0
```

held-out集：seed `20260817–20260818`，I1和`20_2_3_1`，A4/A5，共8条主运行，全部完成。统一重算结果：

```text
strictCoverage=0.3333333333333333
missedPositiveBestRate=0.4117647058823529
meanDiagnosticRegret=0.052263158834960154
p95DiagnosticRegret=0.27709558369776716
```

门槛要求`coverage>=10%`且`missedPositiveBestRate<=5%`，实际漏失率为41.2%，故拒绝冻结阈值。每条held-out结果见`heldout-validation.csv`。

## 证据

- `runs/calibration/`：6条校准主运行及独立shadow证据；
- `threshold-candidates.csv`：25组阈值；
- `threshold-selection.csv`：校准选择；
- `runs/heldout/`：8条held-out A4/A5运行；
- `heldout-validation.csv`：逐运行与合并门；
- `DIAGNOSIS_AUDIT_REPORT.md`：自动门报告；
- 每个运行目录的`bottleneck-pressure-events.csv`、`shadow-probes.csv`、`evidence-sha256.tsv`。

当前P25A在训练机上继续使用冻结旧jar运行，不被本工作包中途修改；其完成结果须标记`legacy_pre_pressure_diagnosis`，不得和本工作包前沿混合。

## 回归与构建

- 当前语义：`jmetal-problem` 67/67、V35压力诊断与主循环定向测试31/31、P25B Runner 1/1通过；
- 完整历史回归：212/215通过，3项分别为旧P10.1前沿快照不兼容及隔离副本项目根路径导致的两项P24冻结路径不一致；
- 根聚合工程及五个子模块打包成功；P25B Runner字节码major version为52；
- 详细分类、构建产物哈希与最终状态见`REGRESSION_AND_BUILD_REPORT.md`。
