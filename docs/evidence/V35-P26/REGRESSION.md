# V35-FC 工程实施回归记录（2026-08-17）

命令（`docs/evidence/V35-P26/jdk17-regression-command.ps1` 同款）：

```powershell
mvn.cmd -q -pl jmetal-algorithm `
  "-Djacoco.skip=true" `
  "-DfailIfNoTests=false" `
  "-DargLine=--add-opens=java.base/java.lang=ALL-UNNAMED" `
  test
```

## 结果

```text
Tests run: 240, Failures: 1
```

失败分类：

| 测试 | 状态 | 说明 |
|---|---|---|
| V35P101TeacherPoolVerificationTest | **既有失败**（D-076 登记） | "pool OFF 复现 pre-P10.1 front 逐位"的前沿快照契约。FC-1 有意让主路径 N3/N4 读 FM3 真实关键结构，front 漂移加剧——基准更新与 FC-1 语义审计运行（服务器恢复后）一并处理，不在本工程包内抢跑。 |
| NSGAIIIT / DifferentialEvolutionTestIT | 未执行（旧残留报告） | `*IT` 后缀不在 surefire test 阶段运行；历史分类为 jMetal 上游/Mockito 环境问题。 |

## 冻结物重建（按 P24.2 先例）

FC 源码改动使 V35-P24/P24.1 的 source-sha256 漂移（其清单含 AGENTS.md 与 docs/ROADMAP.md）：

- `V35P24FreezeCaptureTest` 重写 `docs/evidence/V35-P24/source-sha256.csv` 为当前源码树（该测试每次运行重写，语义为"冻结物跟当前语义树"）。
- `V35P241FreezeRevisionTest` 重建一次后因 ROADMAP.md 后续更新再次漂移；ROADMAP 定稿后**最终重建通过**（2026-08-17）。后续任何 AGENTS/ROADMAP/v35/zhangbo 源码变更都必须同步重建这两处冻结物。

## FC 新增测试清单（全部通过）

| 测试 | 项数 | 关键钉子 |
|---|---:|---|
| V35Fc0PrefinalArchiveTest | 1 | 存档幂等 + 20000FE×3 重放 front 逐位一致 |
| V35Fc1Fm3CriticalStructureTest | 5 | null 回退逐字节；FM3 关键作业/疲劳路由；包一致性；只读 |
| V35Fc2LocalFePacingTest | 5 | β/B_L 数值与校验；**PREFINAL 哈希稳定**；20000FE 集成对照（legacy>0.6 vs pacing∈[0.2,0.7]、外层≥2） |
| V35Fc3CheapTestTest | 4 | standard 永不 probe/抑制；tie→单次 probe；cap 抑制 Re-test |
| V35Fc4SoftFreezeTest | 3 | ρ=0 canonical 字节一致；缩放 TD 数学；20000FE ρ=0.3 冻结侧 Qg TD 严格增多 |
| V35Fc5CfvfGirAuditTest | 2 | cross→GIR 展开/JS 限制；20000FE 重放 front 逐位一致 + observations==cfvfOffspringCount |
| V35MacroCandidateGatewayTest（既有） | 8 | route/行为回归 |
| V35CaTaLiteControllerTest（既有） | 3 | Controller 回归 |

## 行为影响重申

默认（A4-PREFINAL 配置语义）下配置哈希不变；唯一默认路径行为变化是 FC-1——正式主路径 N3/N4 候选生成改读 FM3 真实关键结构（这正是 FC-1 的目的；evaluation 缺失时逐字节回退 PT0 proxy，shadow 审计保持 proxy）。FC-2/3/4 的开关默认关闭（null/0.0/standard），对应行为与存档逐字一致。
