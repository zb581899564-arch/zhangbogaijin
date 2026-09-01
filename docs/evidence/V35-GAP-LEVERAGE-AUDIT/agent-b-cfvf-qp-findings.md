# Agent-B 审计报告：CFVF 与 Qp/个人教师链的可触达杠杆（H_CFVF_QP_GUIDANCE）

日期：2026-08-30（只读取证）。冻结 A4 = HMOPSO + CFVF + 双Q(BLOCK_FROZEN) + CA-TA-Lite，canonical 见
`docs/evidence/V35-PFC5-GAP-PROBE/04-v2-remote-500k-runs/sync/run-GAP500-A4-100_5_3_1-20260827/profile.txt`
（dscr=true/cfvf=true/qg=true/qp=true/caTaLite=true, dualQ.mode=BLOCK_FROZEN, directionalTeacherPool=false,
pddrSelectionMode=GLOBAL_ORIGINAL, localFeBudgetMode=DYNAMIC_BETA）。
本报告不推翻先前结论（teacher-identity 重选旋钮 1.12%、Qp 四动作与 Qg action0/1 确定性单例、唯一多候选点 = Qg action-2 锦标赛）。

## 0. 数据源对齐说明（重要）

- D1 实际为 4 个 A4 run（2 实例 x 2 seed：20260827/20260906），`04-v2-remote-500k-runs/run-records.csv` 中 A4 行恰 4 条，全部 actualFE=500000、utilization=1.0、terminationKind=EXACT_MAX_FE。任务描述的"4+4"按 D1 实况记为 2+2。
- 任务给定的 `V35-FC5-MIDHORIZON-250K/01-root-cause-analysis/remote-results/` 下不存在 telemetry-teacher-concentration.csv / telemetry-teacher-use-events.csv（该目录只有 FC5_250K_ROOT_CAUSE_REPORT.md、checkpoint-metrics.csv 等 13 个文件）。教师集中度遥测实际位于 `V35-FC5-MIDHORIZON-DIAGNOSTICS/18-final-2k-20k-50k-gates/`（A4-20k / A4-50k，scope=ALL_QG/ALL_QP/PREVIOUS_CACHE/HISTORICAL_CACHE/PERSONAL_ARCHIVE，无 TOURNAMENT scope，final 文件无 W1/W2 分窗）；W1/W2 分窗口径见 `V35-FC5-100JOB-TRANSFER/04-positive-negative-contrast/second-tier-100k-analysis-correction/teacher-concentration-report.md`。250K 根因报告的 FC5-T 正例是 100_2_4_1（非 50_2_4_1），下文按 100_2_4_1 对照。
- 更强证据：本次审计直接从 D1 的 `dscr-teacher-uses.csv`（12400 行/run，冻结 500k 口径）自算教师曝光集中度（teacherId 指纹频次），比 D2 的 20k/50k 代理更贴近 Gap Probe。

## 1. 预算占用（逐 seed，全部来自各 run 的 status.properties mechanismSummary）

| 字段 | 50_2_3_1-0827 | 50_2_3_1-0906 | 100_5_3_1-0827 | 100_5_3_1-0906 |
|---|---:|---:|---:|---:|
| fullEvaluations (actualFE) | 500000 | 500000 | 500000 | 500000 |
| cfvfOffspring | 310000 | 310000 | 310000 | 310000 |
| cfvfOffspring/actualFE | 62.00% | 62.00% | 62.00% | 62.00% |
| formalLocalFE | 175439 | 175328 | 173563 | 175380 |
| caTaLiteTest/Apply/FE | 10567/3894/14461 | 10730/3842/14572 | 13023/3314/16337 | 10668/3852/14520 |
| qpActions | 271800 | 271800 | 271800 | 271800 |
| qpTransitions | 136000 | 136000 | 136000 | 136000 |
| cfvfRepairs | 0 | 0 | 0 | 0 |
| archiveInsertions | 6200 | 6200 | 6200 | 6200 |
| qgSelections | 12400 | 12400 | 12400 | 12400 |
| pddrEvents | 62 | 62 | 62 | 62 |
| dualQWarmup/dualQP/dualQG | 382/1360/1358 | 382/1360/1358 | 382/1360/1358 | 382/1360/1358 |
| fixedNeighborhoodEvents | 269586 | 266537 | 172324 | 174280 |
| runtimeSubSwarmSizes | 20/40/20/20 | 20/40/20/20 | 20/40/20/20 | 20/40/20/20 |

预算守恒核对（逐 run）：100(初始种群) + 310000(CFVF) + formalLocalFE + caTaLiteFE = 500000 恰好成立（如 0827 50-job：100+310000+175439+14461=500000）。CFVF 占 62% 在两实例、两 seed 间零差异——它是调度量（pop=100 x 固定周期数），不是行为量；实例差异只体现在 fixedNeighborhoodEvents（50-job 比 100-job 高约 55%）与 CA-TA/LS 细分。qpActions/actualFE = 54.36%，qpTransitions = qpActions 的一半。

## 2. Qp/教师有效性与质量（dscr-summary.properties + dscr-teacher-uses.csv）

| 字段 | 50-0827 | 50-0906 | 100-0827 | 100-0906 |
|---|---:|---:|---:|---:|
| teacherUses | 12400 | 12400 | 12400 | 12400 |
| dominatedTeacherUses | 0 | 0 | 0 | 0 |
| dtur | 0.000000 | 0.000000 | 0.000000 | 0.000000 |
| validityChecks | 24792 | 24792 | 24792 | 24792 |
| replacements | 451 | 456 | 381 | 331 |
| scrr | 0.018191 | 0.018393 | 0.015368 | 0.013351 |
| PA archiveSize（passive-summary.properties） | 913 | 675 | 2180 | 663 |
| PA retentionRate | 0.001826 | 0.001350 | 0.004360 | 0.001326 |

- dscr-teacher-uses.csv dominated 行比例：0/12400（四个 run 全为 0，实测 grep 计数）——DSCR 教师从未以被支配身份被使用，dtur 通道在两个实例上都是空的，dtur 无法区分困难/正常实例。
- 两实例差异方向：100-job 的 replacements（331/381 vs 451/456）与 scrr（0.0134-0.0154 vs 0.0182-0.0184）都更低——困难实例上教师替换/方向成功率更弱，但差距为 15-30% 量级，不是断崖。
- dscr-events.csv 每行 24792 = 12396 PREVIOUS + 12396 HISTORICAL（实测 uniq -c），stale=true 计数为 0，且 cacheType 只有 PREVIOUS/HISTORICAL 两种（无 TOURNAMENT 行；与 V35DscrTeacherCache.CacheType{PREVIOUS,HISTORICAL} 枚举一致，DSCR 观测器只记录两个缓存，action-2 锦标赛选出的教师以 prev/hist 形式落账）。

## 3. 教师曝光集中度

### 3.1 D1 原生 500k 口径（本审计自算：dscr-teacher-uses.csv 全体 12400 行按 teacherId 指纹频次）

| run | uses | unique | top1Share | top5Share | Hnorm |
|---|---:|---:|---:|---:|---:|
| 50_2_3_1-0827 | 12400 | 1398 | 0.0382 | 0.1390 | 0.7730 |
| 50_2_3_1-0906 | 12400 | 1060 | 0.0306 | 0.1274 | 0.7853 |
| 100_5_3_1-0827 | 12400 | 1134 | 0.0536 | 0.1956 | 0.7578 |
| 100_5_3_1-0906 | 12400 | 1094 | 0.1466 | 0.2431 | 0.7422 |

分组（每组 uses=3100）：

| group | 50-0827 top1/top5/Hn | 100-0827 top1/top5/Hn | 100-0906 top1/top5/Hn |
|---|---|---|---|
| G1_CMAX | 0.0881/0.3742/0.6967 | 0.2145/0.6161/0.5752 | 0.5865/0.7332/0.4344 |
| G4_BALANCED | 0.0652/0.2097/0.8204 | 0.0639/0.2023/0.8090 | 0.0987/0.1903/0.8288 |
| G2_TEC | 0.1529/0.4003/0.6890 | 0.0639/0.2445/0.7521 | 0.1087/0.3616/0.7147 |
| G3_TWC | 0.1355/0.4184/0.6849 | 0.1587/0.5577/0.6252 | 0.0994/0.3471/0.6992 |

要点：收缩高度集中在 G1_CMAX（Cmax 边界组）：100-job 两个 seed 的 G1 教师池比 50-job 收缩 2.4-6.7 倍（top1 8.8% -> 21.5%/58.7%），而 G4/G2/G3 无一致方向。这与 H_CFVF_QP_GUIDANCE 的"困难实例方向覆盖收缩"相容，且把收缩定位于个人/社会教师经 CFVF 差分驱动的 Cmax 方向；但 n=2 seed，不能证因果。

### 3.2 D2 代理口径（18-final-2k-20k-50k-gates，A4-20k-effective-20258，seed 20260901）

| scope | 100_5_3_1 (hard) top1/top5/Hn | 100_2_4_1 (positive) top1/top5/Hn |
|---|---|---|
| ALL_QG | 0.1100/0.3083/0.8258 | 0.0400/0.1500/0.9090 |
| ALL_QP = PERSONAL_ARCHIVE | 0.2164/0.8385/0.4738 | 0.2672/0.8891/0.4253 |
| PREVIOUS_CACHE | 0.1948/0.5065/0.8273 | 0.0867/0.3267/0.9140 |
| HISTORICAL_CACHE | 0.1254/0.4373/0.8492 | 0.0902/0.3985/0.9164 |

A4-50k-ON-final（100_5_3_1）：ALL_QG top1=0.0558/top5=0.2167/Hn=0.8347；ALL_QP top1=0.1690/top5=0.5470/Hn=0.6003。对比读法：Qg（社会）教师集中度是实例敏感的（hard 的 top1 是 positive 的 2.75 倍、熵更低），Qp（个人档案）教师集中度在两个实例上结构性都高（top5 约 84-89%）。W1/W2 分窗（teacher-concentration-report.md）：ALL_QG_TEACHERS A4 W1 top1 3-7%、W2 9-16%、top5 14-60%、Hn 0.67-0.85；真实方向代表教师（去重后）W1 top1 73.8%、W2 62.8-93.9% 高度垄断——该报告同时声明"与高频 Qp 使用同时出现，因果关系未验证"。

## 4. 可达性判定（核心）：CFVF/Qp 链逐注入点

| # | 注入点（单旋钮） | 源码位置（java-jmetal58/jmetal-algorithm/src/main/java/org/uma/jmetal/algorithm/multiobjective/ 下） | 可影响事件比例 | 触碰冻结语义？ | 判定 |
|---|---|---|---|---|---|
| K1 | CFVF 步长/概率：resourceCognitiveScale=resourceSocialScale=0.6（硬编码字面量）、resourceInertia=0.5、resourceExploration=0.05 | zhangbo/ZhangBoGlobalSearchConfiguration.forV35 L365-370（0.6 为字面量；DEFAULT_RESOURCE_INERTIA/EXPLORATION=L33-34）；消费点 zhangbo/ZhangBoCfvfUpdater.update L45-48(etaP/etaG)、L67-69(inertia)、L93-96/L249-252(exploration) | 100% cfvfOffspring = 310000 = 62% actualFE（唯一链级全覆盖旋钮） | 是——CFVF 语义禁区；且未暴露到 V35ProductionConfiguration.Builder，需改 forV35 源码（非配置级单旋钮）；exploration 合法域锁在 [0.02,0.10]（L206-210） | 不可达（禁区+需改码） |
| K2 | Qp COMPLEMENTARY 级联 qualityTolerance（默认 0.15）；min-cosine -> max-spacing -> fingerprint 级联本身无其他数值参数（精确 double 比较+指纹字典序；归一化 eps=1e-12 为常量） | zhangbo/ZhangBoQpCandidateSelector.complementary L90-130（tolerance 判据 L104）；zhangbo/ZhangBoQpConfiguration L18/L64/L104/L130/L173；注入通道 v35/V35ProductionConfiguration.Builder.qpConfiguration L354 + forV35 L354-357（非 null 覆盖生效）；canonicalText 新增 qp.qualityTolerance 行 -> configurationHash 漂移 | 不可观测：四 action 分 counts 只写在 qp-summary.properties（v35/V35FairRunner L720-722，仅 observation 模式输出），D1 四个 run 目录均无该文件（GAP500 探针 observation 关闭）。上界=qpActions 271800（54.4% FE），COMPLEMENTARY 实际份额无法用现有数据证明 >=10%；还要求 sorted.size()>1 且 quality 组 >=2 才非空 | 是——改的是"固定 Q 动作返回什么候选"，即 Q action 候选语义 | 不可达（禁区+覆盖率不可证） |
| K3 | Qp eps-greedy（epsilonStart 0.30 -> epsilonEnd 0.05 线性）与 mask | zhangbo/ZhangBoQpController.selectAction L470-487、explorationProbability 于 L147 调用；配置 L16-17。mask 为候选去重派生（CandidateSelector.build L48-59），非独立旋钮 | 100% qpActions（271800） | 是——动作分布属 Q action 语义（任务 4b 的答案：属于禁区）。同族：Qg eps=0.8（DEFAULT_Q_EPSILON，ZhangBoGlobalSearchConfiguration L30），注意 Qg 侧 draw<eps 为贪心、否则只探索 {0,1}，action-2 仅经贪心进入 | 不可达（禁区） |
| K4 | Qg action-2 锦标赛池（teacherPoolSize=10 + directionalTeacherPool 开关） | zhangbo/ZhangBoQgController.tournament L292-306、pool()/setDirectionalTeacherPool L318-345；V35ProductionConfiguration L317-318/L104-107 | A4 中 directionalTeacherPool=false、directionalPoolRequests=0（四 run mechanismSummary）-> 当前 0 覆盖；开启=教师身份重选 | 是——Q action 语义 + 教师身份重选（先前 v1 CAL 已关闭，覆盖 1.12%）；teacherPoolSize 仅在开启时生效 | 不可达（禁区+先前已关闭） |
| K5 | Qp 个人 lineage 档案容量（DEFAULT_CAPACITY=6） | zhangbo/ZhangBoPersonalArchiveConfiguration L9/L52-70（capacity 进四动作候选池） | 候选池上限，影响全部 qpActions 的 mask 分布 | 是——Q action 候选语义；且 D1 无 capacity 触顶证据 | 不可达（禁区） |
| K6 | Passive archive（PA）容量/生存策略 | v35/V35PassiveEvaluationArchive 全文（83 行）：无任何容量/淘汰参数，无界 Pareto 维护，类注释明确 no method participates in any search decision，唯一消费者是证据导出 | 0（不在 CFVF/Qp 引导链上） | 是——PA 语义；且结构上不存在该旋钮 | 不可达（无旋钮+不在链上） |
| K7 | mixture 20/40/20/20 | v35/V35SubSwarmMixture L16-18/L66-74（可配：G1/G2/G3 in [10,30]、G4 in [25,60]、5 的倍数、和=100）；V35ProductionConfiguration L350-353 | 100%（全 CFVF 后代分组） | 是——mixture 禁区 | 不可达（禁区） |
| K8 | LS 预算 DYNAMIC_BETA（betaMin 0.25/betaMax 0.65） | profile.txt L38-41；v35/V35LocalFeBudgetConfiguration | formalLocalFE 约 35% 预算 | 是——CA-TA/LS 预算分配属冻结 CA-TA 边界，且不在 Qp/教师引导链上 | 不可达（禁区+出链） |
| K9 | Qp reward 权重(2.0/1.0/0.5/0.25)、redundancyFloor 0.80、stagnationGenerations 3、convergence/diversityTolerance 1e-4 | zhangbo/ZhangBoQpConfiguration L19-26 | 影响 Q state/reward | 是——Q state/Q reward 禁区 | 不可达（禁区） |

## 5. 结论

H_CFVF_QP_GUIDANCE 在现有结构下无可达杠杆（在"不触碰冻结语义 + 现有证据可证 >=10% 事件覆盖率"双重约束下）：

1. 覆盖率 >=10% 的旋钮全部位于冻结区：CFVF 0.6/0.6/0.5/0.05（62% FE，硬编码于 forV35）、Qp eps（54.4% FE）、mixture（100%）。
2. 不触碰冻结区的残集无一可用：K2（qualityTolerance）覆盖率在 D1 不可观测（observation 关闭，qp-summary.properties 缺失），且改固定动作的候选返回即 Q action 语义；K4 教师身份重选先前已证 1.12% 并关闭；K6 PA 无参数且不在链上。
3. 数据侧仅支持"收缩存在且定位在 G1_CMAX 的教师/CFVF 差分链"（D1 500k：100-job G1 top1 21.5%/58.7% vs 50-job 8.8%；D2 20k：ALL_QG hard/positive top1 比 2.75x），不足以豁免任何冻结边界。
4. 唯一合规的后续动作不是注入旋钮，而是补观测：以 observation-on 重跑（V35ObservationOnOffEquivalenceTest 保证 on/off 行为等价）产出 qp-summary.properties 的 KEEP/DIRECTIONAL/EPSILON/COMPLEMENTARY.count 与 averageReward，才能把 K2 的覆盖率从"不可证"变为可证/可否证；若届时 COMPLEMENTARY >=10% 且总控愿意为其豁免"Q action 候选语义"，再预注册单变量诊断。

## 6. 关键数字清单（供总控复核）

1. cfvfOffspring=310000（=62.00% actualFE），四个 run 完全一致 — status.properties mechanismSummary。
2. 预算守恒：100+310000+formalLocalFE+caTaLiteFE=500000 逐 run 成立（formalLocalFE 175439/175328/173563/175380；caTaLiteFE 14461/14572/16337/14520）。
3. qpActions=271800（54.36% FE）、qpTransitions=136000、qgSelections=12400、archiveInsertions=6200、pddrEvents=62、dualQ 382/1360/1358 — 四 run 一致。
4. dtur=0.000000、dominatedTeacherUses=0/12400（实测 grep）、validityChecks=24792 — 四 run 一致；scrr 0.018191/0.018393 vs 0.015368/0.013351；replacements 451/456 vs 381/331。
5. dscr-events.csv：24792 行 = 12396 PREVIOUS + 12396 HISTORICAL，stale=true=0（实测）。
6. 教师集中度（自算 dscr-teacher-uses.csv）：50-job top1 0.0382/0.0306；100-job 0.0536/0.1466；G1_CMAX 组 top1 0.0881 -> 0.2145/0.5865。
7. PA：archiveSize 913/675 vs 2180/663；retentionRate 0.001826/0.001350 vs 0.004360/0.001326。
8. D2（A4-20k-effective-20258）：100_5_3_1 ALL_QG top1/top5/Hn=0.1100/0.3083/0.8258 vs 100_2_4_1 0.0400/0.1500/0.9090；ALL_QP 0.2164/0.8385/0.4738 vs 0.2672/0.8891/0.4253（路径替代：V35-FC5-MIDHORIZON-DIAGNOSTICS/18-final-2k-20k-50k-gates）。
9. 源码冻结值：CFVF 0.6/0.6/0.5/0.05（forV35 L367-368 硬编码）；Qp standard()：alpha 0.30、gamma 0.80、eps 0.30->0.05、qualityTolerance 0.15、redundancyFloor 0.80、weights 2.0/1.0/0.5/0.25；Qg eps=0.8；个人档案 capacity=6；mixture 校验域 [10,30]/[25,60] 步长 5 和=100。
