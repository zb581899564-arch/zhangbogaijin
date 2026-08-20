# 被后续兼容运行替代的日志

| 日志 | 原因 | 最终准据 |
|---|---|---|
| `TEST_FULL_REGRESSION.log` | 未给jMetal 5.8旧Mockito/CGLIB测试传入JDK 17所需的`--add-opens`，产生模块访问级联失败；不是P6代码回归。 | `TEST_FULL_REGRESSION_JDK17_COMPAT.log` |

最终兼容运行与P5相同：651项、0 failures、3个既有errors、6 skipped；三个错误签名逐项一致。
