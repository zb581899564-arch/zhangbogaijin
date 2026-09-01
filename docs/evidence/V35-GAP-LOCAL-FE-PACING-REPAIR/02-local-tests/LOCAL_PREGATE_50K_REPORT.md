# LOCAL_PREGATE_50K_REPORT（V35-LOCAL-FE-PACING-50K · Agent A · 协议复核与本地前置准备）

- 执行日期：2026-08-31（本次会话实际执行；报告落盘时间 2026-08-31 14:51 本地时间）
- 执行环境（`java -version` 实际输出）：

```
java version "17.0.12" 2024-07-16 LTS
Java(TM) SE Runtime Environment (build 17.0.12+8-LTS-286)
Java HotSpot(TM) 64-Bit Server VM (build 17.0.12+8-LTS-286, mixed mode, sharing)
```

- 沙箱：`02-local-tests/sandbox`（本报告所有路径相对 evidence 目录 `V35-GAP-LOCAL-FE-PACING-REPAIR/`）
- 写入范围声明：本次仅新建 `sandbox/snapshots-50k/`、`sandbox/bindings-50k-20260914/`、`sandbox/50kprep-2k-50_2_3_1-20260914/`、`sandbox/50kprep-2k-100_5_3_1-20260914/`、`sandbox/50kprep-regression-20k/` 及本报告文件；未修改 01/03/04/05/06/07 目录、build_gate.py 及 sandbox 下任何已存在文件。

---

## T1 Jar 与类版本复核 — PASS

### T1.1 jar SHA-256

命令（在 `sandbox/jars/` 下执行）：`sha256sum formal-algorithm-8DAD8F40.jar jmetal-algorithm-5.8-V35-LOCAL-FE-PACING-REPAIR-V1.jar`

```
8dad8f40266feeaa4cdb9b47dbe4e342d9064847df32c7f2933149b9b6bad8b9 *formal-algorithm-8DAD8F40.jar
a0788580684cff71ecc526e0f23d6e186dcd9131aad98776c25419378dc7331c *jmetal-algorithm-5.8-V35-LOCAL-FE-PACING-REPAIR-V1.jar
```

- formal = `8dad8f40...bad8b9`：与预期值一致；与现有 `sandbox/bindings/50_2_3_1.binding.properties`、`100_5_3_1.binding.properties` 中登记的 `formalJarSha256` 完全一致。
- experimental = `a0788580...7331c`：与预期值一致。
- 与 01-implementation 对比：`01-implementation/jmetal-algorithm-5.8-V35-LOCAL-FE-PACING-REPAIR-V1.jar` 实测 SHA-256 = `a0788580684cff71ecc526e0f23d6e186dcd9131aad98776c25419378dc7331c`，与 sandbox 副本逐字节一致（`sha256sum` 直接对比）。说明：`01-implementation/` 下不存在同名 `formal-algorithm-8DAD8F40.jar` 文件（全 evidence 目录 find 仅命中 sandbox/jars 一处），formal jar 因此以现有两个 binding 文件中登记的 `formalJarSha256` 为对比基准，实测一致。另：`01-implementation/snapshots/` 两个 seed-20260907 快照与 `sandbox/snapshots/` 副本 SHA-256 逐一相同（79d1de2a… / 57ecc786…）。

### T1.2 实验 jar 12 个 class 的 major version

命令：python zipfile 读取每个 class 字节第 7-8 位（大端）。

```
org/uma/jmetal/algorithm/multiobjective/mypso/v35/V35LocalFePacingRepairProfile$Label.class  major=52 minor=0
org/uma/jmetal/algorithm/multiobjective/mypso/v35/V35LocalFePacingRepairProfile.class  major=52 minor=0
org/uma/jmetal/algorithm/multiobjective/mypso/v35/V35RepairProfileSelfTest$1.class  major=52 minor=0
org/uma/jmetal/algorithm/multiobjective/mypso/v35/V35RepairProfileSelfTest$2.class  major=52 minor=0
org/uma/jmetal/algorithm/multiobjective/mypso/v35/V35RepairProfileSelfTest$3.class  major=52 minor=0
org/uma/jmetal/algorithm/multiobjective/mypso/v35/V35RepairProfileSelfTest$4.class  major=52 minor=0
org/uma/jmetal/algorithm/multiobjective/mypso/v35/V35RepairProfileSelfTest$5.class  major=52 minor=0
org/uma/jmetal/algorithm/multiobjective/mypso/v35/V35RepairProfileSelfTest.class  major=52 minor=0
org/uma/jmetal/runner/lc_psode/V35LocalFePacingRepairRunner$Arguments.class  major=52 minor=0
org/uma/jmetal/runner/lc_psode/V35LocalFePacingRepairRunner$Budget.class  major=52 minor=0
org/uma/jmetal/runner/lc_psode/V35LocalFePacingRepairRunner.class  major=52 minor=0
org/uma/jmetal/runner/lc_psode/V35RepairSnapshotMaterializer.class  major=52 minor=0
```

12/12 全部 major=52（Java 8）。jar 总条目 13（12 class + 1 目录外条目为 META-INF/MANIFEST.MF，即 class 计数=12）。

### T1.3 formal jar 内容核查

命令：python zipfile 列出 formal jar 全部条目（共 10360 条），过滤 banned 类名。

```
BANNED-CHECK V35LocalFePacingRepairProfile: present=False hits=[]
BANNED-CHECK V35RepairProfileSelfTest: present=False hits=[]
BANNED-CHECK V35LocalFePacingRepairRunner: present=False hits=[]
BANNED-CHECK V35RepairSnapshotMaterializer: present=False hits=[]
V35FairRunner entries: ['org/uma/jmetal/algorithm/multiobjective/mypso/v35/V35FairRunner$1.class',
 'org/uma/jmetal/algorithm/multiobjective/mypso/v35/V35FairRunner$Mode.class',
 'org/uma/jmetal/algorithm/multiobjective/mypso/v35/V35FairRunner$RunRecord.class',
 'org/uma/jmetal/algorithm/multiobjective/mypso/v35/V35FairRunner.class']
```

4 个修复侧类全部不存在；`V35FairRunner.class` 存在。PASS。

---

## T2 物化 seed 20260914 快照 — PASS

工作目录：`sandbox/`。命令形式：

```
java -cp "jars/formal-algorithm-8DAD8F40.jar;jars/jmetal-algorithm-5.8-V35-LOCAL-FE-PACING-REPAIR-V1.jar" \
  org.uma.jmetal.runner.lc_psode.V35RepairSnapshotMaterializer \
  --project-root inputs/java-jmetal58 --instance <instance> --seed 20260914 \
  --output snapshots-50k/<instance>-seed-20260914.fourvec
```

### 完整 stdout（materialized|instance|seed|fileSha256|v35Hash|p8Hash）

```
materialized|50_2_3_1|20260914|5722f3d5319ea31834b0b2f241668193318b23502444b899f3a8f861466df6db|f80de22c4c983ccb9537579ca0b80343f535a65b57f883d7e0d52b3950a6e771|f667bae4094285119441c2553704d0cd68519f9c9474e271039758daad081080
materialized|100_5_3_1|20260914|26e0258a4f406101f622336453fe99f3f0ec8575a24d52ee0e689656679cc3e6|a20e8294afe260e98dbb647ae5d996d67dc402d2511e5d59ff0dd9e59b02ee0b|257eae2154113cfbf89c9548eef3a86f24b4a9c4ed3d0dd28ed46037b10fb9a7
```

`sha256sum` 复核（与 stdout fileSha256 一致）：

```
5722f3d5319ea31834b0b2f241668193318b23502444b899f3a8f861466df6db  snapshots-50k/50_2_3_1-seed-20260914.fourvec
26e0258a4f406101f622336453fe99f3f0ec8575a24d52ee0e689656679cc3e6  snapshots-50k/100_5_3_1-seed-20260914.fourvec
```

### 快照头部字段（head 实际输出，两文件均至 particle=0 行前）

| 字段 | 50_2_3_1 | 100_5_3_1 |
|---|---|---|
| schema | v35-formal-initial-population-v1 | v35-formal-initial-population-v1 |
| instanceId | 50_2_3_1 | 100_5_3_1 |
| instanceSHA256 | D08D6ABC46788D46BC24C135A8DC810B4675E333E3D61E65C7551835FC93E787 | 2E88FA97A6F84AF347A4603F04C387A65C8F9891BCAB8AC6B70FDEC622EA35CF |
| SUTSHA256 | D4C5D8016B5D625303E8E9479F570E59AD3197779FBCE0BA851D2AABD17C0E00 | E7E9FF7F646351FECB5801EC2EC177CEE2C00775173E4DE6841577695E8E58E1 |
| fatigueParameterSHA256 | 99521585E04391901F491943D0D0F50D046447A9474EE898EB00A19983242E5C | 81CAD959F27E461E41882E7353AC5F23574FA6DC50637F59E281B1E8788967A1 |
| problemConfigurationSHA256 | 555570431f7b552e8ac197042fc17fbbe5e340dcbbfb3c292b1db70d30dd0b26 | 892c7c3feddd09848bf35bac1a90a529153ad77b3cb712a36f357cd214cc79f4 |
| seed | 20260914 | 20260914 |
| population | 100 | 100 |
| decoderMode | FM3 | FM3 |
| familyMode | DEGENERATE_SINGLE_FAMILY | DEGENERATE_SINGLE_FAMILY |
| setupMode | SEQUENCE_INDEPENDENT | SEQUENCE_INDEPENDENT |
| shiftMode | NONE | NONE |
| semanticTag | fatigue_fm3 | fatigue_fm3 |
| initialPopulationSHA256 | f80de22c4c983ccb9537579ca0b80343f535a65b57f883d7e0d52b3950a6e771 | a20e8294afe260e98dbb647ae5d996d67dc402d2511e5d59ff0dd9e59b02ee0b |
| initialPopulationP8SHA256 | f667bae4094285119441c2553704d0cd68519f9c9474e271039758daad081080 | 257eae2154113cfbf89c9548eef3a86f24b4a9c4ed3d0dd28ed46037b10fb9a7 |

### 交叉验证（snapshot 头 vs 现有 sandbox/bindings/<instance>.binding.properties，equalsIgnoreCase）

```
50_2_3_1 instanceSha256 matchIgnoreCase=True
50_2_3_1 setupConfigurationSha256 matchIgnoreCase=True
50_2_3_1 fatigueConfigurationSha256 matchIgnoreCase=True
100_5_3_1 instanceSha256 matchIgnoreCase=True
100_5_3_1 setupConfigurationSha256 matchIgnoreCase=True
100_5_3_1 fatigueConfigurationSha256 matchIgnoreCase=True
```

6/6 全部一致 → 同实例输入链未变。PASS。

---

## T3 生成 seed 20260914 binding 文件 — PASS

新目录 `sandbox/bindings-50k-20260914/`，key 集合与现有 binding 完全一致（10 个 key，顺序一致）。生成后逐文件 cat 留痕：

### bindings-50k-20260914/50_2_3_1.binding.properties

```properties
formalJarSha256=8dad8f40266feeaa4cdb9b47dbe4e342d9064847df32c7f2933149b9b6bad8b9
experimentalJarSha256=a0788580684cff71ecc526e0f23d6e186dcd9131aad98776c25419378dc7331c
instanceSha256=d08d6abc46788d46bc24c135a8dc810b4675e333e3d61e65c7551835fc93e787
setupFileSha256=f9bde51a5f873896291527676bcbfaf8291c72b91175a6828dd722eaa54df54e
fatigueFileSha256=46192fe26bfc79201456c98d70d9acc25e40dd84cdd86b050af1718e3715b4da
snapshotSha256=5722f3d5319ea31834b0b2f241668193318b23502444b899f3a8f861466df6db
setupConfigurationSha256=D4C5D8016B5D625303E8E9479F570E59AD3197779FBCE0BA851D2AABD17C0E00
fatigueConfigurationSha256=99521585E04391901F491943D0D0F50D046447A9474EE898EB00A19983242E5C
initialPopulationHashV35=f80de22c4c983ccb9537579ca0b80343f535a65b57f883d7e0d52b3950a6e771
initialPopulationHashP8=f667bae4094285119441c2553704d0cd68519f9c9474e271039758daad081080
```

### bindings-50k-20260914/100_5_3_1.binding.properties

```properties
formalJarSha256=8dad8f40266feeaa4cdb9b47dbe4e342d9064847df32c7f2933149b9b6bad8b9
experimentalJarSha256=a0788580684cff71ecc526e0f23d6e186dcd9131aad98776c25419378dc7331c
instanceSha256=2e88fa97a6f84af347a4603f04c387a65c8f9891bcab8ac6b70fdec622ea35cf
setupFileSha256=4b49b780f6ee887099574f9008bebdc106e9cf5808a11b5af97a5ee4512c1d90
fatigueFileSha256=cf611bfb3690d50f1b4dc8d6d6631dd9d04546d3ca4c4020cc9017475d4bf457
snapshotSha256=26e0258a4f406101f622336453fe99f3f0ec8575a24d52ee0e689656679cc3e6
setupConfigurationSha256=E7E9FF7F646351FECB5801EC2EC177CEE2C00775173E4DE6841577695E8E58E1
fatigueConfigurationSha256=81CAD959F27E461E41882E7353AC5F23574FA6DC50637F59E281B1E8788967A1
initialPopulationHashV35=a20e8294afe260e98dbb647ae5d996d67dc402d2511e5d59ff0dd9e59b02ee0b
initialPopulationHashP8=257eae2154113cfbf89c9548eef3a86f24b4a9c4ed3d0dd28ed46037b10fb9a7
```

说明：instanceSha256/setupFileSha256/fatigueFileSha256 原样复制自现有 binding；setupConfigurationSha256/fatigueConfigurationSha256 保持现有文件的大写风格；snapshotSha256/initialPopulationHashV35/initialPopulationHashP8 取自 T2。binding 文件 SHA-256：50_2_3_1=`68546f9b92a3bc2fd6f408ab7e672936a22a37b082570a15059d8052fff60c30`，100_5_3_1=`797fa5cc0e0473fc5125947bee1725b149470eaa5c4d768d46379693f3317a2c`。

---

## T4 本地 2k 兼容测试（seed 20260914）— PASS

工作目录：`sandbox/50kprep-2k-<instance>-20260914/`，各含 `inputs/`（sandbox/inputs 完整复制）、`bindings/`（T3 新 binding）、`jars/`（两 jar 副本）、`snapshots/`（T2 快照）。

命令形式（串行执行，REF_A4 → C0 → C1 → C2 → C3）：

```
java -Xmx2g -cp "jars/formal-algorithm-8DAD8F40.jar;jars/jmetal-algorithm-5.8-V35-LOCAL-FE-PACING-REPAIR-V1.jar" \
  org.uma.jmetal.runner.lc_psode.V35LocalFePacingRepairRunner \
  --instance <instance> --seed 20260914 --profile <P> --max-fes 2000 \
  --snapshot snapshots/<instance>-seed-20260914.fourvec --output runs/local2k-<P>-<instance>
```

### 10 条运行 stdout 与退出码（实际输出）

```
V35_LOCAL_FE_PACING_REPAIR_COMPLETED profile=REF_A4 FE=100 ... 50kprep-2k-50_2_3_1-20260914\runs\local2k-REF_A4-50_2_3_1   EXIT=0
V35_LOCAL_FE_PACING_REPAIR_COMPLETED profile=C0    FE=100 ... 50kprep-2k-50_2_3_1-20260914\runs\local2k-C0-50_2_3_1       EXIT=0
V35_LOCAL_FE_PACING_REPAIR_COMPLETED profile=C1    FE=100 ... 50kprep-2k-50_2_3_1-20260914\runs\local2k-C1-50_2_3_1       EXIT=0
V35_LOCAL_FE_PACING_REPAIR_COMPLETED profile=C2    FE=100 ... 50kprep-2k-50_2_3_1-20260914\runs\local2k-C2-50_2_3_1       EXIT=0
V35_LOCAL_FE_PACING_REPAIR_COMPLETED profile=C3    FE=100 ... 50kprep-2k-50_2_3_1-20260914\runs\local2k-C3-50_2_3_1       EXIT=0
V35_LOCAL_FE_PACING_REPAIR_COMPLETED profile=REF_A4 FE=100 ... 50kprep-2k-100_5_3_1-20260914\runs\local2k-REF_A4-100_5_3_1 EXIT=0
V35_LOCAL_FE_PACING_REPAIR_COMPLETED profile=C0    FE=100 ... 50kprep-2k-100_5_3_1-20260914\runs\local2k-C0-100_5_3_1     EXIT=0
V35_LOCAL_FE_PACING_REPAIR_COMPLETED profile=C1    FE=100 ... 50kprep-2k-100_5_3_1-20260914\runs\local2k-C1-100_5_3_1     EXIT=0
V35_LOCAL_FE_PACING_REPAIR_COMPLETED profile=C2    FE=100 ... 50kprep-2k-100_5_3_1-20260914\runs\local2k-C2-100_5_3_1     EXIT=0
V35_LOCAL_FE_PACING_REPAIR_COMPLETED profile=C3    FE=100 ... 50kprep-2k-100_5_3_1-20260914\runs\local2k-C3-100_5_3_1     EXIT=0
```

### gate 属性逐条核验（程序化读取，实际输出）

```
50_2_3_1 REF_A4 status=COMPLETED failures=NONE phaseBoundAccepted=true actualFE=100 PASS=True
50_2_3_1 C0     status=COMPLETED failures=NONE phaseBoundAccepted=true actualFE=100 PASS=True
50_2_3_1 C1     status=COMPLETED failures=NONE phaseBoundAccepted=true actualFE=100 PASS=True
50_2_3_1 C2     status=COMPLETED failures=NONE phaseBoundAccepted=true actualFE=100 PASS=True
50_2_3_1 C3     status=COMPLETED failures=NONE phaseBoundAccepted=true actualFE=100 PASS=True
100_5_3_1 REF_A4 status=COMPLETED failures=NONE phaseBoundAccepted=true actualFE=100 PASS=True
100_5_3_1 C0     status=COMPLETED failures=NONE phaseBoundAccepted=true actualFE=100 PASS=True
100_5_3_1 C1     status=COMPLETED failures=NONE phaseBoundAccepted=true actualFE=100 PASS=True
100_5_3_1 C2     status=COMPLETED failures=NONE phaseBoundAccepted=true actualFE=100 PASS=True
100_5_3_1 C3     status=COMPLETED failures=NONE phaseBoundAccepted=true actualFE=100 PASS=True
ALL_10_GATE_OK=True
```

### REF_A4 vs C0 front.csv 字节等价（SHA-256）

```
50_2_3_1  REF_A4=24fda29045474fcd660f35bf121865cfeeff13f72c14d8b0ff9b9c029118d384
50_2_3_1  C0    =24fda29045474fcd660f35bf121865cfeeff13f72c14d8b0ff9b9c029118d384  → ByteMatch=True
100_5_3_1 REF_A4=561234f5fd4f21b73fac2a51ac7a88fbdff1e1e25fb4c4ca11d711fa57f17ea8
100_5_3_1 C0    =561234f5fd4f21b73fac2a51ac7a88fbdff1e1e25fb4c4ca11d711fa57f17ea8  → ByteMatch=True
```

### 预算边界行为说明（重要留痕）

`--max-fes 2000` 下 10 条运行**均未发生** TAIL_TOO_LARGE 类失败。以 50_2_3_1 REF_A4 的 budget-termination.properties 为例（10 条同构）：

```
budgetProtocol=PHASE_CONSISTENT_BUDGET_TERMINATION
requestedMaxFE=2000
actualFE=100
remainingFE=1900
qPhaseFE=5000
utilizationRate=0.050000000000
terminationKind=PHASE_CONSISTENT_TAIL_STOP
phaseBoundAccepted=true
phaseBoundFailure=NONE
```

解释：初始种群恰消耗 100 FE 后，Q 阶段需求 qPhaseFE=5000 超出剩余 1900，phase-consistent 预算协议触发 PHASE_CONSISTENT_TAIL_STOP 正常终止（accepted，非失败）。**最终采用预算 = --max-fes 2000**，无需按预案升级到 5000/6000。

---

## T5 本地 20k 回归测试（seed 20260907）— PASS

工作目录：`sandbox/50kprep-regression-20k/`（inputs 复制、bindings=现有 seed-20260907 binding、jars 副本、snapshots 从 01-implementation/snapshots/ 复制，实测快照 SHA `79d1de2a...` 与登记值一致）。

命令（在 50kprep-regression-20k/ 下执行）：

```
java -Xmx2g -cp "jars/formal-algorithm-8DAD8F40.jar;jars/jmetal-algorithm-5.8-V35-LOCAL-FE-PACING-REPAIR-V1.jar" \
  org.uma.jmetal.runner.lc_psode.V35LocalFePacingRepairRunner \
  --instance 50_2_3_1 --seed 20260907 --profile C0 --max-fes 20000 \
  --snapshot snapshots/50_2_3_1-seed-20260907.fourvec --output runs/local20k-C0-50_2_3_1
```

stdout：`V35_LOCAL_FE_PACING_REPAIR_COMPLETED profile=C0 FE=15258 ...`，EXIT=0；formal-gate.properties：status=COMPLETED、failures=NONE、actualFE=15258；budget-termination：phaseBoundAccepted=true。

front.csv SHA-256 对比（`sha256sum` 实际输出）：

```
a96122f3e543b4f5dc4f9343b808a78de993c7fb44b078c94b72f5ae130fef93  50kprep-regression-20k/runs/local20k-C0-50_2_3_1/front.csv
a96122f3e543b4f5dc4f9343b808a78de993c7fb44b078c94b72f5ae130fef93  sandbox/runs/local20k-C0-50_2_3_1/front.csv   （已登记的本地20k结果）
```

完全一致 → 本地环境确定性成立。PASS。

---

## T6 betaMax 运行时读取验证 — PASS

对 T4 全部 10 条运行 `grep -E "^localFeBudget\.betaMax=|^localFeBudget\.betaMin=" runs/<dir>/profile.txt`，实际输出：

```
50_2_3_1  REF_A4: betaMin=0.250000  betaMax=0.650000   ✓
50_2_3_1  C0:     betaMin=0.250000  betaMax=0.650000   ✓
50_2_3_1  C1:     betaMin=0.250000  betaMax=0.550000   ✓
50_2_3_1  C2:     betaMin=0.250000  betaMax=0.450000   ✓
50_2_3_1  C3:     betaMin=0.250000  betaMax=0.350000   ✓
100_5_3_1 REF_A4: betaMin=0.250000  betaMax=0.650000   ✓
100_5_3_1 C0:     betaMin=0.250000  betaMax=0.650000   ✓
100_5_3_1 C1:     betaMin=0.250000  betaMax=0.550000   ✓
100_5_3_1 C2:     betaMin=0.250000  betaMax=0.450000   ✓
100_5_3_1 C3:     betaMin=0.250000  betaMax=0.350000   ✓
```

10/10 与预期映射（REF_A4/C0=0.650000，C1=0.550000，C2=0.450000，C3=0.350000；betaMin 恒为 0.250000）一致。PASS。

---

## 机器可读结果块

```ini
formalJarSha256=8dad8f40266feeaa4cdb9b47dbe4e342d9064847df32c7f2933149b9b6bad8b9
experimentalJarSha256=a0788580684cff71ecc526e0f23d6e186dcd9131aad98776c25419378dc7331c
experimentalClassMajor=52
formalJarBannedClassesAbsent=true
formalJarFairRunnerPresent=true
snapshotFileSha256-50_2_3_1=5722f3d5319ea31834b0b2f241668193318b23502444b899f3a8f861466df6db
snapshotFileSha256-100_5_3_1=26e0258a4f406101f622336453fe99f3f0ec8575a24d52ee0e689656679cc3e6
snapshotV35Hash-50_2_3_1=f80de22c4c983ccb9537579ca0b80343f535a65b57f883d7e0d52b3950a6e771
snapshotV35Hash-100_5_3_1=a20e8294afe260e98dbb647ae5d996d67dc402d2511e5d59ff0dd9e59b02ee0b
snapshotP8Hash-50_2_3_1=f667bae4094285119441c2553704d0cd68519f9c9474e271039758daad081080
snapshotP8Hash-100_5_3_1=257eae2154113cfbf89c9548eef3a86f24b4a9c4ed3d0dd28ed46037b10fb9a7
snapshotBindingCrossValidationIgnoreCase=true
local2kCompleted=true
local2kCompletedCount=10
local2kRefC0ByteMatch=true
local2kFinalBudget=2000
local2kTailTooLargeOccurred=false
local20kRegressionByteMatch=true
local20kRegressionFrontSha256=a96122f3e543b4f5dc4f9343b808a78de993c7fb44b078c94b72f5ae130fef93
betaMaxRuntimeCorrect=true
javaVersion=17.0.12
```

---

## 总体结论

LOCAL_PREGATE_50K = PASSED（T1–T6 全部 PASS：jar 哈希与类版本合规、formal jar 无修复侧类污染；seed 20260914 双实例快照物化成功且与既有 binding 输入链 6/6 交叉验证一致；新 binding 文件已生成并留痕；2k 兼容 10/10 全 COMPLETED 且 REF_A4≡C0 字节级一致，最终采用预算 --max-fes 2000，未触发 TAIL_TOO_LARGE；seed 20260907 20k 回归 front.csv 与已登记结果 SHA-256 完全一致，本地环境确定性成立；10 条运行 betaMax 运行时读取全部符合预期映射。50k 正式实验（4 profiles × 2 instances × 2 seeds = 16 条，MaxFEs=50000）的本地前置条件全部就绪。）
