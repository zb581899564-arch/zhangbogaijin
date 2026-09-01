# PFC5-1B 快照身份裁决（seed 20260901，100_5_3_1）

- 生成时间：2026-08-29
- 生成工具：总控 Agent（核验命令见下文，全部可复算）
- 输入：`fetched-remote/snapshots/100_5_3_1/seed-20260901.fourvec`、双臂
  `provenance.properties` / `initial-population.sha256` / `confirmation-context.properties`、
  `V35-FORMAL-MANIFEST/FORMAL_INITIAL_POPULATION_MANIFEST.csv`（配置哈希对照）
- 消耗FE：0；改变算法：否
- 授权下一阶段：本裁决解除 F1 预登记的"精确快照"前提；F1 启动仍需用户单独授权

## 裁决

```ini
historicalSnapshotIdentity=EXACT_HISTORICAL_SNAPSHOT_AVAILABLE
historicalStateReproductionClaimAllowed=true
```

## 身份链条（五环，逐环可复算）

1. **物理实体**：文件自训练机确认实验目录
   `/home/inspur/aicomp/zhangbo-v35-a2-a4-confirmation-20260824/input/snapshots/100_5_3_1/seed-20260901.fourvec`
   经 sftp 拉回（2026-08-29），大小 92,117 字节，
   物理 SHA-256 = `84d845233e332a6612e5dfe93c97cbbeef40c4ee05766cbfd0e9446bd3043769`。
2. **运行时锚点**：500k 确认实验 A2/A4 两臂的 `provenance.properties`（2026-08-24
   运行前写入）独立记录 `snapshotSha256=84d84523…3769`，与实测物理哈希一致——
   该物理文件正是当年注入 500k 运行的那份。
3. **逻辑锚点**：文件配发的 `initial-population.sha256` 记录 V35 逻辑初群哈希
   `179a82a3825566380ab6798aa898002d31565dad9d65802e57b295c2a4294c2d`、P8 兼容哈希
   `7c6f8b425f2781653ce9705b82050652f063b461b24c0f93d9486e2c686ca2d3`；
   前者与两臂 provenance 的 `initialPopulationHashV35`、A4 `status.properties` 的
   `initialPopulationHash`、以及 50k/100k 转移运行（同实例同 seed）的
   `initialPopulationHash` 全部一致（跨 campaign 确定性锚点）。
4. **内容绑定**：文件头逐项匹配——`instanceId=100_5_3_1`、
   `instanceSHA256=2E88FA97…5CF`（与 45 实例正式 manifest 同值）、
   `SUTSHA256=E7E9FF7F…`、`fatigueParameterSHA256=81CAD959…`、
   `problemConfigurationSHA256=892c7c3f…`（三项均与正式 manifest 同值）、
   `seed=20260901`、`population=100`；语义绑定 `decoderMode=FM3`、
   `familyMode=DEGENERATE_SINGLE_FAMILY`、`setupMode=SEQUENCE_INDEPENDENT`、
   `shiftMode=NONE`（与冻结语义一致）。四向量内容 500 行 = 100 粒子 × 4 向量记录。
5. **无覆盖冲突**：三本账（远端每 run `evidence-sha256.tsv`、本地
   `acceptance-run-audit.csv`、拉回文件实测）哈希互证一致；项目内未发现任何与
   `84d84523…3769` 或 `179a82a3…4c2d` 冲突的登记。

## 诚实限制

- 快照文件本身未内嵌生成器构建号与创建时间戳；身份由第 2 环（运行前独立写入的
  哈希锚点）确立，而非文件自身时间属性。远端文件 mtime 未在拉取时留痕。
- 本地 `V35-FORMAL-MANIFEST/initial-populations/` 只覆盖正式 seeds 20260808..20260827；
  本次是**拉取原件**，不是"同 seed 重新生成"，因此不适用
  `REGENERATED_CURRENT_SEMANTICS_ONLY` 降级。
- 仅 seed 20260901 的快照在授权范围内拉回；其余 seed（20260902..05）的快照仍在
  训练机，如后续需要应另行授权（本阶段不需要）。

## 对 F1 的直接含义

F1 预登记可绑定：

```text
snapshotPhysicalSha256  = 84d845233e332a6612e5dfe93c97cbbeef40c4ee05766cbfd0e9446bd3043769
initialPopulationHashV35 = 179a82a3825566380ab6798aa898002d31565dad9d65802e57b295c2a4294c2d
replayKind              = HISTORICAL_STATE_FAILURE_REPLAY
```

复算命令（Git Bash）：

```bash
sha256sum docs/evidence/V35-PFC5-PHASE0/fetched-remote/snapshots/100_5_3_1/seed-20260901.fourvec
grep snapshotSha256 docs/evidence/V35-PFC5-PHASE0/fetched-remote/100_5_3_1/seed-20260901/{A2,A4}/provenance.properties
cat docs/evidence/V35-PFC5-PHASE0/fetched-remote/100_5_3_1/seed-20260901/A2/initial-population.sha256
```
