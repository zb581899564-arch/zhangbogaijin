# 官方jMetal核心隔离差异

上游固定为提交`831d62d0bbf384e1770efc1bb6eef69ce0ce75b9`。

对`NSGAII.java`与`SPEA2.java`的算法核心只做：

1. 包名改到`...v35.p25e.official`，避免调用作者已改写同名类；
2. 类名与构造器名增加`OfficialJMetal58`前缀；
3. 增加来源说明注释。

选择、繁殖、非支配排序/拥挤距离、strength/raw fitness、archive和environmental selection控制流保持上游5.8源码。四向量PMX/FA/MA/WA交叉变异位于独立`V35FourVectorVariation`，属于Problem表示适配，不包含张博搜索机制。

上游许可证为MIT；版权与许可来源由jMetal仓库`LICENSE.txt`保留在证据说明中。
