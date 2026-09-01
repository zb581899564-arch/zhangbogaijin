# V35 Stage2-P1 技术债审计

日期：2026-08-23  
冻结算法 jar：`8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9`

## 结论

本轮只修复外部正式执行链，没有修改、重建或替换冻结算法 jar。对正式 A0--A4 路径的
只读审计、定向回归、2000 FE、50000 FE 和远端 500000 FE 五臂验收均未发现新的核心算法
阻断缺陷。此前阻断正式矩阵的关键执行债已经关闭。

## 已关闭的关键执行债

| 编号 | 原问题 | 关闭方式 | 验证 |
|---|---|---|---|
| TD-S2-01 | 冻结正式 Runner 只能运行双臂 | 新增外置 snapshot-bound A0--A4 Runner | 五臂 2k/50k/500k 全部贯通 |
| TD-S2-02 | 单一 config hash 不能表达五个 profile | RunKey 绑定 arm profile 与 runtime config hash | 变更敏感性故障注入通过 |
| TD-S2-03 | Master 未强制完整五臂 roster | v2 要求且只允许 A0--A4 各一次 | 缺臂、重复臂、未知臂均 fail-closed |
| TD-S2-04 | 旧 renderer 串行且只能双臂 | v2 按 3 fairness groups × 5 arms 调度 | 每 wave 上限 15 JVM |
| TD-S2-05 | 完成状态和输出目录契约不统一 | `.partial-*` 写入后原子移动 | 失败输出不能冒充 COMPLETED |
| TD-S2-06 | evidence 清单未逐项反向复算 | Master 在跳过、组验收和归档后均重算 | 缺文件、篡改故障注入通过 |
| TD-S2-07 | snapshot/profile/provenance 未完整进入 RunKey | 全部纳入不可变 RunKey | 任一变化都会改变 RunKey 或启动前失败 |
| TD-S2-08 | 外置 Java 工具曾产生 major 61 | 统一 `javac --release 8` | 外置主类 major version 52 |
| TD-S2-09 | CLI `A0` 与描述性枚举不一致 | 显式 arm label 映射 | A0--A4 与未知标签测试通过 |
| TD-S2-10 | CRLF/LF 可改变输入哈希 | 实例/SUT/疲劳参数按原始字节复制 | 本地与远端 SHA-256 一致 |
| TD-S2-11 | 磁盘和失败重试可能无界增长 | 120 GB 启动门、attempt 不覆盖、组后归档 | 远端启动前可用空间约 252 GB |

## 仍隔离的历史技术债

以下问题不在冻结正式运行路径上，本轮没有用修改测试预期或重建 jar 的方式掩盖：

1. `jmetal-core` 在当前 JDK 17 环境仍复现 P1 登记的 3 个旧错误，均来自历史
   `DefaultIntegerPermutationSolution` 默认读取 `EADHFSP/150_8_5_1.txt`；正式路径使用显式
   snapshot 和规范 Problem，不引用该默认创建路径。
2. 三项历史证据重放测试仍绑定旧语义/旧稳定顺序：`V35Fc0PrefinalArchiveTest`、
   `V35Fc2LocalFePacingTest`、`V35P101TeacherPoolVerificationTest`。前两项是旧哈希钉子，后一项
   的 Pareto 点集合相同但行顺序不同。它们不参与 Stage2 冻结配置或正式输出验收。
3. 单独在本机编译 `jmetal-exec` 会解析到本地仓库中陈旧的 `jmetal-algorithm` artifact；完整
   六模块 reactor 构建通过。正式运行直接使用已冻结 fat jar，因此不受该本地解析债影响。

## 回归结论

- 外置 Master v2 Python 测试：6/6 通过。
- V35 正式路径定向测试：9/9 通过。
- Java 六模块 reactor 打包：通过。
- 冻结 jar SHA-256：未变化。
- 结论：上述历史债继续登记和隔离，但不阻断 Stage2-P1 正式执行链。

