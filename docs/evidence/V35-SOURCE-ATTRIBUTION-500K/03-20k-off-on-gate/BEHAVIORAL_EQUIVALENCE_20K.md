# BEHAVIORAL_EQUIVALENCE_20K — 远端工程门等价结论（主Agent独立比对，流式修正版）

- 日期：2026-09-01（流式修正后重跑）
- 配对：obs20k-OFF / obs20k-ON（同实例100_5_3_1、seed 20260901、A4(C0)、MaxFEs=20000、
  同snapshot 84d84523…、同JVM -Xms1g -Xmx4g、同训练机串行）
- Observer Jar SHA-256（本批）：78bf4d3016a612a9f3073ca00abb94181ef4883b2838540ac9776b1eed046565

## 等价分类（更正：12字节逐字节一致 + 2掩码等价 + 1测量，非初版"14/14逐字节"）

12个文件**逐字节一致**：front.csv、passive-archive.csv、cmax-audit-curves/records、
ca-ta-lite-events.log、dscr-events/teacher-uses、bottleneck-pressure-events、
initial-population.sha256、profile.sha256、budget-termination.properties、
pddr-observation.properties。

2个文件**掩码等价**（含观察器溯源/时间字段，掩码后一致）：configuration.txt、status.properties。

1个文件**测量only**（heap/GC/wallClock按§十四排除）：memory-summary.properties。

```ini
observerBehavioralEquivalent=true
equivalenceDetail=12 byte-identical + 2 mask-equivalent + 1 measurement-only
```
