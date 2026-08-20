# ⚠️ 已否决的平行批次（另一会话产物，勿用于结论）

## 这是什么

2026-08-19 晚（21:03–21:58 服务器时间），另一个会话在服务器 `/home/inspur/aicomp/zhangbo-fc6a1-20260819/` 独立跑了一套 FC-6A.1 平行批（12 跑，名义同规格），并把数据归档到了 `V35-P26/09-STAGE6-COMPOSITION/`（该位置与本目录平级，现已整体移入此处隔离）。

**该批数据已被否决，不得引用。** 正式批次是 `../raw/` + `../tables/`（jar `5233b690…`，中性门 7/7 通过，报告 `../../00-REPORTS/FC6A1_PDDR_COMPOSITION_AUDIT.md`）。

## 否决证据（四条，各自独立成立）

1. **Jar 为 BP 污染版**：其 jar（sha256 `12b83708b591da0e41f912882d46083ea136db287f1b5910d39245fd465fe01d`，上传名 `BUILD-C-bppddr-diag.jar`）内 `ZhangBoEvaluatedPddrSelector.class` 字节码 sha256 = `8e70ad91…`，含 `MAX_BOUNDARY_SLOTS`/`boundaryReserved`（BP-PDDR 槽位逻辑）；而 FC-6A.1 规格（及 QGS 臂的正确性）要求**原始 selector**（字节码 `14040a20…`，即 BUILD-A 原版，本批 C3-COMP jar 内为该版本）。这正是计划阶段预警过的陷阱：QGS formal-baseline 复用 `ZhangBoEvaluatedPddrSelector`，带 BP 槽的 selector 会同时改变 QGS 臂与 BASE 臂的行为。
2. **front 与历史基线不符**：其 QGS 100-job seed22 front = 172 点（历史 fc-time-sanity 为 **201 点**，且本批正式 run 与历史逐字节一致 `d193056a…`）；其 6 个 BASE front sha256 与 stage5 C2-BASE 全部不一致（例：100-job seed23 为 392 行 vs 正式 363 行）。
3. **轨迹分歧从第 2 轮开始**：同 seed、同初始种群哈希（`4715a7cb…` / `c4c7800e…`）、第 1 轮组成行完全一致，但第 2 轮起 fe 与构成全部分歧（BASE seed23：正式 `fe=13435 pool=269 nLT1=12` vs 平行批 `fe=13435 pool=284 nLT1=28`）--selector 行为差异的直接证据。
4. **批次自身不完整**：BASE 100-job seed22 无 front.csv 且组成行 0 条（其 task.log 显示该跑异常，rerun 后仍未拉全）。

## 结论

该平行批的所有数字（包括其 `tables/` 里的统计表）都是在 BP selector 下跑出来的，既不是"李明哲 QGS 原算法"，也不是"A4-Pacing BASE"，不符合 FC-6A.1 的对照设计。保留原始文件仅供溯源（`evidence-sha256.tsv` 是其自带清单）。

## 内容

- `raw/`：其 12 跑数据（BASE 缺 100-job seed22 front；QGS 在 `raw/QGS/<inst>/runs/seed-*/HMOPSO_QGS_F/`）
- `fronts/`：12 个 front 副本（命名 `BASE_<inst>_seed<seed>_front.csv`）
- `tables/`：其自己的解析表（`per_cycle_detail.csv`、`summary_by_group.csv`）--**勿引用**
- `logs/`：其 task.log + rerun 日志
- `analyze_fc6a1_composition.py`：其分析脚本（与正式版不同实现）
- `evidence-sha256.tsv`：其自带文件哈希清单
