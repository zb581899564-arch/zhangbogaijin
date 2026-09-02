# K=1 逐项还原证明（数学与程序级归纳）

证明对象：`CANDIDATE_A_TOPK_UNIFORM` 与 `CANDIDATE_B_TOPK_DETERMINISTIC` 在 $K=1$ 配置下对当前 A4（冻结 Jar `8DAD8F40…BAD8B9`）的逐位等价性。
证明性质：**数学证明 + 程序状态归纳法**（0 FE）。

---

## 1. 形式化数学证明

### 1.1 符号定义

设当前 A4 的候选选择函数为 $f_{\text{A4}}: (E, fp_{req}, g, cur, gb, B, a) \mapsto e^* \in E$。
设候选 A 在参数 $K$ 下的候选选择函数为 $f_{\text{A}}^{(K)}$。

在候选 A 的规范中，对于任意合法动作 $a \in \{\text{KEEP}, \text{DIRECTIONAL}, \text{EPSILON}, \text{COMPLEMENTARY}\}$：
- 其候选池 $Pool(a, K)$ 定义为：在动作 $a$ 的既有比较器 $<_a$ 诱导的全序下，候选集 $S_a \subseteq E$ 的前 $\min(K, |S_a|)$ 个元素构成的列表。
- 比较器 $<_a$ 包含末级指纹字典序破平，故全序 $<_a$ 是严格弱序，排序结果唯一。
- 候选选择规则：
  $$f_{\text{A}}^{(K)}(a) = \begin{cases} Pool(a, K)[0], & \text{if } |Pool(a,K)| = 1 \\ Pool(a, K)[U(0, |Pool(a,K)|-1)], & \text{if } |Pool(a,K)| \ge 2 \end{cases}$$
  其中 $U(0, m)$ 为从区间 $[0, m]$ 均匀抽取的伪随机整数。

### 1.2 定理 1（K=1 候选身份等价定理）

**定理**：对任意合法的输入元组 $(E, fp_{req}, g, cur, gb, B, a)$，当 $K=1$ 时，恒有：
$$f_{\text{A}}^{(1)}(a) = f_{\text{A4}}(a)$$

**证明**：
1. **KEEP 动作**：当前 A4 定义 $f_{\text{A4}}(\text{KEEP}) = find(E, fp_{req}) ?? argmin_\phi(E)$。候选 A 中 $Pool(\text{KEEP}, 1) = [f_{\text{A4}}(\text{KEEP})]$，大小恒为 1。由选择规则，直接返回 $Pool[0] = f_{\text{A4}}(\text{KEEP})$。等式成立。
2. **DIRECTIONAL 动作**：当前 A4 定义 $f_{\text{A4}}(\text{DIRECTIONAL}) = \min_{<_D}(E)$。候选 A 中 $Pool(\text{DIRECTIONAL}, 1)$ 为按 $<_D$ 排序后的首个元素构成的单元素列表，即 $[ \min_{<_D}(E) ]$。其大小为 1，直接返回 $Pool[0] = \min_{<_D}(E) = f_{\text{A4}}(\text{DIRECTIONAL})$。等式成立。
3. **EPSILON 动作**：当前 A4 定义 $f_{\text{A4}}(\text{EPSILON}) = \min_{<_\epsilon}(E)$。同理，$Pool(\text{EPSILON}, 1) = [ \min_{<_\epsilon}(E) ]$，大小为 1，返回 $Pool[0] = f_{\text{A4}}(\text{EPSILON})$。等式成立。
4. **COMPLEMENTARY 动作**：当前 A4 定义 $f_{\text{A4}}(\text{COMPLEMENTARY}) = \min_{<_C}(Q')$（若合法）。同理，$Pool(\text{COMPLEMENTARY}, 1) = [ \min_{<_C}(Q') ]$，大小为 1，返回 $Pool[0] = f_{\text{A4}}(\text{COMPLEMENTARY})$。等式成立。

综上，对所有四种动作，所选条目的对象引用与指纹在 $K=1$ 下恒等于 A4 规范候选。$\blacksquare$

---

## 2. 程序状态转移归纳法证明（端到端执行流一致性）

我们对算法主循环的评价步数 $t$ 作数学归纳法。

**归纳假设 $H(t)$**：在第 $t$ 次完整评价结束时，系统满足：
1. 种群中全部 100 个粒子的四向量染色体与三主目标值完全相同；
2. 全局伪随机数生成器（`PseudoRandomGenerator`）的内部调用次数与状态完全相同；
3. 全部 100 个粒子的谱系档案（`ZhangBoLineageMemory`）内容完全相同；
4. 双 Q 控制器内部的 4 张 Q 表、冻结表与转移账本完全相同；
5. 社会领导池（`DSCR` / `SocialKnowledgeSnapshot`）状态完全相同；
6. 累计评价计数 $FE(t) = t$ 完全相同。

### 2.1 基础步骤（$t = 0$）
初始种群由固定 seed 与共享初始种群生成器（`ZhangBoSharedInitialPopulation`）构造，配置哈希一致，$H(0)$ 显然成立。

### 2.2 归纳步骤（假设 $H(t)$ 成立，推导 $t \to t+1$）

考虑第 $t$ 步所在的生成阶段：

1. **若当前处于 WARMUP 阶段**：
   - 领导选择由 `selectDirectionalWarmupGroup` 执行，不经过 Q 表，不查询候选池，直接取 $argmin_\phi$。
   - $K$ 语义未介入，RNG 消费为 0。$H(t+1)$ 成立。

2. **若当前处于正式 Q 循环的领导选择步（`selectGroup`）**：
   - **动作选择**：
     - 在 `EPSILON_GREEDY` 模式下，消耗 1 次 `nextDouble()`。
     - 若探索，消耗 1 次 `nextInt(0, |valid|-1)`。
     - 由于 $K=1$ 的 mask 完全由规范候选生成，因此 `validActions` 列表完全相同，RNG 抽取的边界与结果完全相同，选出的动作 $a$ 完全相同。
     - 在 `GREEDY_FROZEN` 模式下，直接由冻结 Q 表与 mask 确定性得出，动作 $a$ 完全相同。
   - **候选选择步（关键插入点）**：
     - 由定理 1，当 $K=1$ 时，$Pool(a, 1)$ 大小恒为 1。
     - 候选 A 的代码显式断言：当 $|Pool| = 1$ 时，**不调用 `random.nextInt`，直接返回 `pool.get(0)`**。
     - 因而此步骤**消耗 0 次 RNG**。
     - 所选取的个人领导对象 $e^*$ 与 A4 完全相同（指纹相同、解向量相同、目标值相同）。
   - **后续派生状态**：
     - `selectedDirectionalScore = \phi(e^*)` 完全相同；
     - `eligibleBestDirectionalScore` 完全相同；
     - `pbestSwitches` 累加逻辑完全相同；
     - `ZhangBoQpLineageState` 被赋予相同指纹 $e^*.fp$。

3. **CFVF 全向量更新与疲劳解码步**：
   - CFVF 输入的 `personalLeader` 与 `gbest` 完全相同；
   - 随机数流状态由于候选步 0 消耗而完全未发生偏移；
   - CFVF 产生的后代四向量染色体逐位完全相同；
   - 统一 FM3 疲劳解码器输出的目标值逐位完全相同。

4. **结算步（`settle`）**：
   - 档案更新输入相同，输出的新档案完全相同；
   - 结转查询 `find(nextArchive, e^*.fp)` 结果相同；
   - 奖励计算只依赖父代、子代与档案存活，完全相同；
   - Q 表批量更新与 TD 转移记录完全相同。

5. **PDDR 与 CA-TA-Lite 局部搜索步**：
   - 输入种群与候选完全相同；
   - 全局原版 PDDR 比较与排序结果完全相同；
   - CA-TA-Lite 确定性时钟与宏邻域路由完全相同。

综上，在 $t+1$ 步全部 6 项状态依然完全相同。由数学归纳法，$H(t)$ 对全生命周期恒成立。$\blacksquare$

---

## 3. 候选 B 的 K=1 还原证明简要说明

对于候选 B，当 $K=1$ 时，$|Pool(a, 1)| = 1$。
模运算 $idx = count \bmod 1 = 0$ 恒成立。
返回 $Pool[0] = f_{\text{A4}}(a)$。
候选 B 全程不传入 RNG，额外 RNG 消耗恒为 0。
归纳步骤完全同上成立。$\blacksquare$

---

## 4. 严格逐项还原核对表（9项硬性一致性断言）

| # | 检查项 | 源码对应位置 | A (K=1) 状态 | B (K=1) 状态 | 证明结论 |
|---|---|---|---|---|---|
| 1 | 动作合法性掩码（mask） | `Selector.java:48-56` | 逐位一致 | 逐位一致 | 由规范候选决定，K=1下无任何差异 |
| 2 | 动作选择 RNG 消费流 | `Controller.java:473,477` | 逐位一致 | 逐位一致 | mask 一致保证 valid 集合一致，抽取序列严格同构 |
| 3 | 候选选择步额外 RNG 消费 | `Controller.java:160` | **恒为 0** | **恒为 0** | 单例池走 size==1 分支，跳过 RNG 调用 |
| 4 | 选定个人领导解向量与指纹 | `Controller.java:160,178` | 逐位一致 | 逐位一致 | 定理 1 保证选出 entry 与 A4 规范 candidate 恒等 |
| 5 | CFVF 输入与子代四向量 | `MOHPSOQ.java:3454-3464` | 逐位一致 | 逐位一致 | 领导解与随机流双一致，保证子代染色体一致 |
| 6 | 个人档案更新与截断 | `PersonalArchive.java:45-69`| 逐位一致 | 逐位一致 | 相同子代进入更新，输出档案完全相同 |
| 7 | Qp 奖励与 TD 转移 | `Controller.java:371-396` | 逐位一致 | 逐位一致 | 奖励不读 leader，Q 表数值与梯度完全一致 |
| 8 | Phase-Consistent 评价计数 | `MOHPSOQ.java` 外循环 | 逐位一致 | 逐位一致 | 评价步长与终止条件完全不变 |
| 9 | 最终非支配前沿 front.csv | 输出阶段 | SHA-256 相同 | SHA-256 相同 | 全生命周期状态等价推导终态 front 逐位相同 |
