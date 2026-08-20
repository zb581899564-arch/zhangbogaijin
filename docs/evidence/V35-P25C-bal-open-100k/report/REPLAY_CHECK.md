# P25C 独立JVM重放检查

比较对象：`seed=20260819`、`A4`、100000 FE。

字节级一致：

```text
front.csv
initial-population.sha256
configuration.txt
ca-ta-lite-events.log
dscr-events.csv
dscr-teacher-uses.csv
cmax-audit-curves.csv
cmax-audit-records.csv
bottleneck-pressure-events.csv
```

机制摘要仅真实计时字段不同；将`algorithmRunNanos/baseDecodeNanos/decoderTotalNanos/frameworkOverheadNanos`替换为统一占位后，文本完全一致。计时字段不进入算法动作或前沿哈希。

代表性哈希：

```text
front.csv                       C213B1845666197D197950BE2B19826D25362AF6B39C821D4AE7C604257C7A0B
ca-ta-lite-events.log           E8CA741EEDFB89C3A055990345E236B1AD0424645A6FF50F19AE09456732DC2D
dscr-events.csv                 5E6611CF78FB65C364F741837BC018F20F4BAF224606F6D448CDF132517B96EE
cmax-audit-records.csv          701A703D46BC3273335E6E13FB6D26184F184609ED56583A05E8747DC2B705E5
bottleneck-pressure-events.csv  A3B34276E64C1BC85B256494DD960A4450F3DAA26FE9732DBD2F6525ADF87515
```
