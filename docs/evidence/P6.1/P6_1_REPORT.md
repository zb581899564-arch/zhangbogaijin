# P6.1 CFVF全向量离散飞行报告

日期：2026-08-08  
语义：`fatigue_improved`生产派生线上的`ORIGINAL_QG + CFVF`  
状态：`completed`

## 结论

CFVF已经作为张博派生算法的显式更新模式接入，使用P5疲劳解码、作者个人历史选择和P6.0验收后的Q-gbest。默认开关关闭时仍走P4.1/P5兼容路径；本阶段未实现容量6个人档案、Q-pbest、双Q冻结或CA-TA-VNS。

## 固定配置

```text
seed=20260808
c1R=0.4
c2R=0.4
omegaR=0.5
pExplore=0.05
smokePopulation=100
smokeMaxFEs=2000
```

- 配置文件SHA-256：`CDB250360D0E5A41D61C6C494B6E995635C4C3640C8276001209B5E93FBF9F60`；
- `20_2_3_1.txt` SHA-256：`47D32D48E719219C5FA2B41E08278518C8162E3EB88398B5A4E8F54B61578A08`；
- 疲劳参数清单SHA-256：`7116C996E84C229A5E8CF6B17CC3682B331C035202E7CBB87F7DB3BA978787A9`。

## 实现与诊断

- JS：一次探索交换，加上认知/社会差分交换前缀；
- 资源：JS更新后通过逆映射按工件身份生成`FMW/MW/M/W`；
- 每个惯性、认知和社会动作独立伯努利保留；每个后代最多一个合法探索动作；
- 冲突优先级为`FMW > MW > M/W`；同粒度按`etaP/(etaP+etaG+1e-12)`选择，双零权重时50/50；
- 只修改第一阶段显式MA/WA，扩展WA后续块保持不变；
- 记录四向量Hamming距离、继承数、动作类型/来源、冲突胜者、repair和逐事件随机轨迹；
- 作者固定`150_8_5_1`造成的初始MA越界只在显式CFVF初始化边界确定性合法化并记录，不计入CFVF后置repair。

## 验证

- `ZhangBoCfvfUpdaterTest`：5项测试通过，覆盖四类资源动作、不同JS的工件身份对齐、JS通道、惯性、探索、双方冲突胜出、零权重冲突、异常repair、输入不变性和100次事件重放；
- `20_2_3_1`真实小实例：100粒子，2000次完整评价精确闭合，1900个CFVF后代，非法解0，正常后置repair 0；
- Runner提供`mainexeWithGlobalSearch(...)`独立入口，显式P6输出名携带领导与更新模式；旧`mainexe(...)`签名、关闭配置及原输出名保持不变。
- P2–P6定向回归共56项通过；六模块Java 8目标打包通过，抽检P6四个class均为major version 52；
- 完整旧核心回归仍为651项、0 failures、3个P1既有errors、6 skipped，三个错误签名与P5证据完全一致，没有新增失败。

关键日志：`TEST_P6_COMPONENTS.log`、`TEST_P6_INTEGRATION_2000FE.log`、`TEST_P2_THROUGH_P6_DIRECTED.log`、`BUILD_PACKAGE_JAVA8.log`和`TEST_FULL_REGRESSION_JDK17_COMPAT.log`。源码清单见`P6_SOURCE_SHA256.csv`，报告、配置和日志证据清单见`P6_EVIDENCE_SHA256.csv`。

## 状态边界

`cfvf_engineering_validated=true`、`cfvf_scheme_aligned=true`。工程默认`0.4/0.5/0.05`不代表正式最优参数；P9前不运行正式参数敏感性或500000 FE实验。`sampled_reproduction_accepted=false`、`full_reproduction_accepted=false`。
