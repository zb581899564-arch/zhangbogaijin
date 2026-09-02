# V35-QP-V2-SINGLE-AXIS Phase B1 裁决：语义未定义，工作包在第一硬门处停止

裁决日期：`2026-09-02`
触发条款：任务书第三节（第一硬门：不得自行发明K语义）+ AGENTS.md §10.1（fail-closed）。

## 1. 裁决

Phase B1 的语义来源核查（`../01-semantic-source-audit/SEMANTIC_SOURCE_AUDIT.md`）确认：
获批材料不能唯一确定 Qp-v2 单轴 K 的语义。七项必需定义中五项完全缺失
（计数对象、作用集合、K>1选择/破平规则、候选不足fallback、RNG消费契约），
两项仅有约束或要求而无机制（动作/奖励/容量不变式的确切边界、K=1→A4的还原机制）。

按任务书第一硬门，本工作包立即停止：

```ini
QP_V2_SEMANTICS_UNDERDEFINED=true
QP_V2_IMPLEMENTED=false
QP_V2_EXPERIMENT_STARTED=false
```

## 2. 缺失定义清单（阻断项）

1. K计数的对象（每动作候选池 / 档案层级 / 领导槽位，三者互不等价）；
2. K生效的现有合法候选集合（每动作单例映射 / 个人档案L=6 / COMPLEMENTARY质量集）；
3. K>1时候选选择规则（锦标赛/argmin/均匀随机/轮转）；
4. K>1时稳定破平规则；
5. 候选不足（档案< K、质量集< K、mask去重致动作候选缺失）时的fallback；
6. RNG消费契约（是否新增抽取；现有Qp为每次选择1–2次抽取，Controller :451/:455）；
7. K=1精确还原当前A4的机制证明（"每动作按自身排序取top-1"与当前argmin重合只是最自然读法，
   未经批准；其他读法不还原A4）；
8. K与四动作集合、mask去重、Qp奖励、档案容量的不变式边界（仅有"不得同时调"禁令）。

## 3. 最终状态（任务书第七节要求的完整输出）

```ini
QP_V2_SEMANTICS_FROZEN=false
QP_V2_IMPLEMENTED=false
K1_BEHAVIOR_EQUIVALENT=NOT_OBSERVABLE(未实现未运行，fail-closed记false)
K1_BEHAVIOR_EQUIVALENT=false
K2_K4_MECHANISM_TRIGGERED=false
PHASE_B1_ENGINEERING_GATE=BLOCKED
QP_V2_250K_ELIGIBLE=false
QP_V2_250K_PREREGISTERED=false
QP_V2_250K_STARTED=false
DOE_AUTHORIZED=false
VALIDATION_AUTHORIZED=false
FORMAL_AUTHORIZED=false
formalMatrixRunning=false
formalJarChanged=false
PDDRChanged=false
CFVFChanged=false
DualQActionRewardChanged=false
CaTaChanged=false
```

`PHASE_B1_ENGINEERING_GATE=BLOCKED` 的具体阻塞条件：K语义在获批材料中欠定义，
无法在不自行发明算法的前提下实现 `V35QpV2Profile` 或任何实验Runner。
该阻塞不是执行失败：工作包按设计在第一硬门处 fail-closed。

## 4. 未发生事项（显式声明）

- 未实现任何 `V35QpV2Profile`、Runner或实验Jar（`QP_V2_IMPLEMENTED=false`）；
- 未启动任何实验：本地2000 FE等价门、训练机20k OFF/ON、K2–K4触发测试均未运行
  （`QP_V2_EXPERIMENT_STARTED=false`，`newFEConsumed=0`）；
- 未启动250k配置赛马、DOE、Validation、Final Freeze或正式矩阵；
- 正式Jar（`8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9`）前后实测一致，
  未重建未覆盖；PDDR/CFVF/双Q动作奖励/CA-TA全部零改动；
- 02–06号证据子目录未创建（未到达对应阶段，不造空目录）。

## 5. 恢复条件

须用户以新的明确授权冻结K语义预注册（补齐第2节8项定义）后方可重启Phase B1实现。
本裁决不预设任何具体K语义，也不构成对任何读法的暗示认可。
