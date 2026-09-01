# 2026-08-25 首次 2k 预检失败记录

状态：`FIXED_IN_TOOLING_PENDING_RERUN`。

四条 2k 预检在任何算法评价开始前均以退出码 1 结束；未生成可接受运行，也未进入参考前沿。

根因是训练机没有 `javac`，外置启动器改用 Java 11 source-file launcher 后，启动器类与冻结
`ZhangBoV35FormalAblationArmRunner` 位于不同的类加载器。启动器错误调用了后者的包级
`execute(Path, Path)`，因而触发 `IllegalAccessError`。

修复仅把委托方式改为冻结 Runner 的公开 `main(--plan, --output)` 接口。冻结算法 Jar、算法语义、
实例、种子、快照、FE、配置和搜索机制均未修改。旧 `run-r3` 仅保留为失败尝试，不得用于任何科学
指标；重跑使用新的 `run-r4` 目录，避免覆盖失败证据。
