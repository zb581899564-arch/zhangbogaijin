# OBSERVER_IMPLEMENTATION_REPORT — V35 SOURCE-ATTRIBUTION Observer（Phase A 工作包2）

- 日期：2026-09-01
- 产物：`jmetal-algorithm-5.8-V35-SOURCE-ATTRIBUTION-OBSERVER-V4.jar`（43类，Java 8 major=52）
- 上游：Phase A0 冻结合同（observer-schema.md v1.0、source-taxonomy.csv、source-call-chain.csv）

## 1. 架构（继承V3影子模式，扩展4类新钩子）

| 组件 | 来源 | 说明 |
|---|---|---|
| 影ZhangBoMOHPSOQ | V3 11处patch + V4新增5处 | V3: 9评估站点显式Source + PDDR钩子；V4: Qg选择上下文、Qp选择标记、Q轮/循环context、PDDR钩子重定向到attribution observer |
| 影ZhangBoQpController | 冻结源码+1处patch | settle内PA更新后调onPersonalArchiveUpdate（fingerprint/survived/action） |
| 影V35PassiveEvaluationArchive | V3 patch | observeWithSource → attribution observer.onEvaluated（每评估一行，不按三元组折叠） |
| V35SourceAttributionObserver（新） | 本包 | 有界流式观察器：事件账本+PDDR轮账本+ND sample/forensic reservoir+生命周期注册表+B_0/checkpoint捕获+内存/GC采样 |
| V35ObserverGateRunner | V3 runner改造 | --telemetry OFF|ON；完整性门；内存采样（OFF/ON同方法）；原子输出 |

classpath：V4:FORMAL（V4优先加载影子类），冻结正式Jar逐字节不动。

## 2. 有界容量（写死进代码）

ndSampleCapacityPerSource=512、forensicReservoirCapacity=256、lineageIndexCapacity=4096、
parentRawCacheCapacity=512、maxRowsBeforeFlush=25000、worstCaseBytesPerRowResident=1024。
observerBoundedResidentCap≈2.2 MB、observerUnflushedBufferCap≈25 MB（常驻上界≈27 MB）。

## 3. 来源分类（严格四类）

GLOBAL_CFVF（含FINAL_EVALUATE并入+finalEvaluate=true二级字段）、CATA（Test/Apply分列）、
INHERITED_LS（INTER_FACTORY_LS+INTRA_FACTORY_VNS）、PARENT_CARRYOVER（N_eval=0生存层）。
INITIAL_POPULATION→firstLevelSource=NOT_APPLICABLE、attributionEligible=false（仅构造B_0）。

## 4. 反事实语义

ledger保留每条事件的真实source（Observer写入层不折叠）；producerSet在离线分析层构造
（threshold_recompute.py canonical_groups+producer_set）；firstAdmission字段仅描述性。

## 5. 构建过程事件（诚实记录）

1. V3→V4转换脚本多次因Python heredoc转义导致字符串截断/文件损坏（runner曾为0字节）→
   最终以generate_v4_runner.py一次性生成并逐锚点断言。
2. 内存字段字符串含literal newline（Java字符串截断）→以byte级替换修复为
转义。
3. countDataRows的header匹配（observedFE→actualFE）、B_0捕获空方法（改为从ledger前100行内联
   构造ND）、writeMemorySummary的guard位置（移出observerArmed使OFF/ON均有内存采样）、
   disarm清空在gate读取前（改为先快照completeness状态再disarm）——均为首次运行暴露的
   接线问题，已在2k OFF/ON中确认修复。

## 6. 机器状态

```ini
observerImplemented=true
observerJar= jmetal-algorithm-5.8-V35-SOURCE-ATTRIBUTION-OBSERVER-V4.jar
observerSchemaVersion=v35-source-attribution-observer-schema-v1
formalJarChanged=false
algorithmDecisionSemanticsChanged=false
```
