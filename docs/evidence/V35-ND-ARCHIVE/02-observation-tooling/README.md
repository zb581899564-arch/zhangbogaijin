# 02 Observation tooling

已实现：

- `V35DeterministicObjectiveSubsetter`；
- `V35ArchiveAuditLedger`；
- `V35ArchiveExperimentArtifacts`；
- `V35ArchiveExperimentRuntime`；
- ND0控制臂与专用Runner；
- 增量档案的无副作用结果报告。

本地ND0 2000 FE等价测试要求未挂钩与挂钩运行的初群、FE、机制核心事件和最终前沿完全一致，
并要求精确去重后的decision/observed前沿相等。最终测试数、构建和字节码证据写入本目录的
`IMPLEMENTATION_ACCEPTANCE.md`（在全部回归完成后生成）。

