# V35-P25A 本地实施与启动前验收

状态：`REMOTE_START_BLOCKED_BY_SSH_BANNER_CLOSE`

## 已完成

- P24.1：D-070旧A3值已隔离，JDK17回归命令已冻结，磁盘连续两次重建字节级一致。
- P25A Runner：CLI只开放`seed-slot/arm/project-root/output`，科学参数硬编码。
- 三臂：A0、A4、A5；正式seed固定为20260809至20260813。
- 每臂独立Problem、算法和JVM；同seed初始四向量哈希硬门。
- 方向教师池增加观察型触发计数，不进入动作、配置或随机决策。
- 统一reference汇总器：15条全部完成后才冻结ND并计算共同HV/IGD/Spacing/C-metric。
- 2000 FE Batch-0：5 seed × 3 arm = 15条全部完成，统一reference/report贯通。
- `jmetal-problem`：67/67。
- `jmetal-algorithm`：206/206（原205项加P24.1一项），0失败/0错误/0跳过。
- P9/P25A定向：8/8。
- 全反应堆打包通过；`ZhangBoMOHPSOQ.class` major version=52。

## 远端阻断

2026-08-14启动前检查中，`100.127.244.47:22`与`172.18.64.166:22`均可建立TCP，
但服务器在发送SSH banner前主动关闭连接：

```text
kex_exchange_identification: Connection closed by remote host
```

因此没有创建远端目录、没有上传文件、没有启动15次运行。不得把本地Batch-0当成P25A正式结果。

## 保持状态

```text
formal_matrix_started=false
sampled_reproduction_accepted=false
full_reproduction_accepted=false
pf_sdst_active_experiment=false
shift_formal_path_frozen=true
```
