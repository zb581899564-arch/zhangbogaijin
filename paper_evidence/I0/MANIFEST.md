# I0 P8.6共同空档移位粒子输入阶段证据清单

状态：`input_only_p8_6_common_gap_particle_frozen_waiting_user_manual_submission`

旧I0 v1粒子和旧空白表继续保留为历史草案，不再用于当前本人手算。当前清单对应`fatigue-shift-v2-common-gap/LEFT_RIGHT`新版X0及`S0→FCLS-S1→FCRS-S2`空白手算题包。

```text
inputOnly=true
particleSuitabilityScreened=true
leftCandidates=6
leftAccepted=1
rightCandidatePropagations=41
rightAccepted=1
internalFullPropagations=42
numericScheduleAnswerPublished=false
javaNumericAnswerRun=false
manualSubmissionReceived=false
p8_6DecoderSemanticsFrozen=true
comparisonCompleted=false
```

适用性筛选只证明新版粒子能够真实触发两类接受事件；没有在题包中保存或发布开始时间、结束时间、疲劳轨迹、目标值或甘特图答案。

## 当前冻结文件

| 文件 | 字节数 | SHA-256 |
|---|---:|---|
| `paper_evidence/I0/00_protocol/HAND_CALC_STEPS.md` | 4829 | `b218bf58706be3dc11122efeeb897d3a2e516d2305c10c4aecf47a8690a60871` |
| `paper_evidence/I0/00_protocol/README_FIRST.md` | 2200 | `11c23c8a86b54f2328ed6d67f97d0e6f752e2caa4a79da49b634742f2ce33c36` |
| `paper_evidence/I0/01_input/i0_input.json` | 5576 | `3df2c4516c1290a221f024233ca2b15bf9356de3d9eda9de6852b1c2c9988fbd` |
| `paper_evidence/I0/01_input/I0_INPUT_PRINTABLE.md` | 2187 | `8203951f90885d0c4e84ee5d4de946cabd1807340be0792b17f885924e64025b` |
| `paper_evidence/I0/03_answer_lock/STATUS.md` | 508 | `cff940297c4d1ecf9b8cf0b94023ee05c75d42139fdad2b2ee65a42cc21303ea` |
| `java-jmetal58/jmetal-problem/src/test/java/org/uma/jmetal/problem/multiobjective/dfsp/fatigue/shift/ZhangBoI0ParticleScreeningTest.java` | 3576 | `852d5f56045af4afe1d1fc4cf86d82bbe9c160a5384708763fbaa85e89567fc2` |
| `outputs/019feb24-6dba-7651-be7c-54b0323631aa/I0_v2共同空档左移右移_本人手算空白模板.xlsx` | 30759 | `5d0f108f18bab87e2c764cf296ef6cda9cc904c364a851bd10a06cefb12683fb` |

## 解锁条件

只有用户本人完成S0、FCLS/S1、FCRS/S2及目标手算，提交副本并冻结SHA-256后，才允许在已经冻结的P8.6语义下生成程序数值答案并逐项比较。

本清单自身不纳入哈希闭包；后续本人提交与程序对照将创建新清单，不覆盖本文件。
