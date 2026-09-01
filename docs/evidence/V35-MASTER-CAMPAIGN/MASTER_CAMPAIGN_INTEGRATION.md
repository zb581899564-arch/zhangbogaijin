# V35 Final Master Campaign / Analysis Integration

日期：2026-08-23  
Track：E（Master Campaign、统计与论文接线）  
状态：`BLOCKED_FORMAL_A0_A4_MASTER_LAUNCHER_GAP`

## 1. 目标与边界

本 Track 建立唯一 A0--A4 raw-run 注册、恢复、最小证据和两种独立 reference 的接线。
它不改变核心算法、解码器、PDDR、子群规模、Q/LS 参数或任何 Final 语义；也不启动 500k
矩阵。

正式 roster 固定为：

```text
A0 = 规范、确定性、公平适配 HMOPSO-QGS-F
A1 = A0 + DSCR
A2 = A1 + CFVF
A3 = A2 + PA_i/Qp + P5/G5 hard-frozen dual-Q
A4 = A3 + CA-TA-Lite + dynamic Local-FE pacing
```

这条链是已审核的创新层级，不是 Boolean 开关集合。A4 的因果单元是
`BUDGET_AWARE_CATA_PACKAGE`，不得把 A4 的差异表述为 CA-TA 单独贡献。

## 2. 已接入的冻结输入

| 输入 | 状态 | Track E 使用方式 |
|---|---|---|
| A0--A4 最终语义复核 | `ACCEPTED` | 冻结 roster、机制门和禁止分支 |
| Formal instance/seed/snapshot manifest | `ACCEPTED` | 45 实例、20 seeds、900 snapshots；本地证据清单复核无漂移 |
| Final source/Jar freeze | `ACCEPTED` | clean tag `v35-final-doe1-frozen` → `2b3316b...`；Jar `8dad8f...ad8b9` 与配置哈希均已冻结 |
| Production preflight | `ACCEPTED` | Phase-bound Gate3：A0--A3=50000、A4=48269；decoder闭合、共同初群、五臂范围<5000 |
| Remote max parallel | `ACCEPTED=16` | 最终 frozen jar 的 4/8/12/16 JVM 吞吐均通过；无 swap/OOM |

Formal manifest 当前的已核实事实：45 个实例、20 个预注册 seed、900 个四向量快照，
`FORMAL_MANIFEST_FREEZE=ACCEPTED`，该 bundle 的 `evidence-sha256.tsv` 复核为零漂移。
这只证明公平起点被物化，**不等于** 500k raw run 已经授权或完成。

## 3. 单一矩阵与 RunKey

```text
RunKey = Arm__Instance__Seed__MaxFEs__JarSHA256__ConfigSHA256
physical raw runs = 5 x 45 x 20 = 4500
```

初群在 RunKey 外以 `snapshotSHA256`、V35/P8 双逻辑哈希与 fairness contract 再次绑定。
同一 `(instance,seed)` 的 A0--A4 必须从同一磁盘 `.fourvec` snapshot 读取；禁止 arm 通过
`createSolution()` 自行生成。输出 `COMPLETED` 后永远 skip；失败按 attempt 留存；科学性异常
使整对五臂 invalid，不能进行不完整的 paired 比较。

A0/A4 同时标记为主比较输入。因此主比较是 analysis view：引用其现有 RunKey，而不是再跑
`45x20x2`。

### Master adapter 缺口（当前 fail-closed）

既有 `scripts/v35_campaign_runner.py` 是通用进程调度器：其内部状态 key 是
`algorithm/configHash/instance/seed/budget` 的 SHA-256，不是本阶段要求的显式 Master RunKey，
也不会核验 initial snapshot/provenance。因此它只能在 Stage2 Master adapter 生成并审计 argv
后负责远端进程、resume 和原子 attempt；不得直接把 raw template 当作正式 manifest 投入它。

Stage2 正式 Java arm runner 已接入 `readSnapshot(...)`：每 arm 无 `createSolution()` 初始化路径，
并验证快照、V35/P8 双哈希与 provenance。冻结两臂 runner 确实具备 snapshot 读取能力，
但只支持 `HMOPSO_QGS_F` / `V35_MAIN`，不能执行 A1--A3。现有 Phase adapter 也没有它
声明依赖的 Master renderer。因此旧通用 scheduler 不能直接渲染正式矩阵；当前阻断为
`A0--A4 snapshot-bound launcher/renderer` 缺口，而不是 exact-FE。

## 4. Gate 驱动状态机

```text
PREPARING
  -> READY_TO_RENDER (五项 Gate 全 ACCEPTED)
  -> MASTER_LAUNCHED (远端 scheduler 已持有 rendered manifest)
  -> RAW_ACCEPTANCE (4500 raw runs 完整/失败分流审计)
  -> ANALYSIS_READY (per-problem PFref 与指标母表冻结)
  -> PAPER_READY (图表/表格只读取冻结母表)
```

没有同时满足 Final source、formal manifest、production preflight、maxParallel 和语义 Gate，
状态只允许为 `PREPARING/READY_BUT_BLOCKED`。不得从历史 FC-8/FC-9 阻断回退，但也不得用
旧 candidate jar、未验证 production preflight 或短 SSH 探针替代本阶段 Gate。

## 5. 最小原始证据与状态面板

`02-raw-runs/README.md` 固定每条 raw run 的最小文件集；`01-registry/` 固定只追加的
physical RunKey ledger 与 paired-group ledger。状态面板只由这些原子 completion/failed markers
汇总，不根据目录数量或预测 ETA 猜测完成量。

`FORMAL_MINIMAL` 是默认，以防大量逐周期日志造成无必要 IO。少量 `AUDIT_DETAILED` 运行若
需要，必须在 launch manifest 中预先列出，且不改变算法、随机性或统计输入。

## 6. 两个不可混用的 PFref

```text
PFref_ablation(instance) = ND(A0,A1,A2,A3,A4; all 20 seeds)
PFref_main(instance)     = ND(A0,A4;             all 20 seeds)
```

它们分别服务消融与主比较。每个实例单独归一化，HV reference 固定为 `(1.1,1.1,1.1)`。
禁止跨实例、跨问题或跨 participant-set 混用 reference；将来加入经用户批准的外部算法时，
另建 `PFref_formal_all` 并重算相应参与集合。

现有 `tools/v35-analysis/v35_analysis.py` 已被预接入，使用 raw fronts + metadata 作为唯一
输入，提供 HV、IGD、Spacing、双向 C-metric、front size、三目标极值、runtime、配对
Wilcoxon、Friedman、Holm 与 effect size。没有完整 raw input 时它不得输出正式结论。

## 7. 论文接线

论文只可在 raw acceptance、PFref 和统计冻结后读取 `05-analysis-ablation`、
`06-analysis-main` 的母表。详细约束在：

- `07-paper-integration/MASTER_RESULT_INTEGRATION.md`；
- `docs/paper/RESULTS_PLACEHOLDER_CONTRACT.md`。

当前只允许书写 Final 配置/实验协议事实；不允许写 A0--A4 正式数值、显著性或优越性结论。

## 8. 下一自动动作

当 A0--A4 snapshot-bound launcher/renderer 被版本化交付且验证后，Master 才可：

1. 用最终 jar/config 哈希及已冻结 900 snapshots 渲染 4500 条 manifest；
2. 进行 manifest/schema/RunKey/五臂配对静态审计；
3. 将 Track C 的 `FORMAL_MAX_PARALLEL` 注入远端单一 scheduler；
4. 按 `MASTER_CAMPAIGN_LAUNCH_PLAN.md` 启动，不重复 A0/A4；
5. 同时只准备 raw acceptance 和分析输入，待完整 fronts 后再构造各 PFref。

方案 C 已正式取代 strict-exact 门：A4 的合法 phase 尾停不是阻断。当前安全停止来自正式
执行器不完整；不得以诊断 runner、两臂 runner、补评价或重建 frozen jar 来伪造五臂矩阵。
详细证据和恢复条件见
`../V35-PHASE-BUDGET-PROTOCOL/06-formal-launch-readiness/FORMAL_MATRIX_BLOCKER.md`。
