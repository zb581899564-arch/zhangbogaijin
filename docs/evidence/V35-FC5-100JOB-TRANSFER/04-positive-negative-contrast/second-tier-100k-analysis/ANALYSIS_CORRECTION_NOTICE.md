# ANALYSIS_CORRECTION_NOTICE — 旧100k报告纠错提示

> 本文件不覆盖原报告，仅提示口径失效。

**原报告** `SECOND_TIER_100K_ANALYSIS_REPORT.md` §3 “四方向代表保留率16格全1.0” 的计算公式为 `P(next | PDDR selected)`，误标记为 `pool→next 保留率`。

**纠正** 见 `../second-tier-100k-analysis-correction/`：

- 真实 `pool→next = 95.8% (138/144)`，A4 W2 为 80%/100%/90% per seed（`directional-retention-corrected.csv`）。
- 存在6条 `pddrSelected=false` 拒绝事件（`rejected-representative-events.csv`），非0丢失。
- 教师曝光旧报告未去重，W1/W2对比被放大，纠正后见 `teacher-utilization-normalized.csv`。
- H1a仍为 NOT_CONFIRMED，H1b由“未确认”更正为 **LOCAL_FAILURE_EVENTS_OBSERVED_TRANSFER_UNRESOLVED**。

**仍有效的结论**：无ND>100， archive gap轻微转正3-6（<1% Cmax），教师链路未断裂，500k退化时序无对应。

详见 `FC5_100K_ANALYSIS_CORRECTION_REPORT.md` 总控签字版。

ANALYSIS_CORRECTION_ONLY=true | newTrainingRuns=0 | algorithmChanged=false | pddrChanged=false
