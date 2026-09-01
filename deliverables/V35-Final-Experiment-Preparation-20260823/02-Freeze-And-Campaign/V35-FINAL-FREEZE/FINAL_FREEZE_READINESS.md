# V35 Final Freeze 与 Campaign 基础设施就绪性复核

复核日期：2026-08-23。

## 结论

`READY_FOR_PROTOCOLLED_PREFLIGHT`，但不是最终正式源码冻结，也不授权正式 500,000 FE 运行。

当前工作树存在大量历史 WIP；因此不以 Git `HEAD` 冒充最终源代码版本。已保留旧候选快照的只读事实，并对当前树和新鲜构建作了独立记录。真正的 formal source freeze 必须等 FC-8、主版本、20-seed 清单和正式矩阵全部批准后一次性重建。

## 本次可复核事实

| 项目 | 结果 |
|---|---|
| 旧候选快照 | `FREEZE_MANIFEST.json` 为 `CANDIDATE_WITH_DIRTY_WORKTREE`，不是 Git tag，也不是 formal source freeze |
| 当前 Java/POM 树 | 1146 个 `.java`/`pom.xml` 文件；聚合 SHA-256：`6479e3bddd89be05e2a7423c0fc30a49c4f6756d7cc7ad035bee6d5c163ba4ba` |
| 当前 fat jar | `jmetal-exec-5.8-jar-with-dependencies.jar` SHA-256：`9631C821AD37522059F1BA3CEA278ACD974FD96166E939883FF7CE13373CFC08` |
| 本地重建 | 使用 `E:\javavava` 的 JDK 17 完成 Maven 打包；Javadoc 本地链接缺失仅产生已忽略警告 |
| campaign runner | `scripts/v35_campaign_runner.py` 的 4 项定向测试通过：重复 RunKey 拒绝、已完成跳过、失败保留重试、formal 默认拒绝 |
| 统计流水线 | `tools/v35-analysis/` 的 7 项数学/输入门测试通过；仅是离线分析工具 |

## 并行预检边界

下列证据均为既有 DOE-1 预检命令的吞吐/隔离检查，使用短预算，不是 Final 结果、不是主版本门、不是正式统计的输入：

| campaign state | 完成 | 失败 | 未启动 | 解释 |
|---|---:|---:|---:|---|
| `campaign-state-L4-r2` | 4 | 0 | 0 | 短预算并发 4 的重试成功 |
| `campaign-state-L8-r2` | 8 | 0 | 0 | 短预算并发 8 成功 |
| `campaign-state-L12-r2` | 11 | 1 | 0 | 一条 SSH 启动失败，保留失败记录，不将 L12 视为已验收并发级别 |
| `campaign-state-L16` | 0 | 0 | 16 | 未运行，不能据此宣称 16 路可用 |

失败记录不覆盖、不删除；未来正式 campaign 应从已批准的新 manifest 重新开始，不能复用这些短预算 runId。

## 仍然阻断正式运行的条件

1. `EXP-1=blocked_by_FC-8`；FC-9 也是 45×20 主比较的前置；
2. `formal_20_seed_list_frozen=false`；
3. `formal_algorithm_set_frozen=false`，A4 仍只是候选；
4. 45 个实例及其扩展、疲劳参数和双初群哈希尚未形成 final manifest；
5. 当前源代码改变后，旧候选快照不能被当作正式复现版本；
6. 正式资源、矩阵和执行授权尚未由用户单独给出。

因此 `formal_matrix_started=false`、`sampled_reproduction_accepted=false` 与 `full_reproduction_accepted=false` 保持不变。
