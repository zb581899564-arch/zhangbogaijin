# 外置工具执行模式

记录时间：2026-08-25。

训练机具有 Java 11 运行环境，但未安装 `javac`。为避免改变服务器软件环境，本确认工作包的
两个**外置证据启动器**可使用 Java 11 source-file launcher 运行；该模式只即时编译并执行启动器
源码，搜索算法仍只从冻结 Jar 加载。

这不是算法 Jar 重建，也不改变 FM3、PDDR、子群容量、随机流、FE 或搜索机制。每条运行计划和
`final-candidate-context.properties` 均必须记录：

```text
externalToolExecutionMode=JAVA11_SOURCE_LAUNCHER
```

若训练机后续提供 JDK，则可改用 `JAVA8_COMPILED` 外置类；两种模式的冻结 Jar SHA-256 必须相同，
且不得混合到同一 A0/A2 配对组。当前预检及确认组统一使用 source launcher。
