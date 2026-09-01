# 实现边界（只写计划，未实现）

日期：2026-08-30　|　状态：**未实现、未编译、未上传、未运行**

---

## 1. 最小代码边界

未来实现**只允许**新增以下单元，并只在指定注入点接线：

| 新增单元 | 职责 | 不得做的事 |
|---|---|---|
| `TeacherExposureConfiguration`（不可变） | 承载 `lambda`、`exposureNormalization`、`scope`、配置 hash | 不得持有可变状态，不得在运行中被改写 |
| `ExposureAwareTeacherSelector` | 实现 `adjustedLoss` 成对比较；`lambda==0` 时**直接委托**原 `compare()` | 不得新增/删除候选，不得改掩码，不得改随机流 |
| controller-local exposure 旁路统计 | 按 `(QG, 子群)` 维护 `Map<fingerprint, Long>` 与 `maxCount` | 不得与 Qp 共享，不得回写 Q 表 |
| 专用 `CalibrationRunner` | 仅用于 Calibration 运行 | 正式 Runner 必须拒绝 `lambda>0` |
| Qg teacher identity 选择点注入 | 位于 `ZhangBoQgController.tournament` 的比较调用（源码 `:299`） | **唯一**注入点 |

**禁止修改**：Q 动作选择器、Q 奖励、Q 表更新、DSCR、CFVF 候选生成、PDDR、CA-TA、个人档案容量、`pool()`、`compare()` 的原有分支逻辑、动作合法掩码、随机抽取次数与顺序。

---

## 2. λ=0 的分支契约（构造性保证）

```java
if (lambda == 0) {
    return existingFrozenTeacherSelection(...);   // 原 compare()，逐字未改
} else {
    return exposureAwareSelection(...);           // 仅替换成对比较判据
}
```

C0 分支不归一化、不加 `0.0`、不重排、不触碰 exposure 表（旁路统计仍可记录，但**不参与任何判断**）。

---

## 3. 注入点为何只能是成对比较

源码 `ZhangBoQgController.java:292-303`：锦标赛每轮恰好两次 `nextInt`（`:295-296`），然后**只比较两个元素**（`:299`）。

若改成全池 argmin：
- 随机抽取次数或语义改变 → RNG 序列错位 → C0 等价门必然失败；
- 搜索语义从二元锦标赛变为确定性贪心 → 属「自创另一套搜索机制」，被 §6.2 明确禁止。

因此正确做法是：**保持抽取次数与比较次数不变，只把判据换成 `adjustedLoss`**。

---

## 4. 冻结不变量（实现后必须逐项保持）

```text
FM3                             ShiftMode=NONE                single family
sequence-independent SUT        PDDR=GLOBAL_ORIGINAL          mixture=20/40/20/20
Pacing                          CA-TA-Lite → inherited LS     CFVF 语义
Qg 动作集合                      Qp 动作集合                     Q 状态
Q 奖励                           P5/G5                          warmup=10%
rho=0                           个人档案容量=6                   DSCR
CA-TA                           inherited LS                   方向教师池=OFF
population=100                  objective slots=[0,1,6]
```

---

## 5. 调用链保持（不得移位）

```text
Q action
→ 该 action 原合法候选集合
→ teacher identity selection（此处且仅此处可插入 exposure 判据；Qg action 2 以外的路径不插入）
→ 实际 CFVF 行为
→ reward 返回原 Q action
```
