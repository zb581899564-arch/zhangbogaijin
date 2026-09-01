# FC5-T 首档50k训练机启动记录

状态：`FIRST_TIER_50K_ACCEPTED_PENDING_ANALYSIS`  
启动时间：2026-08-25 18:41（Asia/Shanghai）

## 远端绑定

```text
host=aic-inspur-home (100.127.244.47)
root=/home/inspur/aicomp/zhangbo-v35-fc5-transfer-20260825
tmux=v35-fc5-transfer-50k
runner=org.uma.jmetal.runner.lc_psode.ZhangBoV35Fc5TransferRunner
diagnosticJarSha256=E59698030AF2215994D4FD179AA2B1F26787A0F1239628543339477E119FA8B5
launchScriptSha256=ABB0E9CDB9F619A42EEBE48842E9B516B82370F9065DC06919A6C98271FE489B
telemetryVersion=FC5_100JOB_TRANSFER_V1
PDDR=GLOBAL_ORIGINAL
MaxFEs=50000
parallelJvms=12
runs=24
autoEscalation=false
```

## 输入来源

- `100_2_4_1`、`100_5_3_1`，seed `20260901..20260903`：
  `/home/inspur/aicomp/zhangbo-v35-a2-a4-confirmation-20260824/input/snapshots`
- `100_2_5_1`、`100_8_3_1`，seed `20260911..20260913`：
  `/home/inspur/aicomp/zhangbo-v35-a2-final-candidate-confirmation-20260825/run-r4/input/snapshots`
- 实例、SUT和疲劳参数：
  `/home/inspur/aicomp/zhangbo-v35-stage2-master-v2-20260823/input/java-project`

12份`.fourvec`均已用各自`.receipt.properties`中的`snapshotSha256`逐一复核。未重新生成初始种群。

## 启动门

- 定向回归：13项通过，0失败、0错误；
- 诊断Jar主类字节码：major version 52；
- A0/A2/A4三条2k启动探针均完成，非法解与重复评价为0；
- 2k预算不足一个完整5000-FE Q phase，三个探针仅完成100 FE初始化，因此明确排除于实验结果；
- 正式首档使用预登记的50k预算，完成后脚本检查24条状态、FE/decoder闭合、非法解、重复评价及四类遥测文件；
- 不自动升级预算，不修改PDDR，不恢复4500正式矩阵。

## 完成验收

```text
runs=24/24
pairedGroups=12/12
sameInitialPopulationWithinPair=true
evidenceReverseVerified=true
A0 actualFE=50000 (6/6)
A2 actualFE=50000 (12/12)
A4 actualFE=48269 (6/6; phase-consistent termination)
illegalSolutions=0
duplicateEvaluations=0
autoEscalation=false
```

远端验收文件：

```text
/home/inspur/aicomp/zhangbo-v35-fc5-transfer-20260825/FIRST_TIER_50K_ACCEPTANCE.csv
SHA-256=577E91835852D329E5E562384F4D2782F3F4F49B596308534B8918B20687CA78
/home/inspur/aicomp/zhangbo-v35-fc5-transfer-20260825/FIRST_TIER_50K_ACCEPTANCE.properties
SHA-256=ED3C20F5BFD66DA3B4E935570BE804B156809523ED044226686FEA1C686E7DC1
```

这些结果只进入下一步H1分析；当前尚未作`FC5_TRANSFER_CONFIRMED/NOT_CONFIRMED`裁决。
