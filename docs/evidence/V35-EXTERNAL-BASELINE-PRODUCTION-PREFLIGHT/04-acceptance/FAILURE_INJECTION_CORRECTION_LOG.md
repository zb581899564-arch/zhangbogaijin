# 故障注入修正日志（2026-08-30，复核后）

**发现**：首次执行的 process-interrupt 场景存在 harness 缺陷。被注入的运行
（SPEA2-F / 100_2_4_1 / kill@1.5s）实际**跑完并成功落盘**：`java` 经 Oracle
javapath 启动器解析，`Popen.kill()` 只终止了启动器进程，真实 JVM 以孤儿进程
继续运行至完成（artifacts: `.selftest/fi-interrupt/`，actualFE=20000、自带
evidence-sha256.tsv），而 launcher 仍按设计分支返回 FAIL_CLOSED_INTERRUPTED。
即：该场景首次记录的 PASS 不被产物支持。

**修正**：launcher kill 分支改为 `taskkill /F /T`（进程树终止）并补充产物级
断言；归档首attempt产物为 `.selftest/fi-interrupt-first-attempt-kill-ineffective/`
（原样保留作为缺陷证据）；以 attempt 2 真实重执行该场景：tree-kill @1.5s →
`final_exists=false`、partial `.partial-FI-interrupt-2/` 仅含
initial-population.sha256（无 manifest、无 status=COMPLETED）——不可被误认为
成功运行，FAIL_CLOSED_INTERRUPTED 成立。

**边界**：缺陷仅在注入 harness 的 kill 信号域；六条生产运行的启动/后验路径
未使用该分支，不受影响；搜索语义零改动；零科学实验重跑。
