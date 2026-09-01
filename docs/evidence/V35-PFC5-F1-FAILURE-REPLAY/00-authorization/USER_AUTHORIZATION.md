# V35-PFC5-F1 用户授权记录

生成时间（UTC）：2026-08-29T12:1x:xxZ
证据目录：`docs/evidence/V35-PFC5-F1-FAILURE-REPLAY/`

---

## 1. 授权来源

用户于 2026-08-29 下达的 `V35-PFC5-F1：A4历史失败500k OFF Failure Replay` 执行指令，原文节录：

```text
你现在负责执行：

# V35-PFC5-F1：A4历史失败500k OFF Failure Replay

本任务只允许完成：

F1预检
→ 上传已冻结输入
→ 单条A4/500k/telemetry OFF运行
→ 运行验收
→ 按冻结reference contract计算终态HV/IGD
→ 给出F1裁决
→ 停止
```

该指令本身即为 F1 的运行授权。`docs/ROADMAP.md` §13.1 已于本次执行开始时同步修正为：

```text
当前唯一待授权动作：
PFC5-F1 A4历史失败500k OFF Failure Replay

用户已经批准启动F1
F2/F3仍未获批准
```

ROADMAP 修改前后 SHA-256：

```text
before=2818bdffb38e69826543eb6bcff6e48ed65ded52880a733ecb17817eed235139
after =b603333a50b1c39eb8ea91327450d017613589625c5d9be060e66584761370dc
```

修改范围仅限 §13.1（第 2186 行起）的过期待办文本；第 61 行修订日期、第 2174 行 `PFC5-F1` 状态表行、`A2Promoted/A4Promoted/FINAL_FROZEN/formalMatrix` 冻结边界均未触碰。

---

## 2. 授权范围（允许）

1. F1 预检（Phase 0 证据复核 + 21 项预检清单）
2. 上传已冻结输入到 `/home/inspur/aicomp/zhangbo-v35-pfc5-f1-20260829`
3. **单条** A4 / 100_5_3_1 / seed=20260901 / population=100 / 500000 FE / telemetry=OFF 运行
4. 运行验收（硬门）
5. 按冻结 reference contract 计算终态 HV/IGD
6. 给出 F1 裁决（四值之一）
7. 停止

---

## 3. 明确禁止（用户原文）

```text
禁止自动启动：

F2
F3
Teacher Exposure Calibration
Configuration Race
Gap Probe
Validation
正式矩阵
任何额外seed或实例
```

其它禁止项：

```text
禁止：mvn重新构建正式算法Jar
     修改ZhangBoMOHPSOQ / V35FairRunner / PDDR / Qp/Qg / CFVF / CA-TA / FM3
     重新生成snapshot
     补评价 / partial Q phase / 修改Q_Times / 修改population
     将F1新front加入PFref / 更新ideal或nadir / 重新构造reference
     看结果后修改失败阈值
     使用其它campaign的reference
     删除远端原始数据
```

---

## 4. 授权状态

```ini
f1Authorized=true
f1Started=false
f2Authorized=false
f3Authorized=false
f2Eligible=UNDETERMINED
f2Preregistered=false
f2Started=false
f3Started=false
```

`f1Started` 将在运行时（S5）翻转为 `true`，翻转记录见 `03-raw-run/run-env.properties` 与 `03-raw-run/launch-record.md`。

---

## 5. 训练机上不得触碰的他人资产

远端 `aic-inspur-home` 上存在 4 个他人 tmux 会话，本次执行全程不得触碰、不得 kill、不得抢占：

```text
fc6-stage1: 1 windows (created Wed Aug 19 01:23:52 2026)
fc6-stage2: 1 windows (created Wed Aug 19 11:39:56 2026)
fc6-stage3: 1 windows (created Wed Aug 19 12:24:31 2026)
fc6-stage4: 1 windows (created Wed Aug 19 14:21:38 2026)
```

因此本任务**不使用 tmux**，改用 `setsid + nohup + taskset` 启动独立 JVM，仅管理本任务自身的 PID/PGID/session。
