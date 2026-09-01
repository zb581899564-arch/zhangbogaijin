# 预登记：Qp 冷启动并列策略最小验证

## 固定输入与边界

```text
instance = 20_2_3_1
seeds = 20260822, 20260823, 20260824
population = 100
MaxFEs = 50000
decoder = FM3
ShiftMode = NONE
family = DEGENERATE_SINGLE_FAMILY
setup = SEQUENCE_INDEPENDENT
mixture = [20,40,20,20]
PDDR = GLOBAL_ORIGINAL
local search = CA-TA-Lite -> inherited LS
directional teacher pool = OFF
```

## 唯一变量

Q0和Q1均实际运行Qp四动作、均更新谱系档案、均使用同步Qg/Qp且所有Qp settlement为
`OBSERVE_ONLY_ALL_CYCLES`。Q0的`GreedyTiePolicy=FIRST_VALID`；Q1的唯一变化为
`GreedyTiePolicy=DIRECTIONAL_IF_TIED`。只有在贪心、合法动作Q值并列且DIRECTIONAL合法时，
Q1选择DIRECTIONAL；探索分支、非并列行、奖励、TD写入、Q表、候选评价和随机调用数均不变。

## 运行与来源

新增物理运行只有Q1三个seed。D1与Q0均复用已验收的D1/Q0运行并通过其`sourceRunId`关联。
先以Controller和配置单元测试确认非诊断默认仍为`FIRST_VALID`，再跑Q1；每次运行必须
`COMPLETED`、`actualFE=decoderCalls=50000`、前沿非空、目标有限、非法解/重复评价/CFVF repair为0，
同seed三个臂初始种群哈希一致。

## 预注册裁决

`Q0 -> Q1`的稳定改善定义为：至少2/3 seed出现HV上升且IGD下降，且中位
`DeltaHV >= +2%`或中位`DeltaIGD <= -10%`。

- 若Q0→Q1稳定改善，且Q1相对D1不触发原稳定退化门：`COLD_START_TIE_BREAK_CAUSAL`。
- 若Q0→Q1稳定改善，但Q1相对D1仍触发稳定退化：`PARTIAL_COLD_START_RESCUE`。
- 否则：`COLD_START_TIE_BREAK_NOT_CONFIRMED`。

所有指标在全部9条D1/Q0/Q1前沿验收后一次性冻结的共同参考集下计算，并另报两两reference敏感性。
任何结果均不自动修改正式算法；如证实，也只构成后续独立修复臂的设计依据。
