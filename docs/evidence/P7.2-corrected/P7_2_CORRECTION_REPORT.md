# P7.2 CA-TA 校正报告

日期：2026-08-09

## 校正结果

- Test完成条件由“任一邻域已有记录”改为“当前合法掩码内每个邻域均达到`nTest`”。
- Apply完整评价预算改为`K × nTest × applyMultiplier`，其中`K`是当前合法邻域数。
- 合法动作掩码变化时开启新的Test epoch，不继承不兼容的完成状态。
- CA-TA由每子群单一代表改为对每个已评价全局后代按稳定顺序执行。
- Qg/Qp奖励在局部搜索前结算；局部候选带预评价标记进入PDDR和谱系档案，不回写本轮Q信用，也不重复计FE。
- `B7`禁用FAT瓶颈，`FULL`启用FAT瓶颈；两者不再共享机制向量。

## 验证

- CA-TA上下文测试覆盖`4 × 3 × 2 × 6 = 144`种组合。
- P8校正矩阵中Test-and-Apply标签完成工程运行，预算均未超过2000 FE。
- 228条标签记录全部完成；非法解为0，异常CFVF repair为0。
- 定向测试与Java 8目标构建通过；JDK 17下旧JaCoCo 0.7.7需以`-Djacoco.skip=true`跳过。

当前状态：

```text
ca_ta_engineering_validated=true
ca_ta_scheme_aligned=true
sampled_reproduction_accepted=false
full_reproduction_accepted=false
```

当前运行证据见`docs/evidence/P8-v2`。旧P7.2结果仅保留为`legacy_pre_cata_correction`。
