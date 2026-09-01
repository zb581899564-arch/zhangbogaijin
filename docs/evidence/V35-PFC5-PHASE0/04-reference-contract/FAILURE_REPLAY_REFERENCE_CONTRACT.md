# FAILURE_REPLAY_REFERENCE_CONTRACT（冻结 v1）

- 合同ID：`FAILURE_REPLAY_REFERENCE_CONTRACT_V1`
- 冻结日期：2026-08-29
- 生成工具：`docs/evidence/V35-PFC5-PHASE0/tools/build_reference_contract.py`
  （内嵌历史验收工具 `analyze_confirmation.py` 的精确算法副本；原工具 SHA-256
  见 `reference-contract.properties: historicalToolSha256`）
- 机器可读版：`reference-contract.properties`（SHA-256
  `ecdc5589ab4d36a028a0d53e9fcdbfc40ee1e04864df929c2e5c035b4481235f`）
- 消耗FE：0；改变算法：否
- 适用范围：F1、F2、F3 及其全部 checkpoint，唯一 reference 基准，冻结后禁止更新

## 1. 适用实例与身份

```text
instance = 100_5_3_1
instanceSha256 = 2e88fa97a6f84af347a4603f04c387a65c8f9891bcab8ac6b70fdec622ea35cf
objectiveOrder = [Cmax, TEC, TWC]   objectiveSlots = [0, 1, 6]
```

## 2. PFref 构造（一次性，永不更新）

```text
basis    = 10 份历史 500k raw front（A2/A4 × seeds 20260901..20260905，
           全部经 SHA-256 反向核验，见 historical-reference-inputs.csv）
recipe   = raw-objective 精确去重（sorted set）→ 严格 Pareto 非支配过滤
           （3维最小化，相等不构成支配，EPS = 1e-12）
result   = 757 点，canonical SHA-256 = 4dc85dd4fa3c7824ed2bf302b648355df796be7f15375db84047d23c4de683da
验证     = 与历史验收存档 reference-fronts/100_5_3_1.csv 集合完全一致（pfrefMatchesHistoricalSaved=true）；
           输入行随机打乱后重建结果逐位一致（orderIndependent=true）
```

冻结文件：`pfref-100_5_3_1.csv`（canonical 序列化：去重后按 (Cmax,TEC,TWC) 字典序，
`%.17g` 格式）。

## 3. 归一化与指标实现

```text
ideal  = PFref 各目标最小值   = [755.144349612787, 110254.658096021, 286005.04205418704]
nadir  = PFref 各目标最大值   = [1483.1764464620087, 129014.9215496972, 361084.4776851587]
normalization = (x - ideal)/(nadir - ideal)，span 下限 EPS=1e-12；ideal/nadir 只来自冻结 PFref
HV     = 归一化空间参考点 (1.1, 1.1, 1.1)，值截断到 [0, 1.1]；
         实现 = analyze_confirmation.py hypervolume 的精确副本（x-sweep + yz_union，EPS=1e-12）
IGD    = 对归一化 PFref 的平均最近欧氏距离（实现为同一工具的精确副本）
```

**禁止**：50k 构造一份 PFref、100k 再加入新解、250k 更新边界、500k 替换 reference。
F1/F2/F3 与全部 checkpoint 只用本合同这一份基准。

## 4. Gold 重算对照（历史值可复算性证明）

以冻结 PFref 重算全部 10 条历史 run 的 HV/IGD，与历史验收 `metrics.csv` 对照：

```text
HV  : max |Δ| = 0.0（逐位一致）
IGD : max |Δ| = 1.67e-16（abs），1.33e-15（rel）——浮点求和机器噪声级
冻结容差门：abs ≤ 1e-12 且 rel ≤ 1e-12 → goldGateVerdict = PASS
明细：gold-recalc-comparison.csv（20 行）
```

## 5. F1 失败门（预注册，产出后禁止修改）

```text
比较目标   = 配对历史 A2 终态前沿（seed 20260901，SHA-256
             75d8a44a71428274a591a1c6413ddac0cb7e7deb421da419a11b2d3196a204aa）
锚点值     = HV_histA2 = 0.810244195451609   IGD_histA2 = 0.057804242003353316（本合同重算值）
deltaHV    = (HV_fresh − HV_histA2) / HV_histA2
deltaIGD   = (IGD_histA2 − IGD_fresh) / IGD_histA2   （正 = fresh 更好）
FAILURE    ⇔ deltaHV < −0.05 AND deltaIGD < −0.20（联合条件，与 100-job 否决门同口径）
Cmax       = 不作失败门，仅作机制解释报告
舍入规则   = 不舍入；双精度全值；严格比较
缺数据规则 = fresh 前沿缺失/空/含非有限值/不可读 ⇒ RUN_INVALID，绝不判 FAILURE
```

## 6. Checkpoint 对齐规则（F2/F3 用，一并冻结）

```text
共同 checkpoint = 每个完成的 formal outer cycle 在 phase-consistent 原子边界上的名义点
actualFE        = lastCompletedAtomicBoundaryFE
接受条件        = 0 < actualFE ≤ MaxFEs 且 remainingFE < 5000
终止类别        = PHASE_CONSISTENT_TERMINAL（禁止为凑名义点拆分 partial Q phase）
t* 规则         = 最早的共同 checkpoint 使 F1 同一失败判据成立且下一共同 checkpoint 仍成立；
                  只有 t < t* 的稳定异常具备 root-cause-candidate 资格
```

## 7. 边界

- 本合同由 Phase 0（PFC5-1E）冻结；修改需用户明确批准并生成新版本号，禁止原地改写。
- 合同不授权任何运行；运行授权仅来自用户对 F1 预登记的单独批准。
