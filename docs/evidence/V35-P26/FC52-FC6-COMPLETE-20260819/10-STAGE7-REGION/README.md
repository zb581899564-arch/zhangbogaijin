# 10-STAGE7-REGION: FC-6A.2 Region x PDDR 区域组成审计归档

本目录存放 FC-6A.2（Region-aware Environmental Selection Go/No-Go 数据批）的完整运行数据、分析表及脚本。

## 目录结构

- `raw/`：12 跑原始实验产物（{100_2_3_1, 20_2_3_1} x {BASE, QGS} x 3 seeds）
- `tables/`：分析后生成的核心数据表：
  - `region_per_round.csv`：474 轮逐轮明细数据
  - `rejected_nd_attribution.csv`：被全局 PDDR 淘汰的非支配解在各子群区域的归属与可吸收比例
  - `region_summary.csv`：各规模/算法的分位数汇总
  - `probe_174_summary.csv`：174.44 反事实探针存活率统计
  - `region_by_seed.csv`：逐 seed 稳定性检查
- `analyze_fc6a2_region.py`：分析脚本
- `fc6-stage7-fc6a2.sh`：服务器部署与执行脚本

## 核心结果

- **中性验证门 7/7 通过**：6/6 BASE front sha256 与 Stage 5 C2-BASE / Stage 6 C3-COMP 完全一致；QGS 100-job seed22 与历史 `d193056a...` 一致。
- **Go 判据 1**：被拒 ND 的区域吸收率达 **72.2% ~ 84.4%**（远超 50% 门槛）。
- **Go 判据 2**：174.44 探针在区域机制下的存活率由全局原版的 14.5% 跃升至 **100.0%**（提升 6.89 倍），且 100% 锁定 G1 席位。
- **Go 判据 3**：溢出率极低（中位数仅 1.9% ~ 7.4%）。
- **裁决**：**STRONG GO**。详细报告见 `../00-REPORTS/FC6A2_REGION_PDDR_AUDIT.md`。
