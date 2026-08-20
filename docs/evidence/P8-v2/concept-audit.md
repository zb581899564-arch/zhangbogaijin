# P1–P8概念复核

语义标签：`engineering_audit`

## 结论

- P1–P4.1的来源隔离、论文验证线和作者直接派生线继续有效。
- P5/P5.1实现组件继续有效，但原作者解码与非零疲劳解码之间同时改变了SUT、第一阶段MA和疲劳机制，需要`B0C`控制点。
- P6.0–P6.4组件继续有效，但旧P8的B2/B3绑定分别混入Qg、评价后PDDR，需要`B1Q/B2P`控制点。
- P6.5统一子群语义继续有效。
- P7.1独立O1–O13继续有效。
- P7.2原有Test完整性、Apply预算和逐后代生产接入偏差已校正；旧证据保留为`legacy_pre_cata_correction`。
- P8旧结果隔离为`legacy_pre_ablation_switches`，不计入新版完成门；新版38个标签均已真实暴露。
- 作者直接派生类中保留的`group==1..4`属于`author_actual`物理槽位和未启用旧搜索方法；张博新增Qg/Qp、档案、CFVF、Need及VNS方向语义统一经`ZhangBoSubSwarmSemantics`访问。该保留是零创新兼容边界，不把裸组号扩散到新机制。
- 首轮校正矩阵曾发现`FM1`错误复用了完整双Q骨架；现已改为`B0C→FM1→FM2→FM3`只改变`decoderMode`，旧运行被最终228条矩阵覆盖。

## 当前全局状态

```text
ca_ta_engineering_validated=true
ca_ta_scheme_aligned=true
integration_engineering_validated=true
ablation_engineering_validated=true
sampled_reproduction_accepted=false
full_reproduction_accepted=false
```
