# 论文各章节数据与证据取用指南

## 绪论与问题来源

- 李明哲学位论文：`E:\学习\李明哲-毕业材料\3.毕业论文\104_2022930913_李明哲.pdf`。
- ESWA第四章稿件：`E:\学习\eswa2026-最新李明哲第四.pdf`。
- 当前总体方案：`E:\学习\ziliao\v3.5.md`。
- v2公式与三个创新点来源：`E:\学习\ziliao\HMOPSO_QGS_疲劳_全向量双Q_CA-TA-VNS_综合改进方案_v2.md`。

这些文件只读，不得复制其结果作为本项目实验结果。

## 问题定义与编码

- 四向量`JS/FA/MA/WA`：P2及`paper_evidence/I1/01_input`。
- 目标槽：七槽载体中的`[0,1,6]=[Cmax,TEC,TWC]`。
- 产品族：单族占位，`familyTransition=0`。
- 设置时间：`SUT[job][stage]`，序列无关。
- 论文图建议：I1四向量对齐图和工厂—机器—工人结构图。

## 疲劳解码创新

- 公式、参数、模式：P5/P5.1、I0-v35、I1。
- 人工—程序核验首选：`paper_evidence/I0-v35/04_comparison/validation_report.md`。
- 教学甘特图可用I1基础FM3结果；I1 Shift图必须标记历史诊断并排除。
- 允许表述：序列无关设置时间下的动态疲劳、自然恢复和加工/设置工时反馈。
- 禁止表述：已完成PF-SDST、多技能或主动休息实验。

## 双Q全向量搜索创新

- DSCR、Qg/Qp、CFVF与谱系档案：V35-P5/P7及V35-P10--P19。
- 机制流程图：`docs/evidence/V35-P26/algorithm-diagram/`。
- 参数冻结：P24.2和FC-4；正式值为P=5/G=5、rho=0。
- A2→A3当前先导退化必须在论文实验前解释，不得隐去。

## CA-TA-Lite创新

- 五宏邻域与24上下文：V35-P8、P13--P16。
- 当前正式语义：BAL全开放N1--N5；压力分类只作诊断。
- FC-6保留`CA-TA-Lite→inherited LS`，拒绝ORDER_SWAP和REGION_AWARE。
- Stage2 A3→A4的积极信号只能写作先导结果。

## 参数选择

- 子群容量DOE：`docs/evidence/V35-DOE1-subgroup-mixture/`。
- 开发原始运行完整归档：`D:\CodexTemp\V35-DOE1-Acceptance\development-runs-20260822.tar.gz`。
- held-out决策：`D:\CodexTemp\V35-DOE1-heldout-audit-20260822\FINAL_MIXTURE_DECISION.md`。
- 最终结论：没有候选通过预注册门，保留`20/40/20/20`。

## 实验设置与公平性

- 正式实验协议：`docs/V35_FORMAL_EXPERIMENT_ROADMAP.md`。
- Final Jar/源码：`docs/evidence/V35-FINAL-FREEZE/`。
- 45实例、20 seeds和900共享初群：`docs/evidence/V35-FORMAL-MANIFEST/`。
- FE协议：`docs/evidence/V35-PHASE-BUDGET-PROTOCOL/`。
- 忠实算法适配：`docs/evidence/V35-P25E-corrected-comparison/`。

## 结果章节

当前可写：

- 编解码数值正确性；
- 机制真实触发；
- 参数和失败分支的冻结过程；
- 单实例先导信号及其非统计性边界。

当前不可写：

- 45实例×20次的正式HV/IGD表；
- Wilcoxon、Friedman、Holm或effect size；
- V35相对所有算法的最终排名；
- 论文完整复现或统计显著优越。

