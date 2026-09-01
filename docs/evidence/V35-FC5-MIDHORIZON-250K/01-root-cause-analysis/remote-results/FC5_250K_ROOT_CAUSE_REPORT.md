# V35-FC5-T 250k 根因分析报告

**裁决：`FC5_TRANSFER_NOT_CONFIRMED_AT_250K`。** 原FC-5的“ND候选膨胀→PDDR压缩→四方向代表利用断裂”没有在本次250k正/负对照中复现；它不能解释当前100-job退化。

> 这是对预注册H1机制的否证性诊断，不等于证明PDDR在所有情形都最优，也不授权删除CFVF、双Q或CA-TA-Lite。

## 1. 执行验收

- 运行：12/12 accepted；全部 actualFE=250000。
- 每条均观察到10个名义检查点的三类前沿，并在250000 FE命中真实终止快照。
- 本分析没有启动新训练、没有修改算法、PDDR或冻结参数。

## 2. 最关键证据

1. **没有ND overflow**：所有PDDR轮最大 strictNdCount=92，Nnd>100 的轮数=0。
2. **困难实例没有形成预注册要求的Roverflow强差异**：A4困难实例窗口中位=0.630，正例=0.592，差=+0.038。
3. **四方向即时保留未出现20个百分点断裂**：最大正例减困难实例差=+0.075。
4. **Cmax没有archive-working脱节**：A4困难实例窗口中位gap=0；正例=0。
5. **250k时困难实例尚未出现A4整体崩溃**：A4相对A2的中位ΔCmax=+2.50%、ΔHV=-8.29%、ΔIGD=-13.03%；同时HV/IGD变差seed=3/3。

## 3. 四方向生命周期（A4，全程，label-event口径）

|实例|方向|pool→next|next→next-cycle|next→teacher|teacher→improvement|
|---|---|---:|---:|---:|---:|
|100_2_4_1|ALL|+95.97%|+74.57%|+83.19%|+72.39%|
|100_2_4_1|E_C|+92.47%|+85.71%|+88.37%|+81.58%|
|100_2_4_1|E_E|+95.70%|+58.14%|+85.39%|+93.42%|
|100_2_4_1|E_W|+96.77%|+65.52%|+92.22%|+83.13%|
|100_2_4_1|E_B|+98.92%|+88.76%|+67.39%|+20.97%|
|100_5_3_1|ALL|+94.89%|+82.80%|+87.25%|+74.68%|
|100_5_3_1|E_C|+97.85%|+94.32%|+96.70%|+92.05%|
|100_5_3_1|E_E|+88.17%|+59.26%|+82.93%|+86.76%|
|100_5_3_1|E_W|+93.55%|+84.52%|+91.95%|+81.25%|
|100_5_3_1|E_B|+100.00%|+91.11%|+77.42%|+34.72%|

说明：`next→next-cycle`剔除了每条运行末轮右删失；按代表出生窗口统计的教师/改善属于cohort结果，不冒充改善发生窗口。

## 4. H1预注册门

|判据|结果|自动证据|
|---|---|---|
|H1.1 two consecutive 50k windows with Nnd>100 before degradation in >=2/3 hard seeds|FAIL|all runs maxNnd=92; roundsNndOver100=0|
|H1.2 hard median Roverflow exceeds positive by >=0.25|FAIL|hard=0.630000; positive=0.592000; delta=+0.038000|
|H1.3 at least one directional pool-to-next retention is >=20pp lower in hard case|FAIL|largest positive-minus-hard drop=+0.075269|
|H1.4 archive-working Cmax gap expands before representative loss and performance degradation|FAIL|hard median gap=0; positive median gap=0; hard terminal joint-worse seeds=3/3|

## 5. 根因边界与下一步

当前可下的最强结论是：`CANDIDATE_OVERFLOW_UTILIZATION_BREAK_NOT_SUPPORTED`。250k数据否定了把FC-5候选溢出链作为这两个100-job实例的已确认根因。

尚未被本实验单独拆分的备选方向依优先级为：CFVF规模化编辑产出、Qp/双Q协调、CA-TA与inherited LS预算分配，最后才是FM3景观。若继续，应另行预注册最小单变量诊断；不得直接修改PDDR，也不得恢复4500矩阵。

## 6. 口径限制

- 只有2个100-job实例、3个seed；结论限于预注册FC-5迁移假设。
- 运行只到250k，不能排除500k后才出现的另一种退化机制；但它已足够检验本次预注册的中程迁移链。
- `decisionArchiveFront`用于A2/A4性能轨迹；`observedFullFront`只用于发现审计，未混入正式指标。
- 生命周期按稳定指纹观测；个体退休不等于方向语义从种群消失。
