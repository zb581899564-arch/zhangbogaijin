# V35-P10.1 改善验证报告：Qg 方向 top-k 教师候选池

生成日期：2026-08-13
实例：`20_2_3_1`（seed `20260808`，population `100`，decoder `FM3`，`ShiftMode=NONE`，单族序列无关设置）
初始种群 SHA-256：`07311d31f51e6a71efcbf70435bf8924c02cb8be302023ddeed7f86c2ebca01b`
（与 V35-P10 历史证据同源，控制起点一致）

## 1. 机制（已实现的改善）

V35-P10 教师审计定位：Qg 动作 2 的二元锦标赛从**整个非支配候选集**随机抽 2 个，
新纪录在 100+ 候选里命中率约 `2/N`，教师滞后传导到 CFVF 与 G1 当前种群退化。

V35-P10.1 引入“方向感知社会候选池”（directional teacher pool）：

- 对 boundary 子群（G1_CMAX / G2_TEC / G3_TWC），动作 2 锦标赛先按方向目标升序取
  **top-k**（k=10；候选不足 k 时用全部），再在池内做原随机二元锦标赛（保留探索性与作者锦标赛语义）；
- G4_BALANCED 保持不变（PDDR 比较）；
- 显式开关 `directionalTeacherPool` + 可配 k，**默认关闭**——关闭时 `pool()` 原样返回候选列表，
  行为与改善前逐位一致（可回退）；
- `V35_FULL` 开启（k=10）；`V35_BASELINE` 与 `V35_QG0/QG1` 保持关闭（基线/QG 配对语义不变）；
  `V35_FULL_POOL_OFF` 为新增 FULL 消融臂（池关闭），用于逐位隔离证明。

修改文件：`ZhangBoQgController`（字段/setter/`pool()`/`tournament()`）、
`ZhangBoGlobalSearchConfiguration`（字段/setter/getter、`forV35` 接入）、
`V35ProductionConfiguration`（builder 字段、校验、`canonicalText`）、
`V35FairRunner`（`V35_FULL_POOL_OFF` 模式与 FULL 开关接入）、
`ZhangBoMOHPSOQ`（Qg 控制器创建后接入开关）。
新增测试：`ZhangBoQgControllerTest` 4 项（top-k 池含方向最优、候选不足 k 用全表、
G4 不受池影响、关闭与超尺寸池逐位一致）、`V35ProductionConfigurationTest` 4 项
（默认关闭、forV35 映射、无 Qg 拒绝、k=1 拒绝）。V35 定向回归 38/38 通过。

## 2. 验证设计

| 臂 | 模式 | 预算 | 池 |
|---|---|---|---|
| full-20k-pool-on | V35_FULL | 20 000 FE | 开（k=10） |
| full-20k-pool-off | V35_FULL_POOL_OFF | 20 000 FE | 关 |
| full-100k-pool-on | V35_FULL | 100 000 FE | 开（k=10，带 Cmax 审计） |

三臂同 seed、同初始种群；每臂独立问题实例；jMetal 全局随机源在运行边界重置。

**行为隔离（硬证明）**：`full-20k-pool-off` 的 `front.csv` 与 V35-P10 历史证据
`runs/full-20k/front.csv` **逐位一致**（测试断言通过）——池关闭路径与改善前 FULL
逐位等价，改善只存在于池开启路径；`full-20k-pool-on` 的前沿与历史证据不同（改善确实改变轨迹）。

## 3. 结果对照

### 3.1 100k：历史 P10 FULL vs P10.1 池开启（同起点单次）

| 指标 | P10 历史 | P10.1 开启 | 变化 |
|---|---|---|---|
| 前沿 minCmax | 195.244 | **181.501** | **−7.03%** |
| 前沿规模 | 203 | 225 | +22 |
| 终值 currentBestCmaxG1 | 250.375 | **192.495** | **−23.12%** |
| 全程 min currentBestCmaxG1 | ≈233 | 184.957 | 明显缓解 |
| 审计纪录数 | 56 | 48 | −8 |
| O1–O9 新纪录 | 50 | 41 | −9 |
| CFVF 新纪录 | 0 | 0 | 未变 |
| 被教学纪录数 | 9 | 7 | −2 |
| 教师滞后 max / mean（FE） | 22241 / 13971 | 22240 / 11901 | max 持平 / mean −14.8% |
| DSCR teacherUses | 800 | 800 | 持平 |
| DSCR dominatedTeacherUses（DTUR） | 0 | 0 | 门保持 |
| DSCR replacements / SCRR | 69 / 0.0433 | 84 / 0.0528 | 缓存更活跃 |

### 3.2 20k：池关闭 vs 池开启（同起点单次）

| 指标 | 池关闭 | 池开启 | 变化 |
|---|---|---|---|
| 前沿 minCmax | 208.528 | **196.162** | **−5.93%** |
| 前沿规模 | 65 | 79 | +14 |
| 前沿 minTEC | 8890.455 | 9030.005 | +1.57%（一维回吐） |
| 前沿 minTWC | 13575.604 | 13492.398 | −0.61% |
| 审计纪录数 | 16 | 19 | +3 |
| O1–O9 新纪录 | 10 | 13 | +3 |
| 终值 currentBestCmaxG1 | 244.874 | 236.103 | 改善 |
| 被教学纪录 / 滞后 | 3 / max 94 | 3 / max 94 | 持平 |

## 4. 对照三个核心发现的逐条核对（如实）

1. **CFVF 零新纪录**：仍为 0。top-k 池改变的是教师抽取路径，不改变 CFVF 是否产出
   checkpoint 级新全局纪录的路径；本开关**不解决**该症状。轨迹整体改善（minCmax −7%）
   说明教师质量提升经复合搜索传导，但 CFVF 新纪录口径下仍为零，后续按路线图处理。
2. **教师滞后**：被教纪录的最大滞后几乎不变（22241→22240）。根因：Q 对 G1 仍高频选择
   动作 0/1（保持旧教师），动作 2 锦标赛触发次数本身少；top-k 把单次命中率从 ~2/N
   提升到 ~2/k，但触发频率未变。mean 滞后 −14.8% 为次要信号，不宣称“解决滞后”。
3. **G1 种群退化**：**显著缓解**——`currentBestCmaxG1` 终值 250.375→192.495（贴近全局
   极值 181.5），全程最小值 233→184.96。这是本改善最明确的收益：当前 G1 种群不再与
   档案极值脱节 20% 以上。

**综合判定**：top-k 池是有效的定向改善（Cmax 极值 −7.0%、G1 退化显著缓解），但
CFVF 零纪录与教师滞后两个症状仅部分改善或未改善，不得表述为“全部解决”。

## 5. 前沿膨胀解释段（决策：不动机制，只解释）

用户观察到 100k 前沿 203→225、历史基线 84。前沿点数多**不等于**算法效果好：

- 本算法的 PDDR 环境选择**每代保留全部 rank-0（非支配）解**，三目标下两解互不支配
  的概率远高于两目标，rank-0 集合随搜索自然膨胀；
- 前沿规模是“保留规则 + 目标维度”的产物，不是质量指标；两个前沿规模不同的算法，
  其真实质量只能由 HV/IGD+（统一 reference）比较；
- 因此本报告**只报告前沿规模如实记录，不做“更多=更好”声明**；质量指标统一到
  `V35-P26`（pooled empirical reference、HV、IGD+、配对统计）执行。

## 6. 明确不做与遗留

- G1 物理槽位方向填充（不重排硬规则；先观察 top-k 后的退化是否缓解——已缓解，暂不改）；
- 前沿去重/截断（只解释）；
- 多 seed、500k FE、正式矩阵、PF-SDST——均未执行；
- 单 seed 单实例单次，本报告为工程诊断，不升级统计结论。

## 7. 证据清单

| 文件 | 说明 |
|---|---|
| `runs/full-20k-pool-on/` | 20k FULL 池开启（configuration/status/front/Cmax 审计/DSCR/CA-TA） |
| `runs/full-20k-pool-off/` | 20k FULL 池关闭（与 V35-P10 历史 front 逐位一致） |
| `runs/full-100k-pool-on/` | 100k FULL 池开启（带 Cmax 审计，48 条纪录） |
| `IMPROVEMENT_COMPARISON.csv` | 三臂指标汇总 |
| `evidence-sha256.tsv` | 全部证据文件 SHA-256 清单 |
