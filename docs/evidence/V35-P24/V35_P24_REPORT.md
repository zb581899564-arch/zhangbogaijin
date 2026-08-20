# V35-P24 最终参数冻结

冻结范围：v3.5 生产机制栈（DSCR / CFVF / Q-pbest 双Q / CA-TA-Lite / 方向教师池）、解码边界（FM3、单族退化、序列无关、无班次）、公平协议（seed=20260808、同初始种群 SHA-256、单 seed 500k FE）、Table 9 正式基线（哈希 8C2D808121E4A397A6C31FB82D440A5AB131315BBDECE1BAE0CA13F1706149D2）。

- `FREEZE_MANIFEST.txt`：全部冻结语义版本与参数 + 正式配置 canonicalText + 哈希 ad2c244a4e74927f81a093167d405b91e366e37b0a96b29eb465ac6647dd512a + Table 9 canonicalText；幂等契约：与磁盘既有冻结物逐字节一致（除 generatedAt 时间戳行），漂移即失败
- `source-sha256.csv`：v35/zhangbo/audit 机制源码清单（186 文件）+ AGENTS/ROADMAP
- `environment.txt` / `java-version.txt`：运行环境
- `jdk17-regression-command.ps1`：JDK 17 完整回归命令，固定包含 `--add-opens=java.base/java.lang=ALL-UNNAMED`

## 四个验收标志（全部为 false）

- PF-SDST 真实启用：**未批准**
- 多 seed 统计：**未开始**
- 正式实验矩阵：**未开始**
- 论文数字更新：**未发生**（本包不产生任何生产代码语义变更）

## 门

P25（多 seed 正式矩阵）及之后的一切工作需要另行批准；冻结后任何机制/参数变更必须先更新本清单并重新全量回归。
