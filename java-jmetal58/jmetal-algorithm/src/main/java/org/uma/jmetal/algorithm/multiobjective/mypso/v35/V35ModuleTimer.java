package org.uma.jmetal.algorithm.multiobjective.mypso.v35;

import java.util.Map;
import java.util.TreeMap;

/**
 * FC-TIME-1 纯旁路模块耗时聚合器（运行时间收口计划）。
 *
 * <p>设计约束：只观察、不进入任何决策路径——不改变随机数序列、不改变任何分数、
 * 不改变集合顺序。与 V35CfvfGirAudit 同源的"观察旁路"原则。</p>
 *
 * <p>用法：runner 在 algorithm.run() 前 {@link #setEnabled(true)} + {@link #reset()}，
 * 结束后用 {@link #snapshot()} / {@link #summaryText()} 写入 mechanism-summary；
 * 算法代码插入点形如 {@code long t0 = System.nanoTime(); ...; V35ModuleTimer.record(MOD, System.nanoTime()-t0, 1);}。
 * FC-TIME-1B 另支持纯计数器 {@link #increment(String, long)}（calls/itemsVisited/
 * dominanceComparisons 等），同样不进决策路径。</p>
 *
 * <p>模块与用户清单的对应：DECODE=FM3 full decode；CFVF=updatePosition 全路径
 * （含 prep/group/tail 三段细分）；CATA=CA-TA Test+Apply 整段；OTHER=updateVelocity
 * 组建/排序/select 混合段与循环基建。</p>
 */
public final class V35ModuleTimer {

  /** 模块清单（对应 V35_FC_TIME_PLAN.md FC-TIME-1）。 */
  public static final String DECODE = "FM3Decode";
  public static final String DERIVED = "FM3DerivedCritical";
  public static final String QP = "Qp";
  public static final String QG = "Qg";
  public static final String DSCR = "DSCR";
  public static final String CFVF = "CFVF";
  /** CFVF 更新路径三段细分（FC-TIME-2 第二版账）：预备（lineage/Qp 选择/深拷贝）、
   *  组更新（4×updateCfvfGroup，即真正的"跟老师学"）、尾部（merge/PDDRFFselect 全局支配排序）。
   *  另有 updatePosition 开头的前置：prepareDualQCoordination 与 prepareOriginalQg（Og 准备）。 */
  public static final String CFVF_PREP = "CFVF.Prep";
  public static final String CFVF_GROUP = "CFVF.GroupUpdate";
  public static final String CFVF_TAIL = "CFVF.TailPddr";
  public static final String CFVF_PREP_DUALQ = "CFVF.PrepDualQ";
  public static final String CFVF_PREP_OG = "CFVF.PrepOriginalQg";
  /** FC-TIME-1B：prepareOriginalQg 内部三段（archive 拷贝 / DSCR / leader 选择）。 */
  public static final String OG_ARCHIVE_COPY = "Og.ArchiveCopy";
  public static final String OG_DSCR = "Og.Dscr";
  public static final String OG_LEADER_SELECT = "Og.LeaderSelect";
  /** FC-TIME-1B：QgController leader 选择内 pddr 全档案评分（非边界组）。 */
  public static final String LEADER_PDDR = "Leader.PddrScore";
  /** FC-TIME-1B：CFVF updater 内部（粒子级学习动作）细分。 */
  public static final String CTX_VALIDATE = "Cfvf.ValidateCopy";
  public static final String CTX_JS_CHANNEL = "Cfvf.JsChannel";
  public static final String CTX_PBEST_DIFF = "Cfvf.PbestDiff";
  public static final String CTX_GBEST_DIFF = "Cfvf.GbestDiff";
  public static final String CTX_RESOURCE_MERGE = "Cfvf.ResourceMerge";
  public static final String CTX_ACTION_APPLY = "Cfvf.ActionApply";
  public static final String CTX_REPAIR = "Cfvf.RepairLegality";
  public static final String CTX_TAIL = "Cfvf.TailHamming";
  public static final String LS = "InterFactoryLS";
  public static final String CATA = "CaTaLiteTestApply";
  public static final String PDDR = "PDDR_FF";
  public static final String ARCHIVE = "Archive";
  public static final String DOMINANCE = "Dominance";
  public static final String COPY = "SolutionCopyRepair";
  public static final String AUDIT = "AuditLogging";
  public static final String OTHER = "Other";

  /** 计数器名（FC-TIME-1B）。 */
  public static final String C_DSCR_CALLS = "dscrCalls";
  public static final String C_SOCIAL_CANDIDATE_BUILD = "socialCandidateBuildCalls";
  public static final String C_ARCHIVE_SCAN = "archiveScanCalls";
  public static final String C_ARCHIVE_ITEMS = "archiveItemsVisited";
  public static final String C_PDDR_CALLS = "pddrCalls";
  public static final String C_PDDR_ITEMS = "pddrItemsVisited";
  public static final String C_DOMINATES_CALLS = "dominatesCalls";
  public static final String C_PBEST_DIFF = "pbestDifferenceBuildCount";
  public static final String C_GBEST_DIFF = "gbestDifferenceBuildCount";
  public static final String C_JS_ACTION = "jsActionBuildCount";
  public static final String C_RESOURCE_ACTION = "resourceActionBuildCount";
  public static final String C_CONFLICT = "conflictResolutionCount";
  public static final String C_LEGALITY = "legalityCheckCount";
  public static final String C_PARTICLE_UPDATE = "cfvfParticleUpdateCount";
  public static final String C_SUBGROUP_UPDATE = "cfvfSubgroupUpdateCount";
  /** FC-TIME-2-A1：fingerprint hoist 前后计数。 */
  public static final String C_FP_BEFORE = "fingerprintCallsBeforeEquivalent";
  public static final String C_FP_ACTUAL = "fingerprintCallsActual";
  public static final String C_FP_REUSE = "fingerprintReuseCount";

  /** module -> {calls, totalNanos}。TreeMap 保证 summary 输出次序稳定。 */
  private static final Map<String, long[]> STATS = new TreeMap<>();
  /** counter -> {total}（FC-TIME-1B：纯计数，只增不减，不进决策路径）。 */
  private static final Map<String, long[]> COUNTERS = new TreeMap<>();
  private static volatile boolean ENABLED = false;

  private V35ModuleTimer() {
  }

  public static void setEnabled(boolean enabled) {
    ENABLED = enabled;
    if (!ENABLED) return;
    if (!enabled) {
      STATS.clear();
      COUNTERS.clear();
    }
  }

  public static boolean isEnabled() {
    return ENABLED;
  }

  public static void reset() {
    STATS.clear();
    COUNTERS.clear();
  }

  /** 累加一次模块耗时（calls 通常为 1；per-cycle 聚合可由调用方传增量）。 */
  public static void record(String module, long totalNanos, long calls) {
    if (!ENABLED) {
      return;
    }
    long[] s = STATS.computeIfAbsent(module, k -> new long[2]);
    s[0] += calls;
    s[1] += totalNanos;
  }

  /** 累加一个纯计数器（calls/itemsVisited/dominanceComparisons 等）。 */
  public static void increment(String counter, long amount) {
    if (!ENABLED) {
      return;
    }
    long[] c = COUNTERS.computeIfAbsent(counter, k -> new long[1]);
    c[0] += amount;
  }

  /** 当前累加快照的深拷贝：module -> {calls, totalNanos}。 */
  public static Map<String, long[]> snapshot() {
    Map<String, long[]> copy = new TreeMap<>();
    for (Map.Entry<String, long[]> e : STATS.entrySet()) {
      copy.put(e.getKey(), new long[] {e.getValue()[0], e.getValue()[1]});
    }
    return copy;
  }

  /** 当前计数器快照的深拷贝：counter -> {total}。 */
  public static Map<String, long[]> counterSnapshot() {
    Map<String, long[]> copy = new TreeMap<>();
    for (Map.Entry<String, long[]> e : COUNTERS.entrySet()) {
      copy.put(e.getKey(), new long[] {e.getValue()[0]});
    }
    return copy;
  }

  /**
   * 两快照之差：每个模块 from→to 的增量 {calls, totalNanos}。用于 per-cycle 记录。
   */
  public static Map<String, long[]> delta(Map<String, long[]> from, Map<String, long[]> to) {
    Map<String, long[]> d = new TreeMap<>();
    for (Map.Entry<String, long[]> e : to.entrySet()) {
      long[] f = from.get(e.getKey());
      long fc = f == null ? 0L : f[0];
      long fn = f == null ? 0L : f[1];
      d.put(e.getKey(), new long[] {e.getValue()[0] - fc, e.getValue()[1] - fn});
    }
    return d;
  }

  /** 两计数器快照之差：counter from→to 增量。 */
  public static Map<String, long[]> counterDelta(Map<String, long[]> from, Map<String, long[]> to) {
    Map<String, long[]> d = new TreeMap<>();
    for (Map.Entry<String, long[]> e : to.entrySet()) {
      long[] f = from.get(e.getKey());
      long before = f == null ? 0L : f[0];
      d.put(e.getKey(), new long[] {e.getValue()[0] - before});
    }
    return d;
  }

  /** 汇总文本：{@code module=calls:totalNanos:avgNanos:percentage}，含 total 行；随后输出计数器块。 */
  public static String summaryText() {
    StringBuilder sb = new StringBuilder();
    long totalNanos = 0L;
    for (long[] s : STATS.values()) {
      totalNanos += s[1];
    }
    for (Map.Entry<String, long[]> e : STATS.entrySet()) {
      long calls = e.getValue()[0];
      long nanos = e.getValue()[1];
      double avg = calls == 0L ? 0.0 : (double) nanos / calls;
      double pct = totalNanos == 0L ? 0.0 : 100.0 * nanos / totalNanos;
      sb.append("module.").append(e.getKey())
          .append("=").append(calls)
          .append(':').append(nanos)
          .append(':').append(String.format(java.util.Locale.ROOT, "%.1f", avg))
          .append('%').append(String.format(java.util.Locale.ROOT, "%.1f", pct)).append('\n');
    }
    sb.append("module.TOTAL=").append(totalNanos).append('\n');
    for (Map.Entry<String, long[]> e : COUNTERS.entrySet()) {
      sb.append("counter.").append(e.getKey()).append('=').append(e.getValue()[0]).append('\n');
    }
    return sb.toString();
  }
}