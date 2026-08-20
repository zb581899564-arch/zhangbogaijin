# 09-STAGE6-COMPOSITION 索引

FC-6A.1 / Stage-6（PDDR 种群组成审计：QGS 原算法 vs A4-Pacing BASE）服务器 12 跑的完整数据归档。
报告：`../00-REPORTS/FC6A1_PDDR_COMPOSITION_AUDIT.md`。

## 目录

- `raw/`：服务器 `stage6-fc6a1/<instance>/<arm>/seed-*/` 原样数据
  - BASE 臂：`mechanism-summary.txt`（含 fc6Diag 完整段 + fc6diagComp 组成段）+ `front.csv` + `console.log`
  - QGS 臂：完整 `runs/seed-*/HMOPSO_QGS_F/` 目录（mechanism-summary + front + configuration + run-record 等）
- `tables/`：解析产物
  - `composition_per_round.csv`：474 轮全字段明细（cycle/fe/pool/nLT1/nEq1/nGt1/nND/sel*/rej*/bp*）
  - `composition_summary.csv`：§12 主表（median/Q1/Q3/min/max + R_C=N_LT1/100、R_B=N_EQ1/100 + P(N_LT1>100)、P(N_ND>100)）
  - `composition_by_seed.csv`：逐 seed 中位数（漂移检查）
  - `case_classification.txt`：情况 A/B/C 轮次占比（判定依据）
  - `eq1_fate.txt`：边界孤点命运（选中/拒绝率、挤压规律 69 轮 0 例外、相位漂移）
  - `bp_counterfactual_check.txt`：BP 反事实计数自检（474/474 一致）
- `analyze_fc6a1_composition.py`：主分析（输入 `raw/`，输出 `tables/composition_*`）
- `analyze_fc6a1_cases.py`：补充分析（输入 `tables/composition_per_round.csv`，输出三个 txt）
- `plot_fc6a1_fronts.py`：帕累托前沿可视化（输出 `figures/`：逐 seed 6 张 + 三 seed 合并 2 张；每图 3 个 2D 投影 + 3D 视图；QGS 红 / BASE 蓝）
- `figures/`：上述 8 张 PNG
- `rejected-parallel-batch/`：**已否决**的另一会话平行批（BP 污染 jar），见其 `WARNING.md`，勿引用

## 命名

`<instance>/<ARM>/seed-<SEED>`，ARM=QGS（李明哲 QGS 原算法，HMOPSO_QGS_F formal-baseline）/ BASE（A4-Pacing 正式配置），SEED=20260822/23/24。

## 构建

单一 jar `BUILD-C3-COMP`（**原始 selector** + 组成审计，sha256 `5233b690db12d7130549355228f4da026589f28759d702484ac65d178aaa3b4a`）；本地副本 `../07-BUILDS/c3/`。中性验证 7/7：6 BASE front == stage5 C2-BASE；QGS-100-seed22 == 历史 fc-time-sanity（`d193056a…`）。

## 复现

服务器 `/home/inspur/aicomp/zhangbo-fc6-20260818/`：`jars/jmetal-exec-5.8-BUILD-C3-COMP.jar`、`fc6-stage6-fc6a1.sh`、`results/stage6-fc6a1/`、`logs/stage6-*`。
