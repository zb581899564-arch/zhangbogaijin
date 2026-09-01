# V35论文证据归档、清理与恢复最终报告

## 1. 结论

本工作包已经完成。论文相关资料形成了“当前项目总账 + G盘冷归档 + 训练机权威归档”的可追溯结构；被删除的内容均有经过SHA-256或逐文件清单验证的恢复来源。4500条正式矩阵仍处于用户暂停状态，未生成正式统计结论。

```text
allLocalEvidenceCatalogued=true
allRemoteExperimentsCatalogued=true
allPaperClaimsHaveEvidence=true
allCurrentClaimsHaveHashes=true
allLegacyEvidenceExplicitlyExcluded=true
allRemotePathsMapped=true
allFiguresHaveSourceData=true
staleStatusDocumentsReconciled=true
cleanupTargetsRecoverable=true
cleanupHashesVerified=true
formalMatrixRunning=false
formalStatisticsGenerated=false
```

## 2. 当前科学状态

正式语义保持：`FM3 + ShiftMode.NONE + 单一产品族 + 序列无关SUT`；子群容量为`[20,40,20,20]`；PDDR为`GLOBAL_ORIGINAL`；局部搜索顺序为`CA-TA-Lite → inherited LS`；双Q为`P=5/G=5, rho=0`，方向教师池关闭。

Stage2当前有效数据仍只有`100_2_3_1 × 12 paired seeds × A0--A4`的60条先导运行。A4相对A0表现出积极信号，但A2→A3退化及PDDR Cmax生命周期仍需审计。这些结果不得表述为正式统计优越性。

## 3. 总账规模

| 总账 | 数量/状态 |
|---|---:|
| 原始逐文件artifact记录 | 56,151 |
| 训练机campaign | 25 |
| 清理后仍展开保留 | 6 |
| 删除但可恢复 | 19 |
| 标准化run记录 | 480 |
| 完成run | 477 |
| 当前reference-eligible先导run | 60 |
| Stage2完整公平组 | 12 |

## 4. 归档布局

```text
G:\ResearchArchive\ZhangBo-V35-Paper-Evidence-20260823\
├── catalog\
├── source-freeze\
├── local-primary\
├── local-sandboxes\
├── remote-campaigns\
├── manifests\
└── packages\
```

关键归档：

- `final-freeze.tar.gz`：冻结源码、配置和正式运行入口；
- `current-docs-evidence.tar.gz`：项目证据目录；
- `current-paper-evidence.tar.gz`：I0/I1论文示例；
- `development-runs-20260822.tar.gz`：DOE开发阶段展开数据的恢复源；
- `remote-campaigns/*.tar.gz`：25个训练机campaign的逐目录恢复源。

## 5. 清理结果

本地删除`13,344,486,757`字节，训练机删除展开目录`12,416,766,995`字节；另移除`887,844,682`字节可重建传输分片。所有删除动作均使用解析后的绝对路径和明确白名单，没有使用通配递归删除。

未删除作者资料、论文、原始算例、冻结Jar、900份正式snapshot、I0/I1人工核验、DOE/FC有效结论的唯一副本或Stage2的12个有效五臂组。

## 6. 恢复方法

恢复前先查`cleanup-execution.csv`，取得目标对应的`archivePath`和`archiveSha256`；核对压缩包SHA-256后，解压到新的临时目录，对照`artifact-ledger.tsv`复算文件级哈希。确认无误后再迁回原路径。完整命令与注意事项见`RESTORE_GUIDE.md`。

## 7. 论文使用边界

- `MAIN_METHOD_EVIDENCE`可用于方法、公式和实现说明；
- `PAPER_PARAMETER_SELECTION`可用于参数冻结过程；
- `PILOT_DIAGNOSTIC`只能称为先导信号；
- `NEGATIVE_RESULT_APPENDIX`可用于说明拒绝路线；
- `LEGACY_EXCLUDED`和`FORBIDDEN_FOR_PAPER_CLAIM`不得进入效果结论；
- 正式多实例统计图表保持`PENDING_FORMAL_DATA`。

## 8. 下一步边界

只允许先完成A3与PDDR的纯观察审计。未得到新的明确授权前，不恢复4500矩阵、不修改PDDR、不改变冻结参数、不构造最终PFref或统计显著性结果。
