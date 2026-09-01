# V35-FC5-T：100-job 候选膨胀与利用断裂迁移审计预登记

日期：2026-08-25  
状态：`IN_PROGRESS`  
性质：只读历史核验 + 行为中性的旁路遥测；不是 DOE、不是正式消融、不是 PDDR 修复实验。

## 1. 唯一优先假设

本工作包只优先检验 FC-5 已发现的利用断裂是否迁移到 100-job 退化案例：

```text
合并候选池的严格非支配点膨胀
→ GLOBAL_ORIGINAL PDDR 压缩为 100 个工作槽位
→ Cmax / TEC / TWC / Balanced 代表未被保留或未被后续利用
→ archive 与 working population 脱节
→ 教师使用和有效后代不足
```

这不是“PDDR 实现错误”的假设。FC-5 的已知结论是利用断裂；FC-6 仅否决了
`ORDER_SWAP`、`BP_RESERVED_LEGACY`、`REGION_AWARE` 三种具体修法，**没有否决上述机制本身**。

## 2. 冻结边界

所有历史臂保持其原定义：

- A0/A2 原本没有 Qp、双Q、CA-TA-Lite，本审计不得人为补入；
- A4 的 CFVF、双Q、CA-TA-Lite 不得关闭；
- `FM3`、`ShiftMode=NONE`、单一产品族、序列无关 SUT、`[20,40,20,20]`、
  `GLOBAL_ORIGINAL` 与 `CA-TA-Lite → inherited LS` 保持冻结；
- 本审计不得重做 DOE、启动 ND1–ND4、修改 PDDR、修改冻结 Jar，或恢复 4500 矩阵；
- 结论不得被解释为删除 CFVF、双Q或 CA-TA-Lite 的依据。

## 3. 已有数据与深审对照

| 比较 | 正例 | 退化例 | 已有 seed | 历史来源 |
|---|---|---|---|---|
| A0↔A2 | `100_2_5_1` | `100_8_3_1` | `20260911..20260915` | A2 最终候选确认 |
| A2↔A4 | `100_2_4_1` | `100_5_3_1` | `20260901..20260905` | A2↔A4 多实例确认 |
| Stage2 背景 | `100_2_3_1` | 不作为正/负裁决 | 已接受五臂组 | 已校验冷归档 |

Stage2 冷归档：

```text
G:\ResearchArchive\ZhangBo-V35-Paper-Evidence-20260823\remote-campaigns\
zhangbo-v35-stage2-master-v2-20260823.tar.gz
SHA-256=0202356F28C7013894FB14B7347EB77A66243AD9312139CD0FE2A62F24CAD5FB
```

## 4. 观测定义

每个正式 PDDR 轮记录同一合并候选池：

```text
Nmerge    合并池物理候选数
Nunique   按 (Cmax, TEC, TWC) 精确去重后的点数
Nnd       Nunique 的严格三目标非支配点数
Roverflow = Nnd / 100
```

四个稳定代表采用该池的当前语义：

```text
E_C = min(Cmax, TEC, TWC, poolOrder)
E_E = min(TEC, Cmax, TWC, poolOrder)
E_W = min(TWC, Cmax, TEC, poolOrder)
E_B = min(current G4 normalized Chebyshev phi, Cmax, TEC, TWC, poolOrder)
```

每个代表记录：池内存在、PDDR 选中与拒绝原因、下一代物理槽位/语义角色、Qg/Qp
教师使用、以及后续是否产生该方向严格改善后代。每 50k FE 导出窗口摘要。

## 5. H1 判定规则

仅在退化实例同时满足全部条件时，H1 标为 `FC5_TRANSFER_CONFIRMED`：

1. 至少 2/3 配对 seed 在指标明显分离前，连续两个 50k 窗口 `Nnd > 100`；
2. 退化实例的中位 `Roverflow` 至少高于正例 `0.25`；
3. 至少一种代表的 `pool → next-population` 保留率至少低 `20` 个百分点；
4. archive–working-population 的最佳 Cmax 差距随代表丢失扩大，且时间上先于指标退化。

这只构成“强机制迁移证据 / root-cause candidate”。它不是修复后的因果确认；若成立，后续仍需另行预注册一个只改变一个 PDDR 选择环节的可反驳修复实验。

## 6. 渐进式遥测门

历史数据先做字段可用性核验。只有核心字段确实缺失，才启动独立旁路构建物
`FC5_100JOB_TRANSFER_V1`：

```text
50k → 100k → 250k → 仅在历史分叉确在250k后且仍无法裁决时500k
```

每档先验收，再决定下一档。遥测 ON/OFF 的合格口径是行为等价：初始种群哈希、随机事件
消费序列、候选指纹及三目标序列、PDDR 选中身份和槽位、Q 表、FE 与规范排序后的最终前沿
集合一致；不使用含时间戳 CSV 的字节级比较。

## 7. 预先写定的分流

- `FC5_TRANSFER_CONFIRMED`：停止重审 CFVF、Qp/双Q、CA-TA、FM3；只起草最小 PDDR 单变量修复方案。
- `FC5_TRANSFER_NOT_CONFIRMED`：才依次解锁 CFVF 规模编辑、A4 Qp/双Q协调、CA-TA/LS预算、FM3。
- `INSUFFICIENT_EVIDENCE`：登记字段限制，不将相关性写作根因，不修改算法。
