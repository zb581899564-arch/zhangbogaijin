# V35-P25E 论文算法忠实适配多Seed 50k纠正报告

- 运行：`40`（`5` 个 seed，每 seed 8 算法），实例=`20_2_3_1`。
- 共享边界：四向量、FM3、ShiftMode.NONE、单族序列无关SUT、目标适配`[0,1,6]`、初始种群、FE和指标。
- 搜索机制：每个算法独立；六种比较算法均不引用张博CFVF、Qp、DSCR、CA-TA-Lite或方向教师池。
- 初始种群哈希（同 seed 内 8 算法一致）：
    - seed `20260822`：`58c4c3b6be9869b3666595618cbd11ef9d076ebc4ff6f60e1321d0f63b2f4b00`
    - seed `20260823`：`822810ea710b7d58b3a51543ff345de7f7e24979c93939f13d7324797a7f8289`
    - seed `20260824`：`91439f037899eed0654b33e964f6a08baba9abd1e33003c5678d120700711061`
    - seed `20260825`：`8635ec0750692742409ddf0e4aca29572f1877080fe18e9f649ff046e9d11c0d`
    - seed `20260826`：`4177c16ff47401254d81a9cb39faa548b5b0af5c69cb724d1e7e2244432a1f35`
- 统一reference在全部运行完成后一次冻结，点数：`333`。
- 旧P25D已隔离，不进入本reference。QMOEA仍为来源待核验。

本轮是多seed纠正诊断；不构成显著性结论，也不代表论文最终优越性判断。逐seed指标见`metrics.csv`，按算法中位数见`metrics-median.csv`。
