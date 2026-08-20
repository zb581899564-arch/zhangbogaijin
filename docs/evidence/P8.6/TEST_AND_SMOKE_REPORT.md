# P8.6 测试与小规模烟测

日期：2026-08-11  
JDK：Oracle JDK 17.0.12（仅构建运行时）  
编译目标：Java 8，主Runner字节码 major version `52`。

## 定向与回归测试

- `jmetal-problem`当前全模块：59/59通过；I0共同空档图例门改为严格断言，0条件跳过。
- `jmetal-problem`移位定向包含11项FCLS/FCRS回归及1项I0固定粒子回归，全部通过。
- `jmetal-algorithm`张博/P8定向：100/100通过。
- `jmetal-exec`张博/P8相关：23/23通过。
- P9 2000 FE测试链、P8注册表、I1证据Runner均通过。
- Maven根工程声明的5个模块完成`package -DskipTests`；Javadoc仅出现本地`package-list`链接警告，退出码为0。

## I1移位证据

证据：`paper_evidence/I1/10_common_gap_shift_validation`。

```text
leftAccepted=1 / leftCandidates=24
rightAccepted=3 / rightCandidates=74
internalPropagations=75
illustrationGate=true
```

程序保存S0、S1、S2完整工序表、候选事件、目标/疲劳变化、重传播误差表及三张SVG甘特图。S1疲劳指标来自实际左移结果，不再错误复用S0指标。

## I1 5000 FE解释链

证据：`paper_evidence/I1/11_common_gap_evolution`。

```text
fullEvaluations=5000
qgObserved=true
qpObserved=true
cfvfObserved=true
archiveObserved=true
caTaDecisionObserved=true
caTaAcceptedObserved=true
pddrObserved=true
single_lineage_evolution_trace_validated=true
```

## 20k公平烟测

证据：`docs/evidence/P8.6/smoke/smoke-20k`。

- 实例：`20_2_3_1`；seed：`20260808`；population：100；预算：20000 FE。
- FULL与B1使用相同FM3、相同`LEFT_RIGHT`、相同初始种群哈希。
- FULL：20000 FE，前沿108，CFVF/Qp/档案/CA-TA均真实触发。
- B1：20000 FE，前沿80，正式基线更新、Qg、关键工厂交换/插入和O1–O9均真实触发。
- 两条路径均PASS；本结果只证明链路、预算、合法性和公平接入，不构成算法优越性结论。
- 本次串行烟测耗时约499秒，仅记录，不作为P8.6性能门。

## 未执行

未运行100k、500k、六seed、正式消融矩阵、显著性检验或论文复现验收。

## I0新版本人手算粒子

用户授权后，以seed `20260808`只筛选四向量和图例门，随后删除随机筛选并冻结严格回归。当前1基X0为：

```text
JS=[3,1,2,4,5]
FA=[1,2,2,1,1]
MA=[1,1,1,1,2]
WA=[1,1,1,2,2]
FCLS=1/6 accepted
FCRS=1/41 accepted
internalFullPropagations=42
```

筛选与回归未向本人手算题包写入开始/结束时间、疲劳值、目标值或甘特答案。空白题包只要求本人完整重建S0、一个接受FCLS后的S1和一个接受FCRS后的S2；42次内部传播只作程序审计。
