# V35-P25E 论文算法忠实适配审计

## 公平边界

```text
shared_problem_only=true
search_mechanisms_independent=true
decoder=FM3
shiftMode=NONE
familyMode=DEGENERATE_SINGLE_FAMILY
setupMode=SEQUENCE_INDEPENDENT
objectiveAdapter=0,1,6
legacyP25DExcluded=true
QMOEA=PENDING_SOURCE_VERIFICATION
```

旧 `V35P25DComparativeEngine` 同时重写了六种算法的更新、环境选择和档案，不能作为论文对比依据。新P25E不调用该类。

## 算法身份

| 标签 | 核心来源 | P25E只允许的接线 |
|---|---|---|
| ZHANGBO-A4 | 当前冻结V35 A4 | 统一Runner |
| HMOPSO-QGS-F | 规范结构化基线 | 统一Runner |
| HMOPSO-QLS-F | 作者`MOPSODivSubDE`隔离副本 | Problem、Solution、初群、随机源、FE、日志 |
| MOPSO-F | 作者离散`MOPSO`隔离副本 | 同上 |
| MOPSODS-DE-F | 作者`MOPSODivSub`隔离副本 | 同上 |
| MOHEADE-F | 作者`mymohea.MOHEADE`隔离副本 | 同上 |
| NSGA-II-F | 官方jMetal 5.8 `NSGAII` | 包/类隔离、四向量算子、Problem |
| SPEA2-F | 官方jMetal 5.8 `SPEA2` | 包/类隔离、四向量算子、Problem |

六种比较算法源码静态拒绝引用：`V35P25DComparativeEngine`、`ZhangBoBaselineUpdater`、CFVF、Qp、DSCR、CA-TA-Lite和方向教师池。

## 官方jMetal来源

- repository: `https://github.com/jMetal/jMetal`
- tag line: `jmetal-5.8`
- commit: `831d62d0bbf384e1770efc1bb6eef69ce0ce75b9`
- license: MIT (`LICENSE.txt`)
- isolated files: `OfficialJMetal58NSGAII.java`, `OfficialJMetal58SPEA2.java`
- algorithm control flow: preserved；仅包名和类/构造器名称隔离。

## 当前门状态

八算法2000 FE贯通已通过。A4/QGS精确2000 FE；MOPSO、MOPSODS-DE、MOHEADE、NSGA-II、SPEA2精确2000 FE；HMOPSO-QLS因作者结构组不可拆，安全停在1950 FE。所有运行满足Decoder调用数等于FE、目标有限、前沿非空、Shift耗时为0。50k单seed在完整回归与构建通过后才启动。
