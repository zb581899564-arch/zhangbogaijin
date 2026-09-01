# FC-6B 首次提交：容量一致性硬门捕获的实现缺陷

## 状态

首次区域感知提交未生成任何可用于比较的 FC-6B 运行结果。它在第一个
`REGION_AWARE` 运行进入第二次物理分区时被 fail-closed 中止，未进入指标汇总。

## 根因

PDDR 区域选择器已经返回登记的容量 `15/55/15/15`，但算法运行时仍保留历史
物理槽位字段 `20/40/20/20`。`partitionRegionAwareSwarm` 的一致性检查因此报告
容量不匹配。这个硬门捕获的是接入错误，不是算法性能结果。

## 修复

在 `configureGlobalSearch` 收到 `REGION_AWARE` 配置时，将运行时槽位容量绑定为：

```text
slot1 G1_CMAX     = 15
slot2 G4_BALANCED = 55
slot3 G2_TEC      = 15
slot4 G3_TWC      = 15
```

仅区域感知路径切换；`GLOBAL_ORIGINAL`、FC-6A 顺序实验和历史作者路径不改变。
`swarmSize != 100` 时仍拒绝构建，避免把这套固定容量误用于其他规模。

## 本地验证

- `ZhangBoV35Fc6RunnerTest`：5/5 通过；
- `REGION_AWARE` 20-job 运行进入 20k 测试预算并通过容量门；
- jMetal problem 及 FC-6 相关定向测试保持通过。

首次远端目录保留在训练机：

```text
/home/inspur/aicomp/zhangbo-fc6b-region-20260820
```

修复后的重跑使用独立目录，不覆盖上述失败证据。

随后的一次部署复核还发现训练机副本缺少 `100_2_3_1.txt`。该目录同样在进入
100-job 运行前停止，未混入正式参考集；完整重跑目录会同时保存 20-job 和
100-job 输入及其上传清单。
