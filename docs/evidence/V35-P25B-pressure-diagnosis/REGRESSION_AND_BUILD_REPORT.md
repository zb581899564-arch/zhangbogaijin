# V35-P25B 回归与构建验收报告

## 当前语义测试

当前压力诊断、置信回退、shadow隔离和正式主循环相关的定向测试全部通过：

```text
jmetal-problem                         67/67
V35压力诊断与主循环定向测试            31/31
P25B Runner贯通测试                    1/1
```

覆盖内容包括：目标工厂先选后诊断、五类压力边界、绝对强度/领先差距双门、BAL fail-closed、25组阈值选择、CSV输入顺序无关，以及shadow开启/关闭时主搜索前沿、FE和事件流保持一致。

## 完整历史回归

为避免完整历史证据测试改写主工作区冻结文件，在独立副本中执行：

```text
Tests run: 215
Passed:    212
Failures:  3
Errors:    0
```

三项失败均已分类，不属于当前压力诊断实现的功能回归：

1. `V35P101TeacherPoolVerificationTest.verifyPoolOnOffAnd100kAudit`
   - 旧测试要求逐位重放`pre-P10.1`前沿；
   - 新语义默认采用`BAL`全开放的压力诊断路径，旧冻结前沿不再是当前语义基准；
   - 该项保留为`legacy_pre_pressure_diagnosis`，不修改旧期望值迁就新算法。
2. `V35P241FreezeRevisionTest.rebuildsFreezeTwiceWithIdenticalDiskBytes`
3. `V35P24FreezeCaptureTest.captureFreezeManifest`
   - 两项均因隔离副本的项目根目录增加`.codex-temp/P25B-regression`而触发磁盘冻结路径漂移；
   - 这是隔离运行边界产生的路径差异，不是算法、参数或证据内容计算失败；
   - 主工作区P24冻结物保持只读，P25B held-out未通过，因此不重建P24.2。

因此不能把完整历史回归表述为“215项全绿”；准确口径是：当前语义相关测试全部通过，完整历史回归212/215通过，3项均为已解释的旧快照/隔离路径不兼容。

## 构建验收

根聚合工程构建成功：父聚合加五个子模块`jmetal-core`、`jmetal-algorithm`、`jmetal-problem`、`jmetal-exec`和`tool`全部完成。Javadoc的本地`package-list`链接提示被Maven明确标为`Ignored`，未导致构建失败。

代表性P25B Runner字节码：

```text
major version: 52
```

即保持Java 8目标字节码。构建时主要产物SHA-256：

```text
jmetal-core-5.8.jar       2C37AF9C128F7D16817F6299858ECF09769A3013B124542FA3989DB533A12D90
jmetal-problem-5.8.jar    0B0A5A955B60FF655999CF5A8FDDFC75029C5AA4B6E9565E9FD4D68BAC2F3C51
jmetal-algorithm-5.8.jar  0F36B72B37C5CDBE13326106A9822E32C45197454B35B66AA3B3BD7030B75982
jmetal-exec-5.8.jar       675428D40B8335DCF0882A851B69712435DDF3E0363F4F3B451A900641ED6FA9
tool-5.8.jar              9209816890C7A72D80220FD5BA4F54063814FF5633163887BFF2E165378E7B76
```

## 最终门状态

```text
pressure_diagnosis_engineering_validated=true
confidence_bal_fallback_validated=true
masked_action_shadow_audit_validated=true
diagnosis_thresholds_frozen=false
formal_matrix_started=false
sampled_reproduction_accepted=false
full_reproduction_accepted=false
pf_sdst_active_experiment=false
shift_formal_path_frozen=true
```

held-out漏失率为41.18%，高于5%门槛，因此当前正式路径必须继续使用`BAL`全开放`N1-N5`，不得冻结单瓶颈严格掩码，也不得基于本结果启动新500000 FE实验。
