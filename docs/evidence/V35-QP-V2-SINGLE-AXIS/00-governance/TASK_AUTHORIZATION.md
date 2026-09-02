# V35-QP-V2-SINGLE-AXIS Phase B1 工作包治理记录

工作包ID：`V35-QP-V2-SINGLE-AXIS`（Phase B1：Qp-v2 单轴语义冻结、隔离实现与20k工程门）
执行日期：`2026-09-02`
授权来源：用户直接任务书（本会话），依据 Phase A G4 出口后"Phase B、Qp-v2……均须新的明确授权"条款。

## 1. 上游状态（Phase A 已确认）

```ini
SOURCE_ATTRIBUTION=G4_NO_ACTIONABLE_LEVER
OLD_A4_DIAGNOSTIC_CLOSED=true
SOURCE_LEVER_CANDIDATE=NONE
SOURCE_ATTRIBUTION_ROOT_CAUSE_CONFIRMED=false
SA_A2_CONDITIONAL_ELIGIBLE=false
```

## 2. 本工作包任务边界（按任务书逐条登记）

1. 唯一轴 `K=1,2,3,4`；第一硬门为**不得自行发明K语义**，须先从获批材料核查；
2. 语义唯一确定才允许：`V35QpV2Profile`、独立实验Runner、独立实验Jar、`javac --release 8`（major 52）、
   canonical text + configuration hash、全部改动限于 Qp-v2 实验路径；
3. 禁止同时改变：teacher lambda、PA capacity、tauQ、epsilon、Qp动作集合、Qp奖励、P5/G5时序、
   rho、CFVF、PDDR、CA-TA、betaMax、mixture；
4. K=1 精确等价当前A4（逐字段，不接受指标相近）；先本地2000 FE，再训练机20k OFF/ON 或 A4/K1 配对；
5. K1通过后才允许K2/K3/K4短程工程测试（20k仅为工程门）；
6. 20k通过后停止；不启动250k配置赛马。

## 3. Git 远端同步状态（只读复核，工作包第一步）

```ini
remote=origin (https://github.com/zb581899564-arch/zhangbogaijin.git)
origin/main=051877aa7f6f3f2ec47031f7d54acb9eb02036ca
local main =051877aa7f6f3f2ec47031f7d54acb9eb02036ca
remoteSyncState=IN_SYNC
pushPending=false
```

上次验收时远端停在 `01db6c6a…`；本次 `git fetch` 后确认远端已推进至 `051877aa`，
与 Phase A 本地最终提交一致。**此前登记的推送阻塞已解除**，无需 force-push/rebase/覆盖操作。

工作区存在一批未跟踪文件（`docs/evidence/V35-SOURCE-ATTRIBUTION-500K/07-sa-hard-500k/` 下约7项，
为历史 SA-HARD 验收遗留），按证据保全纪律**只读登记、不删除、不覆盖**：

```text
?? docs/evidence/V35-SOURCE-ATTRIBUTION-500K/07-sa-hard-500k/06-frozen-reference-analysis/
?? docs/evidence/V35-SOURCE-ATTRIBUTION-500K/07-sa-hard-500k/SA_HARD_ACCEPTANCE_REPORT.md
?? docs/evidence/V35-SOURCE-ATTRIBUTION-500K/07-sa-hard-500k/SA_HARD_DECISION.properties
?? docs/evidence/V35-SOURCE-ATTRIBUTION-500K/07-sa-hard-500k/evidence-sha256.tsv
?? docs/evidence/V35-SOURCE-ATTRIBUTION-500K/07-sa-hard-500k/failure-class-reproduction.csv
?? docs/evidence/V35-SOURCE-ATTRIBUTION-500K/07-sa-hard-500k/staging/
?? docs/evidence/V35-SOURCE-ATTRIBUTION-500K/07-sa-hard-500k/sync/
?? docs/evidence/V35-SOURCE-ATTRIBUTION-500K/07-sa-hard-500k/tools/
```

（注：该批文件属上一工作包会话遗留，是否入库由用户裁决，本工作包不处置。）

## 4. 冻结基线复核

```text
formalJar = docs/evidence/V35-FC5-MIDHORIZON-DIAGNOSTICS/26-final-runtime-jar-validation/formal-algorithm-8DAD8F40.jar
formalJarSha256 = 8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9  (实测，与冻结值一致)
```

其余冻结项（FM3 / ShiftMode=NONE / single family / sequence-independent SUT / mixture=20/40/20/20 /
PDDR=GLOBAL_ORIGINAL / CA-TA-Lite→inherited LS / 双Q P5/G5 / rho=0）本工作包零改动。

## 5. 本工作包实际执行范围

仅执行到**第一硬门（语义来源核查）**即停止。见
`../01-semantic-source-audit/SEMANTIC_SOURCE_AUDIT.md` 与 `../07-decision/`。
02-preregistration、03-implementation、04-local-tests、05-2k-equivalence、06-20k-engineering-gate
五个子目录**未创建**：工作包在语义门处 fail-closed，未到达这些阶段，创建空目录反而构成误导。
