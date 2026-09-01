# V35-PFC5-F1 远端部署报告

远端根目录：`/home/inspur/aicomp/zhangbo-v35-pfc5-f1-20260829`
结论：**REMOTE_DEPLOYMENT = PASS**，允许启动运行。

---

## 1. 传输方式与合规说明

按任务要求「训练机不得使用 Git checkout 重新生成实例、SUT 或 snapshot，必须原始字节上传并复核 SHA」，本次全部输入**均从本地已验证源文件以 `scp` 原始字节上传**，未使用远端任何既有目录的 `cp`，未使用 Git checkout，未在远端重新生成任何输入。

上传后对每一个文件在远端执行 `sha256sum` 独立复核，**7 项全部与本地实测值一致**（见 `remote-file-manifest.tsv`）。

| 角色 | 远端 SHA-256 | 字节 | 判定 |
|---|---|---|---|
| launcher Runner 字节码 | `3f35a72a…45c1` | 23753 | MATCH |
| launcher Budget 字节码 | `998187ad…ae7c` | 1486 | MATCH |
| 冻结算法 Jar | `8dad8f40…d8b9` | 48269638 | MATCH |
| 实例 `100_5_3_1` | `2e88fa97…35cf` | 2572 | MATCH |
| SUT setup | `4b49b780…1d90` | 5783 | MATCH |
| 疲劳参数 | `cf611bfb…f457` | 28317 | MATCH |
| 历史 snapshot | `84d84523…3769` | 92117 | MATCH |
| 运行计划 F1.properties | `7fe95dc8…d109` | 1781 | MATCH |

字节码主版本复核：远端 `od -An -tu1 -j6 -N2` 读取两个 launcher class 均为 **major=52**，与冻结 Jar 内 `V35FairRunner` / `V35FinalAblationProfile` / `ZhangBoV35FormalInitialPopulationFreezeRunner` 的 major=52 一致，远端 Java 11 可加载。（远端无 `javac`，因此 launcher 一律使用本地已编译好的 major 52 字节码，不在远端编译。）

---

## 2. 零 FE 的 profile 交叉验证（部署出口条件）

在远端用**冻结 Jar 本体**执行 `V35ProfileRegistryPrinter --seeds 20260901 --max-fes 500000`（该工具只做 `configurationFor` + 哈希，**不消耗任何 FE**），输出：

```text
arm,seed,population,maxFEs,profileSha256,runtimeConfigurationSha256
A0,20260901,100,500000,b17c8698ac84e37b195ef51ec2b9e30ed6327dde6b59ccc51c535c6b24442e89,ec71797bd15cd16114ce76b1e45478d6437550044de68e581e9756a2dbf69756
A1,20260901,100,500000,1687a120c0404a339df42506ab37f14c0b00bd9da221025b0b1c5053f4b8b424,833ef2bcb2ec92e78d911b09ea115d438d525954e8cc85849c33844045f10152
A2,20260901,100,500000,12db64fb7a545e7acbb927c673d1f2377a09a123467f186879b2d469c4a18380,03a7c067dea4f2ca89c8d38cc9db811296a4f87b8bfe136893a70ce61bc7f2b7
A3,20260901,100,500000,0850851cf0f049c82721368d18617eacf7b9b4ca5e23dd7b03914f6bb1735c3b,7398117d88eb9e690be9e3f07d4568cc0109201f5cb8187f8c77feef435fcb9b
A4,20260901,100,500000,5b3cc542dafc22c1a32f1c0994bae25ffef040f6bfdf2aa6090a42f86cfd79d1,8c68f2a5c3ada79cefe0f8900465e6f503cf8295d1810ceac6aaec5f484b44b3
```

A4 行与计划文件中写入的值逐字符一致：

```text
armProfileSha256          = 5b3cc542dafc22c1a32f1c0994bae25ffef040f6bfdf2aa6090a42f86cfd79d1
runtimeConfigurationSha256= 8c68f2a5c3ada79cefe0f8900465e6f503cf8295d1810ceac6aaec5f484b44b3
```

亦与历史 campaign 的 `input/profile-registry.csv` 中同一行一致。

**结论：C04 从 `PASS_PENDING_CROSSCHECK` 闭合为 `PASS`。** 这是在不消耗 FE 的前提下，对「运行机上算法配置身份」最强的实证。

---

## 3. 运行环境

完整键值见 `runtime-environment.properties`。要点：

| 项 | 值 |
|---|---|
| CPU | Intel Xeon Silver 4215R @3.20GHz，32 逻辑核，2 NUMA 节点 |
| 绑定核 | **22-23**（与历史 A4 500k 同一 cpuSet，同属 NUMA node0） |
| JVM | OpenJDK **11.0.27**（与历史运行同一默认 `java`） |
| JVM 参数 | `-Xmx4g`，无附加参数 |
| 可用内存 / 磁盘 | 117 Gi / 249 G |
| 并发 Java / 实验进程 | 0 / 0 |
| 22-23 核上负载 | 仅内核线程与 0.0% 空闲守护进程 |
| 他人资产 | `fc6-stage1..4` 四个 tmux 会话已登记，全程不触碰 |

**进程隔离策略**：不使用 tmux（远端存在 4 个他人会话，任何 kill 类操作都有误伤风险），改用 `setsid + nohup + taskset` 启动独立 JVM。仅通过本任务的 PID 文件管理自身进程，禁用 `pkill`/`killall`/`pgrep -f | xargs kill`。

---

## 4. 启动前最终状态检查

| 检查 | 结果 |
|---|---|
| `output/A4` 是否存在 | **不存在**（launcher 遇已存在目录会 `refusing overwrite`） |
| `.partial-*` 残留 | **0** |
| 同名运行的 runId 结果 | **0** |
| 同名 launcher 进程 | **0** |

只创建了 `output/` 父目录，`output/A4` 交由 launcher 自己创建。

---

## 5. 判定

```ini
REMOTE_DEPLOYMENT=PASS
inputSha256AllMatched=true
profileZeroFeCrosscheck=PASS
outputDirClean=true
noConcurrentWorkload=true
next=启动单条 A4/500k/telemetry OFF 运行
```
