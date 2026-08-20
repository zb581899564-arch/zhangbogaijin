package org.uma.jmetal.problem.multiobjective.dfsp.fatigue;

/**
 * FC-5.1：全运行"已完整评估解"的最小 Cmax 静态观察器（纯旁路）。
 *
 * <p>回答"seed24 到底有没有真正 fully-evaluated 的 Cmax&lt;195.70 的解"：
 * 在 {@link ZhangBoCanonicalProductionProblem#evaluate} 的完整 FM3 评估落盘后调用
 * {@link #observe(double)}，记录整个运行中所有 fully-evaluated 解的最小 Cmax。
 * 与 archive 最小 Cmax 对比即可判定"好解是否存在但没有进 archive / 被记住"。</p>
 *
 * <p>纪律：静态累加、不进任何决策路径、不改变随机序与目标值。由 runner 在运行前后
 * {@link #setEnabled(boolean)} 控制，多运行间以 {@link #reset()} 隔离。</p>
 */
public final class V35CmaxBestEver {
  private static volatile boolean enabled = false;
  private static double bestCmax = Double.POSITIVE_INFINITY;
  private static double bestCmaxTec = Double.NaN;
  private static double bestCmaxTwc = Double.NaN;
  private static long bestCmaxAtEvaluation = -1L;
  private static long evaluatedCount = 0L;

  private V35CmaxBestEver() {
  }

  public static void setEnabled(boolean value) {
    enabled = value;
    if (!value) {
      bestCmax = Double.POSITIVE_INFINITY;
      evaluatedCount = 0L;
    }
  }

  public static boolean isEnabled() {
    return enabled;
  }

  public static void reset() {
    bestCmax = Double.POSITIVE_INFINITY;
    evaluatedCount = 0L;
  }

  /** 记录一次 fully-evaluated 解的 Cmax（FM3 完整解码后调用，纯旁路）。 */
  public static void observe(double cmax, double tec, double twc) {
    if (!enabled) {
      return;
    }
    evaluatedCount++;
    if (cmax < bestCmax) {
      bestCmax = cmax;
      bestCmaxTec = tec;
      bestCmaxTwc = twc;
      bestCmaxAtEvaluation = evaluatedCount;
    }
  }

  public static double bestCmax() {
    return bestCmax;
  }

  public static double bestCmaxTec() {
    return bestCmaxTec;
  }

  public static double bestCmaxTwc() {
    return bestCmaxTwc;
  }

  public static long bestCmaxAtEvaluation() {
    return bestCmaxAtEvaluation;
  }

  public static long evaluatedCount() {
    return evaluatedCount;
  }
}