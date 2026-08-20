# 08-STAGE5-C2-DIAG 索引

FC-6A-POST / Build-C2（BP-PDDR 稳定性诊断）服务器 12 跑的完整数据归档。
报告：`../00-REPORTS/FC6A_BUILD_C_STABILITY_DIAGNOSTIC_REPORT.md`。

## 目录

- `raw-100job/`、`raw-20job/`：12 组 mechanism-summary.txt 的 gzip（服务器 `stage5-c2/<instance>/<arm>/seed-*/`；含完整 fc6Diag 段 + fc52 段 + cfvf GIR 段）
- `parsed-100job/`、`parsed-20job/`：解压后的 txt（每份 ~18MB）
- `parsed-*/tables/`：解析产物 CSV（`parse_fc6diag.py` 生成）
  - `<run>.cycles.csv`：62 轮 × {popSize/popND/archSize/三目标 min-max-range(pop,arch)/rescues(cmax,tec,twc)/displaced/teacherSel/cfvfGbestLearn}
  - `<run>.rescues.csv`：每条 rescue 事件全字段（role/Cmax/TEC/TWC/q/p/score/origRank/slot/lineage + displaced 同构字段；dFp/d* 为 NaN = 该轮无 displaced 配对）
  - `<run>.exposures.csv`：每个被救 fingerprint 的 Qg 教师/CFVF gbest/pbest 曝光（G1-G4 分组）+ lineage 后代/终局判定
- `fronts/`：12 组 front.csv + console.log（sha256 已全部命中历史基线/BP，见报告 §3）
- `parse_fc6diag.py`：fc6Diag 段解析器（tab 分割 key=value）

## 命名

`<NN>_<ARM>_seed-<SEED>`，NN=100/20（实例 100_2_3_1 / 20_2_3_1），ARM=BASE（原始 selector）/ BP（BP-PDDR），SEED=20260822/23/24。

## 复现

服务器 `/home/inspur/aicomp/zhangbo-fc6-20260818/`：`jars/jmetal-exec-5.8-BUILD-C2-{BP,BASE}-diag.jar`、`fc6-stage5-c2.sh`、`results/stage5-c2/`、`logs/stage5-*`。
