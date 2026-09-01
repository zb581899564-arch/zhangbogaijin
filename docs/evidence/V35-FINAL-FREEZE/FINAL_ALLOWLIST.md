# Final Source Freeze Allowlist

该冻结使用隔离 clone，不从主工作树进行清理、重置或不加区分的提交。

允许纳入的内容只有：

- 张博 V35 生产问题、结构化基线、A0--A4 配置、正式比较 Runner 及其定向测试；
- 固定的实例扩展、疲劳参数、P8 桥接输入和正式共享初始种群快照；
- 正式协议、来源清单、运行/分析工具；
- `v35_final_master_campaign.py` 及其测试。它是 Stage-2 专用调度渲染器，RunKey 精确绑定 Arm、Instance、Seed、MaxFEs、JarSHA 和 ConfigSHA，并在启动前检查 jar、配置、snapshot 与 provenance 的 SHA-256。

明确排除：主工作树的未分类改动、`node_modules`、`target`、临时探针、历史运行输出、其他证据树和任何未经 allowlist 审核的文件。历史证据不删除，但不作为该 clean source commit 的组成部分。

正式 Java Runner 不允许生成初始种群；它必须读取 `(instance, seed)` 对应的冻结 snapshot，并验证 snapshot、V35 与 P8 两种初始种群哈希。
