# V3.5-FC-TIME 时间收口阶段——统一文档汇总（2026-08-18）

> 本文件夹是 v3.5 运行时间收口阶段（D-083）的**统一文档汇总**。所有文件为副本，原始位置见每份文件尾部的"证据"与下方索引。本阶段结论：**时间门通过（R=6.20×），恢复 FC-5→FC-8 原路线；FC-4 软冻结裁决删除。**

## 一、阶段时间线（2026-08-18 一天内完成）

```text
路线重订（D-083 + 4 份文档）
  → FC-TIME-1B 诊断：模块账 → 定位 DSCR 外围 43.9%（结论 A）
  → FC-TIME-2 语义等价优化：A1 fingerprint hoist → A2 轻量快照 → A3 G4 预计算
  → 每步逐位等价验收（front/计数器/等价门）
  → FC-TIME-0 同机串行正式计时：QGS 23.6s / Legacy 39.4s / Pacing 146.0s
  → 时间门裁决：R=6.20× ∈ 可接受区间 → 门通过
  → FC-4 软冻结综合裁决：18/18 完成 → 不转正、删除、ρ 封闭
```

## 二、关键结论速查

| 问题 | 答案 | 证据 |
|---|---|---|
| 时间到底花在哪？ | **DSCR 外围 43.9%**（每 Q round 全量重做快照+过滤）；CFVF"跟老师学"本体仅 2.1% | FC_TIME1B 报告 |
| 为什么 62 轮比 18 轮每轮贵 2.6 倍？ | DSCR 的 A×T 严格支配过滤随 archive 增长（dominatesCalls 5.7k→18.3k/Q round），fingerprint 每对重建 5.3M 次 | 1B 报告 §H |
| 怎么优化？ | 三个**纯语义等价**改动：fingerprint hoist（−99.3%）、快照去冗余深拷贝、G4 范围预计算 | FC_TIME2 报告 |
| 优化收益？ | 50k 墙钟 −48%；**500k pacing 976–1131s → 146s（−85%）**；front/事件流逐位一致 | FC_TIME2 报告 + FC_TIME0 报告 |
| 和基线差多少？ | 同机串行：QGS 23.6s / Legacy 39.4s / Pacing 146.0s；**R1=1.67×、R2=3.71×、R=6.20×**（此前跨机粗估 55× 是错的） | FC_TIME0 报告 |
| 时间门过没过？ | **过（6.20× ∈ 5–8× 可接受）**，FC-TIME-3 不触发 | FC_TIME0 报告 §4 |
| FC-4 软冻结？ | 100-job HV 9/9 全输（−20~−31%），判据全败 → **删除软冻结，ρ=0 硬冻结，ρ 参数封闭** | FC_EXPERIMENTS_COMPLETE_DATA.md §8.4 |
| FC-3 cheap-test？ | 永久封禁（教训：CA-TA Test 非纯开销，要算得更便宜而非少算） | 同上 §8.2 |

## 三、文件索引

| 文件 | 内容 | 原始位置 |
|---|---|---|
| `V35_FC_TIME_PLAN.md` | 时间收口阶段完整方案（阶段定义、嫌疑清单、时间门、纪律） | `docs/V35_FC_TIME_PLAN.md` |
| `FC_TIME1B_CFVF_INTERNAL_AUDIT_REPORT.md` | CFVF 内部 13 子模块时间+计数器分解、调用层级图、DSCR 层级核查、结论 A | `docs/evidence/V35-P26/fc-time1b-audit/` |
| `FC_TIME2_SEMANTIC_EQUIVALENCE_REPORT.md` | A1/A2/A3 三阶段优化+验收（front 逐位、计数器逐位、等价门） | `docs/evidence/V35-P26/fc-time2/` |
| `FC_TIME0_REPORT.md` | 同机串行 500k 三臂正式计时、R1/R2/R、时间门裁决 | `docs/evidence/V35-P26/fc-time0/` |
| `FC_EXPERIMENTS_COMPLETE_DATA.md` | 全量实验数据（FC-1..FC-4 共 86 条逐行 + 现状综述 + 桥接对比含三目标/CPU 时间） | `docs/evidence/V35-P26/experiments/` |
| `fc-all-runs.csv` | 机器可读汇总（86 行 × 25 列，含 runNanos/runSeconds） | `docs/evidence/V35-P26/experiments/` |

## 四、运行原始数据位置（未复制，按需访问）

- 模块账运行：`docs/evidence/V35-P26/fc-time1b-audit/{pacing,legacy}-50k/`
- 优化验收运行：`docs/evidence/V35-P26/fc-time2-a{1,2,3}/pacing-50k/`
- 正式计时运行：`docs/evidence/V35-P26/fc-time0/{qgs,legacy,pacing}/run{0..3}/`
- FC-4 全部运行：`docs/evidence/V35-P26/experiments/remote-fc-20260817/fc4-rho/`
- 对比批次原始：`docs/evidence/V35-P25E-corrected-comparison/`

## 五、当前算法状态（2026-08-18 快照）

```
正式候选 = A4-PREFINAL + pacing（βmin=0.25/βmax=0.65）
  + 语义等价优化（fingerprint hoist / 轻量快照 / G4 预计算，行为逐位不变）
  - soft-freeze：已删除（ρ=0）
  - cheap-test：已封禁
时间门：R=6.20×（可接受区间）✅
待跑：FC-5（GIR+Cmax Audit）→ FC-6（Cmax 单支）→ FC-7（消融）→ FC-8（9 算法 Champion Gate）
```

## 六、下一步

1. FC-5：Cmax Audit + CFVF GIR 审计实验（代码已就绪，仅需按方案跑 500k 批次）
2. FC-6：按 FC-5 审计四选一修 Cmax（唯一目标：100-job minCmax 不再输基线 2.2%）
3. FC-7：最终消融（DSCR D0/D1 + CFVF/Qp/CA-TA + pacing 开关）
4. FC-8：四规模 Champion Gate（9 算法 × {50k,500k} × 5 seed），前置 TIME 门已过
5. 可选：FC-TIME-3（βmin 0.30/0.35 冲 3–5× 理想区间），需另行批准