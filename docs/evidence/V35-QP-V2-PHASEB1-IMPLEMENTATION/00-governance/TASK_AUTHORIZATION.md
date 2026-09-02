# Phase B1 治理授权与边界记录（00-governance）

工作包 ID：`V35-QP-V2-PHASEB1-IMPLEMENTATION`
执行日期：`2026-09-02`
性质：**Qp-v2 Candidate A 隔离实现、K=1 等价与 20k 工程门工作包**。

---

## 1. 用户明确授权状态

```ini
USER_APPROVED_QP_V2_CANDIDATE=CANDIDATE_A_TOPK_UNIFORM
PHASE_B1_IMPLEMENTATION_AUTHORIZED=true
LOCAL_2K_AUTHORIZED=true
REMOTE_20K_ENGINEERING_GATE_AUTHORIZED=true
QP_V2_250K_AUTHORIZED=false
```

---

## 2. 冻结面与保护纪律

1. **正式算法 Jar 保持不变**：
   `8DAD8F40266FEEAA4CDB9B47DBE4E342D9064847DF32C7F2933149B9B6BAD8B9`（严禁覆盖、替换或重新打包为正式 Jar）；
2. **所有机制完全冻结**：
   FM3、ShiftMode=NONE、单产品族、序列无关 SUT、mixture=20/40/20/20、PDDR=GLOBAL_ORIGINAL、CA-TA-Lite $\to$ inherited LS、P5/G5、rho=0、Qp四动作、Qp掩码、Qp奖励、Qp TD更新、个人档案容量 L=6、个人档案更新/去重/截断、Qg、CFVF公式、betaMax；
3. **未跟踪工作区保护**：
   严格隔离历史未跟踪目录 `docs/evidence/V35-SOURCE-ATTRIBUTION-500K/07-sa-hard-500k/`，禁止任何形式的清理或修改。
