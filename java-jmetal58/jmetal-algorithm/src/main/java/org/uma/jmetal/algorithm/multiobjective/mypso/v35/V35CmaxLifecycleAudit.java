package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * FC-5：Cmax 生命周期四层被动审计（纯观察旁路，行为不变）。
 *
 * <p>回答"为什么整体 Pareto 强（HV 0.9435 vs QGS 0.8511）但 Cmax 极端方向不稳定
 * （seed23 175.7 vs seed24 195.7）"：</p>
 * <ol>
 *   <li>Generation：每 outer cycle 分段记录 bestCmax（Q 轮/CFVF 后、CA-TA 后、LS 后、最终）——
 *       回答"有没有生成过 180/182/184 级别的好 Cmax"。</li>
 *   <li>Admission：CA-TA/LS 候选产生的好解是否被接受进最终 swarm（bestCmax_generated vs
 *       bestCmax_final 的差距与"好解生成计数 vs 存活计数"）。</li>
 *   <li>Survival：merge pool 与 PDDR 环境选择后的 bestCmax——极端 Cmax 解是否被 PDDR 杀掉
 *       （bestCmax_pool vs bestCmax_nextPopulation），以及 archive 最值轨迹。</li>
 *   <li>Exploitation：G1 条件 GIR——Cmax 改善的 offspring 里四向量各被改动的条件概率 vs
 *       全体 offspring 变化率；Top-5 Cmax 解的 lineage 生命周期（出生算子 → 进 pool →
 *       存活 → 进 archive → 被选 teacher → CFVF 使用次数）。</li>
 * </ol>
 *
 * <p>与 V35ModuleTimer/V35CfvfGirAudit 同源纪律：只读观察，不进入任何决策路径，
 * 不改变随机序、FE、候选数。启用方式由 runner 参数控制。</p>
 */
public final class V35CmaxLifecycleAudit {

  /** 每 outer cycle 的四层漏斗行。 */
  public static final class CycleRow {
    public final int cycle;
    public double bestCmaxAfterQRounds = Double.POSITIVE_INFINITY;
    public double bestCmaxAfterCaTa = Double.POSITIVE_INFINITY;
    public double bestCmaxAfterLs = Double.POSITIVE_INFINITY;
    public double bestCmaxFinal = Double.POSITIVE_INFINITY;
    public double bestCmaxPool = Double.POSITIVE_INFINITY;
    public double bestCmaxNextPopulation = Double.POSITIVE_INFINITY;
    public double bestCmaxArchiveBefore = Double.POSITIVE_INFINITY;
    public double bestCmaxArchiveAfter = Double.POSITIVE_INFINITY;

    public CycleRow(int cycle) {
      this.cycle = cycle;
    }
  }

  /** Top-5 Cmax 解的 lineage 生命周期记录。 */
  public static final class LineageRecord {
    public final long lineageId;
    public int birthCycle;
    public String birthOperator = "?";
    public double birthCmax = Double.POSITIVE_INFINITY;
    public boolean enteredPool;
    public boolean survived;
    public boolean enteredArchive;
    public boolean selectedAsTeacher;
    public int cfvfUsedCount;
    public int lastSeenCycle = -1;
    public double lastCmax = Double.POSITIVE_INFINITY;

    public LineageRecord(long lineageId) {
      this.lineageId = lineageId;
    }
  }

  private final List<CycleRow> cycles = new ArrayList<>();
  private final Map<Long, LineageRecord> lineages = new LinkedHashMap<>();
  private CycleRow current;

  // G1 条件 GIR 计数。
  private long g1Total;
  private long g1TotalJs, g1TotalFa, g1TotalMa, g1TotalWa;
  private long g1Success;
  private long g1SuccessJs, g1SuccessFa, g1SuccessMa, g1SuccessWa;

  // 全局 Top-5 Cmax 观察：每 cycle 末记录的 (cycle, lineageId, cmax, operator)。
  private final List<String> top5Log = new ArrayList<>();

  public void beginCycle(int cycle, double archiveBestBefore) {
    current = new CycleRow(cycle);
    current.bestCmaxArchiveBefore = archiveBestBefore;
    cycles.add(current);
  }

  public CycleRow endCycle(double archiveBestAfter) {
    if (current == null) {
      return null;
    }
    current.bestCmaxArchiveAfter = archiveBestAfter;
    CycleRow row = current;
    current = null;
    return row;
  }

  public void observeGeneration(int cycle, double afterQRounds, double afterCaTa,
      double afterLs, double afterFinal) {
    if (cycle < cycles.size()) {
      CycleRow row = cycles.get(cycle);
      row.bestCmaxAfterQRounds = minKeep(row.bestCmaxAfterQRounds, afterQRounds);
      row.bestCmaxAfterCaTa = minKeep(row.bestCmaxAfterCaTa, afterCaTa);
      row.bestCmaxAfterLs = minKeep(row.bestCmaxAfterLs, afterLs);
      row.bestCmaxFinal = minKeep(row.bestCmaxFinal, afterFinal);
    }
  }

  public void observeSurvival(int cycle, double bestPool, double bestNextPopulation) {
    if (cycle < cycles.size()) {
      CycleRow row = cycles.get(cycle);
      row.bestCmaxPool = minKeep(row.bestCmaxPool, bestPool);
      row.bestCmaxNextPopulation = minKeep(row.bestCmaxNextPopulation, bestNextPopulation);
    }
  }

  /** G1 条件 GIR：offspring 是否 Cmax 改善 + 四向量各自是否改变。 */
  public void observeG1CmaxCondition(boolean cmaxImproved, boolean jsChanged,
      boolean faChanged, boolean maChanged, boolean waChanged) {
    g1Total++;
    if (jsChanged) g1TotalJs++;
    if (faChanged) g1TotalFa++;
    if (maChanged) g1TotalMa++;
    if (waChanged) g1TotalWa++;
    if (cmaxImproved) {
      g1Success++;
      if (jsChanged) g1SuccessJs++;
      if (faChanged) g1SuccessFa++;
      if (maChanged) g1SuccessMa++;
      if (waChanged) g1SuccessWa++;
    }
  }

  // G1 条件 GIR 的跨点配对：CFVF 改动在 evaluateSwarm 前记录（旧 Cmax + 四向量变化标志），
  // 评估后按 slot 判定 Cmax 是否改善。纯观察，不改任何东西。
  private static final class PendingG1Update {
    final double oldCmax;
    final boolean js, fa, ma, wa;
    PendingG1Update(double oldCmax, boolean js, boolean fa, boolean ma, boolean wa) {
      this.oldCmax = oldCmax; this.js = js; this.fa = fa; this.ma = ma; this.wa = wa;
    }
  }
  private final Map<Integer, PendingG1Update> g1Pending = new TreeMap<>();

  /** 每 Q round 的 CFVF 更新开始前调用。 */
  public void beginG1Round() {
    g1Pending.clear();
  }

  /** CFVF 更新 G1 粒子时记录（slot = G1 组内索引；oldCmax = 更新前目标值；flags = 四向量是否变化）。 */
  public void observeG1Update(int slot, double oldCmax, boolean jsChanged,
      boolean faChanged, boolean maChanged, boolean waChanged) {
    g1Pending.put(slot, new PendingG1Update(oldCmax, jsChanged, faChanged, maChanged, waChanged));
  }

  /** Q round 的 evaluateSwarm 之后调用：newCmaxBySwarmSlot 为全 swarm（100 槽）的 objective(0)。 */
  public void resolveG1Improvements(double[] newCmaxBySwarmSlot) {
    for (Map.Entry<Integer, PendingG1Update> entry : g1Pending.entrySet()) {
      int slot = entry.getKey();
      PendingG1Update update = entry.getValue();
      double newCmax = newCmaxBySwarmSlot[slot];
      observeG1CmaxCondition(newCmax < update.oldCmax,
          update.js, update.fa, update.ma, update.wa);
    }
    g1Pending.clear();
  }

  /** 每 cycle 末 Top-5 Cmax 标签（lineageId + 出生算子 + Cmax）。 */
  public void observeTop5Cmax(int cycle, List<double[]> top5) {
    StringBuilder line = new StringBuilder("cycle=" + cycle);
    for (double[] entry : top5) {
      line.append(String.format(Locale.ROOT, ",id=%.0f:c=%.2f:op=%s",
          entry[0], entry[1], operatorText((long) entry[0])));
    }
    top5Log.add(line.toString());
  }

  public void registerLineage(long lineageId, int cycle, String operator, double cmax) {
    LineageRecord record = lineages.computeIfAbsent(lineageId, LineageRecord::new);
    if (record.birthCycle == 0) {
      record.birthCycle = cycle;
      record.birthOperator = operator;
      record.birthCmax = cmax;
    }
    record.lastSeenCycle = cycle;
    record.lastCmax = cmax;
  }

  public void markPool(long lineageId) {
    LineageRecord record = lineages.get(lineageId);
    if (record != null) record.enteredPool = true;
  }

  public void markSurvived(long lineageId) {
    LineageRecord record = lineages.get(lineageId);
    if (record != null) record.survived = true;
  }

  public void markArchive(long lineageId) {
    LineageRecord record = lineages.get(lineageId);
    if (record != null) record.enteredArchive = true;
  }

  public void markTeacher(long lineageId) {
    LineageRecord record = lineages.get(lineageId);
    if (record != null) record.selectedAsTeacher = true;
  }

  // FC-5.1：archive-best Cmax 解的 teacher-exposure 计数（问题 2）。
  private long archiveBestLineageId = -1L;
  private double archiveBestCmax = Double.POSITIVE_INFINITY;
  private long archiveBestLearnedTotal;
  private long archiveBestLearnedByG1;
  private long archiveBestAsQgTeacher;
  private final List<String> archiveBestHistory = new ArrayList<>();

  /** 每 cycle 末：当前外部 archive 中 Cmax 最小解的 lineage（纯观察）。 */
  public void observeArchiveBest(int cycle, double bestCmax, long lineageId) {
    if (lineageId != archiveBestLineageId || bestCmax < archiveBestCmax) {
      archiveBestHistory.add(String.format(Locale.ROOT, "cycle=%d:cmax=%.2f:lineage=%d",
          cycle, bestCmax, lineageId));
    }
    archiveBestLineageId = lineageId;
    archiveBestCmax = bestCmax;
  }

  /** CFVF 学习调用：teacher（leader/personalLeader）是否为当前 archive-best 解。 */
  public void observeLearning(long teacherLineageId, boolean isG1Group) {
    if (teacherLineageId == archiveBestLineageId) {
      archiveBestLearnedTotal++;
      if (isG1Group) {
        archiveBestLearnedByG1++;
      }
    }
  }

  /** Qg leader 选择：被选 leader 是否为当前 archive-best 解。 */
  public void observeQgTeacher(long leaderLineageId) {
    if (leaderLineageId == archiveBestLineageId) {
      archiveBestAsQgTeacher++;
    }
  }

  /** FC-5.1 汇总块。 */
  public String fc51SummaryText() {
    StringBuilder sb = new StringBuilder();
    sb.append("fc51Begin\n");
    sb.append("bestCmaxEvaluatedOverall=")
        .append(org.uma.jmetal.problem.multiobjective.dfsp.fatigue.V35CmaxBestEver.bestCmax())
        .append('\n');
    sb.append("bestCmaxEvaluatedTec=")
        .append(org.uma.jmetal.problem.multiobjective.dfsp.fatigue.V35CmaxBestEver.bestCmaxTec())
        .append('\n');
    sb.append("bestCmaxEvaluatedTwc=")
        .append(org.uma.jmetal.problem.multiobjective.dfsp.fatigue.V35CmaxBestEver.bestCmaxTwc())
        .append('\n');
    sb.append("bestCmaxEvaluatedAt=")
        .append(org.uma.jmetal.problem.multiobjective.dfsp.fatigue.V35CmaxBestEver.bestCmaxAtEvaluation())
        .append('\n');
    sb.append("evaluatedCount=")
        .append(org.uma.jmetal.problem.multiobjective.dfsp.fatigue.V35CmaxBestEver.evaluatedCount())
        .append('\n');
    sb.append("bestCmaxArchive=").append(archiveBestCmax).append('\n');
    sb.append("archiveBestLineageId=").append(archiveBestLineageId).append('\n');
    sb.append("archiveBestLearnedTotal=").append(archiveBestLearnedTotal).append('\n');
    sb.append("archiveBestLearnedByG1=").append(archiveBestLearnedByG1).append('\n');
    sb.append("archiveBestAsQgTeacher=").append(archiveBestAsQgTeacher).append('\n');
    sb.append("archiveBestHistory\n");
    for (String line : archiveBestHistory) {
      sb.append(line).append('\n');
    }
    sb.append("fc51End\n");
    return sb.toString();
  }

  public void markCfvfUse(long lineageId) {
    LineageRecord record = lineages.get(lineageId);
    if (record != null) record.cfvfUsedCount++;
  }

  /** 汇总文本：四层漏斗表 + G1 条件概率 + Top-5 lineage 统计。 */
  public String summaryText() {
    StringBuilder sb = new StringBuilder();
    sb.append("cmaxLifecycleAuditBegin\n");
    // A. 漏斗表
    sb.append("--funnel per cycle (bestCmax, lower is better; inf = no finite solution)--\n");
    sb.append(String.format(Locale.ROOT,
        "%-6s %-12s %-12s %-12s %-12s %-12s %-12s %-12s %-12s%n",
        "cycle", "genQrounds", "genCaTa", "genLs", "genFinal", "pool", "nextPop",
        "archBefore", "archAfter"));
    for (CycleRow row : cycles) {
      sb.append(String.format(Locale.ROOT,
          "%-6d %-12.2f %-12.2f %-12.2f %-12.2f %-12.2f %-12.2f %-12.2f %-12.2f%n",
          row.cycle, row.bestCmaxAfterQRounds, row.bestCmaxAfterCaTa, row.bestCmaxAfterLs,
          row.bestCmaxFinal, row.bestCmaxPool, row.bestCmaxNextPopulation,
          row.bestCmaxArchiveBefore, row.bestCmaxArchiveAfter));
    }
    // B. G1 条件 GIR
    sb.append("--G1 conditional GIR (Cmax-improved offspring vs all offspring)--\n");
    if (g1Total > 0) {
      sb.append(String.format(Locale.ROOT,
          "allOffspring: n=%d  P(JS)=%.3f P(FA)=%.3f P(MA)=%.3f P(WA)=%.3f%n",
          g1Total, p(g1TotalJs, g1Total), p(g1TotalFa, g1Total),
          p(g1TotalMa, g1Total), p(g1TotalWa, g1Total)));
      sb.append(String.format(Locale.ROOT,
          "cmaxImproved: n=%d  P(JS)=%.3f P(FA)=%.3f P(MA)=%.3f P(WA)=%.3f%n",
          g1Success, p(g1SuccessJs, g1Success), p(g1SuccessFa, g1Success),
          p(g1SuccessMa, g1Success), p(g1SuccessWa, g1Success)));
    } else {
      sb.append("G1 CFVF: no offspring observed\n");
    }
    // C. Top-5 lineage 生命周期
    sb.append("--top-5 cmax lineage lifecycle--\n");
    sb.append("lineageId,birthCycle,birthOperator,birthCmax,enteredPool,survived,"
        + "enteredArchive,selectedAsTeacher,cfvfUsedCount,lastSeenCycle,lastCmax\n");
    for (LineageRecord record : lineages.values()) {
      sb.append(String.format(Locale.ROOT,
          "%d,%d,%s,%.2f,%s,%s,%s,%s,%d,%d,%.2f%n",
          record.lineageId, record.birthCycle, record.birthOperator, record.birthCmax,
          record.enteredPool, record.survived, record.enteredArchive, record.selectedAsTeacher,
          record.cfvfUsedCount, record.lastSeenCycle, record.lastCmax));
    }
    if (top5Log.size() <= 8) {
      sb.append("--top5 per-cycle samples--\n");
      for (String line : top5Log) {
        sb.append(line).append('\n');
      }
    }
    sb.append("cmaxLifecycleAuditEnd\n");
    return sb.toString();
  }

  private static String operatorText(long lineageId) {
    return "l" + lineageId;
  }

  private static double minKeep(double current, double candidate) {
    return Math.min(current, candidate);
  }

  private static double p(long count, long total) {
    return total == 0 ? 0.0 : (double) count / total;
  }

  /** 供 runner 读取的原始行（测试用）。 */
  public List<CycleRow> cycleRows() {
    return cycles;
  }

  public Map<Long, LineageRecord> lineageRecords() {
    return lineages;
  }
}