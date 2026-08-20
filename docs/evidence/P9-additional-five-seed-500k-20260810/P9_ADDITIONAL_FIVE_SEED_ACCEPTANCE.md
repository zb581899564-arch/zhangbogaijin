# P9新增五seed 500000 FE实施与六seed验收

日期：2026-08-10  
性质：单实例六seed稳定性诊断，不是论文正式矩阵、显著性检验或完整复现。

## 1. 运行范围

- 实例：`20_2_3_1`；
- 新增seed：`20260809..20260813`；既有对照seed：`20260808`；
- 算法：`ZHANGBO-FULL`与`HMOPSO-QGS-F`；
- 两算法共享FM3疲劳问题、SUT、疲劳参数、种群100、500000 FE和逐seed相同初始四向量种群；
- 每个seed内部严格串行执行`FULL → HMOPSO-QGS-F → REPORT`；五个seed之间使用训练机互不重叠的CPU核并行；
- 未运行其他实例、其他算法、消融或正式20次矩阵。

## 2. 本地实现与构建

- 新增五seed正式Runner和六seed汇总Runner，科学参数不通过CLI开放；
- 相关定向测试共32项，0失败、0错误；
- jMetal 5.8六模块`clean package`通过，Java字节码major version为52；
- 上传fat jar SHA-256：`ddbcae12e8bc4b0d253719d3128dec39e6708acb6a37ecbc5f7208b25c771442`。

## 3. 训练机执行

- 远端目录：`/home/inspur/aicomp/zhangbo-java-p9-five-additional-500k-20260810`；
- CPU亲和性依次为`0-3/4-7/8-11/12-15/16-19`；每个JVM为`-Xmx4g`，未使用GPU；
- 五个脚本退出码均为0；FULL、基线和比较共15个状态文件齐全；
- 下载后逐一验证125个结果及日志文件，SHA-256不一致数为0。

## 4. 工程硬门

|项目|结果|
|---|---:|
|新增配对运行|5/5完成|
|FULL完整评价|每seed 500000|
|基线完整评价|每seed 500000|
|配对初始种群哈希|5/5一致|
|非法解|0|
|CFVF异常repair|0|
|FULL CFVF/档案/Qg/Qp/CA-TA|每seed均触发|
|基线 Qg/PDDR/O1–O9|每seed均触发|
|基线 Qp/档案/CA-TA|每seed均为0|

## 5. 六seed结果

|seed|信号|C(FULL,BASE)|C(BASE,FULL)|Cmax变化|TEC变化|TWC变化|
|---:|---|---:|---:|---:|---:|---:|
|20260808|PROMISING|0.962617|0.000000|-12.655%|-4.695%|-1.270%|
|20260809|PROMISING|0.925743|0.008011|-11.888%|-4.076%|-1.804%|
|20260810|PROMISING|0.766129|0.028829|-6.877%|-3.417%|-0.547%|
|20260811|PROMISING|0.974874|0.000000|-9.559%|-4.595%|-1.530%|
|20260812|PROMISING|0.983333|0.001669|-0.957%|-3.992%|-0.849%|
|20260813|PROMISING|0.970803|0.001595|-16.225%|-5.606%|-1.019%|

汇总结论为`CONSISTENT_PROMISING_SIGNAL`：

- 六个seed均为正向信号，无`INCONCLUSIVE`或`REGRESSION`；
- FULL最小Cmax、TEC、TWC均为6/6胜出；
- 中位`C(FULL,BASE)=0.966710`，反向中位为`0.001632`；
- FULL相对基线的最小Cmax、TEC、TWC中位变化为`-10.723%/-4.335%/-1.144%`；
- 疲劳超阈积分为5/6胜出，高疲劳比例为4/6胜出；疲劳专项指标不能描述为全seed一致改善；
- FULL wall-clock相对基线的中位倍率为`39.145×`。

## 6. 证据与边界

- 逐seed数据：`results/seed-*`；
- 训练机日志：`logs/`；
- 六seed明细：`six-seed-summary/per-seed-comparison.csv`；
- 六seed报告：`six-seed-summary/P9_SIX_SEED_STABILITY_REPORT.md`；
- 远端下载校验：`remote-result-sha256.tsv`。

当前结论说明正向信号并非由单个seed偶然造成，但仍只有一个实例，不能证明跨实例普遍性或统计显著性。保持：

```text
sampled_reproduction_accepted=false
full_reproduction_accepted=false
formal_20_run_matrix_started=false
ablation_started=false
```

本轮到此停止，等待用户决定是否扩大到代表实例或正式矩阵。
