# 训练机实验目录地图

训练机：`aic-inspur-home`  
根目录：`/home/inspur/aicomp`  
删除前文件级清单：`inventory/pre-cleanup/remote-artifact-ledger.tsv`  
删除后清单：`inventory/post-cleanup/remote-artifact-ledger.tsv`  
机器可审计的当前状态：`remote-location-map.csv`

清理执行后，下面表中的“清理”均已完成并可由G盘恢复；“保留”的6个目录仍在训练机展开保存。

| 远端目录 | 大小 | 定位 | 权威状态 | 去留 |
|---|---:|---|---|---|
| `zhangbo-cmax-audit-20k-20260812` | 0.04GB | Cmax教师生命周期诊断 | 历史有效诊断 | 已归档并清理展开目录 |
| `zhangbo-fc-500k-20260817` | <0.01GB | FC早期入口 | 已被FC6替代 | 留摘要后清理 |
| `zhangbo-fc4-20260817` | <0.01GB | FC构建源 | Final freeze已替代 | 留构建哈希后清理 |
| `zhangbo-fc6-20260818` | 0.88GB | FC-2--FC-6完整批次 | 参数裁决权威源 | 训练机展开保留，G盘完整归档 |
| `zhangbo-fc6a1-20260819` | 0.23GB | BP污染平行批 | 拒绝 | 留WARNING/哈希，清理展开数据 |
| `zhangbo-fc6a4-order-20260820` | 0.21GB | CURRENT vs ORDER_SWAP | FC-6A.4权威源 | 训练机展开保留，G盘完整归档 |
| `zhangbo-fc6b-region-20260820` | 0.13GB | FC6B失败初版 | 被r3替代 | 留失败摘要，清理展开数据 |
| `zhangbo-fc6b-region-20260820-r1` | 0.05GB | 上传/失败重试 | 被r3替代 | 留清单，清理 |
| `zhangbo-fc6b-region-20260820-r2` | 0.20GB | 20-job中间重试 | 被r3替代 | 留摘要，清理展开数据 |
| `zhangbo-fc6b-region-20260820-r3` | 0.82GB | 20/100-job Region-aware权威批次 | 负结果权威源 | 训练机展开保留，G盘完整归档 |
| `zhangbo-java-p9-decoder-timing-500k-20260811` | 0.04GB | Shift-on decoder计时 | 历史语义 | 压缩保留 |
| `zhangbo-java-p9-five-additional-500k-20260810` | 0.05GB | 旧P9五seed | 历史语义 | 压缩保留 |
| `zhangbo-java-p9-pilot-20260810` | 0.05GB | 早期机制贯通 | 历史语义 | 压缩保留 |
| `zhangbo-java-p9-single-500k-20260810` | 0.05GB | 旧单seed对照 | 历史语义 | 压缩保留 |
| `zhangbo-p86-pair-100k-20260811` | 0.05GB | Shift v2 100k | 历史语义 | 压缩保留 |
| `zhangbo-runtime-audit-100k-20260810` | <0.01GB | 旧性能审计 | 历史语义 | 压缩保留 |
| `zhangbo-v35-doe1-20260820` | 7.39GB | 15 treatments×3实例×3seed | DOE1开发权威源 | 双副本归档后清理展开数据 |
| `zhangbo-v35-doe1-heldout-20260822` | 3.03GB | held-out参数确认 | DOE1确认权威源 | 双副本归档后清理展开数据 |
| `zhangbo-v35-p25a-main-variant-20260814` | 0.12GB | A0/A4/A5主版本门 | 旧压力语义 | 压缩保留 |
| `zhangbo-v35-p25d-8alg-50k-20260815` | 0.05GB | 增强比较器 | 无论文资格 | 留隔离证据后清理展开数据 |
| `zhangbo-v35-p25e-corrected-50k-20260815` | 0.67GB | 忠实八算法适配 | 先导权威源 | 训练机展开保留，G盘完整归档 |
| `zhangbo-v35-stage2-master-v2-20260823` | 0.41GB | 当前五臂Stage2 | 当前最新先导 | 训练机展开保留，12个配对组未清理 |
| `zhangbo-v35-stage2-phasebudget-20260823` | 0.04GB | Phase-budget初版 | 被r3替代 | 留失败摘要后清理 |
| `zhangbo-v35-stage2-phasebudget-20260823-r2` | 0.04GB | Phase-budget r2 | 被r3替代 | 留失败摘要后清理 |
| `zhangbo-v35-stage2-phasebudget-20260823-r3` | 0.10GB | Gate3及并发验收 | 当前权威源 | 训练机展开保留，G盘完整归档 |

## Stage2当前目录

```text
/home/inspur/aicomp/zhangbo-v35-stage2-master-v2-20260823/
├── acceptance/
├── formal/
│   └── PAUSED_BY_USER.properties
├── input/
├── pilot-a0-a4/output/
├── results/formal-a0-a4-4500/
└── tool/
```

只允许12个完整`instance=100_2_3_1, seed=20260808..20260819, arms=A0..A4`组进入当前先导分析。
