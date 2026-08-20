# 遗留问题与P8.1验收门

| ID | 已确认问题 | 正式修复口径 | 必须通过的证据 |
|---|---|---|---|
| L-01 | 作者评价中WA未进入活动赋值，工人实际全为0 | 正式解码按JS逆映射读取第一阶段WA | 非标准JS、非零WA资源及目标响应测试 |
| L-02 | 作者评价计算了首阶段MA但活动路径未使用 | 正式解码按工件身份直接使用首阶段MA | 非零MA改变对应工件机器、轨迹和目标 |
| L-03 | 作者路径以`0.1×ST`替代SUT | FM0–FM3统一读取实例绑定SUT | PT/SET/AT手算与扩展哈希测试 |
| L-04 | 默认Solution固定8阶段、静态域、默认实例和浅复制 | 实例绑定四向量工厂及深复制 | 双实例同JVM、复制隔离、无默认文件依赖 |
| L-05 | 普通FULL的CA-TA启用依赖P8 profile，smoke为空路径 | 生产/P8统一局部搜索配置 | 普通FULL的CA-TA FE/Test/Event均大于0 |
| L-06 | 双Q用包含局部FE的计数推进B=5区块 | 预热后按完成的外层代推进区块 | 注入不同局部FE仍保持相同P/G边界 |
| L-07 | 固定O1–O13路径没有O13恢复增益门 | 共享候选语义验收门 | fixed与CA-TA均拒绝无恢复增益O13 |
| L-08 | AUTHOR_ACTUAL运行被写成fatigue_improved | semanticTag由显式解码模式生成并校验 | 配置、Solution、结果tag一致性测试 |
| L-09 | 疲劳参数Codec未逐值锁定范围和`-0.0` | 严格范围、有限值、规范零与UTF-8校验 | 非法参数/字节/哈希拒绝测试 |
| L-10 | 黄金桥只证明输入物化，未证明生产解码 | 对规范生产问题输出完整黄金轨迹 | 资源身份、公式分量和三主目标oracle对照 |

`A0_AUTHOR_DIAGNOSTIC`仅用于证明L-01至L-04的来源，不进入P8-v3运行或参考前沿。

## 完成核对

| ID | 状态 | 当前证据 |
|---|---|---|
| L-01/L-02 | fixed | `ZhangBoCanonicalProductionProblemTest`覆盖非标准JS下的MA/WA工件身份与资源响应 |
| L-03 | fixed | `ZhangBoCanonicalFormulaTest`逐值核对PT、SET、AT、倍率和delta |
| L-04 | fixed | canonical solution factory不读取默认实例；跨实例同JVM与深复制测试通过 |
| L-05 | fixed | 普通FULL与P8 FULL烟测均产生非零CA-TA FE、Test/Apply及事件 |
| L-06 | fixed | `ZhangBoDualQCoordinatorTest`证明局部FE不推进五代区块 |
| L-07 | fixed | `ZhangBoNaturalRecoveryGate`由fixed与CA-TA两路径共用并有组件测试 |
| L-08 | fixed | FM0、FM1、FM2、FM3分别使用`deterministic_canonical`、`fatigue_fm1`、`fatigue_fm2`、`fatigue_fm3` |
| L-09 | fixed | 严格UTF-8、范围、非有限值、负零、manifest和实例哈希拒绝测试通过 |
| L-10 | fixed | 黄金桥由独立canonical loader加载并执行七槽生产评价；P3仍只作共同论文语义oracle |

原作者实现未被“就地修补”；它被完整保留为只读诊断证据，正式P8-v3调用图中不再出现作者问题类、默认Solution或作者巨型更新器。
