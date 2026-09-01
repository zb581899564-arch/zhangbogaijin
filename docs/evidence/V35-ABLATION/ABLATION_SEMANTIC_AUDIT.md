# V35 Final A0--A4 语义审计

审计版本：`v35-final-a0-a4-ablation-v1`。

## 已核对的 fail-closed 约束

- 仅允许 A0--A4 五个预注册臂，且只允许相邻比较；
- 每臂明确绑定 `V35ProductionConfiguration`，运行前校验其开关与该臂一致；
- 所有臂固定 FM3、原 Qg、`GLOBAL_ORIGINAL` PDDR、`CA-TA-Lite → inherited LS`、方向教师池关闭；
- A0--A2 不得隐式继承双Q；A3/A4 必须是 `P=5/G=5`、`rho=0` 的硬冻结双Q；
- A2 起 CFVF 取代结构化基线更新，避免同一臂混入两种全局更新；
- A4 的动态 local-FE budget 与 CA-TA-Lite 被明确定义为同一个组合贡献块；
- 小烟测 Runner 固定为 `population=10`、`MaxFEs=2000`，拒绝 CLI 改成正式预算，不能成为 500k 后门；
- `V35FairRunner` 的正式问题仍采用 FM3、单族、序列无关设置和 `ShiftMode=NONE`。

## 未被该审计证明的事项

本审计不证明五臂的性能优劣、不构成显著性检验、不授权任何 500k 或多实例运行，也不代表 PF-SDST、产品族、设置序列相关或时间移位已经进入 Final 主线。

## 代码与测试锚点

- `java-jmetal58/jmetal-algorithm/.../v35/V35FinalAblationProfile.java`
- `java-jmetal58/jmetal-exec/.../ZhangBoV35FinalAblationSmokeRunner.java`
- `V35FinalAblationProfileTest`：2 项通过。
- `ZhangBoV35FinalAblationSmokeRunnerTest`：2 项通过。

根代理于 2026-08-22 在当前工作树复跑了上述 4 项定向测试；该工作树存在其他历史 WIP，测试只用于该消融入口的定向核验。
