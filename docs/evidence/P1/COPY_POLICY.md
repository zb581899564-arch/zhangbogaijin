# P1 净化复制策略

## 纳入

- 当前作者工作树中的Java、POM、测试、资源、README、许可证和源码说明；
- 当前未跟踪但属于源码或已由根POM声明的 `tool` 模块；
- `EADHFSP` 中45个非Apple sidecar的 `.txt` 实例。

## 排除

- 版本库和IDE状态：`.git`、`.idea`；
- 构建产物：任意 `target`；
- Apple元数据：`.DS_Store`、`._*`；
- 运行输出：`jMetal.log*`、`FUN.tsv`、`VAR.tsv`、根目录 `NSGA-II` 和 `SPEA2` 结果目录；
- 外部历史结果：`50%`、`50%-两个目标`、`data result` 和 `实验结果.xls`。

最初复制时，目录名大小写匹配曾把源码包 `multiobjective/spea2` 中3个Java文件一并排除。三文件已从原源逐项补入基线和工作副本，随后来源—基线及基线—初始工作副本的1806项SHA-256比较均为0差异。

基线全部1806个文件设置为只读。工作副本构建产生的 `target` 和路径烟测产生的 `results` 属于验证产物，不进入净化内容清单。
