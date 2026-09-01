# V35 Stage2 Gate3 与远端吞吐验收

日期：2026-08-23  
协议：`v35-phase-consistent-budget-v1`  
远端目录：`/home/inspur/aicomp/zhangbo-v35-stage2-phasebudget-20260823-r3`  
训练机：`inspur-NP5570M5`，OpenJDK 11.0.27，32 逻辑 CPU，约 125 GiB RAM。

## 结论

`A0--A4` 的 50k Gate3 和 4/8/12/16 JVM 吞吐测试均通过协议门。接受的并发上限为：

```text
FORMAL_MAX_PARALLEL = 16
```

这只是基础设施容量结论；它不启动 500k 正式矩阵，也不构成任何算法质量或论文优越性结论。

## 冻结身份

| 项目 | 值 |
|---|---|
| Frozen fat jar SHA-256 | `8dad8f40266feeaa4cdb9b47dbe4e342d9064847df32c7f2933149b9b6bad8b9` |
| Java 外部预检工具 | Java 8 classfile major `52`，在远端 Java 11 中通过分类器测试 |
| 算法修改 | 无；预算审计只读取运行后的 `RunRecord` |
| Q phase | `100 × 50 = 5000 FE` |
| 远端证据清单 SHA-256 | `e53edd1c7d23f7d21e5d9ef718fa8c137658cf72e3160763b48b9b0fb4b78729` |

远端输入逐文件 SHA-256：`20_2_3_1.txt=47d32d48...61578a08`；
`20_2_3_1.setup.txt=c39040db...c5a3b2bc`；
`20_2_3_1.fatigue.txt=7116c996...978787a9`。

首次远端部署因外部工具 classfile major `61` 与 Java 11 不兼容而在 A0 前停止；第二次因
外部 CLI 把论文标签 `A0` 错作 Java enum 名而在 A0 前停止。两次均未执行算法。最终 r3
部署使用 Java 8 工具，并加入 `A0--A4` 标签到 enum 的显式、fail-closed 映射；该映射与
预算分类单元测试均通过。

## Gate3：`20_2_3_1`、seed `20260828`、50k

五臂共享初始四向量哈希：

```text
9fb09e602019393d54f8083448839b9193a78afef2f122cb8d89f4511afc4466
```

| Arm | actual FE = decoder calls | remaining FE | 利用率 | 终止类型 | Q 外层周期 / 轮次 |
|---|---:|---:|---:|---|---:|
| A0 | 50000 | 0 | 100.000% | `EXACT_MAX_FE` | 2 / 100 |
| A1 | 50000 | 0 | 100.000% | `EXACT_MAX_FE` | 2 / 100 |
| A2 | 50000 | 0 | 100.000% | `EXACT_MAX_FE` | 2 / 100 |
| A3 | 50000 | 0 | 100.000% | `EXACT_MAX_FE` | 2 / 100 |
| A4 | 48269 | 1731 | 96.538% | `PHASE_CONSISTENT_TAIL_STOP` | 6 / 300 |

组审计为 `VALID`：实际 FE 范围 `1731 < 5000`。全部五臂均为 `COMPLETED`，且非法解、
重复评价、异常 repair 均为 0；各臂的批准机制门亦通过。

50k 是协议正确性门而非正式 500k 利用率门，因此 A4 的 96.538% 不构成失败。对于正式
500k，协议仍要求利用率严格大于 99%。

## 吞吐：A4、冻结 jar、20k phase-bound 诊断

每个 JVM 独占一个逻辑 CPU；每条运行均产生 `15258` FE、`4742` 的尾段、
`PHASE_CONSISTENT_TAIL_STOP`，并通过 decoder 闭合、非法/重复解与机制门。20k 是吞吐
诊断，故不适用正式 500k 的利用率大于 99% 约束。

| JVM 并发 | 完成 / 请求 | 总 FE | 墙钟秒 | FE / 小时 | 失败 |
|---:|---:|---:|---:|---:|
| 4 | 4 / 4 | 61032 | 17.6740 | 12.43M | 0 |
| 8 | 8 / 8 | 122064 | 17.4443 | 25.19M | 0 |
| 12 | 12 / 12 | 183096 | 17.9212 | 36.78M | 0 |
| 16 | 16 / 16 | 244128 | 17.4615 | 50.33M | 0 |

16 JVM 档：CPU pin 为 `0..15`；单 JVM 平均峰值 RSS 约 433611 KiB，最大 484396 KiB；
未见 `OutOfMemory`、GC 错误或 swap 使用。结束快照仍有约 117.7 GiB 可用内存，swap 使用
为 0。因此选择已实际验证且不超订阅的 `16`，不外推至 17--32。

## 逐文件核验

远端共检查 45 份 `preflight-gate.properties`（5 份 Gate3 + 40 份吞吐）。每份均满足：

```text
gateStatus=PASS
status=COMPLETED
actualFE=decoderCalls
0 < actualFE <= requestedFE
remainingFE < qPhaseFE
phaseBoundAccepted=true
illegalSolutions=0
duplicateEvaluations=0
```

远端 `evidence/` 的 1180 个文件已生成 SHA-256 清单。该目录保留在训练机，不将 61 MiB
诊断原始日志重复落盘到本地；其路径、总清单哈希和本报告中的逐项数值构成可复查指针。
