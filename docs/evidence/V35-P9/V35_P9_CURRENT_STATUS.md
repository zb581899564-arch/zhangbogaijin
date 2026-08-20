# v3.5 当前实现状态

## 已完成

- V35-P0：源码/配置/历史证据冻结；
- V35-P1：单产品族占位契约；
- V35-P2：序列无关设置时间契约；
- V35-P3：v3.5正式入口永久 `ShiftMode=NONE`；
- V35-P4：FM0–FM3规范疲劳解码回归；
- v3.5结构化配置到Qg/CFVF/PDDR/档案/Qp/双Q/CA-TA组件的桥接；
- DSCR和CA-TA-Lite基础契约组件及单元测试。

## 当前未完成

- DSCR尚未嵌入每个Qg决策周期；
- CA-TA-Lite尚未替换旧O1–O13状态机并接入主循环；
- 未运行v3.5正式500000 FE、多seed、多实例或消融矩阵；
- `sampled_reproduction_accepted=false`、`full_reproduction_accepted=false`保持不变。

## 本轮验证

- jmetal-problem全回归：67 tests，0 failures，0 errors；
- v3.5/算法定向回归：45 tests，0 failures，0 errors；
- v3.5最小机制烟测：10粒子、500 FE，完成且前沿非空；
- 目标字节码 major version：52（Java 8）。
