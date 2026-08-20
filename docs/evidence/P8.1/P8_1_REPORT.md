# P8.1 规范生产基线校正与重新验收报告

## 结果

P8.1已完成。正式生产基线不再调用李明哲遗留的`EDHHFSPW.calculate()`、`DefaultIntegerPermutationSolution`或作者巨型更新器；这些源码保持原哈希，只作为`A0_AUTHOR_DIAGNOSTIC`来源证据。

当前正式调用链为：

```text
ZhangBoP8EngineeringRunner
→ P8V3ExperimentRunner
→ ZhangBoCanonicalProblemLoader
→ ZhangBoCanonicalProductionProblem
→ ZhangBoMOHPSOQ + structured baseline/CFVF/Q/VNS components
```

## 已修复内容

- B0/FM0改为`deterministic_canonical`，显式SUT、第一阶段MA/WA和JS工件身份映射生效；
- FM1/FM2/FM3以明确模式区分累积、恢复和疲劳感知选工，不再依赖`r=0`隐式路由；
- 正式Solution按实例创建，不使用固定8阶段、静态资源域、默认实例或反射；
- B0真实启用结构化原GA更新、原Qg、评价后PDDR及O1–O9；
- 双Q预热后按完成的外层代执行五代P/G区块，局部FE不再推动切换；
- 普通FULL和P8 FULL进入相同CA-TA路径；Test、Apply预算、逐后代执行和O13恢复增益门闭合；
- 参数、SUT、UTF-8、实例哈希及solution semanticTag均执行严格校验；
- P8-v2整体保留为`legacy_pre_canonical_baseline`，不进入当前前沿。

## P8-v3验收

- 正式标签：34；实例：2；种子：3；记录：204；
- 状态：204 `COMPLETED`，0 `FAILED`，0 `NOT_EXPOSED`；
- 每个标签恰有6条记录；共同初始种群哈希无漂移；
- 完整评价范围：1942–2000，无超预算；
- 非法解0，CFVF异常repair 0；
- B7与FULL机制哈希不同，唯一差异为`vnsMode: TA_COST→TA_FAT_FULL`；
- 参考前沿只由P8-v3正式运行构造，未混入A0或P8-v2。

## 原始资料完整性

四个作者源文件SHA-256仍分别为：

- `EDHHFSPW.java`: `231CF9BC8D0DC3B1541157A42E7526DAE08B49CEE58329E249E1D75C0BAE27B6`
- `MOHPSOQ.java`: `33E0A56F2A854900723E998A82B6B8CB85C49FA2079C4E221938BF4283FDAB60`
- `MOHPSOQBuilder.java`: `71C07F9AB084C1F8F7CA17D7AF9166AC53011D95C2EBE45E4281C209871C92C5`
- `MOHPSOQRun.java`: `A8C32A4CD3BCAB821286BF8B571CFEECD2ED0C46B8B5D8926CA35E9374A45BF7`

## 验收状态

```text
canonical_production_baseline_validated=true
decoder_engineering_validated=true
fatigue_model_scheme_aligned=true
dual_q_block_freeze_scheme_aligned=true
ca_ta_engineering_validated=true
ca_ta_scheme_aligned=true
integration_engineering_validated=true
ablation_engineering_validated=true
sampled_reproduction_accepted=false
full_reproduction_accepted=false
```

本轮是2000 FE小规模工程验收，不是500000 FE正式实验，不含显著性检验，也不支持论文完整复现结论。P9继续等待用户单独批准。
