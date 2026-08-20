# P9两算法单次500000 FE实施与远程验收

日期：2026-08-10  
性质：单实例、单seed的正式预算诊断，不是显著性实验或论文完整复现。

## 1. 实施内容

新增独立P9参数、执行器和Runner，未修改P8短程执行器：

- `ZhangBoP9FormalParameters`：锁定Table 9参数；
- `ZhangBoP9FormalExecutor`：使用规范FM3问题、共同初始种群和独立Table 9 Builder参数；
- `ZhangBoP9SingleComparisonRunner`：只接受`FULL/HMOPSO_QGS_F/REPORT`三个阶段和路径参数；
- `ZhangBoP9SingleComparisonRunnerTest`：以2000 FE贯通两算法与报告，并验证阶段硬门和指标。

正式参数为：种群100、最大500000 FE、物理子群`[20,40,20,20]`、随机系数上界0.6、FA/MA/WA交叉率`0.2/0.5/0.5`、变异率`0.08/0.15/0.25`、Q次数50、局部搜索30、gamma/epsilon均为0.8。

## 2. 本地验收

- P8.1/P9相关定向回归：31项，0失败、0错误；
- Maven六模块`clean package`成功；
- 主类字节码major version：52（Java 8）；
- fat jar SHA-256：`421081771ffd376b3775262bc42675df8433b5e6cd26031162e7fd25f323677c`；
- 2000 FE端到端顺序`FULL → HMOPSO_QGS_F → REPORT`通过；
- FULL缺失时基线会拒绝启动。

测试XML/TXT保存在`local-test-reports/`。

## 3. 训练机边界

- 主机：`aic-inspur-home`；
- 独立目录：`/home/inspur/aicomp/zhangbo-java-p9-single-500k-20260810`；
- tmux：`zhangbo-p9-single-500k`；
- Java 11运行Java 8产物；`-Xmx4g`；CPU亲和性`0-3`；未使用GPU；
- 开始：`2026-08-10T13:01:13+08:00`；完成：`2026-08-10T13:19:38+08:00`；
- 未停止、修改或占用其他tmux任务；会话完成后自动关闭。

启动tmux时，外层“把退出码写入文件”的包装命令因Shell转义保留了字面量`$code`，因此没有形成可信的会话退出码文件。该问题没有改变内层`set -euo pipefail`串行脚本：`completed-at.txt`已生成，FULL、基线和REPORT三份独立`status.properties`均为`COMPLETED`，报告已落盘并通过下载后SHA-256复核。本验收以这些阶段级证据为准，不把缺失的tmux退出码作为成功证据。

远端jar、实例、SUT扩展和疲劳参数哈希与本地一致。下载后的29个远端证据文件与`remote-result-sha256.tsv`逐一复核，哈希不一致数为0。

## 4. 运行硬门

|项目|ZHANGBO-FULL|HMOPSO-QGS-F|
|---|---:|---:|
|状态|COMPLETED|COMPLETED|
|完整评价|499952|500000|
|停止原因|BUDGET_BEFORE_PARTIAL_GENERATION|MAX_FES_REACHED|
|前沿点|592|214|
|非法解|0|0|
|CFVF异常repair|0|0|
|Qg选择|3992|2168|
|PDDR事件|998|542|
|CFVF后代|99800|0|
|档案插入|99800|0|
|Qp动作|89800|0|
|CA-TA Test|230483|0|
|CA-TA Apply|169569|0|
|固定O1–O9事件|适用但非基线硬门|487137|

共同初始四向量种群SHA-256：

`ffca83d43be7a67b8860ad5ccbd5e3d51c2a0f7880509879c59ecbeac0dc9ebe`

## 5. 单次结果

|指标|ZHANGBO-FULL|HMOPSO-QGS-F|FULL相对变化|
|---|---:|---:|---:|
|最小Cmax|181.513888838|207.812054799|-12.65%|
|最小TEC|8342.04566127|8752.98231360|-4.69%|
|最小TWC|12462.0762651|12622.3274157|-1.27%|
|临时HV|0.985823023176|0.741193744275|+33.00%|
|临时IGD|0.00255139598945|0.140344013608|-98.18%|
|疲劳超阈积分FE|30.8655201145|36.7536107544|-16.02%|
|高疲劳比例|0.0674130564274|0.0769125012614|-12.35%|
|负载不均衡|0.463270629110|0.468996033291|-1.22%|
|wall-clock|1073.364 s|30.359 s|约35.4倍|

双向覆盖为`C(FULL,BASE)=0.962616822430`、`C(BASE,FULL)=0`。因此诊断标签为`PROMISING_SIGNAL`。

同时需注意：FULL的`Fmax/Favg`略高，最长连续工作时间也更长；算法运行成本显著增加。本结果不能单独证明统计显著性或在其他实例上的普遍优势。

## 6. 状态边界

```text
sampled_reproduction_accepted=false
full_reproduction_accepted=false
formal_20_run_matrix_started=false
ablation_started=false
```

本轮到此停止，等待用户决定是否扩到3个seed、代表实例或最终20次矩阵。
