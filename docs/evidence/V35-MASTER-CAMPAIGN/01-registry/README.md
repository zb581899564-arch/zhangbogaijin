# Master 注册表契约

`master-run-registry.csv` 是只追加的物理运行账本，初始只有表头。完成态只在输出目录
原子移动成功、`front.csv`/`status.properties`/provenance/机制摘要/哈希清单齐全且通过
run acceptance 后写入。

## 唯一性与恢复

RunKey 固定为：

```text
Arm__Instance__Seed__MaxFEs__JarSHA256__ConfigSHA256
```

它的五个科学字段不得省略、截断或用目录名替代。初始种群不在 RunKey 中，但以
`snapshotSHA256`、V35 与 P8 两种初群逻辑哈希及公平契约哈希共同绑定。

- `COMPLETED`：只跳过，不重新计算；
- `FAILED`：保留每一次失败目录与真实 FE，可在 `maxAttempts` 内新增 attempt；
- `INVALID`：不是可静默重试。若为公平性/语义/FE/hash/前沿异常，则同一
  `(instanceId, seed)` 的五臂都进入 `paired-group-registry.csv`，在人工调查前停止；
- `PENDING/RUNNING`：由远端 scheduler 的原子状态文件维护。

禁止仅补一条 arm 后就把该 `(instance,seed)` 放入 paired 指标或统计。
