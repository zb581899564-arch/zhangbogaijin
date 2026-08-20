# P4 语义差异登记

## published_baseline 与 author_actual

- 原 `MOHPSOQ` 仍使用 `PermutationSolution<Integer>`、七目标槽位及大量静态/全局状态；P4新闭环固定为 `DhhfspFourVectorSolution` 和 `[Cmax,TEC,TWC]`，原类保持原样。
- 作者 `PDDRFFSelection` 活动实现只比较目标0和1；P4按已确认论文基线使用三目标严格Pareto支配，公式为 `dominatedBy + 1/(dominates+1)`。
- 作者 `MOHPSOQBuilder` 活动子群为 `15/15/55/15`；P4采用用户确认的ESWA Table 9 `20/20/20/40`。
- 作者部分 worker 变异调用被注释且随机源分散；P4四向量算子全部使用注入的 `PseudoRandomGenerator`。
- 原 `MOHPSOQ + EDHHFSPW` 保留为 `author_actual`，没有被新闭环覆盖或改名。

## O1–O9编号

代码与日志固定采用总体v2：`O1–O3=JS`、`O4–O6=WA`、`O7–O9=MA`。ESWA Table 8中MA/WA编号相反，仅作为来源差异记录，不在运行时提供第二套编号。

## 明确未实现

未出现疲劳、CFVF、容量6个人档案、Q-pbest、双Q、O10–O13或CA-TA。当前个人历史是不设容量上限的原始非支配历史，只服务原pbest选择。
