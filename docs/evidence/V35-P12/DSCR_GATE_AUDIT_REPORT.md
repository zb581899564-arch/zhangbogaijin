# V35-P12 验收报告：DSCR 机制门（DTUR=0 且无 post-action override）

生成日期：2026-08-13
验收标准：`DTUR=0` 且无 post-action override（`docs/ROADMAP.md` V35-P12 行）。
前置阶段产物（工程诊断，本报告不覆盖）：`DSCR_MECHANISM_GATE.md`、`TEST_LOG.md`（同目录，
D-062 时期，其"I1 完整链路仍未完成"状态已由 V35-P11 的 I1 链路复核收口）。

## 1. 验收结论

**通过。** 机制门由构造闭合，且已用测试钉子与双实例运行证据固定：

1. **DTUR=0**：20k FULL（20_2_3_1）与 5k FULL（I1 10_2_2_1）两次真实运行均为
   `dominatedTeacherUses=0`、`dtur=0.000000000000`、`dturDefined=true`；
   逐行验证：全部教师使用事件 `dominated=false` 且 `dominatorCount=0`（200/200、400/400）。
2. **无 post-action override**：教师缓存全部写点穷尽盘点（下表）证明不存在任何
   sanitize 之后重新安装被支配教师的代码路径；每次选择后立即有硬门抛异常兜底。

## 2. 教师缓存写点穷尽盘点

`previous`/`historical`（`ZhangBoQgController:25-26`，private final，无反射访问、
无自定义序列化钩子）的全部 9 处写点：

| # | 位置 | 分支 | 相对 sanitize 的顺序 |
|---|---|---|---|
| 1-2 | `ZhangBoQgController:62-63` | `select()` init（首次周期） | 后（sanitize 空操作后由候选列表直接初始化，随即被门校验） |
| 3 | `:85` | `select()` 每次 | 后：来源=刚清洗的 previous（动作0）/historical（动作1）/过滤后候选锦标赛胜者（动作2） |
| 4 | `:88` | `select()` historical 条件更新 | 后：来源同上三者之一，全部非支配 |
| 5-6 | `:101-102` | `selectGreedy()` init | 后（同 1-2） |
| 7 | `:116` | `selectGreedy()` 每次 | 后（同 3） |
| 8 | `:119` | `selectGreedy()` historical 条件更新 | 后（同 4） |
| 9 | `:323` | `sanitizeOne()` | 即 DSCR 清洗本身 |

周期顺序链（`ZhangBoMOHPSOQ.prepareOriginalQg:2206-2216`）：
`applyV35Dscr`（冻结快照→四角色 `sanitizeTeacherCaches`→过滤被支配候选，`:2206/2225-2265`）
→ `selectQgLeader`×4（`:2207-2210`：`select/selectGreedy`→`recordTeacherUse` 对照同一冻结快照
→ 若 `use.isDominated()` 则 `:2291-2293` 抛 `IllegalStateException("DSCR selected a strictly
dominated Qg teacher")`）。

**无 post-action override 的证据：**
- 写点清单穷尽（9 处全部列出，无第 10 处）；全仓无反射访问、无 override 标志/开关/TODO；
- 每个写点来源要么是刚清洗的缓存值，要么是过滤后（非支配）候选列表的锦标赛胜者；
  写 #4/#8 的条件更新只可能把三者之一拷入 historical，均非支配；
- 生产代码中每次缓存读取都发生在 sanitize 之后同一周期内（`select/selectGreedy` 仅由
  `selectQgLeader` 调用，而 `selectQgLeader` 仅在 `applyV35Dscr` 之后调用）；DSCR 开启但
  快照缺失时 `:2278-2280` 抛异常，杜绝"先选后洗"的次序缺陷；
- `Selection.getLeader()`（`ZhangBoQgController:461`）返回防御性副本，粒子飞行不能反写缓存；
- 账本不变式 `V35DscrTeacherCache:143-145` 拒绝"替换非支配教师"（`replaced && !stale` 抛异常），
  一旦出现即运行崩溃，不会静默通过。

## 3. 测试钉子（本轮新增）

| 测试 | 位置 | 钉住什么 |
|---|---|---|
| `sanitizeThenSelectNeverInstallsADominatedTeacher` | `ZhangBoQgControllerTest` | 清洗后从过滤候选列表选择：leader 与 select 后 PREVIOUS/HISTORICAL 均相对快照非支配；负向对照证明账本检测器会把被支配教师标记为 dominated（即任何越狱都会被硬门拦截） |
| `dscrRefreshInvariantRejectsNondominatedReplacement` | `ZhangBoQgControllerTest` | 账本拒绝非支配替换（不变式，此前无测试覆盖） |
| `V35P12DscrGateTest.dscrGateHoldsOnBothInstances` | `v35/V35P12DscrGateTest` | 双实例 FULL 真实运行：DTUR=0、逐行 clean、事件 CSV 与 summary 计数一致、每个替换行 stale=true |

## 4. 双实例门指标（GATE_METRICS.csv）

| 指标 | full-20k-20_2_3_1 | full-5k-I1-10_2_2_1 |
|---|---|---|
| 状态 / FE | COMPLETED / 20000 | COMPLETED / 5000 |
| teacherUses | 200 | 400 |
| dominatedTeacherUses | **0** | **0** |
| DTUR | **0.0**（defined） | **0.0**（defined） |
| validityChecks / replacements / SCRR | 392 / 40 / 0.1020 | 792 / 24 / 0.0303 |
| use 行数 / 全部 clean | 200 / 200 | 400 / 400 |
| event 行数 / 替换行数 / 替换行全 stale | 392 / 40 / 40 | 792 / 24 / 24 |
| 前沿规模 / minCmax | 79 / 196.162 | 64 / 45.667 |

（20k 数字与 V35-P10.1 的 full-20k-pool-on 同臂一致：replacements=40、SCRR=0.1020。）

## 5. 残余设计性质（如实登记，非违规）

- **首周期**：缓存为空时 sanitize 空操作，init 分支直接以过滤候选初始化，随后立即被
  每次选择硬门校验；
- **快照范围陈旧性**：只被"已不在候选列表中的未来解"支配的教师可在该周期存活，但
  每次选择都对照当前冻结快照即时校验（强于终值 DTUR 检查），且下一周期 sanitize 恒在
  select 之前再清洗；
- **计数口径**：账本对每次生产教师使用恰好记账一次（唯一调用点 `ZhangBoMOHPSOQ:2282`），
  DTUR 分母为选择次数而非粒子消费次数，口径与 D-062 冻结一致。

## 6. 证据清单

| 文件 | 说明 |
|---|---|
| `runs/full-20k-20_2_3_1/`、`runs/full-5k-I1-10_2_2_1/` | 两臂完整运行证据（configuration/status/front/Cmax 审计/DSCR 事件与使用表） |
| `GATE_METRICS.csv` | 门指标汇总 |
| `evidence-sha256.tsv` | 全部证据文件 SHA-256 清单 |
| `DSCR_MECHANISM_GATE.md`、`TEST_LOG.md` | 前置工程诊断阶段产物（保留，本报告为其验收续篇） |
