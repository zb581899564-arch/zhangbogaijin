# V35-P21 算法树消融梯子证据

诊断性证据：单 seed 20260808，20_2_3_1 六梯级各 500k FE，I1 10_2_2_1 链路臂（A2/A3）各 5k FE。无统计、无正式结论。

## 梯子定义

- A0-baseline：Q-gbest controller only (degenerate baseline)；相邻新增开关 = 无（基准）
- A1-dscr：+DSCR dominance-safe cache refresh；相邻新增开关 = dscr
- A2-cfvf：+CFVF all-vector flight；相邻新增开关 = cfvf
- A3-qp：+Q-pbest lineage archive with block-frozen dual Q；相邻新增开关 = qp
- A4-catalite：+CA-TA-Lite 24x5 test/apply/re-test；相邻新增开关 = caTaLite
- A5-full：+directional top-k teacher pool；相邻新增开关 = directionalTeacherPool

## 受控起点

同一初始种群哈希：`07311d31f51e6a71efcbf70435bf8924c02cb8be302023ddeed7f86c2ebca01b`（六臂一致）。

## 逐臂状态

| 臂 | 状态 | FE | 前沿大小 | minCmax | minTEC | minTWC |
|---|---|---|---|---|---|---|
| A0-baseline | COMPLETED | 500000 | 219 | 190.73314811915202 | 8555.322865359572 | 12405.640378792106 |
| A1-dscr | COMPLETED | 500000 | 261 | 184.17944668402595 | 8483.134128426194 | 12474.12796286122 |
| A2-cfvf | COMPLETED | 500000 | 236 | 192.85418931969986 | 8374.060919678715 | 12621.651485095808 |
| A3-qp | COMPLETED | 500000 | 418 | 176.52784883579258 | 8466.050245453927 | 12500.699474068337 |
| A4-catalite | COMPLETED | 500000 | 371 | 190.62590079155416 | 8484.151493117375 | 12414.464644315716 |
| A5-full | COMPLETED | 500000 | 356 | 173.0293265557577 | 8412.310318879867 | 12600.282364937153 |

池化参考（六前沿并集）：非支配解 589 个。

## 禁止格

FORBIDDEN_CELL=FULL-minus-DSCR(dscr=false,cfvf=true,qp=true,caTaLite=true) is excluded: caTaLite requires dscr at runtime (ZhangBoGlobalSearchConfiguration.isV35CaTaLiteEnabled gated on dscrEnabled); dscr=false with caTaLite=true would silently run the legacy CA-TA controller.

## I1 链路臂

A2 5k：CFVF offspring=1000，DSCR teacherUses=400；A3 5k：archiveInsertions=20，DSCR teacherUses=400。

## 数据文件

- `ABLATION_LADDER_METRICS.csv`：逐臂机制计数与极值
- `ABLATION_HV_METRICS.csv`：池化参考 HV、边际差、IGD、相邻覆盖
- `runs/`：每臂 configuration.txt / front.csv / 审计与 DSCR 文件

## FE 收口登记

- A0-baseline：500000
- A1-dscr：500000
- A2-cfvf：500000
- A3-qp：500000
- A4-catalite：500000
- A5-full：500000

A2/A3 曾溢出 +100 的原因（验收整改更正，2026-08-13）：正式基线循环先给全局后代打预评价标记、局部搜索后再调 evaluateSwarm；但标记只在"局部搜索启用或结构化基线更新"时被尊重，CFVF 更新模式（A2/A3）落入无条件整群重评分支——每外层周期重复评价整群（500k 下 18×100=1800 次，I1 下 2×10=20 次），末批整群越过预算 100。已修复为"正式基线循环启用即尊重标记"并增加 FE 上界硬门（<= 预算），修复后各臂收口 <= 预算；原"critical-factory 检查粒度"判断作废。
