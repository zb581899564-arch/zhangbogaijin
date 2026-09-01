# RunKey 契约（预登记，未运行）

## 1. RunKey 结构

```text
V35CAL-<instance>-<seed>-<configuration>
```

例：`V35CAL-100_8_3_1-20260907-C2_MEDIUM`

## 2. RunKey 必须绑定的字段

每个 RunKey 必须包含且仅由下列字段决定：

```text
configuration                      C0_CURRENT / C1_WEAK / C2_MEDIUM / C3_STRONG
lambda                             0 / 0.05 / 0.15 / 0.30
instance                           20_2_3_1 / 50_2_3_1 / 100_2_4_1 / 100_8_3_1
seed                               20260906 / 20260907
snapshotSha256                     由冻结 ZhangBoV35FormalInitialPopulationFreezeRunner 零 FE 生成
initialPopulationHashV35 / P8      同上
algorithmJarSha256                 冻结正式算法 Jar 的 SHA-256（正式身份锚点）
calibrationImplementationJarSha256 承载 Calibration 实现的 Jar 的 SHA-256（与上一项不同，必须分别记录）
profileHash                        armProfileSha256（A4 profile）
problemConfigurationHash           由冻结代码实测
MaxFEs                             250000
budgetProtocol                     PHASE_CONSISTENT_BUDGET_TERMINATION
```

**任何字段变化均形成新 RunKey。** 同一 `(instance × seed)` 的四个配置必须共用同一 `snapshotSha256` 与初群哈希。

## 3. 两个 Jar 身份必须分开记录

按任务 §四与 §七的要求：

```ini
formalAlgorithmIdentityAnchor=<8DAD8F40...>
calibrationDeploymentArtifact=<Calibration 实现 Jar 的 SHA-256>
algorithmDecisionSemanticsBoundTo=FormalAlgorithmIdentityAnchor
```

即：F2 审计确立的原则在此延续 —— **部署实体可以是 Calibration 实现 Jar，但算法决策语义的身份锚点始终是冻结正式 Jar**。实现后若两者语义出现偏差，C0 等价门必然失败，届时按 `CAL_IMPLEMENTATION_GATE=FAILED` 处理。

## 4. 禁止覆盖失败 attempt

```text
失败 attempt 的输出目录必须完整保留，不得覆盖、不得删除、不得伪装为成功。
重跑必须使用新的 RunKey（附加 -r2 / -r3 后缀），并保留前一次的全部证据。
```

## 5. 执行单元约束

同一 `(instance × seed)` 的 C0/C1/C2/C3 必须：

```text
· 使用同一显式初始四向量 snapshot
· 独立 JVM
· 独立 Problem
· 相同 MaxFEs
· 相同预算协议
· 相同 FM3 与问题配置
```

## 6. 清单

32 条 RunKey 见 `configuration-race-manifest.csv`（4 配置 × 4 实例 × 2 seed）。
其中所有只能由冻结代码产出的哈希字段当前为 `TO_BE_COMPUTED_BY_FROZEN_CODE`，**禁止手工推测或复制其它实例/配置的值**。
