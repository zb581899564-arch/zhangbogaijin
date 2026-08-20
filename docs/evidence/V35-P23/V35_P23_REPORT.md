# V35-P23 3/5 工件精确前沿核验证据

诊断性证据：穷举四向量全空间（3_2_2_1：3,072；5_2_2_1：3,932,160 解码），与 baseline/FULL 单 seed 500000 FE 前沿对比（IGD/C 指标）。交叉验证：算法前沿任何解不得严格支配精确前沿解。无统计、无正式结论。

## 数据文件

- `EXACT_FRONT_METRICS.csv`：精确前沿规模、穷举解码数、双臂 IGD/C
- `exact-front-3_2_2_1.csv` / `exact-front-5_2_2_1.csv`：精确前沿
- `runs/`：各臂 configuration.txt / front.csv / 审计文件
