# P7.2 CA-TA-VNS 实施报告

## 结论

P7.2已完成工程接入。CA-TA在P6.5统一子群语义上实现144类上下文、六类瓶颈、80/20工厂选择、等评价预算Test-and-Apply，以及局部候选进入PDDR/谱系但不回写本轮双Q奖励的信用隔离。

状态：

```text
ca_ta_engineering_validated=true
ca_ta_scheme_aligned=true
sampled_reproduction_accepted=false
full_reproduction_accepted=false
```

## 固定配置

```text
nTest=1
applyMultiplier=1
applyExploreProbability=0.10
stagnationThreshold=3
factoryNeedProbability=0.80
factoryUniformExploreProbability=0.20
phase=EARLY[0,0.33),MIDDLE[0.33,0.67),LATE[0.67,1]
bottleneck=SEQ,MAC,WOR,SET,FAT,BAL
```

## 生产顺序与预算

```text
全局CFVF后代生成并唯一评价
→ 冻结本轮Qg/Qp奖励
→ CA-TA选择上下文、工厂和邻域
→ 局部候选经计数网关完整评价
→ 携带INTRA_FACTORY_VNS、父槽位、谱系和预评价标记
→ 评价后PDDR合并global/local/parent
→ 谱系档案和个人领导继承
```

外层评价识别预评价标记，局部候选不会重复计FE。预测、动作掩码、排序和非法候选拒绝均不计FE。剩余预算不足一个完整100粒子全局代时，算法在`MaxFEs`前安全停止，不生成半代；因此普通2000 FE上限烟测的最终值可能低于2000，这不是预算泄漏。

## Test-and-Apply

- Test对每个有效邻域稳定取一个候选并各评价一次。
- 胜出字典序为成功、方向收益、wall-clock、完整评价数、少调用、邻域编号。
- Apply集中调用胜出邻域，并以10%概率从合法动作探索。
- 连续3次失败返回Test；上下文变化切换统计桶但不清空历史。
- 接受规则为G1严格改善Cmax、G2严格改善TEC、G3严格改善TWC、G4沿用三目标平衡改善。

## 验证结果

- 全部`4×3×2×6=144`上下文均由组件测试覆盖。
- 六类瓶颈、Need权重、80/20选择、冻结随机事件、方向接受和预评价来源映射均有定向测试。
- `ZhangBoCaTaIntegrationSmokeTest`两项通过：真实`20_2_3_1`链路与固定显式初始种群重放。
- 张博相关测试共65项通过，见`zhangbo-regression-tests.log`。
- 六模块Maven package成功；关键类Java 8 major version为52。
- 完整兼容回归仅保留P1登记的3个作者既有errors，无新增failure/error签名。

## 可重复性边界

固定显式初始种群和seed时，CA-TA事件、Qg/Qp表哈希和结果可重放。普通作者Runner仍含作者既有未受控初始化随机性，所以本报告不声称整条作者实验运行固定seed完全一致。

## 未做事项

- 未启动P8集成消融矩阵。
- 未运行500000 FE正式实验。
- 未设置`sampled_reproduction_accepted`或`full_reproduction_accepted`为true。
