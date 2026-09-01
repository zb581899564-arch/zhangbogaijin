# V35 Stage2：隔离回归范围与结果

日期：2026-08-23  
状态：`PRODUCTION_DIRECTED_REGRESSION_ACCEPTED`

## 隔离原则

冻结副本中的历史测试会生成或覆写历史证据。因此本轮建立字节保真的临时测试副本，保留
实例的 CRLF 与疲劳参数的 LF 原始物理字节；不对冻结副本、冻结 jar 或正式输入做写入。

常规 Git 检出会把所有文本统一转换为 CRLF 或 LF，导致两类已有严格输入检查之一失败：

- 疲劳参数 Codec 按设计拒绝 CR 字符；
- 既有实例/扩展 manifest 绑定其原始 SHA-256。

因此此次回归以字节保真副本运行，不把“换行符自动转换”误判成算法缺陷。

## 通过项

| 范围 | 数量 | 结果 |
|---|---:|---|
| `jmetal-problem` 全部测试 | 67 | PASS |
| V35 定向机制测试 | 32 | PASS |
| 外部预算分类器 `V35ProductionPreflightBudgetTest` | 6 个边界类 | PASS |
| 外部 Master budget adapter 测试 | 分类与五臂组审计 | PASS |
| 六模块 Maven package（隔离副本，`maven.javadoc.skip=true`, `skipTests=true`） | Java 8 target 构建 | PASS |
| 冻结 fat jar `V35FairRunner` | `CAFEBABE`, major version 52 | PASS |

V35 定向测试覆盖 `V35FairRunner`、CA-TA-Lite、DSCR、教师缓存、最终消融 profile、
宏候选网关、DSCR Gate、CA-TA 事件链、生命周期和被动档案隔离。

## 历史套件的可移植性限制

历史 205 项套件包含一部分“证据文件必须逐字节重放”的测试。`V35Fc0PrefinalArchiveTest`、
`V35Fc2LocalFePacingTest` 与 `V35P101TeacherPoolVerificationTest` 将原始 `projectRoot` 绝对路径
写入或比较至历史证据正文。将同一冻结源码放入另一路径时，测试按设计报告内容不同；这不是
运行时算法、预算、随机流或 jar 的差异。

本阶段没有为让这些路径相关测试通过而修改冻结源码、重写历史证据或改动 jar。它们不是
`v35-phase-consistent-budget-v1` 的接受依据。

## 结论

预算协议是外部只读审计层：它未改变 V35 搜索逻辑。Gate3 的生产相关回归、预算分类器、
五臂 50k 实测、jar SHA-256 与 Java 8 目标共同构成可移植接受证据。历史 205 项的原工作树
验收状态保留，但不在隔离路径下重新宣称为“全绿”。
