# V35-EXT-PREFLIGHT-20K 预登记（生产预检，ENGINEERING_VALIDATION）

- 日期：2026-08-30
- 性质：`PRODUCTION_PREFLIGHT_ONLY / ENGINEERING_VALIDATION`——不是科学性能实验；
  不构造 PFref、不计算 HV/IGD/Spacing/C-metric、不排名、不评价优劣
- 起点状态：`NSGA_II_F=FAIR_READY`、`SPEA2_F=FAIR_READY`（V35-EXTERNAL-BASELINE-FAIR-READY）；
  `formalMatrixRunning=false`、`FinalCandidateApproved=false`

## 1. 范围

```text
2 algorithms (NSGA-II-F, SPEA2-F) × 3 DEVELOPMENT instances (20_2_3_1, 50_2_3_1, 100_2_4_1)
= 6 physical runs，独立 JVM / 独立 Problem / 独立算法对象
seed=20260822  population=100  MaxFEs=20000
decoder=FM3  ShiftMode=NONE  familyMode=DEGENERATE_SINGLE_FAMILY
setupMode=SEQUENCE_INDEPENDENT  objectiveSlots=[0,1,6]
```

禁止使用 VALIDATION_RESERVED 实例与历史失败诊断实例 `100_5_3_1`（三实例均为
Phase 0 实例角色登记中的 CONTAMINATED_DEVELOPMENT，允许工程用途）。
不重复 2k 阶段的同 seed 重放（2k 已证确定性，本阶段每 RunKey 只 1 次成功运行）。

## 2. 输出原子化边界（本包第一步，已实现）

- Runner 新增生产模式：`--final-output` + `--run-id` + `--attempt-id` →
  全部产物写入 `.partial-<runId>-<attempt>` → 清单逐文件 SHA-256 自校 →
  `ATOMIC_MOVE` 升级为最终 run 目录；终目录已存在或残留同名 partial → fail-closed；
  异常退出只留 partial（永不带 COMPLETED 语义出现在最终目录）。
  修改仅限输出/失败隔离/幂等/证据落盘；搜索语义零改动。
- 外部 launcher（`tools/preflight_launcher.py`）：启动前冻结门（Jar/快照/实例哈希、
  非法算法标签、终目录已存在、重复 RunKey）、启动、独立后验（重哈希清单+必
  备文件+状态门+前沿有限非空）、故障注入自检（9 场景）。
- 新比较构建物（独立名称）：`external-fair-baseline-comparison-preflight-966da3d2.jar`
  SHA-256 `966da3d2d23842f4ea5892e8da57404c88b076be2f9fcb568b54953f525447d9`；
  前序构建物 `…-585ca315.jar`（`585ca315…93e6`）原样保留不覆盖。

## 3. 六条 RunKey（笛卡尔积，预登记锁定）

```text
NSGA-II-F|20_2_3_1|20260822|100|20000   runId=PRE20-NSGAII-20_2_3_1
NSGA-II-F|50_2_3_1|20260822|100|20000   runId=PRE20-NSGAII-50_2_3_1
NSGA-II-F|100_2_4_1|20260822|100|20000  runId=PRE20-NSGAII-100_2_4_1
SPEA2-F|20_2_3_1|20260822|100|20000     runId=PRE20-SPEA2-20_2_3_1
SPEA2-F|50_2_3_1|20260822|100|20000     runId=PRE20-SPEA2-50_2_3_1
SPEA2-F|100_2_4_1|20260822|100|20000    runId=PRE20-SPEA2-100_2_4_1
```

同一 instance×seed 的双臂读取同一份冻结快照（逐字节相同，哈希见 02-input-freeze）。
故障注入（含进程中断）使用独立 `FI-*` runId 与 `.selftest` 区域，不占 RunKey。

## 4. 验收（摘要）

逐运行门：status=COMPLETED、actualFE=decoderCalls=fullEvaluations=20000、
remainingFE=0、illegal/duplicate/unexplained/forbidden 全 0、front 非空且有限、
身份事件为正（NSGA-II：tournament/crossover/mutation/ranking/crowding/
RankingAndCrowding replacement；SPEA2：tournament/crossover/mutation/strength/
density/archive/environmentalSelection）；同实例双臂五同（快照 SHA、V35 初群哈希、
P8 初群哈希、实例/SUT/疲劳 provenance、[0,1,6] 映射）。
仅当 6/6 运行与全部故障门通过才写
`EXTERNAL_BASELINE_PRODUCTION_PREFLIGHT=PASSED`。

## 5. 禁止项确认

不合并前沿构造 reference；不按前沿外观评价强弱；不上传/启动 50k/250k/500k；
不启动 Gap Probe/Validation/Champion Gate/Formal；不修改任何 V35 搜索机制；
20k 结果不进论文性能表；完成后停止，不自动进入下一阶段。
