# FC-5.2 -> FC-6 -> FC-6A-POST -> FC-6A.1 完整实验数据与报告（2026-08-18 至 2026-08-20）

> 一次收拢、可整体打包。本文件夹内含 FC-5.2（诊断审计）、FC-6（BP-PDDR 修复实验）、FC-6A-POST（Build-C2 稳定性诊断 + 指标口径修正）、FC-6A.1（PDDR 种群组成审计，含 8 张帕累托前沿可视化）从任务布置到当前状态的全部实验数据、报告、对照基线、指标工具与构建说明。
> 生成时间：2026-08-20 12:15（本地，FC-6A.1 figures 完成后）。运行环境：训练机 `aic-inspur-home`（`/home/inspur/aicomp/zhangbo-fc6-20260818`，stage1–6 批次全部完结；stage7 region审计后台运行中）。

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
| FC-6A.1 Stage-6 | PDDR 种群组成审计（QGS 原算法 vs A4-Pacing BASE，{20,100}-job × 3 seed，Build C3-COMP） | **完成 · 中性门 7/7** | **落点情况 C**：成熟期中心强势非支配常态超 100，边界孤点被结构性挤出（69 轮 0 例外），详见 `00-REPORTS/FC6A1_PDDR_COMPOSITION_AUDIT.md` |
| FC-6B | 修复方向（教师通道治理 / 重复救回治理 / 容量自适应） | **未开始 · 等用户裁决** | 本阶段禁止实现，已遵守 |

## 二、目录地图

```
00-REPORTS/                  全部报告（最终诊断结论以 FC6A_BUILD_C 为准）
  01-FC52-SEED22-FINAL/      FC-5.2 seed22 最终验收运行（mechanism-summary 全文）
  02-STAGE1-BASELINE/        Build A 三 seed：front + mechanism-summary + console
  03-STAGE2-BP20/            Build B 20-job 三 seed：front + summary + console
  04-STAGE3-BP100/           Build B 100-job 三 seed：front + summary + 基线 front 副本
  05-BASELINE-REFERENCE/     FC-2 pacing 官方基线 front（20-job，对比锚点）
  06-METRICS-TOOL/           fc6_metrics.py（paired/arm-stats/双管线）+ fc6-100job/20job.json
  07-BUILDS/                 BUILD-SUMMARY.md（全部构建校验和/代码 diff/测试清单）+
                              c2/（C2-BP、C2-BASE jar + stage5 脚本）+
                              c3/（C3-COMP jar + stage6 脚本）本地 jar 副本
  08-STAGE5-C2-DIAG/         Build-C2 诊断批全部数据（见其内 README.md）：
                              raw-100job/ raw-20job/（12 组 summary gzip）
                              parsed-*/tables/（cycles/rescues/exposures CSV ×36）
                              fronts/（12 front + 12 console）parse_fc6diag.py
  09-STAGE6-COMPOSITION/      FC-6A.1 组成审计批（见其内 README.md）：
                               raw/（12 跑原样数据）tables/（474 轮明细 + §12 主表
                               + 情况分类 + Eq1 命运 + BP 自检）两个分析脚本
                               figures/（8 张帕累托前沿图：per_seed 6 张 + merged 2 张，
                               每张 4 面板 Cmax-TEC/TEC-TWC/Cmax-TWC + 3D，QGS红/BASE蓝，
                               数据源 12 front 已中性验证）plot_fc6a1_fronts.py
                               rejected-parallel-batch/（已否决平行批，见 WARNING.md）
```

## 三、结论速览（截至 2026-08-19 晚）

1. **FC-5.2 根因锁定（B）**：author PDDR score `dominatedBy + 1/(dominates+1)` 系统性压制非支配边界极值；三个 seed 的 best-ever evaluated Cmax 全部 VNS->评估->局部接受->进池->PDDR REJECT->archive NEVER->final NO。
2. **BP-PDDR（20-job）有效**：Cmax 中位数 175.35（−6.9%）；HV/IGD/TEC/TWC 无系统性退化；R_retain=1.0000。
3. **BP-PDDR（100-job）veto 维持**：paired 口径 seed22 +8.56%/−13.69%、seed23 +4.06%/−21.04%、seed24 **−6.76%/+28.10%**（corrected 管线 −6.94%/+28.08%，方向稳定）。三目标极值反而全 seed 改善；失败根源=final front 中段变稀（seed24：513->213 点）。
4. **FC-6A-POST 裁决（C4 主 + C1 次；C2/C3 证伪）**：rescue 机制本身 100% 兑现（6/6 BP 运行的 rescued 解全部进最终 front，极值全面更优）；seed24 失稳=「**重复救回驻留（c37–46 Cmax 686.26 连续 8 轮等）× G2 教师垄断（gbest 学习 7772 次 = seed23 的 44 倍）× 边界槽角色互踩（c51–53）**」的耦合动态压垮中段（archive accept 崩落、418->213）。两臂 front 零共享点（首 rescue 即去相关）。分野判据：seed22/23 的 rescue 是一次性事件，seed24 是驻留态+互踩；rescue 频次本身无关（20-job 62/86/71 次全过门）。
5. **FC-6B 建议（未实现）**：第一刀指向教师通道治理（rescued 解维度-角色匹配）与重复救回治理（同指纹连续被救 ≥4 轮降权）；不推荐 Conditional Cmax Rescue（Cmax rescue 本身工作正常）与 passive archive（不解决 population 内教师垄断）。
6. **FC-6A.1 裁决（情况 C 为主）**：BASE 成熟期 53%（100-job）/86%（20-job）轮次中心强势非支配自身超 100，边界孤点（score=1）全部被挤出（69 轮 0 例外）；QGS 同机制但频率减半（生成侧弱 -> 池 ND 少）。挤压是 PDDR score 固有慢变量（相位单调上升），非 seed24 突变主因。容量参数锚点：central quota ~100–110（R_C 中位 1.02–1.08，max 1.27）。
7. **诚实声明**：fc52"出生"钩子未包接 `vnd()`/`factorySearch()` 的整群评估路径，问题侧 V35CmaxBestEver 全量可见，报告以问题侧 `bestCmaxEvaluatedOverall` 为权威值；displaced 配对按预注册口径（NaN=无配对轮）；BASE 100-job 无本地 runtime（历史批次未留）。

## 四、配置（全部运行统一）

- 实例：`20_2_3_1` / `100_2_3_1`；500000 FE；population 100；FM3、单族、序列无关、ShiftMode.NONE
- 正式配置：`--local-search-times 30 --g-block-length 5 --local-fe-budget 0.25:0.65`（PACING 转正配置，FC-2 确定）
- seed 包：20260822 / 20260823 / 20260824（FC-2 配对 seed，共同初始种群协议）
- 指标口径：fc6_metrics.py 双管线（old=历史 clamp 管线；corrected=raw 去重->raw 非支配->统一 min/max->不 clamp）；逐 seed paired + arm-stats；3 seed 不做显著性结论（FC-8 Wilcoxon 接口已预留 --json）

## 五、运行证据溯源（服务器路径）

- 工作目录：`/home/inspur/aicomp/zhangbo-fc6-20260818/`
- jars：`jars/jmetal-exec-5.8-BUILD-{A,B,C,C2-BP,C2-BASE,C3-COMP}-*.jar`（C2 两罐 sha256：BP=29e2aa4b…、BASE=67b91008…；C3-COMP=5233b690…，selector 类字节码 == BUILD-A 原版）
- 结果：`results/stage{1-fc5p2-baseline,2-bp20,3-bp100,4-diag,5-c2,6-fc6a1}/`
- stage5 脚本：`fc6-stage5-c2.sh`（已修正 stage4 的 xargs jobs 追加 bug）；运行日志 `logs/stage5-*`
- 本地构建工件：归档内 `07-BUILDS/c2/`（C2 两 jar + stage5 脚本）、`07-BUILDS/c3/`（C3-COMP jar + stage6 脚本）
- 已否决平行批：另一会话在服务器 `/home/inspur/aicomp/zhangbo-fc6a1-20260819/` 用 BP 污染 jar（`12b83708…`）跑的同名批次，front 与历史不符、轨迹第 2 轮起分歧且不完整；数据隔离在 `09-STAGE6-COMPOSITION/rejected-parallel-batch/`，勿引用

## 六、后续（待用户裁决）

1. **FC-6B 方向裁决**：教师通道治理 vs 重复救回治理 vs 其它（Build-C2 报告 §14 给出证据强度排序与预注册验收门：20/100-job 各 3 seed，Cmax 中位数明显优于 baseline、HV 中位数退化 ≤2%、IGD ≤10%、无单 seed HV<−5% 且 IGD>+20%）。
2. **P10.1/P24.1 冻结测试**维持已知红，FC-6B 落地后统一重冻结（旧->新哈希对照表）。
3. selector 备份：`ZhangBoEvaluatedPddrSelector.java.bk-bp`（BP 版备份）保留在源码目录，C2-BASE 构建法见报告 §16。
