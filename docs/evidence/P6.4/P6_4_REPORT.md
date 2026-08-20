# P6.4 预热与分块冻结双Q协同实施报告

日期：2026-08-09  
语义：`fatigue_improved`生产派生线上的`B5/QP5`  
状态：`completed`

## 结论

P6.4已在李明哲作者Java直接派生主线上完成。最终版本在前10%完整评价预算内预热，随后从P-block开始以5个完整代为一块交替冻结Qg和Qp。冻结方仍按当前状态与冻结Q表贪婪执行动作、刷新环境状态，但不累计奖励、不提交TD，运行时逐代校验其Q表SHA-256不变。

P6.3同步模式保持原入口和规范化配置文本；所有创新开关关闭时仍退化到P4.1作者直接派生路径。本阶段没有实现O10–O13、CA-TA-VNS、额外中间评价或500000 FE正式实验。

## 调度与控制契约

- `targetWarmup=ceil(0.10*MaxFEs)`，初始种群评价计入预热预算；阈值按种群大小向上对齐到完整代边界；
- 预热：Qg按P6.0选择和更新；Qp不选择动作、不产生转移、不消耗Qp动作随机事件；认知领导使用当前子群方向锚点；
- P-block：Qg使用当前状态和冻结表贪婪选领导并只观察状态；Qp按P6.3探索、奖励和批量TD正常学习；
- G-block：Qp使用当前状态、有效动作掩码和冻结表贪婪选领导，只做档案预演与状态刷新；Qg按P6.0正常学习；
- Qp探索率继续按全局已消耗FE线性下降，预热结束不重置；区块切换不重置Q表、控制器状态、档案或个人领导；
- 每个CFVF后代只执行一次P5疲劳评价，Qg/Qp结算均在评价后PDDR和任何后续搜索之前完成。

## 2000 FE烟测

实例：`EADHFSP/20_2_3_1.txt`；疲劳清单：`fatigue-parameters/v1/20_2_3_1.fatigue.txt`；种群100；seed `20260808`。

- 初始100 FE计入预热；1个预热代、10个P-block代、8个G-block代；
- 1900个CFVF后代、1900次评价后PDDR选择，最终FE恰为2000；
- Qg共76次群动作，其中36次TD更新；
- Qp共1800次动作，其中1000条P-block训练转移；
- 非法解0，正常CFVF后置repair 0；
- 所有P-block的Qg表和所有预热/G-block的Qp表前后哈希相同；
- 固定显式初始种群连续3次的阶段日志、Qg/Qp表和结果字节级一致；seed改为`20260809`后产生可定位轨迹差异。

## 验证边界

- P2–P6.4定向回归101项通过，0 failures、0 errors；
- 六模块Java 8打包成功，P6.4核心类major version 52；
- 完整旧核心回归保持651项、0 failures、3个P1既有errors、6 skipped；三个错误仍为`PMXCrossoverTest`、`PermutationSwapMutationTest`和`DefaultIntegerPermutationSolutionTest`的`bound must be positive`；
- 作者`MOHPSOQ/Builder/EDHHFSPW/Runner`四个原文件与P4.1冻结哈希一致；
- `dual_q_block_freeze_engineering_validated=true`、`dual_q_block_freeze_scheme_aligned=true`；
- `sampled_reproduction_accepted=false`、`full_reproduction_accepted=false`继续保持。

分块冻结只证明降低了两个控制器同时改变带来的非平稳性，不证明完全因果隔离，也不构成正式论文实验结论。下一允许阶段是P7.1，尚未开始。
