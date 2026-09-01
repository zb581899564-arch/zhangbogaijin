# V35-DOE-1 四子群容量 Mixture Design

本目录是 DOE-1 的证据根目录。当前已完成设计器、配置绑定和 Runner/预检代码；**尚未启动 135 条 500000 FE 开发运行，也未启动 held-out confirmation**。

固定主线：`GLOBAL_ORIGINAL` PDDR、`CA-TA-Lite → inherited LS`、FM3、单一产品族、序列无关 SUT、`ShiftMode=NONE`、方向教师池关闭。搜索期基线容量是 `[G1_CMAX,G4_BALANCED,G2_TEC,G3_TWC]=[20,40,20,20]`；`[15,55,15,15]` 仅作为历史容量控制点，不是 Region-aware 生存配额的重新启用。

运行入口：`ZhangBoV35Doe1MixtureRunner`。

```text
--phase REGISTRY   只生成候选格点、15点设计和设计摘要
--phase PREFLIGHT  执行15条、每条2000 FE的预检
--phase RUN --treatment 0..14 --instance <id> --seed <seed> --max-fes 500000
```

开发阶段必须在 15 条预检全部通过后，按 15×3×3=135 条独立运行；确认阶段只允许使用开发质量门通过的最多三个 treatment 和基线，并使用独立 reference front。

当前状态：

```text
doe1_design_implemented=true
doe1_preflight_completed=true
doe1_development_started=false
doe1_confirmation_started=false
formal_matrix_started=false
sampled_reproduction_accepted=false
full_reproduction_accepted=false
```
