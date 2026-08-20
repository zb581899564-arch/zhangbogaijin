# P6.0 原Q-gbest生产接入校正报告

日期：2026-08-08  
语义：`fatigue_improved`生产派生线上的`ORIGINAL_QG + AUTHOR_UPDATE`  
状态：`completed`

## 结论

作者Q相关代码确实存在，但P4.1冻结活动主循环中的`perturbation()`为空。本阶段以独立开关接入原Q-gbest，不与CFVF捆绑；默认`AUTHOR_ACTIVE + AUTHOR_UPDATE`不创建Q表、不消耗P6随机事件，继续执行冻结作者路径。

## 实现

- 四个子群各自维护`2×3`Q表；
- 动作0/1/2分别为上一轮领导、本群历史最优领导、全局非支配集合种子化二元锦标赛；
- `epsilon=0.8`、`alpha=1.0`、`gamma=0.8`，Q并列按小动作编号；
- 边界群奖励为对应目标平均改善，中心群为`Cmax/TEC/TWC`相对改善率之和；
- Qg在更新前选择领导，在本轮完整评价后、历史更新前结算，不增加额外评价；
- P6入口要求启用非零P5疲劳参数清单；
- 作者固定资源文件产生的扩展WA有8个块，而当前实例有2阶段。仅在显式P6模式下限制作者工厂交叉/变异和工人变异访问真实阶段；默认作者路径不变。

## 验证

- `ZhangBoQgControllerTest`：3项测试通过，覆盖三动作、两状态、四群奖励、Q更新、稳定破平与seed重放；
- `ZhangBoP6IntegrationSmokeTest#originalQgRunsAsAnIndependentAuthorUpdateAblation`：100粒子、200 FE通过；
- 非疲劳或`r=0`问题在P6入口被拒绝；
- 原作者四个源文件SHA-256仍与P4.1冻结值一致。

## 状态边界

`qg_restoration_engineering_validated=true`。这只是工程和方案对齐，不是多实例、多种子论文复现；`sampled_reproduction_accepted=false`、`full_reproduction_accepted=false`。

