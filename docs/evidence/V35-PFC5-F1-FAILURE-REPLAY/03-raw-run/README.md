# 03-raw-run 说明

## 目录结构

```text
03-raw-run/
├── remote/     # 从训练机 /home/inspur/aicomp/zhangbo-v35-pfc5-f1-20260829/output/A4 下载的全部原始输出
├── logs/       # 启动与控制台记录
├── runtime-environment.properties   # 与 02-remote-deployment 中同一份运行环境记录
└── README.md   # 本文件
```

## stdout / stderr

启动命令为 `> "$LOG" 2>&1`，即 stdout 与 stderr 合并写入同一文件。本次运行的 **stderr 无任何输出**（`exitcode=0`，进程正常终止），因此：

- `logs/stdout.log` — 合并日志，内容仅一行 launcher 的成功收尾输出：
  `V35_FORMAL_A0_A4_ARM_COMPLETED arm=A4 FE=500000 output=.../output/A4`
- `logs/stderr.log` — 空文件（0 字节），如实记录 stderr 无输出这一事实，**未**填充任何代填内容。

`logs/launch-env.properties` 为启动瞬间采集的运行环境（host / CPU / affinity / JVM / heap / 内存 / 磁盘 / 并发进程 / wall-clock start 等）。
`logs/exitcode.txt` 为进程退出码（`0`）。

## 输出完整性

`remote/evidence-sha256.tsv` 由 launcher 在输出目录内自行生成，列出 21 个输出文件。下载并解压后本地反向复算：

```text
total=21 matched=21 missing=0 mismatch=0
```

## 远端原始数据

按任务要求，下载成功后**未删除**远端原始数据：

```text
/home/inspur/aicomp/zhangbo-v35-pfc5-f1-20260829/output/A4   22 个文件   85M
```
