# P8.4 疲劳一致左移/右移阶段报告

状态：`in_progress`

## 已实现

- 新增`fatigue-shift-v1`与`NONE/LEFT_ONLY/RIGHT_ONLY/LEFT_RIGHT`四种显式模式。
- 建立以`(job,stage)`为身份的不可变工件—机器—工人DAG、稳定拓扑排序和完整恢复—疲劳—工时—目标重传播器。
- FCLS按固定检查顺序枚举，每道工序至多评价8个候选，并以三主目标Pareto-safe规则接受。
- FCRS保持全部资源序列不变，只增加`releaseOverride`，按反向拓扑和最多10次二分寻找最大安全右移。
- 一次外部`evaluate()`仍只记录1 FE；内部候选传播不触发jMetal评价计数。
- 正式P8/P9 profile升级为`p8-ablation-v4-shift`，34项正式配置统一显式使用`LEFT_RIGHT`；A0作者诊断保持`NONE`。
- P8.3、P8-v3和旧P9证据已增加`legacy_pre_shift_decoder`隔离说明。

## 已通过的工程门

- 不变DAG的全局重传播与现有S0在20道工序、七槽目标及疲劳指标上以`1e-9`一致。
- `NONE`保持当前S0规范序列化字节不变。
- 合成尾部调度产生真实、Pareto-safe的FCLS接受事件，证明左移不是空实现。
- I1/X0的FCRS产生4个接受事件，Cmax不变，TEC与TWC不增。
- 同一I1/X0连续执行结果、候选事件哈希和最终调度哈希一致。
- 对I1/X0的S2再次执行完整refine没有新接受事件，目标和轨迹不变。
- 正式profile注册表、问题模块和执行模块完成Java编译。

验证记录：`jmetal-problem`全模块54项测试为`0 failure / 0 error / 1 skipped`，其中唯一跳过项就是未满足的I1双方向图例门；P8注册表、双Q与CA-TA定向13项通过；P9正式Runner 6项通过并以2000 FE真实贯通FULL、HMOPSO-QGS-F和REPORT；I1证据Runner测试通过；六模块Java 8打包通过，新增主类字节码major version为52。

## I1/X0硬门结果

I1/X0在锁定的严格规则下得到：

```text
FCLS = 0 accepted / 14 evaluated candidates
FCRS = 4 accepted / 44 evaluated candidates
illustrationGate = false
```

因此I1图13只能如实表示`S1=S0`，不能画成一次成功左移。按照用户批准的停止条件，本阶段保持`in_progress`，以下标志不得设置为真：

```text
fatigue_consistent_left_shift_validated=false
fatigue_consistent_right_shift_validated=false
shift_decoder_idempotence_validated=false
shared_shift_decoder_fairness_validated=false
shift_i1_evidence_locked=false
```

这里的`false`表示P8.4整体验收尚未闭合，不表示FCRS或幂等单项测试失败。

## I1证据

当前证据位于`paper_evidence/I1/09_shift_validation`，包括S0/S1/S2调度CSV、58个候选事件、三目标和疲劳前后表、三张SVG甘特图、哈希清单和诚实验收说明。

关键变化：S0到S1无变化；S2保持`Cmax=62.572483768803`，`TEC`由`2161.278038954161`降为`2154.795023027821`，`TWC`由`3960.678066912318`降为`3788.783539210590`。`Fmax`上升只记录为诊断，不进入接受条件，符合“不隐藏引入第四目标”的约束。

## 未执行

- 未运行I1 5000 FE解释搜索。
- 未运行`20_2_3_1`的FULL/BASE各20000 FE烟测。
- 未运行100k、500000 FE、六seed、正式消融或统计矩阵。
- 未生成一个虚构的I1左移成功示例。

后续若需要论文同时展示一次成功FCLS和FCRS，应由用户决定是否允许在同一I1内冻结第二个粒子。不得更改seed、放宽Pareto-safe规则或篡改X0结果。
