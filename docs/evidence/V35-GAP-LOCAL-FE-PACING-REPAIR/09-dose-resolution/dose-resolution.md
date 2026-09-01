# DOSE-RESOLUTION-50K — V35-LOCAL-FE-PACING (Agent C, Task A)

- 生成脚本: `09-dose-resolution/dose_resolution_50k.py`（全部数字由脚本从 16 条 run 文件计算，无手抄）
- 输入: `08-remote-50k/sync/seed-<S>/results/run-GAPL50K-<P>-<I>-<S>/`（16 条，验收 16/16 PASS，fairness 4/4 PASS，见 08 目录）
- 调度重建: `simulate_schedule(betaMax, 50000)` 逐字复用 `04-mechanism-analysis/build_gate.py`（预登记 §4-D2 闭合调度；不 import 以免执行其模块级 20k 代码）
- 预登记: `07-50k-preregistration/50K_PREREGISTRATION.md` §5 预测 / §7 门定义

## 1. 闭合调度重建与逐 run 验证（§4-D2）

| profile | betaMax | kind | cycles | finalFE | cumulative B_L | per-cycle avg | 预测(§5) |
|---|---|---|---:|---:|---:|---:|---|
| C0 | 0.65 | TAIL | 6 | 48269 | 18169 | 3028.17 | TAIL,6,48269,18169 |
| C1 | 0.55 | TAIL | 6 | 45359 | 15259 | 2543.17 | TAIL,6,45359,15259 |
| C2 | 0.45 | EXACT | 7 | 50000 | 14900 | 2128.57 | EXACT,7,50000,14900 |
| C3 | 0.35 | TAIL | 7 | 49036 | 13936 | 1990.86 | TAIL,7,49036,13936 |

逐 run 验证（导出 terminationKind/formalOuterCycles/totalLocalFE 与闭合模拟精确相等）：

| run | exported kind/cycles/totalLocalFE | predicted | verdict |
|---|---|---|---|
| run-GAPL50K-C0-50_2_3_1-20260907 | PHASE_CONSISTENT_TAIL_STOP/6/18169 | kind=PHASE_CONSISTENT_TAIL_STOP,cycles=6,total=18169 | MATCH |
| run-GAPL50K-C1-50_2_3_1-20260907 | PHASE_CONSISTENT_TAIL_STOP/6/15259 | kind=PHASE_CONSISTENT_TAIL_STOP,cycles=6,total=15259 | MATCH |
| run-GAPL50K-C2-50_2_3_1-20260907 | EXACT_MAX_FE/7/14900 | kind=EXACT_MAX_FE,cycles=7,total=14900 | MATCH |
| run-GAPL50K-C3-50_2_3_1-20260907 | PHASE_CONSISTENT_TAIL_STOP/7/13936 | kind=PHASE_CONSISTENT_TAIL_STOP,cycles=7,total=13936 | MATCH |
| run-GAPL50K-C0-100_5_3_1-20260907 | PHASE_CONSISTENT_TAIL_STOP/6/18169 | kind=PHASE_CONSISTENT_TAIL_STOP,cycles=6,total=18169 | MATCH |
| run-GAPL50K-C1-100_5_3_1-20260907 | PHASE_CONSISTENT_TAIL_STOP/6/15259 | kind=PHASE_CONSISTENT_TAIL_STOP,cycles=6,total=15259 | MATCH |
| run-GAPL50K-C2-100_5_3_1-20260907 | EXACT_MAX_FE/7/14900 | kind=EXACT_MAX_FE,cycles=7,total=14900 | MATCH |
| run-GAPL50K-C3-100_5_3_1-20260907 | PHASE_CONSISTENT_TAIL_STOP/7/13936 | kind=PHASE_CONSISTENT_TAIL_STOP,cycles=7,total=13936 | MATCH |
| run-GAPL50K-C0-50_2_3_1-20260914 | PHASE_CONSISTENT_TAIL_STOP/6/18169 | kind=PHASE_CONSISTENT_TAIL_STOP,cycles=6,total=18169 | MATCH |
| run-GAPL50K-C1-50_2_3_1-20260914 | PHASE_CONSISTENT_TAIL_STOP/6/15259 | kind=PHASE_CONSISTENT_TAIL_STOP,cycles=6,total=15259 | MATCH |
| run-GAPL50K-C2-50_2_3_1-20260914 | EXACT_MAX_FE/7/14900 | kind=EXACT_MAX_FE,cycles=7,total=14900 | MATCH |
| run-GAPL50K-C3-50_2_3_1-20260914 | PHASE_CONSISTENT_TAIL_STOP/7/13936 | kind=PHASE_CONSISTENT_TAIL_STOP,cycles=7,total=13936 | MATCH |
| run-GAPL50K-C0-100_5_3_1-20260914 | PHASE_CONSISTENT_TAIL_STOP/6/18169 | kind=PHASE_CONSISTENT_TAIL_STOP,cycles=6,total=18169 | MATCH |
| run-GAPL50K-C1-100_5_3_1-20260914 | PHASE_CONSISTENT_TAIL_STOP/6/15259 | kind=PHASE_CONSISTENT_TAIL_STOP,cycles=6,total=15259 | MATCH |
| run-GAPL50K-C2-100_5_3_1-20260914 | EXACT_MAX_FE/7/14900 | kind=EXACT_MAX_FE,cycles=7,total=14900 | MATCH |
| run-GAPL50K-C3-100_5_3_1-20260914 | PHASE_CONSISTENT_TAIL_STOP/7/13936 | kind=PHASE_CONSISTENT_TAIL_STOP,cycles=7,total=13936 | MATCH |

**scheduleValidation = 16/16 MATCH**（精确等式，比 08 目录验收表的 ±250 容差更严）。

## 2. 每窗口分配表（6 个公共窗口；C0/C1 共 6 窗、C2/C3 共 7 窗，公共=前 6）

| window k | u(open) | C0 alloc | C1 alloc | C2 alloc | C3 alloc | 严格递减 |
|---|---|---:|---:|---:|---:|---|
| 1 | 0.1020 | 1703 | 1694 | 1685 | 1675 | YES |
| 2 | 0.2361 | 1870 | 1818 | 1766 | 1716 | YES |
| 3 | 0.3735 | 2202 | 2057 | 1920 | 1790 | YES |
| 4 | 0.5175 | 2777 | 2452 | 2162 | 1901 | YES |
| 5 | 0.6730 | 3790 | 3085 | 2520 | 2056 | YES |
| 6 | 0.8488 | 5827 | 4153 | 3051 | 2263 | YES |

C2/C3 第 7 窗（超出公共窗口，截断于 MaxFEs）：C2 open=48204 close=50000 alloc=1796；C3 open=46501 close=49036 alloc=2535。

## 3. per-u 对齐理论分配 B_L(u)=floor(beta(u)/(1-beta(u))*5000)

| u | C0 | C1 | C2 | C3 | 严格递减 |
|---|---:|---:|---:|---:|---|
| 0.1 | 1702 | 1693 | 1684 | 1675 | YES |
| 0.2 | 1811 | 1775 | 1738 | 1702 | YES |
| 0.3 | 2002 | 1915 | 1830 | 1747 | YES |
| 0.4 | 2288 | 2122 | 1963 | 1811 | YES |
| 0.5 | 2692 | 2407 | 2142 | 1896 | YES |
| 0.6 | 3250 | 2788 | 2374 | 2002 | YES |
| 0.7 | 4025 | 3291 | 2668 | 2132 | YES |
| 0.8 | 5121 | 3960 | 3038 | 2288 | YES |
| 0.9 | 6737 | 4861 | 3503 | 2473 | YES |

## 4. 门判定

### G1 结构门

- 运行时读回（profile.txt `betaMax=` 与 `localFeBudget.betaMax=`，每 profile 4 条 run 完全一致）：C0=('0.65', '0.650000'); C1=('0.55', '0.550000'); C2=('0.45', '0.450000'); C3=('0.35', '0.350000')
- 逐值匹配配置 {C0:0.65, C1:0.55, C2:0.45, C3:0.35} 且严格降序: PASS

### G2 分配门（三视图）

- 50_2_3_1 累计分配上限: [18169, 15259, 14900, 13936] → 严格递减 PASS
- 50_2_3_1 每 outer cycle 平均: ['3028.17', '2543.17', '2128.57', '1990.86'] → 严格递减 PASS
- 50_2_3_1 每窗口匹配分配（6 公共窗）严格递减: PASS；per-u 对齐严格递减: PASS
- 100_5_3_1 累计分配上限: [18169, 15259, 14900, 13936] → 严格递减 PASS
- 100_5_3_1 每 outer cycle 平均: ['3028.17', '2543.17', '2128.57', '1990.86'] → 严格递减 PASS
- 100_5_3_1 每窗口匹配分配（6 公共窗）严格递减: PASS；per-u 对齐严格递减: PASS

### G3 消费门（localFeShare）

| instance | seed | C0 | C1 | C2 | C3 | C0>C3 | C0>C1>=C2>=C3 | 相邻降幅(pp) |
|---|---|---|---|---|---|---|---|---|
| 50_2_3_1 | 20260907 | 0.3764 | 0.3364 | 0.2980 | 0.2842 | Y | Y | 4.00/3.84/1.38 |
| 50_2_3_1 | 20260914 | 0.3764 | 0.3364 | 0.2980 | 0.2842 | Y | Y | 4.00/3.84/1.38 |
| 100_5_3_1 | 20260907 | 0.3764 | 0.3364 | 0.2980 | 0.2842 | Y | Y | 4.00/3.84/1.38 |
| 100_5_3_1 | 20260914 | 0.3764 | 0.3364 | 0.2980 | 0.2842 | Y | Y | 4.00/3.84/1.38 |

- 四组全部 localFeShare(C0)>localFeShare(C3): **PASS**
- 总体中位数（2 instance × 2 seed；各 profile 预算确定性故 2-seed 中位与总体一致）C0=0.3764 > C1=0.3364 >= C2=0.2980 >= C3=0.2842: **PASS**
- 相邻档降幅(pp): ['4.00', '3.84', '1.38'] → ≥1pp 的相邻档计数 = 3（需 ≥2）: **PASS**
- totalLocalFE 中位数 [18169.0, 15259.0, 14900.0, 13936.0] 严格递减: **PASS**（exact-stop 恒等式 caveat：C2 终止于 EXACT_MAX_FE，其 totalLocalFE 为截断值 14900，本批无并列，caveat 不触发）
- G3 整体: **PASS**

### G4 行为门

| instance | seed | outerCycles C0→C3 | cfvfOffspring C0→C3 | 非递减 |
|---|---|---|---|---|
| 50_2_3_1 | 20260907 | [6, 6, 7, 7] | [30000, 30000, 35000, 35000] | Y |
| 50_2_3_1 | 20260914 | [6, 6, 7, 7] | [30000, 30000, 35000, 35000] | Y |
| 100_5_3_1 | 20260907 | [6, 6, 7, 7] | [30000, 30000, 35000, 35000] | Y |
| 100_5_3_1 | 20260914 | [6, 6, 7, 7] | [30000, 30000, 35000, 35000] | Y |

- ≥3/4 公平组满足 outerCycles 或 cfvfOffspring 非递减: 4/4 → **PASS**

## 5. 聚合裁决

- 失败判据（仅 C0 分开而 C1/C2/C3 在 localFeShare 中位数完全并列）: C1=C2=C3 = FALSE → 未触发
- **DOSE_RESOLUTION_GATE = PASSED**
- 依据预登记 §7，剂量门通过，准许进入 B（双口径指标与配对响应）与 C（性能筛查）。

## 6. 状态块（机器可读）

```ini
[dose-resolution-50k]
scheduleValidation=16/16_MATCH
G1_structural=PASS
G2_cumulativeStrict[50_2_3_1]=PASS
G2_perWindowStrict[50_2_3_1]=PASS
G2_perUStrict[50_2_3_1]=PASS
G2_cumulativeStrict[100_5_3_1]=PASS
G2_perWindowStrict[100_5_3_1]=PASS
G2_perUStrict[100_5_3_1]=PASS
G3_shareC0gtC3=PASS
G3_shareMedianOrdering=PASS
G3_adjacentDropsGE1pp=3
G3_totalLocalStrict=PASS
G4_behaviour=4/4_groups
localFeShareMedian=C0=0.376411;C1=0.336405;C2=0.298000;C3=0.284199
cumulativeAllocation=C0=18169;C1=15259;C2=14900;C3=13936
DOSE_RESOLUTION_GATE=PASSED
```
