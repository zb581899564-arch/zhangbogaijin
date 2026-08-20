# P8.2 论文统一黄金示例与人工验算报告

状态：`completed`  
示例：`Illustrative Instance I1`（ESWA第四章10工件×2工厂×2阶段）  
粒子：`X0`，seed：`20260808`

## 结论

I1已冻结为全文唯一运行示例。一个显式粒子的FM3与FM0解码、20道工序人工重建、三目标及疲劳指标重建、固定谱系的Qg/Qp/CFVF/CA-TA/PDDR真实事件链和11组以上论文图均已形成来源锁定证据。

人工核算不调用Java decoder；共比较`1400`个工序字段和`22`个目标/诊断字段。最大工序绝对误差为`5.5511151231257827e-17`，最大目标绝对误差为`1.3877787807814457e-17`，均小于`1e-9`。

## X0程序结果

| 模式 | Cmax | TEC | TWC | Fmax | FE |
|---|---:|---:|---:|---:|---:|
| FM3 | 62.5724837688025 | 2161.27803895416 | 3960.67806691232 | 0.65086469814739 | 0 |
| FM0 | 60.6887052341598 | 2018.47880906972 | 3782.86787454969 | 0 | 0 |

FM0使用统一SUT和显式第一阶段MA/WA，但不启用疲劳；FM3增加疲劳累积、任务间自然恢复、工时反馈与后续阶段疲劳ECT选工。P3只核对共同的追加式公开语义，共`90`项资源、第一阶段时间和ETC/FIFO顺序字段全部通过；P3与生产FM0在后续阶段工人/资源分支上的60项数值差异单独登记，微调和右移也未混入生产结果。

## 进化和局部搜索证据

- 固定解释运行：population=10，子群物理槽位`[2,4,2,2]`，MaxFEs=5000；X0位于首个G4槽位，初始lineage=2。
- Qg、Qp、CFVF、容量档案、CA-TA和PDDR全部真实触发；CA-TA Test=`2044`、Apply=`1966`、局部完整评价=`4010`。
- 追踪谱系产生多个稳定后代，最终终态为`DELETED_AFTER_TRACE`。这是PDDR真实淘汰结果，不强行改seed制造存活。
- 注入仅用于验证的确定性单调时钟后，连续100次解释运行的9份核心事件/结果文件SHA-256全部一致（共882个追加哈希比较，加首轮/第二轮基准）。生产默认仍使用`System.nanoTime()`，时钟不参与目标或FE。
- 预热由FE边界决定；实际越过10%后，以观测到的外层代为锚点严格执行5代P-block/5代G-block，CA-TA局部FE不再延长区块。

## 图和母表

`08_figures`中的全部图只读取本目录冻结CSV/日志生成；每个图同时输出SVG、PDF和PNG，共12个图形stem。连续100轮独立复生成的36个图文件（12×SVG/PDF/PNG）全部一致，共完成3600次SHA-256比较；绘图脚本固定SVG哈希盐和导出元数据时间。`manual_calculation.xlsx`包含可复核公式列，CSV保留全精度。

## 验证

- jmetal-problem：46 tests，0 failure/error；
- P8.2/P9 exec定向：7 tests，0 failure/error；
- 双Q/Qg/CA-TA/基线定向：16 tests，0 failure/error；
- P8、CFVF、Qp、档案、邻域与CA-TA扩展回归：47 tests，0 failure/error；
- 五模块Java 8目标打包成功；两个新Runner class major version均为52；
- 作者原Problem/Algorithm/Builder/Runner四文件与P4.1冻结SHA-256一致；
- 解释性进化完成100次固定输入重放；重复输出逐轮哈希后立即清理，只保留882行哈希审计。论文图完成100轮、3600次导出哈希审计。单粒子解码的既有100次字节一致性测试继续通过。

## 边界

本阶段证明的是“同一I1/X0下公式、程序、人工核算和机制事件可追溯”，不证明算法统计优越性。P9正式矩阵、性能优化、消融和新500000 FE实验均未在本阶段启动。

```text
canonical_running_example_frozen=true
manual_decoder_validation_passed=true
objective_reconstruction_passed=true
fm0_fm3_regression_documented=true
single_lineage_evolution_trace_validated=true
paper_figures_source_locked=true
sampled_reproduction_accepted=false
full_reproduction_accepted=false
formal_matrix_started=false
```
