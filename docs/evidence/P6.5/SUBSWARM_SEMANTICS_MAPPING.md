# P6.5 子群语义映射

语义版本：`P6.5-subswarm-semantics-v1`

## 统一映射

| 语义角色 | 目标职责 | 目标槽 | 作者物理槽位 | Need权重 `(C,E,WC,IM,IW,SUT,FRisk)` |
|---|---|---:|---|---|
| `G1_CMAX` | 最小Cmax | 0 | `groupU1` / physical slot 0 | `(2,1,1,1,1,1,1)` |
| `G2_TEC` | 最小TEC | 1 | `groupD3` / physical slot 2 | `(1,2,1,1,1,1,1)` |
| `G3_TWC` | 最小TWC | 6 | `groupUNew` / physical slot 3 | `(1,1,2,1,1,1,1)` |
| `G4_BALANCED` | 三目标平衡/PDDR | 无单一目标 | `groupC2` / physical slot 1 | `(1,1,1,1,1,1,1)` |

作者物理执行顺序保持为 `groupU1, groupC2, groupD3, groupUNew`，对应语义顺序为 `G1, G4, G2, G3`。本次迁移不移动作者数组、粒子槽或调用顺序。

## 集中规则

- `G1/G2/G3`的方向标量分别来自归一化目标0、1、6。
- `G4`使用归一化三目标最大偏差，PDDR-FF仅作稳定次级比较。
- Qg/Qp奖励、档案方向锚点、CFVF领导、Need权重及VNS接受统一从`ZhangBoSubSwarmSemantics`读取。
- 活动创新代码禁止使用enum ordinal、裸组号或作者变量名推断目标职责。
- 配置和日志携带语义版本及稳定映射SHA-256。

