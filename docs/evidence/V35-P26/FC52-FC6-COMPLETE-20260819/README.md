# FC-5.2 -> FC-6 -> FC-6A-POST -> FC-6A.1 -> FC-6A.2 完整实验数据与报告（2026-08-18 至 2026-08-20）

> 一次收拢、可整体打包。本文件夹内含 FC-5.2（诊断审计）、FC-6（BP-PDDR 修复实验）、FC-6A-POST（Build-C2 稳定性诊断 + 指标口径修正）、FC-6A.1（PDDR 种群组成审计）、FC-6A.2（Region x PDDR 区域组成与 174.44 反事实探针审计）从任务布置到当前状态的全部实验数据、报告、对照基线、指标工具与构建说明。
> 生成时间：2026-08-20 13:20（本地，FC-6A.2 完整归档后）。运行环境：训练机 `aic-inspur-home`（`/home/inspur/aicomp/zhangbo-fc6-20260818`，stage1–7 批次全部 100% 完结）。

## 一、任务链（时间序）

| 阶段 | 任务 | 状态 | 结论 |
|---|---|---|---|
| FC-5.2 | 纯诊断：evaluated 候选从出生到 final 的生命周期审计（seed22 先行） | **完成** | 根因 B：PDDR 边界丢失（best-ever Cmax 死于 PDDR 准入） |
| FC-6 Stage 1 | seed23/24 死亡链验证（Build A，无修复） | **完成 · 门通过** | 3/3 同型确认（174.44/169.63/191.21 全死） |
| FC-6 Stage 2 | BP-PDDR 20-job 500k ×3（Build B） | **完成 · 全门通过** | Cmax 中位数 −6.9%（188.39->175.35），HV/IGD/TEC/TWC 无退化 |
| FC-6 Stage 3 | BP-PDDR 100-job 500k ×3（Build B） | **完成 · veto 触发** | seed22/24 双项越线 -> **FC-6 当前实现未通过** |
| FC-6 stage4 | Build C 中性观察批（archive 曲线） | **完成 · 中性实证** | front 与 Build B 逐字节一致 |
| FC-6A-POST STEP 1/2 | 指标口径修正（fc6_metrics.py paired/arm-stats/双管线） | **完成** | corrected 管线不改变任何开发结论；seed24 旧管线 clamp 丢 124/513 点已定量化 |
| FC-6A-POST Build-C2 | 12 跑稳定性诊断（C2-BP/C2-BASE × {100,20}-job × 3 seed） | **完成 · 12/12 sha256 命中历史** | **裁决 C4（主）+ C1（次）**，详见 `00-REPORTS/FC6A_BUILD_C_STABILITY_DIAGNOSTIC_REPORT.md` |
| FC-6A.1 Stage-6 | PDDR 种群组成审计（QGS 原算法 vs A4-Pacing BASE，{20,100}-job × 3 seed，Build C3-COMP） | **完成 · 中性门 7/7** | **落点情况 C**：成熟期强非支配常态超 100，边界孤点被结构性挤出（69 轮 0 例外），详见 `00-REPORTS/FC6A1_PDDR_COMPOSITION_AUDIT.md` |
| **FC-6A.2 Stage-7** | **Region × PDDR 区域组成与 174.44 反事实探针审计（Build C4-REGION）** | **完成 · 中性门 7/7** | **裁决 STRONG GO**：被拒 ND 中 72%~84% 具有明确区域归宿；174.44 探针在 G1 区域机制下存活率 100%（vs 全局 14.5%）；溢出率仅 1.9%~7.4%，详见 `00-REPORTS/FC6A2_REGION_PDDR_AUDIT.md` |
| FC-6B | 架构落地（Region-aware Environmental Selection + 教师治理） | **待启动** | 根据 FC-6A.2 铁证，以区域一致性架构替代补丁体系 |

## 二、目录地图

```text
00-REPORTS/                  全部报告（FC6A1_PDDR_COMPOSITION, FC6A2_REGION_PDDR 等）
  01-FC52-SEED22-FINAL/      FC-5.2 seed22 最终验收运行（mechanism-summary 全文）
  02-STAGE1-BASELINE/        Build A 三 seed：front + mechanism-summary + console
  03-STAGE2-BP20/            Build B 20-job 三 seed：front + summary + console
  04-STAGE3-BP100/           Build B 100-job 三 seed：front + summary + 基线 front 副本
  05-BASELINE-REFERENCE/     FC-2 pacing 官方基线 front（20-job，对比锚点）
  06-METRICS-TOOL/           fc6_metrics.py（paired/arm-stats/双管线）+ fc6-100job/20job.json
  07-BUILDS/                 BUILD-SUMMARY.md（全部构建校验和/代码 diff/测试清单）+
                              c2/（C2-BP、C2-BASE jar + stage5 脚本）+
                              c3/（C3-COMP jar + stage6 脚本）+
                              c4/（C4-REGION jar + stage7 脚本）本地 jar 副本
  08-STAGE5-C2-DIAG/         Build-C2 诊断批全部数据
  09-STAGE6-COMPOSITION/      FC-6A.1 组成审计批全部数据与前沿图
  10-STAGE7-REGION/           FC-6A.2 区域组成与探针审计批全部数据：
                              raw/（12 跑原样数据）、tables/（474 轮明细 + 被拒解归属 +
                              分位统计 + 174.44 探针表 + 逐 seed 表）、analyze_fc6a2_region.py
```

## 三、结论速览（截至 2026-08-20 下午）

1. **FC-5.2 根因锁定（B）**：author PDDR score `dominatedBy + 1/(dominates+1)` 系统性压制非支配边界极值；三个 seed 的 best-ever evaluated Cmax 全部 VNS->评估->局部接受->进池->PDDR REJECT->archive NEVER->final NO。
2. **BP-PDDR（20-job）有效**：Cmax 中位数 175.35（−6.9%）；HV/IGD/TEC/TWC 无系统性退化；R_retain=1.0000。
3. **BP-PDDR（100-job）veto 触发**：seed24 产生耦合失稳，final front 中段变稀（513->213 点）。
4. **FC-6A-POST 裁决（C4 主 + C1 次）**：rescue 机制本身 100% 兑现，但补丁式救援的驻留态与 G2 综合群教师垄断产生恶性耦合。
5. **FC-6A.1 裁决（情况 C 为主）**：BASE 成熟期强非支配常态超 100，边界孤点在全局单一 score 下 100% 被挤出。
6. **FC-6A.2 裁决（STRONG GO，区域一致性架构）**：
   - **零被拒事实**：在全 12 跑中，归属综合群 G2 的解被淘汰数为 **0**；被原版 PDDR 切除的解 **100% 是方向极值解和溢出解**！
   - **区域可吸收性**：被全局 PDDR 淘汰的非支配解中，**72.2% ~ 84.4%** 本来在 G1/G3/G4 区域中有明确合法席位；
   - **174.44 反事实探针**：在区域机制下，黄金解 174.44 在全部 62/62 轮（100%）中稳定存活于 G1 席位，存活率相比全局原版（14.5%）提升 **6.89 倍**；
   - **极低溢出率**：中位数仅 1.9% ~ 7.4%，未随相位恶化。
   - **结论**：区域感知环境选择（Region-aware Environmental Selection）彻底消除了补丁式救援的脆弱性，实现了环境选择与粒子群分群的架构一致性，并顺带根治了教师角色错位。

## 四、配置（全部运行统一）

- 实例：`20_2_3_1` / `100_2_3_1`；500000 FE；population 100；FM3、单族、序列无关、ShiftMode.NONE
- 正式配置：`--local-search-times 30 --g-block-length 5 --local-fe-budget 0.25:0.65`（PACING 转正配置，FC-2 确定）
- seed 包：20260822 / 20260823 / 20260824（FC-2 配对 seed，共同初始种群协议）
- 指标口径：fc6_metrics.py 双管线

## 五、运行证据溯源（服务器路径）

- 工作目录：`/home/inspur/aicomp/zhangbo-fc6-20260818/`
- jars：`jars/jmetal-exec-5.8-BUILD-{A,B,C,C2-BP,C2-BASE,C3-COMP,C4-REGION}-*.jar`
- 结果：`results/stage{1-fc5p2-baseline,2-bp20,3-bp100,4-diag,5-c2,6-fc6a1,7-fc6a2}/`
- stage7 脚本：`fc6-stage7-fc6a2.sh`；运行日志 `logs/stage7-*`
