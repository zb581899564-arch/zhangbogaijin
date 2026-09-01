# 论文结论—证据矩阵

| ID | 可用结论 | 证据 | 级别 | 限制 |
|---|---|---|---|---|
| C01 | 四向量按工件身份映射，MA/WA控制第一阶段资源 | P2、P8.1、I0-v35 | `ENGINEERING_VALIDATED` | 不等于作者旧解码正确 |
| C02 | FM3实现疲劳累积、自然恢复和工时反馈 | P5/P5.1、I0-v35 | `ENGINEERING_VALIDATED` | 参数为工程标准化场景 |
| C03 | 正式比较使用相同FM3、SUT、Shift.NONE与三目标 | V35-P0--P9、Final freeze | `ENGINEERING_VALIDATED` | PF-SDST未启用 |
| C04 | DSCR、CFVF、Qp/Qg和谱系档案进入正式主循环 | V35-P7、P10--P19 | `ENGINEERING_VALIDATED` | 单个机制效果仍需消融 |
| C05 | CA-TA-Lite N1--N5真实产生候选并计FE | V35-P8、P13--P16 | `ENGINEERING_VALIDATED` | 压力严格掩码未转正 |
| C06 | 方向教师池未稳定优于A4，因此默认关闭 | P25A | `NEGATIVE_PARAMETER_DECISION` | 不是教师池普遍无效结论 |
| C07 | gb15在100-job退化，因此保持双Q 5/5 | P24.2 | `NEGATIVE_PARAMETER_DECISION` | 不外推到其他问题 |
| C08 | soft-freeze rho>0未通过多实例门 | FC-4 | `NEGATIVE_PARAMETER_DECISION` | rho=0继续使用 |
| C09 | ORDER_SWAP改善Cmax但IGD越门，未转正 | FC-6A.4 | `NEGATIVE_PARAMETER_DECISION` | 3 seed诊断级 |
| C10 | REGION_AWARE在100-job触发否决门 | FC-6B | `NEGATIVE_PARAMETER_DECISION` | 不证明所有区域选择均无效 |
| C11 | DOE1未找到稳定替代20/40/20/20的比例 | DOE1开发+held-out | `PAPER_PARAMETER_SELECTION` | 参数选择，不是优越性实验 |
| C12 | P25E忠实适配后A4有积极50k信号 | P25E | `PILOT_PROMISING_SIGNAL` | 单实例、短预算、非正式统计 |
| C13 | Stage2单一100-job先导A4相对A0 HV/IGD积极 | Stage2 60条配对运行 | `PILOT_PROMISING_SIGNAL` | 不能外推45实例 |
| C14 | A2→A3在当前先导中退化 | Stage2 pilot | `PILOT_DIAGNOSTIC` | 需复核Qp/档案归因 |
| C15 | PDDR存在Cmax记录未持续保留现象 | Stage2 lifecycle | `PILOT_DIAGNOSTIC` | 尚不能判定为bug或批准修改 |
| C16 | 正式MaxFEs采用完整Q phase尾停 | Phase-budget protocol | `REPRODUCIBILITY_ONLY` | 必须报告actualFE和利用率 |
| C17 | V35最终显著优于全部基线 | 无 | `FORMAL_STATISTICAL_RESULT_PENDING` | 禁止当前使用 |
| C18 | P25D证明A4排名7/8 | P25D | `FORBIDDEN_FOR_PAPER_CLAIM` | 比较算法被统一增强引擎污染 |
| C19 | Shift改善正式算法 | P8.4/P8.6/P9 | `FORBIDDEN_FOR_PAPER_CLAIM` | Shift已永久退出正式主线 |
| C20 | 当前正式档案保持无界完整前沿，K30仅供展示 | V35-ND-ARCHIVE、D-094--D-096 | `ENGINEERING_VALIDATED` | 不代表有界档案已经实验验证 |
| C21 | ND1--ND4提升算法质量或效率 | 尚无远端实验 | `FORMAL_STATISTICAL_RESULT_PENDING` | 只能称为休眠候选实现 |

| C22 | 覆盖崩塌源于PDDR压缩/利用断裂 | PARETO-COVERAGE-LEVERAGE-AUDIT-V1 | `FORBIDDEN_FOR_PAPER_CLAIM` | 审计裁决NO_ACTIONABLE_LEVER：recovery≤0.79%、困难vs正常方向相反、FC5-250K已否证溢出链；因果未确认 |
| C23 | V35 100-job覆盖崩塌指向生成侧多样性不足 | PARETO-COVERAGE-LEVERAGE-AUDIT-V1 | `PILOT_DIAGNOSTIC` | 观察性证据（front级指纹分析+teacher事件级），非因果确认；候选级归因遥测缺口已登记 |
| C24 | LOCAL_FE_PACING修复族（betaMax）有效 | 250k确认 | `NEGATIVE_PARAMETER_DECISION` | 18/18确认实验NO_REPAIR_CANDIDATE；C2三重失败、C3检查点冲突；族按证伪条款关闭 |
