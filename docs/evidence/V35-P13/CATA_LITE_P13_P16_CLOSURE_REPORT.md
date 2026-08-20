# V35-P13–P16 收口报告：CA-TA-Lite 全上下文集成证据

生成日期：2026-08-13
验收标准（ROADMAP）：P13=N1–N5/24 上下文/Test-Apply-Re-test；P14=N3 确定性路由；
P15=N4 确定性路由；P16=N5 结构恢复契约。
前置阶段产物（保留，不覆盖）：同目录 `N3_N5_MACRO_AUDIT.md`。

## 1. 验收结论

**通过。** N1–N5 五宏邻域、24 上下文掩码与 Test/Apply/Re-test 机制均已实现且由
构造确定性成立；本轮补齐测试钉子与双实例运行时证据，并把两处冻结边界语义正式落定。

## 2. 机制状态（审计确认，未改生产代码）

| 包 | 验收项 | 状态 |
|---|---|---|
| P13 | N1–N5 五宏邻域（`V35MacroCandidateGateway`，N1/N2/N3/N4/N5 确定性构造，只改副本、无随机） | ✅ |
| P13 | 24 上下文 = 4 角色 × 6 瓶颈（`V35CaTaContext.contextCount()==24`），每瓶颈动作掩码 | ✅ |
| P13 | Test/Apply/Re-test：不完整测试→TEST；`CONSECUTIVE_APPLY_FAILURE_RETEST`（连续 3 次失败）、`APPLY_HORIZON_COMPLETE_TEST`（配额耗尽）、mask 变更即新 epoch | ✅ |
| P14 | N3 路由：SET 瓶颈→`SETUP_EDGE_SOURCE`，其余→`CRITICAL_SOURCE`；整包（JS+FA+MA+WA）随工件迁移、同厂约束 | ✅ |
| P15 | N4 路由：`WOR/MAC/SET/FAT/BAL_RESOURCE_ROUTE`；SET 按设置压力、其余按加工+设置压力选位；机器∈[0,machineCount)、工人∈合格集 | ✅ |
| P16 | N5=两部件动作（结构 JS 迁移 + 恰好一个资源动作），只改 JS/FA/MA/WA 四向量、永不改时间/移位状态；资源腿不可行时整候选丢弃（不泄漏 JS-only 变更） | ✅ |

## 3. 两处语义落定（写进 D-067）

1. **P14 family 源路由 = 空集合语义**：v3.5 正式线永久冻结
   `FamilyMode.DEGENERATE_SINGLE_FAMILY`（`V35ProductionConfiguration:45-48`），单族边界下
   不存在族区分，`route()` 合法 N3 路由集恰为 {`CRITICAL_SOURCE`, `SETUP_EDGE_SOURCE`}；
   family 路由**不实现**（实现即为死代码），由配置校验 + 本报告登记闭环。
2. **P16 N5 吞并 = 契约强制**：`mixed()`（`V35MacroCandidateGateway:269-275`）先做结构迁移、
   再做资源腿；两腿不可行时返回 `NO_APPLICABLE_ACTION`，JS-only 中间态被整体丢弃——
   这正是"只返回两部件可遗传变更"的契约，非缺陷。

## 4. 测试钉子（本轮新增）

| 测试 | 位置 | 钉住什么 |
|---|---|---|
| `n1SwapsFirstSameFactoryAdjacentPackage` | GatewayTest | N1 首对同厂相邻包交换 + 包一致性 |
| `n2RelocatesFirstCrossFactoryJobAndRebaselinesResources` | GatewayTest | N2 首个跨厂迁移 + MA=0/WA=合格首选的必要重基线 |
| `n3SetBottleneckRoutesSetupEdgeSource` | GatewayTest | SETUP_EDGE_SOURCE 路由（P14 缺口） |
| `n4SetFatBalRoutesRemainLegallyAssigned` | GatewayTest | SET/FAT/BAL 路由 + 资源合法性矩阵（P15 缺口） |
| `n5FallsBackWithoutResourceLegAndNeverReturnsJsOnly` | GatewayTest | 单机单工资源腿不可行 → NO_APPLICABLE_ACTION、JS-only 不泄漏（P16 契约） |
| `allTwentyFourContextsHaveLegalMasksAndDistinctStates` | V35CaTaLiteControllerTest | 24 上下文全扫掠：mask 非空、decide 动作∈mask、状态键独立 |
| `maskChangeStartsAFreshTestEpoch` | V35CaTaLiteControllerTest | mask 签名变更 → 新 TEST epoch |
| `applyHorizonRetestTriggersFreshTestEpoch` | V35CaTaLiteControllerTest | APPLY_HORIZON_COMPLETE_TEST 触发 |
| `V35P13P16CaTaLiteEvidenceTest` | v35 | 双实例运行时证据（下节） |

## 5. 双实例运行时证据（CATA_LITE_METRICS.csv，事件流容量 131072 保全文）

| 指标 | 20k（20_2_3_1） | 5k（I1 10_2_2_1） |
|---|---|---|
| 状态 / FE | COMPLETED / 20000 | COMPLETED / 5000 |
| caTaLiteTest / Apply / FE | 72 / 64 / 136 | 24 / 6 / 30 |
| 决策行（TEST/APPLY） | 100（36/64） | 20（14/6） |
| **掩码合法性（每行 actions ∈ 上下文掩码）** | **100/100** | **20/20** |
| action 行 == test+apply 计数 | 136 == 136 ✅ | 30 == 30 ✅ |
| 出现 reason | TEST、APPLY_BEST、APPLY_EXPLORE、**APPLY_HORIZON_COMPLETE_TEST** | TEST、APPLY_BEST、**APPLY_HORIZON_COMPLETE_TEST** |
| 出现宏（N1–N5） | N1、N2、N3、N4 | N1、N2、N3、N4 |
| N4 accepted（⇒ 上游自然恢复门通过） | 3 | 1 |
| 前沿 / minCmax | 79 / 196.162 | 64 / 45.667 |

如实登记：
- **N5 在两臂运行时均未被选中**（WOR/FAT 上下文未轮到 N5 或 APPLY 选了其他动作），
  其机制证据由单元层（gateway 两部件契约 + 掩码）承担；N5 运行时出现依赖搜索动态，
  不构成 P16 契约缺口；
- `CONSECUTIVE_APPLY_FAILURE_RETEST` 未在运行时出现（两臂均经 `APPLY_HORIZON_COMPLETE_TEST`
  重测），连续失败触发路径由单元测试钉住；
- N4 的 accepted 行与 P19 母表中 `CA_TA_LITE/N4=2` 纪录共同构成"恢复增益集成证据"链：
  accepted ⇒ `recoveryGain`（`ZhangBoMOHPSOQ:4295-4299` 自然恢复门）⇒ 角色接受。

## 6. 证据清单

| 文件 | 说明 |
|---|---|
| `runs/full-20k-20_2_3_1/`、`runs/full-5k-I1-10_2_2_1/` | 两臂完整运行证据（含全量 CA-TA 事件流） |
| `CATA_LITE_METRICS.csv` | 运行时指标汇总 |
| `evidence-sha256.tsv` | SHA-256 清单 |
| `N3_N5_MACRO_AUDIT.md` | 前置工程诊断（保留） |
