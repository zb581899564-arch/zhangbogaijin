# P8.6 疲劳一致共同空档左移/右移报告

## 结论

`fatigue-shift-v2-common-gap`的生产代码、I1证据、5000 FE解释链和20k FULL/B1公平烟测均已通过。正式P8配置统一使用`LEFT_RIGHT`，历史兼容入口仍默认为`NONE`。

当前可确认：

```text
fatigue_shift_semantics_version=fatigue-shift-v2-common-gap
common_gap_left_shift_validated=true
frozen_cmax_right_shift_validated=true
full_fatigue_replay_validated=true
single_pass_shift_determinism_validated=true
shared_shift_decoder_fairness_validated=true
internal_replay_fe_isolation_validated=true
runtime_100k_gate_current_semantics=false
sampled_reproduction_accepted=false
full_reproduction_accepted=false
formal_matrix_started=false
```

## I0本人手算图例门

用户随后明确授权按新版规则重做I0粒子。固定seed筛选只输出四向量和图例计数，没有保存或发布开始时间、结束时间、疲劳轨迹、目标或甘特答案。冻结粒子在`fatigue-shift-v2-common-gap`下通过：

```text
I0_FCLS=1/6 accepted
I0_FCRS=1/41 accepted
I0_internal_full_propagations=42
P8.6=completed
I1_public_illustration_gate=true
I0_private_manual_illustration_gate=true
I0_manual_submission_received=false
```

下一步不是继续改粒子，而是由用户本人先完成空白手算表并提交冻结副本；在此之前继续禁止生成I0程序数值答案。

## 证据入口

- 根因和语义映射：`docs/evidence/P8.6/ROOT_CAUSE_AND_FIX.md`
- 测试和烟测：`docs/evidence/P8.6/TEST_AND_SMOKE_REPORT.md`
- I1移位：`paper_evidence/I1/10_common_gap_shift_validation`
- I1进化链：`paper_evidence/I1/11_common_gap_evolution`
- 20k烟测：`docs/evidence/P8.6/smoke/smoke-20k`
- I0输入和规则：`paper_evidence/I0`
