# Agent A — NORMAL 解析与 Reference 证据方法说明（V35 SOURCE-ATTRIBUTION-500K Phase A0）

日期：2026-08-31。性质：0-FE 只读取证（newFEConsumed=0，算法与既有文件零改动）。
授权边界：`docs/V35_SOURCE_ATTRIBUTION_500K_PHASE_A_PLAN.md` §1/§3.1/§3.2/§3.6；`AGENTS.md` §22–30。
本文件只说明 Agent A 的解析方法、证据路径与可重算命令。产物（本目录）：
`normal-control-resolution.csv`、`hard-case-binding.properties`、`hard-reference-binding.properties`、`normal-reference-binding.properties`。

## 1. 解析方法（无硬编码、全库检索定位）

### 1.1 instance-exposure-role-registry 定位
- 检索方式：`grep -rln "FINAL_TEST_RESERVED|VALIDATION_RESERVED"` 全证据树。
- 真实路径：`docs/evidence/V35-PFC5-PHASE0/02-instance-role-registry/instance-exposure-role-registry.csv`
  （sha256 `94f5a38fe60b8b3f8656cb69a71551ff87f62fdf18fa7208137239ef7c3fe191`；summary JSON 记录
  generatedAtUtc=2026-08-29T10:54:56Z、totalInstances=49、consumedFE=0）。
- 角色分布：CASE_SELECTED_DIAGNOSTIC_ONLY×1（100_5_3_1）、CONTAMINATED_DEVELOPMENT×17、
  VALIDATION_RESERVED×27、LEGACY_EXCLUDED×4；FINAL_TEST_RESERVED×0。
- 角色语义判定（写入 CSV 的 interpretation note）：冻结注册表没有裸 `DEVELOPMENT` 标签；开发类 =
  `CONTAMINATED_DEVELOPMENT`（roleReason="eligible for development/Race, never validation"，与
  AGENTS.md §22.3 角色分类一致）。§3.1 的操作化排除集是三个保留/诊断角色。候选宇宙 = 注册表中
  size=100 的全部 9 个实例。

### 1.2 accepted run ledger 定位
- 论文证据账本：`docs/PAPER_EVIDENCE_MASTER/run-ledger.csv`（481 数据行；100-job 行仅有
  100_2_3_1 与 100_5_4_1）。
- Master 正式矩阵：`docs/evidence/V35-MASTER-CAMPAIGN/01-registry/master-run-registry.csv` 仅有表头
  （4500 RunKey 计划未运行，`formalMatrixRunning=false`）；已接受组登记在
  `docs/evidence/V35-STAGE2-MASTER-V2/STAGE2_STATUS.properties`（acceptedGroupsAtPause=12、
  acceptedRunsAtPause=60、pilotInstance=100_2_3_1、pilotSeeds=20260808..20260819）与
  `docs/evidence/V35-STAGE2-PILOT-A0-A4-20260823/{PAUSED_BY_USER.properties,PILOT_DECISION.md}`。
- 多实例确认账本：`docs/evidence/V35-A2-A4-MULTIINSTANCE-CONFIRMATION/06-remote-analysis-import/acceptance-run-audit.csv`
  （60/60 ACCEPTED，100_2_4_1 A4 500k×5，jar=8DAD8F40…）。
- 背景汇总：`docs/evidence/V35-FC5-100JOB-TRANSFER/01-existing-100job-background/100job-background-summary.csv`
  （各 100-job 实例的历史 accepted 500k 运行数与 HV/IGD 中位数）。

### 1.3 逐候选筛选（详见 normal-control-resolution.csv）
五项冻结判据逐项核验；淘汰/存活证据全部来自上述账本与对比矩阵：
- 存活：`100_2_3_1`（12 条 accepted A4 500k，A0→A4 ΔHV+25.24%/ΔIGD+19.02%）、`100_2_4_1`
  （5 条 accepted A4 500k，A4 vs A2 ΔHV+13.29%/ΔIGD+35.55%）。
- 100_2_3_1 的 veto 排查：BP-PDDR 否决（`docs/evidence/V35-P26/fc6-bp-pddr/STAGE3_BP_PDDR_100JOB_VETO.md`）
  与 FC6 S20 的 IGD 门 FAIL 均为变体臂（BP-PDDR Build B、ORDER_SWAP、REGION_AWARE），对照基线
  才是 Current-A4；未发现针对 Current-A4（frozen jar 8dad8f40…）在该实例的任何 failure veto。
- 选择规则执行：两者 HV/IGD 同时 positive → 并列 → 字典序最小 → **NORMAL=100_2_3_1**。
  附注：100_2_4_1 的 20260901 seed 已被 V35A2A4-100_2_4_1-20260901-A4 消耗；100_2_3_1×20260901
  全库无记录（未消耗）。
- `normalControlResolved=true`（若无可合法解析的候选才写 false；本轮不适用）。

## 2. HARD 病例哈希（全部机器计算，sha256sum，2026-08-31）

```bash
cd /e/学习/李明哲-毕业材料/张博改进
sha256sum java-jmetal58/EADHFSP/100_5_3_1.txt \
          java-jmetal58/instance-extensions/v1/100_5_3_1.setup.txt \
          java-jmetal58/fatigue-parameters/v1/100_5_3_1.fatigue.txt \
          docs/evidence/V35-PFC5-PHASE0/fetched-remote/snapshots/100_5_3_1/seed-20260901.fourvec \
          docs/evidence/V35-PFC5-F1-FAILURE-REPLAY/03-raw-run/remote/profile.txt
```

结果：instance `2e88fa97…`、setup `4b49b780…`、fatigue `cf611bfb…`、snapshot `84d84523…`、
profile `5b3cc542…`。副本一致性批量验证（19/18/18 份副本逐一同哈希）：

```bash
find . -name "100_5_3_1.txt" -not -path "*/.git/*" -type f | xargs sha256sum | sort | uniq -c -w64
# 对 setup / fatigue 文件同理
find . -name "seed-20260901.fourvec" -not -path "*/.git/*" -type f   # 全库唯一物理副本
```

snapshot 头字段（problemConfigurationSHA256=`892c7c3f…`、initialPopulationSHA256=`179a82a3…`、
initialPopulationP8SHA256=`7c6f8b42…`）由 `head -25` 从快照头机器读取，并与 Failure Replay
`provenance.properties`、`initial-population.sha256`、`configuration.txt`（runId=V35PFC5F1-100_5_3_1-20260901-A4）
三方交叉一致。profile 来源 run：F1 Failure Replay（RUN_ACCEPTANCE=PASS 33/33，
verdict=FAILURE_CLASS_REPRODUCED）。frozenJarSha256=`8dad8f40…bad8b9` 与
provenance/configuration/STAGE2_STATUS/README 四处记录一致。

## 3. Reference 合同绑定（只登记，不重建不重算）

### 3.1 HARD（既有 Failure Replay Reference Contract）
- 合同文件：`docs/evidence/V35-PFC5-PHASE0/04-reference-contract/reference-contract.properties`
  （sha256 `ecdc5589…`，与 F1 `frozen-reference-analysis.properties` 的 referenceContractSha256 逐位一致；
  contractId=FAILURE_REPLAY_REFERENCE_CONTRACT_V1，pfrefPoints=757）。
- PFref：`docs/evidence/V35-PFC5-PHASE0/04-reference-contract/pfref-100_5_3_1.csv`
  （sha256 `4dc85dd4…`；ideal/nadir/HV(1.1,1.1,1.1)/normalization/EPS 均按合同原文登记）。
- HV/IGD 实现：`docs/evidence/V35-A2-A4-MULTIINSTANCE-CONFIRMATION/tools/python/analyze_confirmation.py`
  （sha256 `13692c03…`＝合同 historicalToolSha256）；F1 分析工具
  `docs/evidence/V35-PFC5-F1-FAILURE-REPLAY/tools/analyze_f1_frozen_reference.py`（`1e612843…`）。
- 排除歧义：GAP-PROBE 的 `pfref-100_5_3_1.csv`（`7fcf13fa…`，146 点）属 GAP_PROBE 合同，不是本合同。

### 3.2 NORMAL（100_2_3_1 已接受历史 raw fronts）
- 物理载体（冷归档，整包 sha256 `0202356f…`＝PAPER_EVIDENCE_ARCHIVE-MANIFEST 登记值）：
  `G:\ResearchArchive\ZhangBo-V35-Paper-Evidence-20260823\remote-campaigns\zhangbo-v35-stage2-master-v2-20260823.tar.gz`
- 12 条 accepted A4 500k raw front 逐条流式哈希（不落盘解包）：

```bash
T=/g/ResearchArchive/ZhangBo-V35-Paper-Evidence-20260823/remote-campaigns/zhangbo-v35-stage2-master-v2-20260823.tar.gz
for s in 20260808 20260809 20260810 20260811 20260812 20260813 20260814 \
         20260815 20260816 20260817 20260818 20260819; do
  tar -xzOf "$T" \
    "zhangbo-v35-stage2-master-v2-20260823/results/formal-a0-a4-4500/100_2_3_1/seed-$s/A4/front.csv" \
    | sha256sum | awk -v s=$s '{print s, $1}'
done
```

  12 条结果与 `run-ledger.csv` 对应行 frontHash 全部一致（明细见 normal-reference-binding.properties）。
- 聚合参考材料（本地哈希已验）：`reference-front.csv`（1979 点，`4b2c96b6…`）、`metrics.csv`
  （`52178a99…`）、`arm-summary.csv`（`a85c9cbe…`）。
- passive-archive.csv：Stage2 每 run 目录未导出该文件（只有 passive-summary.properties，已抽查
  seed 20260808/20260819 两条流式哈希）；已如实登记为 NOT_PRESENT_IN_STAGE2_ARCHIVE。
- 回灌禁令：`normalReferenceBackfillPolicy=FORBIDDEN`（新诊断 run 任何 checkpoint 前沿不得进入
  NORMAL reference/PFref/ideal/nadir）。

## 4. 消耗与边界
consumedFE=0；未运行任何算法；未修改任何既有文件；未上传；仅写入本目录 5 个专属产物。
