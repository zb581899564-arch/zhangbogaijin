# P4 来源映射

语义目标：`published_baseline`。论文以 ESWA 第四章为主，学位论文第四章交叉复核；作者当前 Java 只作为 `author_actual` 诊断证据。

| 实现项 | 主来源 | 交叉来源 | 作者代码证据 | 落地 |
|---|---|---|---|---|
| Table 9 参数 | ESWA Table 9 | 学位论文第四章参数表 | `MOHPSOQBuilder` 参数入口 | `HmopsoQgsConfiguration` |
| JS交换序列 | ESWA Fig.5 | P2结构化夹具 | `MOHPSOQ` 离散更新 | `FourVectorOperators` |
| FA/MA/WA交叉变异 | ESWA Fig.6 | P2六类夹具 | `MOHPSOQ` 资源交叉/变异方法 | `FourVectorOperators` |
| 四子群 | ESWA HMOPSO-QGS流程 | 学位论文第四章 | `MOHPSOQ.updateVelocity()`四份粒子副本与分组 | `SubSwarmDecomposer` |
| PDDR-FF | ESWA公式 | 学位论文第四章 | `PDDRFFSelection.Calculated_PDDRFF()` | `PddrFf` |
| Q-gbest | ESWA Q引导流程 | 学位论文第四章 | `MOHPSOQ.actionset()`与历史领导集合 | `QGbestController` |
| 工厂间搜索、O1–O9 | ESWA局部搜索流程、总体v2编号 | 学位论文第四章 | 作者局部搜索方法 | `OriginalNeighborhoods` |
| 完整闭环 | ESWA算法流程 | 学位论文第四章 | `MOHPSOQ`结构参考 | `PublishedHmopsoQgs` |
| 真实实例补全 | 用户批准的P4计划 | 无 | 原文件工厂级工人只读保留 | `CanonicalEadhfspInstanceLoader` |

论文与作者源码均未修改。ESWA PDF SHA-256、学位论文 SHA-256 延用P2/P3已经冻结的来源清单；P4不重新生成第二套论文语义。
