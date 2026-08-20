# P3 人工核算与黄金目标

## Fig.3首位置

论文首位置是`J6/F2/S1/M2/W2`，Java运行态为`job=5,factory=1,stage=0,machine=1,worker=1`。

```text
SUT=2
ST=8
MS=1.1
WE=1.2
setup = 2 / 1.2 = 1.6666666666666667
processing = 8 / (1.1 * 1.2) = 6.06060606060606
duration = 7.727272727272727
```

JUnit以`1e-9`容差逐项断言上述值，并对全部十个第一阶段工件逐一核对FA/MA/WA。

## 三阶段目标分解

| 阶段 | Cmax | 加工能耗 | 待机能耗 | TEC | TWC |
|---|---:|---:|---:|---:|---:|
| 初始追加式 | 60.94964799510254 | 1974.2777071413434 | 78.82843587389044 | 2053.106143015234 | 2576.2587412587413 |
| 微调主动式 | 60.68870523415978 | 1982.796225659862 | 40.30991735537191 | 2023.106143015234 | 2602.9254079254083 |
| 右移最终 | 60.68870523415978 | 1982.7962256598619 | 28.636363636363647 | 2011.4325892962256 | 2602.9254079254083 |

待机率固定为`1.0`，来源标签为`author_actual_compatibility`。微调会重新执行完整后续阶段资源选择，因此其加工能耗和TWC可以与初始追加式不同；右移固定资源分配与顺序，故相对微调保持TWC不变，并将TEC从`2023.106143015234`降至`2011.4325892962256`。

三份完整20工序轨迹位于：

- `jmetal-problem/src/main/resources/dfsp/chapter4/p3-fig3-initial-deterministic.csv`
- `jmetal-problem/src/main/resources/dfsp/chapter4/p3-fig3-fine-deterministic.csv`
- `jmetal-problem/src/main/resources/dfsp/chapter4/p3-fig3-right-deterministic.csv`

