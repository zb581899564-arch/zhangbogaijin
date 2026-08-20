# V35-P25D 八算法五种子 50k FE 独立验收报告

## 1. 验收结论

- 工程运行验收：**通过**。40/40 次运行完成，40/40 精确达到 50000 FE，40/40 的成功 Decoder 调用数与 FE 一致，前沿均非空且数值有限。
- 指标计算验收：**通过**。独立重建 480 点经验参考前沿，与程序输出逐点一致；独立重算 HV、IGD、Spacing 和 C-metric，最大绝对误差为 0。
- 公平输入验收：**通过**。同一 seed 的 8 种算法共享相同初始四向量种群；五个 seed 的初始种群互不相同。40 次运行均使用 FM3、ShiftMode.NONE、单一产品族、序列无关 SUT、100 粒子和目标槽 [0,1,6]。
- 论文级对比验收：**未通过**。除 A4 与 HMOPSO-QGS-F 外，其余六种算法是规范四向量的结构化重写，而不是已核实原始可执行实现；QMOEA 尚未运行。因此当前结果只能用于工程先导诊断。
- 主算法表现判断：A4 **明显优于 HMOPSO-QGS-F 基线，但不是八算法中的最强者**。按 median HV 排名第 7/8，仅优于 HMOPSO-QGS-F。

总体评级：**Share with caveats / 带严格限制可分享**。可以据此决定继续查算法机制，但不能据此撰写“优于所有对比算法”的论文结论。

## 2. 数据与范围

```text
instance=20_2_3_1
population=100
MaxFEs=50000
seeds=20260822..20260826
decoder=FM3
ShiftMode=NONE
familyMode=DEGENERATE_SINGLE_FAMILY
setupMode=SEQUENCE_INDEPENDENT
objectives=[0,1,6]=[Cmax,TEC,TWC]
```

下载包 SHA-256：

```text
f9f219f8a2f417f41022e3da99831290120e627c29e73d893d71be8b87294be6
```

远端与本地压缩包 SHA-256 完全一致；下载后共提取 433 个文件。

## 3. 完整性与计算复核

| 检查项 | 结果 |
|---|---:|
| 运行记录 | 40/40 |
| COMPLETED | 40/40 |
| FE=50000 | 40/40 |
| successfulDecoderCalls=FE | 40/40 |
| 非空有限前沿 | 40/40 |
| 单次运行证据清单哈希 | 345/345 |
| 同 seed 共同初始种群 | 5/5 seed |
| 不同 seed 初始种群不同 | 是 |
| 统一参考前沿 | 480 点 |
| 独立参考前沿一致 | 是 |
| 指标最大绝对误差 | 0 |
| 错误/异常日志签名 | 0 |

五个前沿包含少量“目标完全重复、基因型不同”的点：NSGA-II 三次、MOPSO 一次、MOHEADE 一次，共 6 个重复行。指标计算前会稳定去重并重新做严格 Pareto 过滤，因此不影响当前 HV/IGD 数值，但正式输出应在写入 front.csv 前清理。

## 4. 五种子汇总排名

| HV排名 | 算法 | median HV | std HV | median IGD | median Cmax | median TEC | median TWC | median时间(s) | 对参考前沿贡献点 |
|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 1 | MOPSODS-DE-F | 0.941687 | 0.022971 | 0.065042 | 183.391 | 8472.061 | 12495.876 | 9.354 | 142 |
| 2 | MOPSO-F | 0.936414 | 0.007256 | 0.066416 | 177.009 | 8508.512 | 12494.125 | 9.223 | 126 |
| 3 | HMOPSO-QLS-F | 0.923411 | 0.040717 | 0.072098 | 179.446 | 8546.590 | 12520.878 | 8.596 | 177 |
| 4 | NSGA-II-F | 0.858067 | 0.012952 | 0.089297 | 205.688 | 8536.642 | 12591.791 | 6.522 | 14 |
| 5 | SPEA2-F | 0.845045 | 0.018081 | 0.098856 | 214.916 | 8613.201 | 12508.183 | 12.581 | 16 |
| 6 | MOHEADE-F | 0.718220 | 0.016433 | 0.160935 | 214.918 | 8799.072 | 12731.049 | 7.538 | 1 |
| 7 | ZHANGBO-A4 | 0.701958 | 0.031623 | 0.188567 | 201.327 | 8667.943 | 12827.335 | 11.399 | 4 |
| 8 | HMOPSO-QGS-F | 0.521563 | 0.063177 | 0.324841 | 203.009 | 8979.909 | 12838.470 | 4.809 | 0 |

前三种算法贡献了参考前沿 445/480 点（92.7%）。A4 只贡献 4 点，HMOPSO-QGS-F 贡献 0 点。因此当前统一 reference 并未偏袒 A4；恰恰相反，A4 的 IGD 较差是真实诊断信号。

## 5. A4 与 HMOPSO-QGS-F

A4 相对基线的五种子描述性结果：

- HV：胜 4、负 1；median 提升 34.59%。
- IGD：median 降低 41.95%。
- Cmax 极值：只胜 1/5；median 降低 0.83%。
- TEC 极值：胜 4/5；median 降低 3.47%。
- TWC 极值：胜 4/5；median 降低 0.09%。
- 双向覆盖中位数：C(A4,QGS)=0.5917，C(QGS,A4)=0.0187。
- 运行时间中位数：A4 约为基线的 2.37 倍。

结论：三个创新点组合对“原基线”有明确的多目标改善信号，主要来自 TEC、TWC 和前沿覆盖；但 Cmax 改善不稳定，不能声称 A4 稳定改善三项目标。

## 6. A4 与其他结构化适配器

- A4 对 HMOPSO-QLS、MOPSO、MOPSODS-DE、NSGA-II、SPEA2 的 HV 均为 0胜5负。
- A4 对 MOHEADE 为 2胜3负。
- MOPSO 对 A4 的逐 seed 覆盖中位数为 0.9435；MOPSODS-DE 为 0.9627；HMOPSO-QLS 为 0.9159。
- A4 的 HV 标准差 0.0316，约为均值的 4.5%，不属于失控波动，但稳定落后于前三名。

这说明当前 A4 不是“稍微输一点”，而是在 50k FE 的经验前沿覆盖上存在明显收敛差距。

## 7. A4 机制是否真实触发

五次 A4 运行均观察到：

```text
formalOuterCycles=2
formalQgRounds=100
qgSelections=400
qgTdUpdates=296
qpActions=5100
qpTransitions=2600
cfvfOffspring=10000
cfvfRepairs=0
archiveInsertions=200
CA-TA-Lite FE=360..470
formalLocalFE=39430..39540
dualQWarmup/P/G=49/26/25
pressureDiagnosisEvents=200
DSCR teacherUses=400, dominatedTeacherUses=0, DTUR=0
```

因此不是“创新开关没有打开”。但预算结构值得重点复核：约 79% FE 进入 formalLocalFE，而 CA-TA-Lite 只占约 0.72%–0.94%；整个 50k 预算只有 2 个 formal outer cycles 和 2 次 PDDR 事件。当前弱势可能来自预算调度/环境选择节奏，而不是单个创新点完全失效。这个判断是机制诊断假设，尚未由单变量实验确认。

## 8. 发现的问题

### 高优先级：比较算法来源边界

HMOPSO-QLS-F、MOPSO-F、MOPSODS-DE-F、MOHEADE-F、NSGA-II-F、SPEA2-F 均由 `V35P25DComparativeEngine` 重新实现。它们保留算法族结构和 Table 9 参数，但不是原论文作者源码的逐语句迁移。例如 MOPSO 使用统一 `ZhangBoBaselineUpdater` 和通用环境选择，SPEA2 使用简化强度适应度与通用截断。这些结果适合检查“当前搜索框架是否容易被基本算法击败”，不适合作为正式论文排行榜。

### 中优先级：初始种群元数据冲突

A4 与 HMOPSO-QGS-F 的 10 个 `status.properties` 中，`initialPopulationHash` 与 `initial-population.sha256`、`configuration.txt`、`run-record.csv` 不一致。后三者在同 seed 八算法间完全一致，说明实际公平输入仍可证明；但状态文件写入了 V35FairRunner 的另一种哈希口径。正式 Runner 应统一为一个规范字段，或明确命名两种哈希。

### 中优先级：验收字段不足

当前硬门证明了有限目标、精确 FE 和 Decoder 调用闭合，但没有独立的 `illegalSolutions`、`duplicateCandidateEvaluations`、`repairCount` 通用字段。A4 的 `cfvfRepairs=0` 可核实；六个结构化适配器会执行合法域修复，但没有计数。因此“非法解=0、重复评价=0、所有 repair=0”不能从当前证据完全重建。

### 低优先级：前沿重复点

5/40 个 front.csv 含 1–2 个目标重复行。指标实现会去重，结果未被改变，但正式证据应输出严格非支配且目标唯一的前沿。

### 低优先级：根目录报告未纳入逐运行清单

345 个逐运行文件哈希全部通过；根目录统一报告、总日志和 reference 文件依赖下载压缩包 SHA-256 保证传输完整性，但没有单独的总证据清单。正式实验应增加根目录 `evidence-sha256.tsv`。

## 9. 建议的下一步

1. 暂停扩大到 500k 或多实例正式矩阵。
2. 先修复状态哈希字段、前沿去重和通用异常/repair/重复评价计数。
3. 审计 50k 中 `formalOuterCycles=2`、`PDDR=2`、`formalLocalFE≈39500` 是否符合预期；这是最可能影响 A4 收敛速度的结构问题。
4. 只做低成本单变量诊断：固定同一 seed 与初始种群，比较当前预算调度和“增加环境选择频率/限制固定邻域预算”的版本；保持总 FE 不变。
5. 在正式论文对比前，为六个经典算法完成来源级验证，或者明确把它们称为“canonical structured adaptations”，不能直接称为原论文复现结果。
6. QMOEA 继续保持 `PENDING_SOURCE_VERIFICATION`，不得用相近类替代。

## 10. 可复核文件

- `results/independent-validation.json`：完整独立验收结果。
- `results/independent-rankings.csv`：独立排名与参考前沿贡献。
- `results/independent-a4-pairwise.csv`：A4逐算法胜负和双向覆盖。
- `results/metrics.csv`：原Runner逐seed指标。
- `results/reference-front.csv`：统一经验参考前沿。
- `results/stability-summary.csv`：原Runner五seed汇总。

本报告不构成统计显著性结论，也不升级 `sampled_reproduction_accepted` 或 `full_reproduction_accepted`。
