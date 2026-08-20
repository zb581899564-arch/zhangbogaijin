# V35-P19 Cmax lifecycle audit

日期：2026-08-13

`V35FairRunner` 现在默认挂接只读 `ZhangBoCmaxAudit(1000 FE)`。审计器记录 Global/G1/Generated/Survived 曲线，以及 candidateId、parentId、lineageId、generation、FE、source、PDDR、个人/全局档案、下一轮存活和 G1 教师使用字段。审计不生成随机数、不评价候选、不参与算法决策。

定向 Runner 测试已验证 checkpoint 和 CSV 结构存在；6750 FE 纪录是否继续作为 G1 教师，必须用 I1/20k 真实生命周期母表确认。本工作包保持 `in_progress`，旧 Shift-on/P9 审计不并入当前语义。

20k FULL 已生成 `cmax-audit-curves.csv`、`cmax-audit-records.csv` 和 summary；PDDR 映射现在同时按评价序号和稳定四向量指纹回退，避免预评价复制导致记录丢失。当前审计仍只保留新的 Cmax record，不将所有未刷新 Cmax 的候选伪装成记录，因此完整候选级生命周期与 6750 FE 复核仍保持待办。
