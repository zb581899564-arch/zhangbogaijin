# V35 Final A0--A4 消融依赖图

## 适用范围

本文件只定义 V35 Final 主线的**创新块级**消融语义。它不启动任何正式实验，也不把 2,000 FE 烟测当作性能结论。

所有臂共同固定：FM3、单一退化产品族、序列无关 `SUT[job][stage]`、`ShiftMode=NONE`、三目标 `[0,1,6]`、`GLOBAL_ORIGINAL` PDDR、`CA-TA-Lite → inherited LS` 的调用顺序、`rho=0` 和方向教师池关闭。种群容量采用已冻结的 `20/40/20/20`。

```text
A0  规范 HMOPSO-QGS 公平基线
 │  + DSCR
A1
 │  + CFVF
A2
 │  + 谱系个人档案 PA_i、Qp、P=5/G=5 硬冻结双Q
A3
 │  + 预算感知 CA-TA-Lite N1--N5 与共享 dynamic local-FE budget
A4
```

## 因果可解释性

| 配对 | 允许解释的增量 | 不可解释为 |
|---|---|---|
| A0 → A1 | DSCR 对实际 Qg 社会教师缓存的清洗 | 新教师机制、Q 表或额外 FE 的贡献 |
| A1 → A2 | CFVF 四向量认知—社会飞行 | Qp、PA_i 或 CA-TA 的贡献 |
| A2 → A3 | 谱系 PA_i、Qp 和 P=5/G=5 硬冻结双Q这一组相关机制 | 单独 Qp 或单独档案的因果效应 |
| A3 → A4 | 预算感知 Test-and-Apply CA-TA-Lite **与共享动态 local-FE 配额的完整第三创新包** | 纯 CA-TA-Lite 的单变量因果效应 |

因此 A0--A4 是一条预注册的递进链，而不是布尔幂集。不得从该链推导任何未运行的拆分组合。

## 排除项

`ORDER_SWAP`、`REGION_AWARE`、`BP_RESERVED_LEGACY`、方向教师池、压力阈值掩码、PF-SDST、任何 active Shift 与 `rho>0` 均不属于该消融链，也不得在 Final 论文结果中重新激活。

## 正式运行门

仅在下列事项全部成立时，才可另行批准 A0--A4 的正式 500,000 FE 消融：

1. `docs/V35_FORMAL_EXPERIMENT_ROADMAP.md` 中适用的 FC-8/正式矩阵门已闭合；
2. 用户以单独指令批准正式消融；
3. 每个 RunKey 的实例、seed、预算、初始种群和配置哈希均已冻结；
4. A4 的“不可生成半代时安全停机”口径被接受，并在全部臂中如实记录实际 FE。
