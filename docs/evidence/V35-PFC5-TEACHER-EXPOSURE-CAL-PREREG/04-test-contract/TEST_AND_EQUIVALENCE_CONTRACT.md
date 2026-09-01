# 测试与行为等价合同（未执行）

日期：2026-08-30　|　状态：**计划，未实现、未运行任何实验**

---

## 1. 十五项测试

| # | 测试 | 判据 | 失败后果 |
|---|---|---|---|
| 1 | λ=0 精确调用旧路径 | 静态可验证 `if (lambda==0) return existingFrozenTeacherSelection(...)`，且该分支不经过任何 exposure 计算 | `CAL_IMPLEMENTATION_GATE=FAILED` |
| 2 | C0 行为逐项一致 | 与当前 A4 逐项比：RNG consumption sequence、teacher identity sequence、Qg/Qp action sequence、Q table hashes、CFVF candidate sequence、PDDR selection sequence、CA-TA sequence、evaluation trace、actualFE、working population hash、canonical final front | `CAL_IMPLEMENTATION_GATE=FAILED` |
| 3 | Qp / Qg exposure 互相隔离 | 两个 controller 的 exposure 表无共享引用；Qp 侧计数不进入 Qg 判据，反之亦然 | 实现缺陷，须修复后重测 |
| 4 | 非法 teacher 永不进入候选 | 对 `LCS-01…LCS-10` 逐条断言；任一非法候选出现在比较中即失败 | 实现缺陷 |
| 5 | DSCR 清洗后才计算候选 | `sanitizeTeacherCaches` 先于候选过滤与 `selectQgLeader`（源码 `:2794-2797` 先于 `:2809-2839` 先于 `:2754-2757`） | 实现缺陷 |
| 6 | 完全重复 objective 使用稳定指纹破平 | 构造同目标值不同指纹的两个候选，判定结果可复现且与 `ZhangBoQgController.java:404` 一致 | 实现缺陷 |
| 7 | 不增加 FE | `actualFE == decoderCalls`，且与 C0 同 seed 同 MaxFEs 下 FE 完全相同 | 实现缺陷 |
| 8 | 不改变 Q 动作与奖励归属 | Q table hash、action sequence、reward 序列与 C0 一致（第 2 项已覆盖） | 实现缺陷 |
| 9 | 正式 Runner 拒绝 C1–C3 | 正式链路传入 `lambda>0` 必须直接拒绝，不得静默忽略 | 实现缺陷 |
| 10 | 不同 λ 配置 hash 唯一 | 各配置 `configurationHash()` 两两不同，且可复现 | 实现缺陷 |
| 11 | 2k C0 行为等价 | 2k 尺度上 C0 与当前 A4 逐项一致 | `CAL_IMPLEMENTATION_GATE=FAILED` |
| 12 | 2k C1–C3 机制真实触发 | 2k 尺度上 λ>0 确实产生至少一次与 C0 不同的 teacher identity（否则旋钮未接上） | 记录为 `MECHANISM_NOT_ACTIVATED`，须审查后再谈 Race |
| 13 | 同 seed 四配置初群 hash 一致 | 四个配置的 `initialPopulationHashV35/P8` 与 `snapshotSha256` 完全相同 | 实现缺陷 |
| 14 | 审计 ON/OFF 不改变搜索 | 开/关旁路审计不影响任何行为哈希 | 实现缺陷 |
| 15 | Java 8 major version 52 | 所有新增 class 字节码主版本实测为 52 | 实现缺陷 |

---

## 2. 等价门的性质

第 1 项是**结构性**的：λ=0 直接调用原有 `compare()`，不经过归一化与 exposure 项，因此第 2 项的逐项一致是构造保证，不依赖"性能相近"。

第 12 项是本次新增的重点防线：由于离线分析已提示杠杆偏弱（见 `02-calibration-definition/lambda-candidate-audit.csv`），**必须在 2k 尺度先验证 C1–C3 真的会改变 teacher identity**。若 2k 下 λ=0.30 都触发不了任何改变，则 Race 的 32 次运行将全部退化为 C0，属机械性零结果，不能用于证伪假设 —— 此时应停止并上报，而不是照跑。

---

## 3. 执行顺序约束

```text
1. 实现 → 2. 第 1/9/10/15 项静态与单元级测试
         → 3. 第 11 项 2k C0 等价
         → 4. 第 12 项 2k 机制触发
         → 5. 其余各项
         → 6. 全部通过后才允许向用户申请 Configuration Race 授权
```

**第 11 项不通过 → `CAL_IMPLEMENTATION_GATE=FAILED` → 禁止运行 Race。**
**第 12 项不通过 → 停止上报，禁止运行 Race。**
