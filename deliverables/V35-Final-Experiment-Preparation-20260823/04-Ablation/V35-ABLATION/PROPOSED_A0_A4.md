# V35 Final 拟议 A0--A4 消融表

> 状态：语义与 2,000 FE 烟测已核验；正式 500,000 FE 消融未获授权。

| 臂 | 名称 | 相对于前一臂新增 | 必须关闭 | 运行时机制门 |
|---|---|---|---|---|
| A0 | 规范 HMOPSO-QGS 公平基线 | — | DSCR、CFVF、PA_i/Qp、CA-TA-Lite、方向教师池 | 原 Qg、严格 PDDR、结构化基线更新、继承工厂间/O1--O9 局部搜索 |
| A1 | A0 + DSCR | DSCR | CFVF、PA_i/Qp、CA-TA-Lite、方向教师池 | 实际教师使用数大于零，`dominatedTeacherUses=0` |
| A2 | A1 + CFVF | CFVF | PA_i/Qp、CA-TA-Lite、方向教师池 | CFVF 后代大于零；基线更新不再混入该臂 |
| A3 | A2 + PA_i/Qp | 谱系个人档案、Qp 与 P=5/G=5 硬冻结双Q | CA-TA-Lite、方向教师池 | 档案插入、Qp 动作和 P/G 阶段均大于零 |
| A4 | A3 + 预算感知 CA-TA 包 | CA-TA-Lite N1--N5 与动态 local-FE budget | 方向教师池 | Test/Apply 均大于零；局部候选不回写当轮 Q 奖励 |

各臂共同使用 FM3、`GLOBAL_ORIGINAL` PDDR、当前 LS 顺序、`ShiftMode=NONE`、单族与序列无关设置时间。A0 的表述必须是“规范、确定性、公平适配 HMOPSO-QGS 基线”，不是作者原始代码的直接可执行复现。
