# PREREGISTRATION — V35-FC5-MIDHORIZON-DIAGNOSTICS-V1

**DIAGNOSTIC_TOOLING_ONLY=true | algorithmChanged=false**
**Instances: 100_2_4_1, 100_5_3_1 | Arms A2(DSCR+CFVF) / A4(A2+archive+Qp+CA-TA) | Checkpoints nominal 2k:1k,2k 20k:5k,10k,15k,20k 250k:25k..250k**

- 目标：为12条250k建立可信低开销观测工具，不改算法，不启250k
- 验收：telemetryContractFrozen, 2k/20k OFF/ON行为等价 (initialHash/RNG/PDDR/Q-table/front), observerErrors=0, overhead<=15%, historical gold reproduce 1e-12
- 竞争假设 H-PDDR / H-QP / H-CATA 仅登记，不在本轮裁决，允许 COMPOSITE_OR_UNRESOLVED
