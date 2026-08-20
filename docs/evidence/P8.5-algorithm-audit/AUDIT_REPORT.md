# P8.5 全链路算法审计报告

日期：2026-08-11  
范围：P1–P8.4及当前P9入口  
主方案：`HMOPSO_QGS_疲劳_全向量双Q_CA-TA-VNS_综合改进方案_v2.md`

## 结论

本轮确认并关闭了正式基线参数未真正驱动运行、PDDR非严格支配、P9资源系数记录与执行不一致、影子档案回归解释不足、语义标签混写等阻断项。三个创新点在规范FM3与`LEFT_RIGHT`共享解码语义上均已进入生产链，并在I1解释运行和`20_2_3_1`中程烟测中真实触发。

P8.5支持的是`engineering validated / scheme aligned`，不是论文完整复现。I0本人手算尚未提交，移位后的100k性能门、500000 FE和正式20次矩阵均未执行。

## 阻断项关闭情况

| 原问题 | 修复 | 直接证据 | 状态 |
|---|---|---|---|
| B0/B1只记录Table 9，运行时未完整执行 | `formal-hmopso-qgs-v1`统一驱动Fig.5/Fig.6更新、Qg、PDDR、关键工厂搜索、O1–O9及Q/LS循环 | `smoke-20k.csv`中B1：50个Q轮次、65次交换、65次插入、14770次O1–O9评价 | `ALIGNED` |
| 相同目标被PDDR当作支配 | 三目标`[0,1,6]`严格Pareto支配；相同目标互不支配 | `ZhangBoEvaluatedPddrSelectorTest`及100项算法定向测试 | `ALIGNED` |
| P9记录0.6、CFVF实际0.4 | 正式入口使用0.6；0.4仅保留为显式工程测试参数 | 正式参数一致性测试、20k配置摘要 | `ALIGNED` |
| 谱系档案影子模式日志不同 | 分离lineage元数据后比较搜索状态、随机事件、CFVF、Qg、PDDR和FE | `ZhangBoP6IntegrationSmokeTest` | `ALIGNED` |
| 全局标签固定为`fatigue_improved` | solution/config/result按FM0–FM3分别标记 | P8 profile与canonical problem测试 | `ALIGNED` |
| CA-TA Apply重复及真实时钟回放歧义 | Apply跨父调用；随机键加入epoch/ordinal/operator；正式真实时钟、审计确定性时钟分离 | 3×2000重放、I1 5000和FULL 20k | `ALIGNED` |
| P8.4 I1无左移接受 | 不放宽规则、不伪造图；继续等待I0本人手算对照 | I1：FCLS 0/14、FCRS 4/44 | `UNVERIFIED`（P8.4门） |

## 三个创新点核验

### 1. 疲劳恢复解码

- 四向量通过JS逆映射按工件身份读取FA/第一阶段MA/WA。
- FM0–FM3显式分派；FM3顺序为资源可用时间、自然恢复、PT/SET、疲劳倍率、完工、疲劳累积、更新时间线。
- 生产三目标存于七槽`[0,1,6]`，疲劳仍是诊断和引导量，不是第四目标。
- FCLS/FCRS每个候选均完整重传播且不增加外部FE；I1基础/移位测试通过，但I0人工门仍未完成。

### 2. CFVF与双Q

- CFVF通过JS逆映射生成FMW/MW/M/W动作，只更新第一阶段显式资源。
- 容量6个人档案只接收本谱系已评价分支；影子模式去除追踪字段后不改变搜索行为。
- Qg为四个`2×3`表，Qp为四个`16×4`表；10% FE预热后按已完成外层代执行5代P/G块，CA-TA局部FE不推进块边界。
- Qg/Qp奖励在局部搜索前结算；I1 5000 FE中Qg、Qp、CFVF和档案均有非零事件。

### 3. CA-TA-VNS

- O1–O13编号、四子群语义、六类瓶颈、Need 80/20选择、等预算Test及跨父Apply均由统一生产路径执行。
- O13在固定VNS和CA-TA路径共享自然恢复增益门。
- 局部候选携带父槽位、谱系、来源和预评价标记，不回写当轮双Q奖励、不重复计FE。
- I1 5000 FE记录2068次Test、1202次Apply和3270次局部完整评价；FULL 20k记录7946次Test和4954次Apply。

## 动态验收

| 验收 | 结果 |
|---|---|
| 34标签×I1×2000 FE | 34/34 PASS；27个唯一物理机制向量；7个精确别名 |
| B1/FULL 3次2000 FE确定性审计回放 | 初群、FE、front、Q表和机制摘要哈希一致 |
| I1 10粒子、5000 FE解释链 | Qg/Qp/CFVF/档案/CA-TA/PDDR全部真实触发；谱系存活 |
| `20_2_3_1` FULL 20k | 20000 FE，front 76，CFVF 7000，档案7000，Qg 280，Qp 6300，CA-TA 12900 |
| `20_2_3_1` B1 20k | 20000 FE，front 36，正式Q轮次50，关键交换/插入各65，O1–O9评价14770 |
| 共同输入 | 初始种群SHA-256均为`ffca83d43be7a67b8860ad5ccbd5e3d51c2a0f7880509879c59ecbeac0dc9ebe` |

## 测试与构建

- `jmetal-problem`：55 tests，0 failure，0 error，1 skip；skip为I1没有FCLS接受的诚实图例门。
- 张博算法定向：100 tests，0 failure/error。
- 张博Runner定向：23 tests，0 failure/error。
- Java 8六模块打包成功；关键class major version均为52。
- 最终打包命令为`mvn -q "-DskipTests" "-Dgpg.skip=true" "-Dmaven.javadoc.skip=true" package`，退出码0；跳过Javadoc是因为当前PowerShell环境的`JAVA_HOME`未暴露`javadoc`，不改变编译或算法字节码。
- `jmetal-core`兼容回归仍只有P1登记的3个旧错误，共651 tests、0 failure、3 errors、6 skipped。
- 完整algorithm模块另有作者遗留`NSGAIIBuilderTest`默认值漂移（主类3000、测试期待25000）；只读基线同样存在，且张博生产路径不引用该Builder。本轮按范围不修改作者遗留非生产代码。

## 仍未完成

1. I0本人手算副本及程序逐项对照；因此P8.4仍未完成。
2. `LEFT_RIGHT`当前语义下的100k性能门。
3. 500000 FE复验、六seed复验、正式20次矩阵和显著性统计。
4. 论文`sampled/full reproduction`验收。

## 当前标志

```text
formal_baseline_runtime_matches_configuration=true
strict_pddr_validated=true
decoder_identity_and_formula_validation=true
fatigue_shift_idempotence_validated=true
cfvf_and_dual_q_scheme_aligned=true
ca_ta_state_machine_validated=true
shadow_archive_isolation_validated=true
all_34_ablation_switches_exposed=true
20k_full_and_baseline_smoke_passed=true
sampled_reproduction_accepted=false
full_reproduction_accepted=false
formal_matrix_started=false
```

其中`fatigue_shift_idempotence_validated`是代码级门；P8.4面向I0本人手算的完整`shift_decoder_idempotence_validated`仍保持false。
