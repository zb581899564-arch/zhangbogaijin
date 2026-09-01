# F1 FAILURE REPLAY 预登记（只预登记，不启动）

- 预登记日期：2026-08-29
- 状态：`PREREGISTERED_NOT_STARTED`
- f1Started=false
- 授权状态：**未授权运行**。启动 F1 需要用户在本预登记之上单独明确批准。
- 上游依据：`docs/V35_POST_FC5_EXECUTION_MASTER_PLAN.md` §8–13、§43 STEP D/E；
  `AGENTS.md` §21.4；本阶段 `00-preregistration/PHASE0_PREREGISTRATION.md`

## 1. 身份定义

```text
replayKind   = HISTORICAL_STATE_FAILURE_REPLAY
说明         = 精确历史初始快照已恢复并核验（见 PFC5-1B 裁决），
               故 F1 有权声称 historical-state 复现；
               算法语义为当前冻结 A4（与历史确认运行同一冻结 Jar）。
algorithm    = A4_BUDGET_AWARE_CATA（冻结语义，禁止任何创新开关关闭）
formalJar    = jmetal-exec-5.8-jar-with-dependencies.jar
               SHA-256 = 8dad8f40266feeaa4cdb9b47dbe4e342d9064847df32c7f2933149b9b6bad8b9
instance     = 100_5_3_1（SHA-256 = 2e88fa97a6f84af347a4603f04c387a65c8f9891bcab8ac6b70fdec622ea35cf）
seed         = 20260901（历史失败类最小 ID，见 01-historical-failure-case/）
population   = 100
MaxFEs       = 500000
telemetry    = OFF
freshness    = F1 必须 fresh 运行；禁止直接复用历史 500k A4 run
               （复用条件——语义/Jar/配置/问题/快照/随机流/FE 合同全等——虽然文件身份
               已可核，但历史运行的随机流控制未登记，按主计划 §10 默认 fresh）
```

## 2. 冻结语义（全部继承冻结基线，逐项与 `f1-run-contract.properties` 一致）

```text
PDDR=GLOBAL_ORIGINAL            FM3 解码          ShiftMode=NONE
single family（DEGENERATE_SINGLE_FAMILY）        sequence-independent SUT
subSwarm mixture = 20/40/20/20   P=5 / G=5         rho=0
directionalTeacherPool=OFF      CA-TA-Lite → inherited LS
mixture/Pacing/rho/P5G5/Q 状态动作奖励/PA 容量/CFVF/CA-TA/PDDR 全部禁止修改
```

## 3. 输入绑定（详见 `f1-input-manifest.tsv` 与 `f1-reference-binding.properties`）

```text
snapshot 物理文件       = fetched-remote/snapshots/100_5_3_1/seed-20260901.fourvec
snapshot SHA-256        = 84d845233e332a6612e5dfe93c97cbbeef40c4ee05766cbfd0e9446bd3043769
initialPopulationHashV35= 179a82a3825566380ab6798aa898002d31565dad9d65802e57b295c2a4294c2d
arm profile（A4）       = profile.txt，SHA-256 = 5b3cc542dafc22c1a32f1c0994bae25ffef040f6bfdf2aa6090a42f86cfd79d1
reference contract      = 04-reference-contract/reference-contract.properties
                          SHA-256 = ecdc5589ab4d36a028a0d53e9fcdbfc40ee1e04864df929c2e5c035b4481235f
PFref（冻结，757 点）   = canonical SHA-256 = 4dc85dd4fa3c7824ed2bf302b648355df796be7f15375db84047d23c4de683da
SUT/疲劳/问题配置哈希   = 见 manifest（与正式 manifest 及历史 provenance 同值）
```

## 4. 预算与终止协议

```text
formalBudgetSemantics = PHASE_CONSISTENT_BUDGET_TERMINATION
接受条件             = 0 < actualFE(=decoderCalls) ≤ 500000 且 remainingFE < 5000
禁止                  = 为凑满 500000 拆分 partial Q phase；补评价；改 Q/LS 参数填预算
actualFE 必须如实报告
```

## 5. 失败判据（冻结，产出后禁止修改）

对 fresh A4 终态前沿（本合同 reference 下）：

```text
HV_fresh、IGD_fresh 由冻结 PFref/ideal/nadir/实现计算
deltaHV  = (HV_fresh − 0.810244195451609) / 0.810244195451609
deltaIGD = (0.057804242003353316 − IGD_fresh) / 0.057804242003353316
FAILURE_REPRODUCED ⇔ deltaHV < −0.05 AND deltaIGD < −0.20
```

- Cmax 不是失败门；仅随附报告（历史现象允许 Cmax 改善而 HV/IGD 恶化）。
- 舍入：无；缺数据：fresh 前沿缺失/空/非有限 ⇒ `RUN_INVALID`，绝不判 NOT_REPRODUCED。
- reference 不可用 ⇒ `REFERENCE_INVALID`，返回合同修复，不解释算法。

## 6. 结果分流（唯一允许的四值）

```text
FAILURE_CLASS_REPRODUCED   → 才允许单独预登记 F2（A4/500k/ON，同实例同 seed 同快照）
FAILURE_CLASS_NOT_REPRODUCED → FC5_HISTORICAL_CASE=CLOSED；禁止 F2/F3；
                              转入 prospective current-semantics 稳定性或
                              HYPOTHESIS_DRIVEN_DEVELOPMENT_CALIBRATION
RUN_INVALID                → 只修运行链，不解释算法
REFERENCE_INVALID          → 回到 reference contract 修复，不解释算法
```

## 7. 运行环境要求（预登记，运行时必须留痕）

```text
CPU affinity 固定核集合并记录；JVM 版本/heap/host/并发进程表/wall-clock 条件全部记录
F1 计时域内禁止并发任何 baseline 或其它运行
（CA-TA credit 为调用计数域，但资源隔离按算法正确性问题对待）
构建声明：javaSourceChanged=false；若运行前发现需要重建 Jar，须先停下重新审批
```

## 8. F2 / F3 未来门（仅登记，不建脚本、不上传、不启动）

```text
F2 前置 = F1 = FAILURE_CLASS_REPRODUCED（且 F2 自身单独预登记 + 用户批准）
F2 固定 = same instance / seed / snapshot / A4 / 500k / telemetry ON
F2 双门 = Outcome Equivalence AND Behavioral Equivalence
          （比较域：actualFE、phase 边界、RNG/事件流、Qg/Qp trace、Q 表哈希、
           CFVF 候选序列、PDDR survivor、CA-TA 动作、working population、front、核心事件哈希）
F2 行为等价失败 ⇒ FC5_MECHANISM=UNRESOLVED；禁止定义 t*、禁止以 F2 选 repair、禁止 F3
F3 前置 = F1 复现 AND F2 行为等价通过 AND historicalA2 checkpoint 前沿不可用（当前证据预判成立）
F3 固定 = 同实例/seed/快照，A2，500k，telemetry ON（配对对照，非探索实验）
```

## 9. 停止点

本预登记完成后立即停止。不得因已预登记而自动上传、调度或运行任何 500k 任务。

```text
F1 has NOT started.
A separate user authorization is required to run F1.
```
