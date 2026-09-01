# V35-DOE-1 开发阶段冻结报告

开发运行完整性：135/135条、每条500000 FE，均已由报告重建器检查。

二次 Scheffe 模型为全秩（rank=10），全部项可估计；条件数单独报告为 275.7587094292299。

模型选择：**REJECTED**。尽管拟合内 adjusted R2=0.10450190785985569，其 out-of-sample predicted R2=-0.041356609831044944 < 0；因此预注册的 observed paired-median fallback 生效。LOTO-RMSE=0.04050613308114172。

候选仅来自通过质量门并经五维观测 Pareto 过滤后的前三： [30/50/10/10, 25/25/25/25, 20/40/30/10]。它们尚未通过 held-out confirmation；正式搜索容量仍未冻结。

```text
development_campaign_integrity=ACCEPTED
fairness_and_budget_closure=ACCEPTED
mechanism_trigger_closure=ACCEPTED
reference_front_construction=ACCEPTED
response_surface_selection=REJECTED
observed_fallback_selection=ACCEPTED
final_search_mixture=NOT_FROZEN
heldout_confirmation=REQUIRED
```
