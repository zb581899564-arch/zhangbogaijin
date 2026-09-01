# V35-FC5 250k ON 遥测实验预注册

状态：已获批准，启动前封存。此包只调查 FC-5 利用断裂是否在 100k 以后形成；不改变算法、不运行 OFF 控制、不恢复 4500 矩阵。

## 固定矩阵

| 维度 | 固定值 |
|---|---|
| instances | `100_2_4_1`（正常/正例）、`100_5_3_1`（困难/退化例） |
| seeds | `20260901`, `20260902`, `20260903` |
| arms | `A2`, `A4` |
| runs | `2 × 3 × 2 = 12` |
| MaxFEs | `250000` |
| termination | `PHASE_CONSISTENT_BUDGET_TERMINATION` |
| telemetry | `ON` only |
| PDDR | `GLOBAL_ORIGINAL` |
| mixture | `20/40/20/20` |
| ShiftMode | `NONE` |
| decode | `FM3` |
| population | `100` |

检查点固定为 `25000,50000,75000,100000,125000,150000,175000,200000,225000,250000`。每个 run 使用独立 JVM；A2/A4 在同一 instance×seed 下由同一确定性 canonical factory（seed 与 ordinal `0..99`）生成相同初始群体。遥测只读，不进入选择、奖励、PDDR、FE 或停止逻辑。

## Jar 绑定

```ini
formalAlgorithmJarSha256=8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9
diagnosticRuntimeJarSha256=A0A1E74D00403CAC69FBC25B52AEAEB454A6CC2D9FA6BF2A1F6A0D12FFE15FF7
diagnosticLauncherJarSha256=0E13E6DAC59E7593C4B3B55720327CEFC0AF86EF070E3617D04ECBC3AE4A831E
diagnosticLauncherBytecodeTarget=11
diagnosticToolingValidated=true
250kReadyForPreregistration=true
250kApproved=true
250kStarted=false
formalMatrixRunning=false
FC5=INCONCLUSIVE
```

诊断 runtime Jar 不通过旧的 50k-only `main` 直接启动。外部 launcher 仅调用其中已经验收的 `createTelemetry` 与 phase-consistent `runPhaseConsistentDiagnostic` wiring；运行时会检查 `V35MidHorizonDiagnosticDriver` 的 `CodeSource` SHA-256 必须等于上述 runtime SHA。launcher 不包含算法类，也不重打 runtime Jar。

## 启动与验收顺序

1. 封存本预注册包并反算 `PREUPLOAD_SHA256.tsv`。
2. 上传 121 Jar、外部 launcher、正式算法 Jar 和本表登记的实例/配置输入。
3. 远端反算 Jar 与输入 SHA-256；任一不一致则不启动。
4. 仅启动 `campaign-plan.tsv` 中的 12 条 ON run。
5. 全部完成后先按 run 验收 FE、终止快照、三类前沿、PDDR/teacher/CA-TA 元数据、行为哈希和文件哈希，再做 FC-5 根因分析。

不得追加 run、替换 seed、改变 arm、使用 OFF、执行 2k/20k/A2 预门、恢复 4500 矩阵或修改算法源码。`cataFullLifecycleValidated=false` 是本实验直接输出的统一 CA-TA 字段，因为本包不运行短门；禁止事后归一化改写。

## 重启修订

2026-08-27 重启包将外部 launcher 按 Java 11 字节码目标重新构建。此前远端 Java 11 对 Java 17 launcher 的启动尝试在类加载阶段失败，未进入算法、评价或终止快照阶段，不计入实验数据；本次使用新的远端根目录重新启动。

本次重启同时修正了终止原子边界精确命中时的检查点登记缺口，并将修订后的诊断 runtime 重新封存；前一轮虽完成了算法运行，但因终止检查点字段仍为 `-1` 不计入实验数据。
