# 报告数据与图表说明

## 数据来源

- 主结果：`results/comparison/metrics.csv`
- 运行状态：两个算法目录下的`status.properties`
- 机制事件：两个算法目录下的`mechanism-summary.txt`
- 参数口径：两个算法目录下的`configuration.txt`
- 前沿与reference：`front.csv`和`results/comparison/reference-front.csv`
- 运行资源：下载的console日志及训练机`/usr/bin/time -v`摘要

## 图表契约

“FULL相对基线变化”图只展示离散的相对百分比，基准为HMOPSO-QGS-F工程配置。正值表示FULL数值更大，负值表示更小；不同指标的优化方向必须结合标签解释，不能把所有正值理解为改善。

报告中的HV/IGD/Spacing是pair-only reference下的工程诊断。reference的78.5%由FULL贡献，所以没有绘制或宣称正式统计优越性图。

## 省略项

- 未绘制置信区间：每算法只有一次运行。
- 未绘制显著性图：没有多seed统计样本。
- 未给出正式跨算法统一reference：当前只包含两条运行。
- 未把疲劳指标合并为综合分：方案没有批准第四目标或隐藏加权总分。

