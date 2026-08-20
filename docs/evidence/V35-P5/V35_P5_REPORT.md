# V35-P5 结构化 HMOPSO-QGS 基线接入报告

状态：`completed_engineering_smoke`

已完成：

- `V35ProductionConfiguration` 固定 v3.5 单族、序列无关、无移位边界；
- `ZhangBoGlobalSearchConfiguration.forV35(...)` 将配置绑定到结构化 Qg/CFVF/PDDR/档案/Qp/双Q/CA-TA 组件；
- `ZhangBoMOHPSOQBuilder.setV35Configuration(...)` 原子绑定种群规模、物理子群 `[20,40,20,20]`、Table 9 基线和 v3.5 全局配置。
- `V35ProductionSmokeTest` 已验证 FM3、CFVF、Qg、Qp、评价后 PDDR、谱系档案和 CA-TA-Lite 桥接可在同一规范问题上运行；配置与运行时资源飞行系数固定为 `0.6`。

尚未完成：正式 500000 FE Runner、统计实验和论文复现验收。本报告只证明工程烟测链路，不代表正式实验或论文完整复现。

当前最小烟测：`V35ProductionSmokeTest`，10粒子、500 FE，规范 FM3 问题与结构化算法链路完成；DSCR 事件、v3.5 Lite Test/Apply 事件、局部完整评价和最终非空结果均被断言，FE 未超预算。该测试只证明连接可运行，不代表三个创新点的完整验收。
