# 交付内容清单

本包按“结论 → 证据 → 可执行入口 → 验证”的顺序组织。

| 目录 | 内容 | 用途 |
|---|---|---|
| `01-Final-Planning` | `FINAL_EXPERIMENT_DAG.md`、`FINAL_EXPERIMENT_STATUS.md` | 读取当前阶段、依赖关系与阻断门 |
| `02-Freeze-And-Campaign` | `V35-FINAL-FREEZE` 证据及 campaign runner | 核验候选冻结、短程并发预检、恢复机制 |
| `03-Formal-Comparison` | `V35-FORMAL-EXPERIMENTS` 协议与模板 | 正式比较启动前的公平性与来源绑定 |
| `04-Ablation` | `V35-ABLATION` 全部证据 | 理解 A0–A4 的真实语义和 2k FE 机制烟测 |
| `05-Analysis-And-Statistics` | `V35-ANALYSIS` 与 Python 工具 | 后续冻结 PFref、计算指标和统计检验 |
| `06-Paper-Skeleton` | 论文骨架和结果占位契约 | 写作时避免将先导证据写成正式结论 |
| `07-Implementation` | Java Runner/Profile、Python 调度与分析源码/测试 | 代码审查、重建和复跑 |
| `08-Verification` | `SHA256SUMS.tsv`、构建与测试摘要 | 文件级完整性检查 |

包内原始证据文件保持 UTF-8。若任何校验失败，应回到项目工作区中的同路径原件核查，不应以本包替代原始工作区。
