# V35-A3-D：A2→A3 最小因果拆分诊断裁决

## 范围与完整性

- 12 条独立 JVM 诊断运行均为 `COMPLETED`，每条均为 50,000 FE 和 50,000 次成功 Decoder 调用。
- 同一 seed 的四臂初始四向量哈希一致；三个 seed 的哈希彼此不同。
- 这是诊断性、配对、重新遥测运行；D0/D3 不计为独立论文样本，且不改变正式 Jar、PDDR、DOE 或正式矩阵。
- 共同参考前沿由全部 12 个最终前沿一次性严格 Pareto 过滤得到，点数为 198。

## 机制闭合

- D0：个人档案、Qp、双Q 均为 0。
- D1：三个 seed 均产生容量 6 谱系档案和确定性方向个人领导；Qp 与双Q均关闭。
- D2：三个 seed 均产生同步 Qg/Qp 动作；预热、P 块与 G 块均为 0。
- D3：三个 seed 均产生 10% 预热和 P5/G5 块冻结；`rho=0`。

## 预注册稳定退化门

规则：至少 2/3 seed 同时出现 HV 下降与 IGD 变差，且中位 ΔHV≤-2% 或中位 ΔIGD≥+10%。

- D0_A2_CONTROL→D1_PA_DIRECTIONAL：bad seeds=2/3；median ΔHV=-9.0179%；median ΔIGD=+102.5632%；stableRegression=True.
- D1_PA_DIRECTIONAL→D2_QP_SYNCHRONOUS：bad seeds=3/3；median ΔHV=-4.3230%；median ΔIGD=+30.1074%；stableRegression=True.
- D2_QP_SYNCHRONOUS→D3_A3_BLOCK_FROZEN：bad seeds=1/3；median ΔHV=-0.6580%；median ΔIGD=-3.0230%；stableRegression=False.

## 根因裁决

`a2_a3_root_cause = COMPOSITE_BLOCK_UNRESOLVED`

D1 共记录 30000 次确定性方向个人领导选择，其中 fallback=0。因此仅当 D0→D1 同时满足稳定退化门且出现 fallback/无有效领导时，才允许归因于 `PERSONAL_ARCHIVE_COLLAPSE`；本脚本不会把普通的性能波动误写为档案失效。

## 参考敏感性

`independent-reference-sensitivity.csv` 用每一相邻对照的六个前沿独立重建参考集；它与共同参考集的差异仅用于检查指标方向是否依赖共同参考集。主诊断仍使用共同参考前沿。

## 后续边界

本裁决不授权调整 Qp 奖励、个人档案容量、双Q时序、PDDR、子群配比或局部搜索顺序。若结论为 `COMPOSITE_BLOCK_UNRESOLVED`，下一步只能提出新的、单变量、另行批准的修复计划。
