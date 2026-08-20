# P5.1 生产疲劳解码SUT/MA校正报告

日期：2026-08-09  
状态：`completed`

## 结论

非零疲劳生产路径已从P5历史语义校正为实例级固定SUT、显式第一阶段MA/WA和统一PT/SET分解。默认构造器及全部`r_k=0`仍直接执行P4.1冻结的作者评价体，没有改变作者兼容入口。

## 已实现

- `instance-extensions/v1`包含45份严格UTF-8扩展和总manifest；SUT按实例SHA-256、固定seed、工件、阶段键控生成，范围为`1..9`。
- 非零疲劳时长为`PT0=ST/(MS*WE)`、`SET0=SUT/WE`、`AT0=PT0+SET0`；同一疲劳倍率分别作用于PT与SET。
- 轨迹schema升级为2，记录基础/实际加工、设置及总时长，并保存实例扩展配置哈希。
- 第一阶段按工件身份读取MA和WA；后续阶段仍采用作者FAM和疲劳ECT。
- 非零疲劳初始化及作者更新分支在完整评价前执行确定性的第一阶段MA/WA合法域闭合；评价器本身仍严格拒绝非法资源，不进行随机修复。该闭合只作用于张博活动疲劳路径，作者默认/`r=0`路径不变。
- `CanonicalEadhfspInstanceLoader`复用同一SUT生成器；`20_2_3_1`的两条读取路径逐项一致。

## 验证

- 45份清单：数量、实例绑定、维度、`1..9`范围、遍历顺序无关、字节稳定全部通过。
- 手算/合成/集成测试：PT、SET、AT、倍率分解、MA响应、WA响应、输入不变性通过，容差`1e-9`。
- P6.4 100粒子/2000 FE重验通过：1个预热代、10个P-block代、8个G-block代；Qg 76动作/36 TD；Qp 1800动作/1000训练转移；最终FE=2000。
- P2–P7.1定向回归共102项，0 failures、0 errors；旧核心回归仍为651项、0 failures、3个P1既有errors、6 skipped。
- Java 8目标构建通过。

## 边界

- 未修改作者原类、只读基线和历史P5证据。
- 未实现主动休息、多技能、第五染色体、第四目标或正式500000 FE实验。
- `sampled_reproduction_accepted=false`、`full_reproduction_accepted=false`。

## 状态标志

```text
sut_instance_extension_engineering_validated=true
fatigue_duration_decomposition_validated=true
first_stage_ma_evaluation_validated=true
sampled_reproduction_accepted=false
full_reproduction_accepted=false
```
