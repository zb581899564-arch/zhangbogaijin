# 证据归档恢复指南

冷归档根目录：

```text
G:\ResearchArchive\ZhangBo-V35-Paper-Evidence-20260823
```

## 恢复前检查

1. 找到`V35-PAPER-EVIDENCE-ARCHIVE-MANIFEST.tsv`中的campaign记录。
2. 核对压缩包SHA-256、字节数和文件数。
3. 恢复到新的临时目录，不覆盖当前项目。
4. 用对应`files.sha256.tsv`逐文件复算。
5. 全部一致后才能用于指标重算或复制回原路径。

归档布局：

```text
source-freeze/      原论文、学位论文及v2/v3.5方案
local-primary/      Final freeze、当前docs/evidence、paper_evidence、DOE开发运行
local-sandboxes/    已删除回归沙箱的完整可恢复副本
remote-campaigns/   训练机25个campaign逐目录完整归档
manifests/          文件级校验、归档SHA和清理记录
packages/           可直接交付的总账ZIP
```

## Windows归档

```powershell
tar -xzf <archive.tar.gz> -C <empty-directory>
Get-FileHash -Algorithm SHA256 <archive.tar.gz>
```

## Linux训练机归档

```bash
sha256sum <archive.tar.gz>
mkdir -p <empty-directory>
tar -xzf <archive.tar.gz> -C <empty-directory>
sha256sum -c files.sha256.tsv
```

## 恢复等级

| 等级 | 内容 | 要求 |
|---|---|---|
| `FULL_RAW` | 全部原始前沿、事件、状态、配置和日志 | 完整逐文件验证 |
| `THIN_EVIDENCE` | 报告、关键CSV、配置、状态、日志摘要和哈希 | 用于历史审计，不重跑指标 |
| `REBUILDABLE` | Maven target、临时缓存 | 从冻结源码重新构建 |

任何记录若`restoreArchive`为空，说明它仍是唯一副本，不得清理。训练机展开目录即使已删除，也必须同时保留
G盘归档和训练机归档两份校验一致的压缩副本。
