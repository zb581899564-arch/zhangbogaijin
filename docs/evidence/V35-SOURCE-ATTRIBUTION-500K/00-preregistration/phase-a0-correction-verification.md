# phase-a0-correction-verification.md — PhaseA0-CORRECTION-V1 主Agent独立复核报告

- 日期：2026-09-01
- 复核人：主Agent（与修正实现开发者路径分离的独立复核）
- 复核脚本：`../06-independent-verification/main_agent_correction_verification.py`
- 独立性规则：全部期望值由测试合同**显式给定**（脚本内 EXPECTED_* 字面常量，手工推导），
  不从被测实现调用结果照抄；实现仅被调用后与合同期望值比对。

## 1. 复核范围与结果

| 项 | 内容 | 结果 |
|---|---|---|
| §九.1 双来源相同点反例 | producerSet={GLOBAL_CFVF,CATA}；WHVG双方=0；ExclusiveND双方=0 | PASS |
| §九.2 epsilon相等反例（5e-13） | 折叠为1个规范三元组；语义同T1 | PASS |
| §九.3 单来源回归 | nndAll=1（P_EXCL_G支配P_SHARED，合同显式给定）；ExclusiveND=1；WHVG>0 | PASS |
| §九.4 20次随机行序 | producerSet/Wt/Wt^-s/WHVG/ExclusiveND 全部不变 | PASS |
| §九.5 内存公式边界 | baseline-only=B+max(0.2B,256MiB) 非×25；cap单调；无disk参数；<0.60 PASS / ==0.60 FAIL / >0.60 FAIL | PASS |
| §九.6 清单反算 | 30行（28内部证据文件 + 2条跨目录绑定：独立复核清单与脚本）0缺失0不匹配 | PASS |
| 附加：first-admission限制 | 字段仍记录（描述性=最earliest事件GLOBAL）；不泄漏进门控（WHVG_GLOBAL=0 despite first-admission=GLOBAL） | PASS |

**INDEPENDENT_VERIFICATION = PASSED**

## 2. 开发者自测与独立复核的双路径对照

| 测试 | 开发者路径 | 独立路径 |
|---|---|---|
| T1–T7 | `threshold_recompute.py --selftest` PASS | 逐合同字面期望值比对 PASS |
| T8 | `threshold_recompute.py --memory-selftest` PASS | 手工推导边界值比对 PASS |
| --audit（可比性/充足性 vs 冻结JSON） | PASS（15项MATCH） | —（audit本身即主Agent交叉验证） |
| 清单 | 27项0/0 | 独立反算 0/0 |

## 3. 抽查的关键语义点（主Agent人工验证）

1. **共享点反事实**：T1/T4中删除任一来源后共享点仍在（另一来源事件保留），WHVG归零——与任务书§3.2公式逐字一致；修正前的`attribution[r] != s`过滤会把混合producerSet的组也剔除（正是初版错误），已改为`producer_sets[r] != {s}`。
2. **epsilon折叠**：T2两点差5e-13折叠为1组，producerSet含双来源——fc6.equal口径与去重规则一致。
3. **T6支配关系**：t3obj=(85,470,940)在并集中支配p_shared=(90,480,950)→严格ND新点仅1个——初版测试合同误写2/2，修正为1/1（合同修正，非实现修正）。
4. **内存公式**：baseline-only = B+max(0.2B, 256MiB)；示例（2GiB基线/4GiB堆）ratio=0.609>0.60→fail-closed进MEMORY_MODEL_INSUFFICIENT路径——公式对"加堆掩盖"形成制度性阻断。

## 4. 结论

Phase A0 两项验收阻断问题（多来源反事实语义、内存外推公式）已修正并通过双路径0-FE验证；
NORMAL文字勘误完成且不改变100_2_3_1选择；正式Jar/冻结语义/FE/上传边界全部遵守。
`phaseA0Decision=PHASE_A0_PREREGISTRATION_PASSED`（修正版）可提交独立复验。


---

## 证据重包装补记（2026-09-01，独立复验意见落实）

- 初版修正包清单登记27项，遗漏 `phase-a0-correction-verification.md`（实际28个内部证据文件）；
- 独立复核脚本此前未被任何SHA清单绑定 → 已生成 `../06-independent-verification/evidence-sha256.tsv`
  （绑定 main_agent_correction_verification.py），并在顶层清单以 `../06-independent-verification/…`
  两条跨目录绑定行登记该清单与本脚本的SHA；
- 独立复核脚本增加跨清单绑定自检（own-manifest closure + 顶层绑定一致性），重跑 PASS；
- 全部反算：顶层30行（28内部+2外部绑定）0缺失0不匹配；06清单1项0/0。
