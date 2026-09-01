# V35-FC5-T 第二档 100k 筛查实验：远端执行与验收记录

日期：2026-08-25
负责：Luna B（远端执行与验收；结果/统计分析由 Luna C 负责，本文件不作任何科学结论）
结论状态：`SECOND_TIER_100K_ACCEPTED_PENDING_ANALYSIS`（未授权分析之前不写裁决）

---

## 0. 冻结实验规格（未改动）

```text
workpackage  = V35-FC5-T second-tier 100k screening
instance     = 100_5_3_1            # EADHFSP
arms         = A2_CFVF, A4_BUDGET_AWARE_CATA
seeds        = 20260901, 20260902, 20260903
population   = 100
MaxFEs       = 100000
runs         = 6                     # 3 seed x 2 arm，物理运行
runner       = org.uma.jmetal.runner.lc_psode.ZhangBoV35Fc5TransferRunner
diagnosticJarSHA-256 = E59698030AF2215994D4FD179AA2B1F26787A0F1239628543339477E119FA8B5
```

新远端目录：`/home/inspur/aicomp/zhangbo-v35-fc5-transfer-100k-20260825`（新建）。
权威只读来源：`/home/inspur/aicomp/zhangbo-v35-fc5-transfer-20260825`（50k 目录，仅读取，未修改/删除/续写）。

---

## 1. (a) SSH 与资源检查结果

- 主机：`100.127.244.47`，用户 `inspur`，SSH 批量模式连接正常（首次握手超时一次，重试后成功）。
- CPU/内存/磁盘：
  - `nproc` = 32
  - `free -g`：total=125, used=6, available=117, swap=127（空闲）
  - `df -h /home`：`/dev/sda2` 879G，可用 **258G**（远大于 10GB 门槛）
- 遗留 Java 进程：`ps aux | grep -i java | grep -v grep` → **无**（为空）。
  - 方法学注：使用 `grep -v grep` 会把任何包含子串 "grep" 的行（含 SSH 远端 `bash -c "ps…grep java"` 自身命令行）一并过滤掉，
    因此该检查不会被 SSH/bash 自匹配干扰，只反映真实 Java 进程；启动前确无本项目遗留 Java 进程。
- tmux 会话：仅存在无关的历史会话 `fc6-stage1..4`；**无** `v35-fc5-transfer-100k` / `v35-fc5-transfer-20260825` 同名会话。

资源检查：**PASS**。

---

## 2. (b) 启动硬门（7 项逐项）

| # | 硬门 | 结果 | 依据 |
|---|---|---|---|
| 1 | 资源检查（CPU/RAM/磁盘/无遗留 Java/无同名 tmux） | **PASS** | CPU=32，RAM 可用=117G，磁盘可用=258G>10G，无 java 进程，无同名会话 |
| 2 | 从 50k 权威目录复制诊断 Jar + instance/setup/fatigue + 3 seed 快照与 receipt | **PASS** | 从只读 50k 目录 `bin/`、`project-root/`、`input/snapshots/` 复制到新目录 |
| 3 | 逐文件 sha256 复核并写入 `INPUT_SHA256.tsv`/`RUNNER_SHA256.txt` | **PASS** | Jar=`e5969803…`（=E59698…）；快照与 receipt.snapshotSha256 逐项一致；instance 与 receipt.instanceSha256 一致 |
| 4 | 同 seed 的 A2/A4 读取**完全相同**的 `.fourvec` 快照 | **PASS** | 每个 seed 只有一份 `seed-<s>.fourvec`，A2/A4 共用 |
| 5 | 新输出目录不存在 | **PASS** | 启动时 `ls` 新目录 `output` 不存在 |
| 6 | 2k 预算探针（A2/A4）验证 jar 可执行/结构/非法解=0 | **PASS** | 见第 3 节 |
| 7 | 配对 seed 初群与 provenance 一致性 | **PASS** | 同 seed 的 A2/A4 的 v35/p8/snapshot 一致，且 instance/setup/fatigue provenance 一致 |

7 项全部 **PASS**。

---

## 3. (c) 探针结果（`PROBE_ONLY_NOT_IN_RESULTS`）

- 探针输出目录：`output/probe/…`（不入 `output/100k`，不进入正式验收与统计）。
- 受诊断 Jar 硬约束：`--max-fes` 只接受 `2000/50000/100000/250000/500000`，故唯一短预算为 **2000**。
- 在 2000 预算下（< Q-phase 预算 100×50=5000），runner 按阶段一致尾停仅物化初始种群（fullEvaluations=decoderCalls=100）。

| 探针 | seed | arm | status | fullEval | decoder | illegal | dup | initV35 与 receipt 一致 |
|---|---|---|---|---|---|---|---|---|
| probe-a2 | 20260901 | A2_CFVF | COMPLETED | 100 | 100 | 0 | 0 | 是（179a82…） |
| probe-a4 | 20260901 | A4_BUDGET_AWARE_CATA | COMPLETED | 100 | 100 | 0 | 0 | 是（179a82…） |

结论：jar 可执行、输出结构完整（26 项证据文件 + 全部必需工件）、非法解=0、初群哈希与 receipt 一致。
探针标记为 `PROBE_ONLY_NOT_IN_RESULTS`；未混入正式统计。

---

## 4. (d) 6 条正式运行状态与 actualFE

启动方式：tmux 会话 `v35-fc5-transfer-100k`（6 个窗口，每窗口一条独立 JVM）。
CPU 分配（taskset）：`0-3 / 4-7 / 8-11 / 12-15 / 16-19 / 20-23`；Java 11、`-Xmx4g`、`CUDA_VISIBLE_DEVICES=""`。
输出目录：`output/100k/100_5_3_1/seed-<s>/{arm}/`；同 seed 的 A2/A4 共用同一 `.fourvec` 快照。

| 运行 | seed | arm | cpuset | status | actualFE(=decoderCalls) | remainingFE | illegal | dup |
|---|---|---|---|---|---|---|---|---|
| 1 | 20260901 | A2_CFVF | 0-3 | COMPLETED | **96680** | 3320 | 0 | 0 |
| 2 | 20260901 | A4_BUDGET_AWARE_CATA | 4-7 | COMPLETED | **96025** | 3975 | 0 | 0 |
| 3 | 20260902 | A2_CFVF | 8-11 | COMPLETED | **96672** | 3328 | 0 | 0 |
| 4 | 20260902 | A4_BUDGET_AWARE_CATA | 12-15 | COMPLETED | **96025** | 3975 | 0 | 0 |
| 5 | 20260903 | A2_CFVF | 16-19 | COMPLETED | **96653** | 3347 | 0 | 0 |
| 6 | 20260903 | A4_BUDGET_AWARE_CATA | 20-23 | COMPLETED | **96025** | 3975 | 0 | 0 |

- **A4**：三条 actualFE 均 = **96025**（与规格预判一致），remainingFE=3975 < 5000。
- **A2**：actualFE ≈ 96680/96672/96653，remainingFE=3320/3328/3347，均 < 5000。

### 关于「A2 预期 100000」的重要说明
规格备注写「A4 阶段一致尾停 expected≈96025；A2 expected 100000」。实际 A2 在 100k 也发生**阶段一致尾停**（未用到 100000）。
这是**合法且符合冻结协议**的：
- 冻结的 `PHASE_CONSISTENT_BUDGET_TERMINATION`（AGENTS §18.1）：`remainingFE = MaxFEs - actualFE`，且 `0 <= remainingFE < qPhaseFE`（qPhaseFE = 100×50 = 5000）。A2 的 remainingFE=3320/3328/3347 ∈ [0,5000) —— **通过**。
- 协议明文**禁止**「补评价/填满预算」：不得为凑到 100000 而人为加评价或重跑。A2 保持 96680 等真实值。
- 规格验收硬门仅要求 `remainingFE < 5000`，不是 `actualFE==100000`；A2 全部满足。
- 同 seed 配对 `max(actualFE)-min(actualFE) < 5000`：seed{20260901,02,03} 的 A2−A4 = 655/647/628，均 < 5000 —— 通过。

故 A2 未到 100000 **不构成 FAIL**；它只是同 A4 一样按阶段一致尾停收敛。已如实记录，供分析方知悉（本文件不就此下科学结论）。

---

## 5. (e) 验收明细（每条 PASS/FAIL + 反向验证失败数）

验收脚本 `v35-fc5-transfer-100k-accept.sh`（建模于 50k `accept-50k.sh`，并按 100k 规格加强）。

逐运行判定（全部满足才算 PASS）：
`status=COMPLETED`；`0 < actualFE=decoderCalls <= 100000`；`remainingFE = 100000-actualFE < 5000`；`illegal=0`；`duplicate=0`；`front.csv` 非空且全有限；必需工件全存在；`evidence-sha256.tsv` 反向复核通过。

| instance | seed | arm | actualFE | remainingFE | illegal | dup | frontRows | frontFinite | evFiles | evMismatch | accept |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 100_5_3_1 | 20260901 | A2_CFVF | 96680 | 3320 | 0 | 0 | 168 | true | 26 | **0** | **PASS** |
| 100_5_3_1 | 20260901 | A4_BUDGET_AWARE_CATA | 96025 | 3975 | 0 | 0 | 241 | true | 26 | **0** | **PASS** |
| 100_5_3_1 | 20260902 | A2_CFVF | 96672 | 3328 | 0 | 0 | 160 | true | 26 | **0** | **PASS** |
| 100_5_3_1 | 20260902 | A4_BUDGET_AWARE_CATA | 96025 | 3975 | 0 | 0 | 224 | true | 26 | **0** | **PASS** |
| 100_5_3_1 | 20260903 | A2_CFVF | 96653 | 3347 | 0 | 0 | 103 | true | 26 | **0** | **PASS** |
| 100_5_3_1 | 20260903 | A4_BUDGET_AWARE_CATA | 96025 | 3975 | 0 | 0 | 287 | true | 26 | **0** | **PASS** |

必需工件：每条均完整生成并校验 —— `fc5-transfer-merge-rounds.csv`、`fc5-transfer-windowed-merge-overflow.csv`、`fc5-transfer-directional-representative-lifecycle.csv`、`fc5-transfer-archive-working-gap.csv`、`fc5-transfer-summary.properties`、`cmax-audit-curves.csv` 全部存在。

配对 seed 复核（6 条全部通过）：

| seed | initialPopulationHashV35 | initialPopulationHashP8 | snapshotSha256 | instance | instanceExtension | fatigue | 配对 |
|---|---|---|---|---|---|---|---|
| 20260901 | 179a82a382556638… | 7c6f8b425f278165… | 84d845233e332a66… | 2E88FA97… | E7E9FF7F… | 81CAD959… | **一致** |
| 20260902 | d99b848b230b849d… | 813fff4cfb9ba997… | a6dba976cb77ed29… | 2E88FA97… | E7E9FF7F… | 81CAD959… | **一致** |
| 20260903 | 4a96719b592173a8… | cb7e886df11e0a91… | 367378c54506c336… | 2E88FA97… | E7E9FF7F… | 81CAD959… | **一致** |

- 同 seed 两臂的 v35/p8/snapshot 哈希完全一致（`sameInitialPopulationWithinPair=true`）。
- 同 seed 两臂的 instance/setup/fatigue provenance（instanceSha256 / instanceExtensionSha256 / fatigueConfigurationSha256）完全一致（`sameProvenanceWithinPair=true`）。
- 配对 FE 差 < 5000（`pairFESpreadBelowQPhase=true`）。

**结论：6 条全部 PASS；反向验证失败数 = 0（evidenceMismatchFiles=0）；runFailures=0。**
汇总状态：`FIRST_TIER_100K_ACCEPTANCE.properties` → `status=ACCEPTED`。

---

## 6. (f) 远端产物文件清单与 SHA

### 6.1 根部 5 个远端产物（已下载本地留档，SHA 双向一致）

| 远端文件（`/home/inspur/aicomp/zhangbo-v35-fc5-transfer-100k-20260825/`） | sha256 | bytes |
|---|---|---|
| `FIRST_TIER_100K_TASKS.tsv` | `9fb73c9a69b2aab5e7cfbc92f812f98568b32d5fa13e739afc094f8191b4fbf8` | 243 |
| `FIRST_TIER_100K_ACCEPTANCE.csv` | `2ef8290e48f3f039af48883c2357b1c72b2f740da2a469189fad93a2342af63e` | 3201 |
| `FIRST_TIER_100K_ACCEPTANCE.properties` | `059d6ea2f3df6ac62165f64e120e29e45968af1ca91c0f8b5b08e593d66cd109` | 491 |
| `RUNNER_SHA256.txt` | `44a030c6690f3fbb2903df88fd5b92c2cae690aa0553e47cae97735b141a74f0` | 183 |
| `INPUT_SHA256.tsv` | `52c3dbde1871b82a518c5132c927c18f94e44e3290e7dc9583ef59686df5c373` | 1166 |

### 6.2 运行器与输入（远端持有）

- `RUNNER_SHA256.txt` 指向诊断 Jar：
  `sha256=e59698030af2215994d4fd179aa2b1f26787a0f1239628543339477e119fa8b5`（=E59698…，等），`bytes=48393864`。
- `INPUT_SHA256.tsv` 记录 instance/setup/fatigue + 3 seed 快照与 receipt 的逐项 sha256+bytes（该文件内容即输入完整性证据）。

### 6.3 输出目录结构（`output/100k/100_5_3_1/seed-<s>/{arm}/`，每 arm 26 项证据）

```text
output/100k/100_5_3_1/
  seed-20260901/A2_CFVF, A4_BUDGET_AWARE_CATA
  seed-20260902/A2_CFVF, A4_BUDGET_AWARE_CATA
  seed-20260903/A2_CFVF, A4_BUDGET_AWARE_CATA
```
每个 arm 目录含：`front.csv`、`status.properties`、`initial-population.sha256`、`configuration.txt`、
`fc5-transfer-merge-rounds.csv`、`fc5-transfer-windowed-merge-overflow.csv`、
`fc5-transfer-directional-representative-lifecycle.csv`、`fc5-transfer-archive-working-gap.csv`、
`fc5-transfer-summary.properties`、`cmax-audit-curves.csv` 及 `evidence-sha256.tsv` 等 26 项。
探针目录 `output/probe/…` 标记为 `PROBE_ONLY_NOT_IN_RESULTS`，不参与验收统计。

---

## 7. (g) 本地登记文件路径

靶目录：`E:\学习\李明哲-毕业材料\张博改进\docs\evidence\V35-FC5-100JOB-TRANSFER\03-transfer-telemetry\second-tier-100k\`

| 文件 | 说明 |
|---|---|
| `REMOTE_LAUNCH_AND_ACCEPTANCE.md` | 本文件：启动时间/硬门/探针/6 条验收/远端清单+SHA/结论状态 |
| `run-registry.csv` | diagnosticRunId / comparison / instance / seed / arm / budget / sourceRunId / status |
| `remote-artifact-map.csv` | 远端路径 → 本地留档路径 → sha256 |
| `FIRST_TIER_100K_TASKS.tsv` | 从远端下载，sha256 双向一致 |
| `FIRST_TIER_100K_ACCEPTANCE.csv` | 从远端下载，sha256 双向一致 |
| `FIRST_TIER_100K_ACCEPTANCE.properties` | 从远端下载，sha256 双向一致 |
| `RUNNER_SHA256.txt` | 从远端下载，sha256 双向一致 |
| `INPUT_SHA256.tsv` | 从远端下载，sha256 双向一致 |

`scripts/` 下留档（本地 + 已上传远端执行）：
`v35-fc5-transfer-100k-prepare.sh`、`v35-fc5-transfer-100k-launch.sh`、`v35-fc5-transfer-100k-accept.sh`、
`v35-fc5-transfer-100k-check.sh`、`v35-fc5-transfer-100k-collect.sh`。

---

## 8. 结论状态

`SECOND_TIER_100K_ACCEPTED_PENDING_ANALYSIS`

- 6 条正式运行全部 `COMPLETED`、`illegal=0`、`duplicate=0`、`remainingFE<5000`、必需工件齐全、
  `evidence-sha256.tsv` 反向复核全部通过（0 失败）。
- 同 seed 配对初群哈希与 provenance 一致；配对 FE 差 < 5000。
- A4 actualFE=96025（符合规格预判）；A2 actualFE≈96653–96680（阶段一致尾停，`remainingFE<5000`，符合冻结协议，
  未补评价/未重跑）。二者差异已在第 4 节说明。
- **未作任何科学结论**：结果/指标/统计/显著性分析由 Luna C 负责，本文件不写裁决。
