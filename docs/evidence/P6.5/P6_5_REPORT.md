# P6.5 子群语义迁移报告

## 结论

P6.5已完成。张博创新链路现统一为`G1_CMAX/G2_TEC/G3_TWC/G4_BALANCED`，作者四个物理粒子槽位和零创新调用顺序未改变。

状态：

```text
subswarm_semantics_migration_validated=true
sampled_reproduction_accepted=false
full_reproduction_accepted=false
```

## 实施内容

1. 以`ZhangBoSubSwarmSemantics`集中定义物理槽位映射、目标索引、方向标量、Need权重和稳定映射哈希。
2. 迁移partition、Qg/Qp、个人档案、CFVF领导、PDDR、邻域方向比较、配置和诊断日志。
3. G4固定为平衡/PDDR角色，不再伪装为单目标子群。
4. 新Q表按当前语义零初始化；迁移前子群感知结果按`legacy_pre_subgroup_migration`隔离。
5. 保留作者原类内部兼容代码，不通过重排物理槽或大范围改写破坏P4.1零创新母线。

## 验证结果

- `ZhangBoSubSwarmSemanticsTest`：3项通过，覆盖四角色、物理槽位、目标职责和稳定映射。
- 论文验证基线：10项通过，见`paper-baseline-regression.log`。
- 张博跨模块定向回归：通过，见`runner-regression.log`和P7.2的`zhangbo-regression-tests.log`。
- 完整兼容回归：651项，0 failures，3 errors，6 skipped；3个errors与P1登记的作者既有错误一致，见`full-regression-jdk17-compat.log`。
- Java 8字节码：关键类major version 52；六模块Maven package成功。

## 边界

- 本阶段证明语义迁移和工程回归，不证明正式抽样复现或论文统计结论。
- 作者Runner包含既有未受控随机初始化；只有固定显式初始种群路径可用于新增控制器事件的字节级重放主张。
- 映射表见`SUBSWARM_SEMANTICS_MAPPING.md`，历史证据范围见`LEGACY_EVIDENCE_ISOLATION.md`。

