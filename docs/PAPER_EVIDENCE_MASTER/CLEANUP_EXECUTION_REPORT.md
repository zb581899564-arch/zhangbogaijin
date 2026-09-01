# 归档与清理执行报告

状态：`COMPLETED_AND_RECOVERABLE`  
执行日期：2026-08-23 至 2026-08-24（Asia/Shanghai）

## 删除前空间

| 位置 | 可用空间 |
|---|---:|
| D盘 | 99.93GB |
| E盘 | 13.75GB |
| G盘 | 361.62GB |
| 训练机 `/` | 252GB |

## 删除前清单

- 本地文件级清单：`inventory/local-artifact-ledger.tsv`。
- 训练机文件级清单：`inventory/remote-artifact-ledger.tsv`。
- 两端均存在`INVENTORY_COMPLETE`完成标记。

## 安全门

```text
archiveExists=true
archiveHashMatches=true
archiveFileManifestMatches=true
sourceNotActive=true
sourceWithinApprovedTarget=true
restorePathRecorded=true
```

任一门不满足时，该目标自动标为`SKIPPED_NOT_SAFE_TO_DELETE`。

## 执行结果

### 本地

- 删除18个本地目标，共`13,344,486,757`字节（约12.43GiB）。
- 其中包括3个DOE回归沙箱、D盘DOE展开副本、6个Maven `target`和7个`.codex-temp*`目录。
- 三个回归沙箱逐文件验证`98,802`项、失败`0`；DOE展开副本验证`2,035`项、失败`0`。
- D盘可用空间由99.93GB增至106.54GB；E盘由13.75GB增至19.41GB。

### 训练机

- 删除19个已归档展开目录，共`12,416,766,995`字节（约11.57GiB）。
- 另删除传输完成后的DOE临时分片`887,844,682`字节；该分片不是独立证据。
- 训练机可用空间由`266,255,974,400`增至`278,692,921,344`字节，暂停标记保持存在。
- 清理后25个登记目录中，19个为`DELETED_WITH_RESTORE_PATH`，6个主线目录仍为`PRESENT`。

### 受保护对象复核

以下目录未删除：

```text
zhangbo-fc6-20260818
zhangbo-fc6a4-order-20260820
zhangbo-fc6b-region-20260820-r3
zhangbo-v35-p25e-corrected-50k-20260815
zhangbo-v35-stage2-phasebudget-20260823-r3
zhangbo-v35-stage2-master-v2-20260823
```

冻结算法Jar的SHA-256仍为：

```text
8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9
```

### 冷归档

- G盘保存25个训练机campaign归档，合计`2,844,733,865`字节，全部匹配训练机权威manifest。
- 本地主要证据4个归档、回归沙箱3个归档及8份来源文件均通过内容级哈希复核。
- 每一项删除记录、恢复位置和归档哈希见`cleanup-execution.csv`；逐文件去向见`artifact-ledger.tsv`。

本次没有恢复4500矩阵，没有修改算法、PDDR、参数或正式运行数据。
