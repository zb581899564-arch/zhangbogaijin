# V35 Competitive Superiority Route Decision

日期：`2026-08-30`

## 决策

当前开发主线从“继续扩大 FC5 Failure Replay 根因追踪”转为：

```text
Gap Probe
-> one-family leverage audit
-> C0/C1/C2/C3 staged development
-> DOE migration
-> untouched Validation
-> Final Freeze
-> leave-one-out ablation
-> staged formal baseline comparison
```

原因：FC5 250k未复现候选膨胀与经典利用断裂；Teacher Exposure提议只覆盖全部教师事件的1.12%，已在实现前因结构性无杠杆关闭。继续扩大旧诊断对“形成可在正式实验中击败强baseline的算法”的边际价值低于竞争力摸底和一次有杠杆的协调修订。

## 吸收的关键修正

1. Gap Probe同时纳入NSGA-II-F和SPEA2-F，避免事后挑最强external。
2. 未来只允许一个repair family的四档强度，不允许三个异质修法赛马。
3. 100k只作cheap rejection；250k作Top2筛选；500k作development final。
4. Development、Validation、Final Test三层实例永久区分。
5. DOE迁移先四配比250k；单challenger先18条500k；只有广泛重排才重做完整DOE。
6. 正式主消融优先leave-one-component-out，渐进链保留为历史/附录证据。
7. Formal Main先10 seeds；是否补20由预注册功效与稳定性规则决定。

## 不变量

```ini
algorithmChanged=false
PDDRChanged=false
CFVFRemoved=false
DualQRemoved=false
CaTaRemoved=false
gapProbeStarted=false
validationStarted=false
formalMatrixRunning=false
FinalCandidateApproved=false
```

本决策只更新治理与路线文档，不授权任何FE、上传或远端运行。

