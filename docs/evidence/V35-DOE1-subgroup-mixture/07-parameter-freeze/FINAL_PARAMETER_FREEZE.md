# V35-DOE-1 Parameter Freeze

确认阶段的 60 条独立 500000 FE 运行已经完成并通过独立验收。没有新容量同时满足预注册的确认门，故冻结：

```text
FINAL_SEARCH_MIXTURE = [G1_CMAX,G4_BALANCED,G2_TEC,G3_TWC] = [20,40,20,20]
```

冻结的主线还包括：`GLOBAL_ORIGINAL` PDDR、`CA-TA-Lite → inherited LS`、FM3、A4-Pacing、双Q `P=5/G=5`、`rho=0`、方向教师池关闭、单一产品族、序列无关 SUT 与 `ShiftMode=NONE`。

详见 [`../06-heldout-confirmation/HELDOUT_ACCEPTANCE_REPORT.md`](../06-heldout-confirmation/HELDOUT_ACCEPTANCE_REPORT.md)。DOE-2 Pacing 维持 `pending_user_approval`。
