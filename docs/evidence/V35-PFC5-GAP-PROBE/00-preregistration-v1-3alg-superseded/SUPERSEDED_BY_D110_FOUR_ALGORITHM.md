# SUPERSEDED BY D-110：三算法 Gap 预登记废止说明

- 日期：2026-08-30
- 废止依据：`docs/ROADMAP.md` D-110、`AGENTS.md` §22、用户 2026-08-30 执行指令
- 本目录（`00-preregistration/` 与 `01-2k-shakedown/`）按原样保留，不删除、不改写；
  后续一切 Gap Probe 工作以 `02-v2-four-algorithm-preregistration/` 为唯一有效预登记。

## 废止内容与效力

1. **三算法预登记不再授权 500k**：v1 的 12 条 500k RunKey（`run-registry.csv`）
   全部作废，不得启动；v1 的"每实例 6 条 reference"构造规则作废，
   不得构造、不得用于任何裁决。
2. **三条旧 2k 结果只证明快照与输出接线**：v1 `01-2k-shakedown/` 的
   SHAKEDOWN_PASSED 仅覆盖快照注入/解码器/输入哈希链/输出工件接线。
3. **A0/A4 实际只有 100 FE，机制未触发**：冻结相位一致协议下 2k < 单个
   Q phase（5000 FE），A4/A0 在初始种群后 `PHASE_CONSISTENT_TAIL_STOP`
   （`formalOuterCycles=0`），qgSelections/pddrEvents/cfvfOffspring/qpActions/
   caTaLite 等机制计数全部为 0。v1 报告已如实记载；该结果**不能承担机制门**。
4. **外部算法单一化错误**：v1 将 SPEA2-F 定为唯一 external，违反 D-110 的
   四算法并行要求（避免事后择优）；P25E 5seed-50k 指标降级为开发背景。
5. **工具身份勘误**：v1 `artifact-binding.tsv` 把 bb9d1ce3…记为启动器 Jar——
   该 SHA 是含旧 seed 探针的首版编译，已被重编译覆盖；f5de5272…是 v1 探针+
   启动器合并 Jar（探针 seed 改为 20260827/20260906 后重编）而非"0-FE 探针专用"。
   V2 起使用干净分离的启动器 `gap-probe-arm-launcher-v2.jar`
   （SHA `c8fb7e005f2fbe110adfc8c48e5de30c83848147b69322904650bda400c12f09`，
   `--release 8` 编译，major version 52，Java 11 远端可载），仅含
   `ZhangBoV35FormalAblationArmRunner`，不含探针。

## V2 变更摘要

- 算法 3→4（加入 NSGA-II-F，SPEA2-F 与 NSGA-II-F 并行作为 external）；
- 500k run registry 12→16 条（每实例 4 算法 × 2 seed = 8 条）；
- reference 合同改为每实例等 8 条全部 ACCEPTED 后构造；
- 预算门修正（A0/A4 允许 phase-consistent tail stop，公平组 actualFE 跨度 < 5000）；
- seed 措辞修正（20260827 = snapshot/materialization registered、
  performance-unexposed、never used in a completed scientific run；
  20260906 = closed CAL route 预登记、零 FE、确定性物化）；
- 执行域迁移：20k 机制贯通与 16×500k 全部在训练机
  `/home/inspur/aicomp/zhangbo-v35-gap-probe-v2-20260830` 执行。
